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
	protected List<Asyncer> childs = new ArrayList<Asyncer>();
	protected Task run;

	public Asyncer(){
		this.root = this;
		this.h = new Handler();
	}
	
	protected Asyncer(Task run, Handler h){
		this.run = run;
		this.root = this;
		this.h = h;
	}

	protected Asyncer(Asyncer root, Task run, Handler h){
		this.root = root;
		this.run = run;
		this.h = h;
	}

	public Asyncer worker(Task run){
		Asyncer a = new Asyncer(root, run, h);
		childs.add(a);
		return a;
	}
	
	public Asyncer drawer(Task run){
		Asyncer a = new Drawer(root, run, h);
		childs.add(a);
		return a;
	}

	public Asyncer loopdrawer(final Loop run){
		Asyncer a = new Looper(root, run, true, h);
		childs.add(a);
		return a;
	}
	
	public Asyncer loop(final Loop run){
		Asyncer a = new Looper(root, run, false, h);
		childs.add(a);
		return a;
	}
	
	public Asyncer branch(final Task... runs){
		final AtomicInteger ai = new AtomicInteger(runs.length);
		final Asyncer a = new Brancher(root, ai, runs, h);
		childs.add(a);
		return a;
	}
	
	public void run(final Flag r){
		if(run != null){
			AsyncTask<Integer, Integer, Integer> async 
					= new AsyncTask<Integer, Integer, Integer>(){
				@Override
				protected Integer doInBackground(Integer... params) {
					run.task(Asyncer.this, r);
					runChild(r);
					return null;
				}
				
			};
			async.execute(1);
		}else{
			runChild(r);
		}
	}
	
	protected void runChild(Flag r){
		if(childs.size() > 0 && r.runnable){
			for (Iterator<Asyncer> ite = childs.iterator(); ite.hasNext();) {
				ite.next().run(r);
			}
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
		public Map<String, Object> map = new HashMap<String, Object>();
		public boolean runnable = true;
		public void stop(){
			runnable = false;
		}

		public void set(String key, Object value){
			map.put(key, value);
		}
		
		public Object get(String key){
			return map.get(key);
		}
	}
	
	public static interface Task{
		public void task(Asyncer a, Flag f);
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
			Asyncer a = new Asyncer(new LoopTask(this, uithread, loop, new Runnable() {
				@Override
				public void run() {
					Looper.this.runChild(r);
				}
			},h ), h);
			a.run(r);
		}
	}

	private static class LoopTask implements Task {
		private Handler h;
		private Asyncer current;
		private Loop run;
		private Runnable complete;
		private boolean uithread;
		public LoopTask(Asyncer current, boolean uithread, Loop run, Runnable complete, Handler h){
			this.current = current;
			this.run = run;
			this.complete = complete;
			this.uithread = uithread;
			this.h = h;
		}
		
		@Override
		public void task(Asyncer acync, Flag r) {
			boolean result = run.loop(current, r);
			if(result){
				if(uithread){
					Asyncer a = new Drawer(new LoopTask(current, uithread, run, complete, h), h);
					a.run(r);
				}else{
					Asyncer a = new Asyncer(new LoopTask(current, uithread, run, complete, h), h);
					a.run(r);
				}
			}else{
				complete.run();
			}
		}
		
	}
	private static class Brancher extends Asyncer {
		private Handler h;
		private AtomicInteger ai;
		private Task[] runs;
		public Brancher(Asyncer root, final AtomicInteger ai, final Task[] runs, Handler h){
			super(root, null, h);
			this.runs = runs;
			this.ai = ai;
			this.h = h;
		}

		@Override
		public void run(final Flag r){
			for (int i = 0; i < runs.length; i++) {
				Asyncer a = new Asyncer(new BranchTask(this,ai,runs[i],new Runnable() {
					@Override
					public void run() {
						Brancher.this.runChild(r);
					}
				}), h);
				a.run(r);
			}
		}
	}
	
	private static class BranchTask implements Task {
		private Asyncer current;
		private AtomicInteger ai;
		private Task run;
		private Runnable complete;
		public BranchTask(Asyncer current, AtomicInteger ai, Task run, Runnable complete){
			this.current = current;
			this.ai = ai;
			this.run = run;
			this.complete = complete;
		}
		
		@Override
		public void task(Asyncer acync, Flag r) {
			run.task(current, r);
			int now = ai.decrementAndGet();
			if(now <= 0){
				complete.run();
			}
		}
		
	}
	
	private static class Drawer extends Asyncer {
		private Handler h;
		public Drawer(Task run, Handler h){
			super(run, h);
			this.h = h;
		}
		
		public Drawer(Asyncer root, Task run, Handler h){
			super(root, run, h);
			this.h = h;
		}

		@Override
		public void run(final Flag r){
			if(run != null){
				h.post(new Runnable() {
					@Override
					public void run() {
						run.task(Drawer.this, r);
						Drawer.this.runChild(r);
					}
				});
			}else{
				runChild(r);
			}
		}
	}
}
