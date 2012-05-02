package org.qrone.android.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import android.os.AsyncTask;
import android.os.Handler;

public class Asyncer{
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
		Asyncer a = new Asyncer(root, new Task() {
			@Override
			public void run(Asyncer a, final Flag f) {
				Flag fl = addtask.worker(new Task() {
					@Override
					public void run(Asyncer a, Flag f) {
						Asyncer.this.runChild(f);
					}
				}).go();
				f.addFlag(fl);
			}
		}, h);
		child = a;
		return a;
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
		final Asyncer a = new Brancher(root, runs, h);
		child = a;
		return a;
	}
	
	public void run(final Flag r){
		task.run(this, r);
	}
	
	protected void runChild(Flag r){
		if(child !=null && !r.isStopped()){
			child.run(r);
		}
	}
	
	public Flag go(){
		if(root != null && root != this){
			return root.go();
		}else{
			Flag f = new Flag();
			run(f);
			return f;
		}
	}

	public class Flag{
		private Map<String, Object> map = new HashMap<String, Object>();
		private boolean abort = false;
		private List<Flag> childs = new ArrayList<Flag>();
		
		public void stop(){
			abort = true;
			for (Iterator<Flag> iter = childs.iterator(); iter.hasNext();) {
				iter.next().stop();
			}
		}
		
		public void addFlag(Flag fl) {
			childs.add(fl);
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
	}
	
	public static interface Task{
		public void run(Asyncer a, Flag f);
	}
	
	public static interface Loop{
		public boolean loop(Asyncer a, Flag f);
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
				public void run(Asyncer a, Flag f) {
					boolean result = loop.loop(Looper.this, r);
					if(result){
						Looper.this.run(r);
					}else{
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
		public Brancher(Asyncer root, final Task[] runs, Handler h){
			super(root, null, h);
			this.runs = runs;
			this.ai = new AtomicInteger(runs.length);
			this.h = h;
		}

		@Override
		public void run(final Flag r){
			for (int i = 0; i < runs.length; i++) {
				final int idx = i;
				Asyncer a = new Asyncer(Brancher.this.root, new Asyncer.Task() {
					@Override
					public void run(Asyncer a, Flag f) {
						Brancher.this.runs[idx].run(Brancher.this, r);
						int now = ai.decrementAndGet();
						if(now <= 0){
							Brancher.this.runChild(r);
						}
					}
				},h);
				a.run(r);
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
						task.run(Worker.this, r);
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
						task.run(Drawer.this, r);
						Drawer.this.runChild(r);
					}
				});
			}else{
				runChild(r);
			}
		}
	}
}
