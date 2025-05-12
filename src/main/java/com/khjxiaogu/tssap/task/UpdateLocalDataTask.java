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
		this.data=data;
	}

	@Override
	public void runTask() throws IOException {
		Path curfile = file.toPath();
		/*Path mainloc = new File("./").toPath();
		if (!curfile.startsWith(mainloc)) {// found path outside minecraft, ignore.
			LogUtil.addLog("illegal path "+curfile.toAbsolutePath()+" found, download failed.");
			this.setFailed();
			return;
		}*/
		file.getParentFile().mkdirs();
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

	@Override
	public long getTaskDifficulty() {
		return data.length();
	}

}
