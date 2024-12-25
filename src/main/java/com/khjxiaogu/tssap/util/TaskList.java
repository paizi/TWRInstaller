package com.khjxiaogu.tssap.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.khjxiaogu.tssap.task.AbstractTask;
import com.khjxiaogu.tssap.ui.DefaultUI;
import com.khjxiaogu.tssap.ui.Lang;

public class TaskList {
	public boolean isFailed() {
		return hasFailed;
	}
	LinkedList<AbstractTask> tasks=new LinkedList<>();
	AtomicInteger it=new AtomicInteger(0);
	public static final int THREAD_NUM=10;
	ExecutorService executor=Executors.newFixedThreadPool(THREAD_NUM, t->new Thread(t,"TSSAP-Download-Thread-"+it.getAndAdd(1)));
	Thread updateProgress=new Thread() {

		@Override
		public void run(){
			try {
				while(true) {
					long cplt=0;
					long uclt=0;
					for(AbstractTask taskx:tasks) {
						long tn=taskx.getTaskDifficulty();
						uclt+=tn;
						
						if(taskx.isCompleted()||taskx.isFailed()) {
							cplt+=tn;
							//System.out.println("completed "+tn);
						}
					}
					System.out.println(cplt+"/"+uclt);
					DefaultUI.getDefaultUI().setProgress(null,cplt*1f/uclt);
					Thread.sleep(50);
				}
			} catch (InterruptedException e) {
			}
		
		}
		
	};
	int complete;
	int total;
	int failed;
	boolean hasFailed;
	public LinkedList<AbstractTask> getTasks() {
		return tasks;
	}
	public synchronized void addTask(AbstractTask task) {
		total++;
		task.setWhenComplete(this::whenTaskComplete);
		tasks.add(task);
	}
	public void start() throws InterruptedException {
		addAllTask();
		DefaultUI.getDefaultUI().setCloseAction(()->{
			terminate();
		});
		updateProgress.setDaemon(true);
		updateProgress.start();
		executor.shutdown();
		
		while(true)
			if(executor.awaitTermination(1, TimeUnit.SECONDS))
				break;
		updateProgress.interrupt();
		try {
			updateProgress.join();
		} catch (InterruptedException e) {
		}
		if(hasFailed) {
			boolean needRollback=DefaultUI.getDefaultUI().confirm(Lang.getLang("prompt.failed.title"), Lang.getLang("prompt.failed.message"));
			if(needRollback) {
				DefaultUI.getDefaultUI().setProgress(Lang.getLang("progress.rollback"), -1);
				for(AbstractTask rtask:tasks) {
					if(rtask.isCompleted()||rtask.isFailed()) try {
						rtask.rollback();
					} catch (Exception e) {
						LogUtil.addError("rollback failed", e);
					}
				}
			}
			System.exit(0);
		}
		DefaultUI.getDefaultUI().setCloseAction(null);
	}
	private synchronized void addAllTask() {
		for(AbstractTask task:tasks)
			executor.submit(task);
	}
	public void terminate() {
		if(executor.isShutdown()) {
			hasFailed=true;
			executor.shutdownNow();
			updateProgress.interrupt();
			try {
				updateProgress.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			DefaultUI.getDefaultUI().setProgress(Lang.getLang("progress.terminating"), -1);
			
			
		}
	}
	private synchronized void whenTaskComplete(AbstractTask task) {
		if(hasFailed)return;
		if(task.isFailed()&&!task.isOptional()) {
			failed++;
			hasFailed=true;
			DefaultUI.getDefaultUI().setProgress(Lang.getLang("progress.failed_rollback"), -1);
			
			terminate();
			System.exit(0);
		}
	}
}
