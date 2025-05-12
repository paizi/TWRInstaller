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
