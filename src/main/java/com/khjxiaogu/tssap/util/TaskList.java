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
		executor.shutdown();
		while(true)
			executor.awaitTermination(1, TimeUnit.SECONDS);
	}
	private synchronized void addAllTask() {
		for(AbstractTask task:tasks)
			executor.submit(task);
	}
	private synchronized void whenTaskComplete(AbstractTask task) {
		if(hasFailed)return;
		if(task.isFailed()) {
			failed++;
			hasFailed=true;
			DefaultUI.getDefaultUI().setProgress(Lang.getLang("progress.failed_rollback"), -1);
			executor.shutdownNow();
			boolean needRollback=DefaultUI.getDefaultUI().confirm(Lang.getLang("prompt.failed.title"), Lang.getLang("prompt.failed.message"));
			if(needRollback) {
				DefaultUI.getDefaultUI().setProgress("progress.rollback", -1);
				for(AbstractTask rtask:tasks) {
					if(rtask.isCompleted()||rtask.isFailed()) try {
						rtask.rollback();
					} catch (Exception e) {
						LogUtil.addError("rollback failed", e);
					}
				}
			}
		}
		if(task.isCompleted()) {
			complete++;
		}
		DefaultUI.getDefaultUI().setProgress(null, complete*1f/total);
	}
}
