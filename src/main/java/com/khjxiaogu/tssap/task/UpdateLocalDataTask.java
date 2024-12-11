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

public class UpdateLocalDataTask extends AbstractFileTask {
	String data;
	public UpdateLocalDataTask(File packfile,String data) {
		super(packfile);
	}

	@Override
	public void runTask() throws IOException {
		Path curfile = file.toPath();
		Path mainloc = new File("./").toPath();
		if (!curfile.startsWith(mainloc)) {// found path outside minecraft, ignore.
			LogUtil.addLog("illegal path found, download failed.");
			this.setFailed();
			return;
		}
		super.backup();
		if (!isFailed()) {
			FileUtil.transfer(data, file);
			this.setCompleted();
		}

	}

	@Override
	public String getTaskDesc() {
		return Lang.getLang("config.update", file);
	}

	@Override
	public String getBackupEntry() {
		return null;
	}

}
