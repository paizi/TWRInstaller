package com.khjxiaogu.tssap.task;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.zip.InflaterInputStream;

import com.khjxiaogu.tssap.entity.ModPackFile;
import com.khjxiaogu.tssap.ui.Lang;
import com.khjxiaogu.tssap.util.FileUtil;
import com.khjxiaogu.tssap.util.HashUtil;
import com.khjxiaogu.tssap.util.LogUtil;

public class DeleteOldFileTask extends AbstractFileTask {
	ModPackFile packfile;

	public DeleteOldFileTask(ModPackFile packfile) {
		super(new File(packfile.file));
		this.packfile = packfile;
	}

	@Override
	public void runTask() {
		Path curfile = file.toPath();
		Path mainloc = new File("./").toPath();
		if (!curfile.startsWith(mainloc)) {// found path outside minecraft, ignore.
			LogUtil.addLog("illegal path found, download failed.");
			this.setFailed();
			return;
		}
		if (!file.exists()) {
			this.setCompleted();
			return;
		}
		super.backup();
		if (!isFailed()) {
			if(file.delete()) {
				this.setCompleted();
			}else
				this.setFailed();

		}

	}

	@Override
	public String getTaskDesc() {
		return Lang.getLang("file.delete", file);
	}

	@Override
	public String getBackupEntry() {
		return packfile.file;
	}

}
