/*
 * MIT License
 *
 * Copyright (c) 2025 TeamMoeg
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
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

/**
 * The IO task list.
 */
public class TaskList {
	
	/**
	 * Checks if the task list is failed.
	 *
	 * @return true, if failed
	 */
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
					//System.out.println(cplt+"/"+uclt);
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
	
	/**
	 * Get existing tasks.
	 *
	 * @return the tasks
	 */
	public LinkedList<AbstractTask> getTasks() {
		return tasks;
	}
	
	/**
	 * Add task.
	 *
	 * @param task the task
	 */
	public synchronized void addTask(AbstractTask task) {
		total++;
		task.setWhenComplete(this::whenTaskComplete);
		tasks.add(task);
	}
	
	/**
	 * Start all task and no longer allow task addition
	 *
	 * @throws InterruptedException the interrupted exception
	 */
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
			boolean needRollback=(failed>0&&DefaultUI.getDefaultUI().shouldExitImmediate())||DefaultUI.getDefaultUI().confirm(Lang.getLang("prompt.failed.title"), Lang.getLang("prompt.failed.message"));
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
			ShutdownHandler.exitNormally();
		}
		DefaultUI.getDefaultUI().setCloseAction(null);
	}
	
	private synchronized void addAllTask() {
		for(AbstractTask task:tasks)
			executor.submit(task);
	}
	
	/**
	 * Terminate all tasks.
	 */
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
	
	/**
	 * task complete handler
	 *
	 * @param task the task
	 */
	private synchronized void whenTaskComplete(AbstractTask task) {
		if(hasFailed)return;
		if(task.isFailed()&&!task.isOptional()) {
			failed++;
			hasFailed=true;
			DefaultUI.getDefaultUI().setProgress(Lang.getLang("progress.failed_rollback"), -1);
			
			terminate();
			//ShutdownHandler.exitNormally();
		}
	}
}
