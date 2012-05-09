package org.qrone.android.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;

public class Asyncer implements OnClickListener, OnLongClickListener{

	private Handler h;
	protected Asyncer root;
	protected Asyncer child;
	protected Task task;

	public Asyncer(){
		this.root = this;
		this.h = new Handler();
	}

	protected Asyncer(Asyncer root, Task run, Handler h){
		this.root = root;
		this.task = run;
		this.h = h;
	}

	public Asyncer add(final Asyncer addtask){
		final Asyncer achild = new Asyncer(root, null, h){
			@Override
			public int size() {
				if(this.child != null)
					return addtask.root.size() + this.child.size();
				else
					return addtask.root.size();
			}
		};
		achild.task = new Task() {
			@Override
			public void run(final Flag f) {
				addtask.worker(new Task() {
					@Override
					public void run(Flag flag) {
						achild.runChild(f);
					}
				}).go(f);
			}
		};
		child = achild;
		return achild;
	}
	
	public Asyncer worker(Task run){
		Asyncer a = new Worker(root, run, h);
		child = a;
		return a;
	}
	
	public Asyncer drawer(Task run){
		Asyncer a = new Drawer(root, run, h);
		child = a;
		return a;
	}

	public Asyncer loopworker(final Loop run){
		Asyncer a = new Looper(root, run, false, h);
		child = a;
		return a;
	}
	
	public Asyncer loopdrawer(final Loop run){
		Asyncer a = new Looper(root, run, true, h);
		child = a;
		return a;
	}
	
	public Asyncer brancher(final Task... runs){
		final Asyncer a = new Brancher(root, runs, false, h);
		child = a;
		return a;
	}
	
	public void run(final Flag r){
		r.proceed(1);
		if(task != null){
			task.run(r);
		}else{
			runChild(r);
		}
	}
	
	protected void runChild(Flag r){
		if(r.isStopped()) return;
		if(child !=null){
			child.run(r);
		}else{
			r.complete();
		}
	}
	
	public Flag go(){
		if(root != null && root != this){
			return root.go();
		}else{
			return new Flag(this).go();
		}
	}

	protected Flag go(Flag f){
		if(root != null && root != this){
			return root.go(f);
		}else{
			return new Flag(this,f).go();
		}
	}
	
	public Dialog progress(Context context, String msg){
		if(root != null && root != this){
			return root.progress(context,msg);
		}else{
			Dialog d = new Dialog(context, new Flag(this));
			d.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			d.setMessage(msg);
			d.setCancelable(false);
			return d;
		}
	}

	public Dialog progress(Context context, String msg, int theme){
		if(root != null && root != this){
			return root.progress(context,msg,theme);
		}else{
			Dialog d = new Dialog(context, theme, new Flag(this));
			d.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			d.setMessage(msg);
			d.setCancelable(false);
			return d;
		}
	}
	
	public class Flag{
		private int size = -1;
		private int progress = 0;
		private Asyncer root;
		private Map<String, Object> map = new HashMap<String, Object>();
		private boolean abort = false;
		private Flag parent;
		private List<Asyncer.Listener> listeners = new ArrayList<Asyncer.Listener>();
		
		public Flag(Asyncer root){
			this.root = root;
		}
		
		public Flag(Asyncer root, Flag f) {
			this.root = root;
			this.parent = f;
		}

		public Flag go(){
			root.run(this);
			return this;
		}
		
		public void complete(){
			for (Iterator<Asyncer.Listener> iter = listeners.iterator(); iter.hasNext();) {
				iter.next().completed();
			}
		}
		
		public void stop(){
			abort = true;
			for (Iterator<Asyncer.Listener> iter = listeners.iterator(); iter.hasNext();) {
				iter.next().stoped();
			}
		}

		public int size(){
			if(size < 0){
				size = root.size();
			}
			return size;
		}
		
		public void proceed(int n){
			if(parent != null)
				parent.proceed(n);
			
			progress += n;
			for (Iterator<Asyncer.Listener> iter = listeners.iterator(); iter.hasNext();) {
				iter.next().progress(progress);
			}
		}
		
		public void addAsyncerListener(Asyncer.Listener l){
			listeners.add(l);
		}

		public boolean isStopped(){
			return abort;
		}

		public void set(String key, Object value){
			map.put(key, value);
		}
		
		public Object get(String key){
			return map.get(key);
		}
		
		public Asyncer getRoot(){
			return root;
		}
	}
	
	public static class Dialog extends ProgressDialog implements Listener{
		private Flag f;
		private int size;
		public Dialog(Context context, Flag f) {
			super(context);
			this.f = f;
			init();
		}
		
		public Dialog(Context context, int theme, Flag f) {
			super(context,theme);
			this.f = f;
			init();
		}

		public Flag go(){
			show();
			return f.go();
		}
		
		public void init(){
			f.addAsyncerListener(this);
			setMax(f.size());
		}

		@Override
		public void completed() {
			dismiss();
		}
		
		@Override
		public void stoped() {
			dismiss();
		}

		@Override
		public void progress(int n) {
			setProgress(n);
		}
	}
	
	public static interface Task{
		public void run(Flag f);
	}
	
	public static interface Loop{
		public boolean loop(Flag f);
	}
	
	private static class Looper extends Asyncer {
		private Handler h;
		private Loop loop;
		private boolean uithread;
		public Looper(Asyncer root, Loop loop, boolean uithread, Handler h){
			super(root, null, h);
			this.loop = loop;
			this.uithread = uithread;
			this.h = h;
		}

		@Override
		public void run(final Flag r){
			if(r.isStopped()){
				return;
			}
			
			Asyncer.Task task = new Asyncer.Task() {
				
				@Override
				public void run(Flag f) {
					boolean result = loop.loop(r);
					if(result){
						Looper.this.run(r);
					}else{
						r.proceed(1);
						Looper.this.runChild(r);
					}
				}
			};
			
			if(uithread){
				Asyncer as = new Drawer(root, task, h);
				as.run(r);
			}else{
				Asyncer as = new Worker(root, task, h);
				as.run(r);
			}
		}
	}
	
	private static class Brancher extends Asyncer {
		private Handler h;
		private AtomicInteger ai;
		private Task[] runs;
		private boolean uithread;
		public Brancher(Asyncer root, final Task[] runs, boolean uithread, Handler h){
			super(root, null, h);
			this.runs = runs;
			this.ai = new AtomicInteger(runs.length);
			this.h = h;
			this.uithread = uithread;
		}

		public int size() {
			if(child != null)
				return runs.length + child.size();
			else
				return runs.length;
		}
		
		@Override
		public void run(final Flag r){
			for (int i = 0; i < runs.length; i++) {
				final int idx = i;
				
				Task task = new Task() {
					@Override
					public void run(Flag f) {
						Brancher.this.runs[idx].run(r);
						r.proceed(1);
						int now = ai.decrementAndGet();
						if(now <= 0){
							Brancher.this.runChild(r);
						}
					}
				};
				
				if(uithread){
					Asyncer as = new Drawer(root, task, h);
					as.run(r);
				}else{
					Asyncer as = new Worker(root, task, h);
					as.run(r);
				}
			}
		}
	}

	private static class Worker extends Asyncer {
		private Handler h;
		
		public Worker(Asyncer root, Task run, Handler h){
			super(root, run, h);
			this.h = h;
		}

		@Override
		public void run(final Flag r){
			if(task != null){
				AsyncTask<Integer, Integer, Integer> async 
						= new AsyncTask<Integer, Integer, Integer>(){
					@Override
					protected Integer doInBackground(Integer... params) {
						task.run(r);
						r.proceed(1);
						runChild(r);
						return null;
					}
					
				};
				async.execute(1);
			}else{
				runChild(r);
			}
		}
	}
	
	private static class Drawer extends Asyncer {
		private Handler h;
		
		public Drawer(Asyncer root, Task run, Handler h){
			super(root, run, h);
			this.h = h;
		}

		@Override
		public void run(final Flag r){
			if(task != null){
				h.post(new Runnable() {
					@Override
					public void run() {
						task.run(r);
						r.proceed(1);
						Drawer.this.runChild(r);
					}
				});
			}else{
				runChild(r);
			}
		}
	}

	public int size() {
		if(child != null)
			return 1 + child.size();
		else
			return 1;
	}
	
	@Override
	public boolean onLongClick(View v) {
		go();
		return true;
	}
	
	@Override
	public void onClick(View v) {
		go();
	}
	
	public interface Listener {
		public void progress(int n);
		public void stoped();
		public void completed();
	}
	
}
