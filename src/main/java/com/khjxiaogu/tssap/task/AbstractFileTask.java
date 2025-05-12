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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import com.khjxiaogu.tssap.util.FileUtil;
import com.khjxiaogu.tssap.util.LogUtil;

public abstract class AbstractFileTask extends AbstractTask {

	private byte[] fileData;
	protected File file;
	private boolean existsBefore;
	private boolean hasBackup;
	public AbstractFileTask(File path) {
		super();
		this.file = path;
	}
	protected void backup() {
		hasBackup=true;
		if(!file.exists()) {
			return;
		}
		existsBefore=true;
		try {
			fileData=FileUtil.readAll(file);
		} catch (IOException e) {
			LogUtil.addError("backup failed", e);
			super.setFailed();
		}
		
	}
	@Override
	public void run() {
		super.run();
		if(isFailed()&&hasBackup)
			try {
				rollback();
			} catch (IOException e) {
				LogUtil.addError("rollback failed!",e);
			}
	}
	public void rollback() throws IOException {
		if(existsBefore) {
			if(fileData!=null)
				FileUtil.transfer(new ByteArrayInputStream(fileData), file);
			else
				LogUtil.addLog("rollback failed, no previous backup.");
		}else
			if(file.exists())
				file.delete();
			
	}
	public byte[] getFileData() {
		return fileData;
	}
	public File getFile() {
		return file;
	}
	public abstract String getBackupEntry();
}
