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
