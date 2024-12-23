package com.khjxiaogu.tssap.task;

import java.io.IOException;
import java.util.function.Consumer;

public abstract class AbstractTask implements Runnable {
	private boolean completed;
	private boolean failed;
	private boolean optional;
	public boolean isOptional() {
		return optional;
	}

	public AbstractTask setOptional(boolean optional) {
		this.optional = optional;
		return this;
	}
	private Consumer<AbstractTask> whenComplete;
	public void setWhenComplete(Consumer<AbstractTask> whenComplete) {
		this.whenComplete = whenComplete;
	}

	public boolean isCompleted() {
		return completed;
	}

	public void setCompleted() {
		this.completed = true;
	}

	public boolean isFailed() {
		return failed;
	}

	public void setFailed() {
		this.failed = true;
	}
	public abstract void rollback() throws Exception;
	@Override
	public void run() {
		try {
			runTask();
		}catch(Throwable ex) {
			ex.printStackTrace();
			failed=true;
		}
		if(whenComplete!=null)
			whenComplete.accept(this);
		
	}
	public abstract void runTask() throws Exception ;
	public abstract String getTaskDesc();
	public abstract long getTaskDifficulty();
	public long getTaskCompleted() {
		return 0;
	}
}
