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
package com.khjxiaogu.tssap;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;

import com.khjxiaogu.tssap.util.FileUtil;

public class PreMain {
	public static void premain(String options, Instrumentation inst) {
		String javaExecutable = System.getProperty("java.home") + File.separator + "bin" + File.separator;
		if (System.getProperty("os.name").startsWith("Win")) {
			javaExecutable = javaExecutable + "javaw.exe";
		} else {
			javaExecutable = javaExecutable + "java";
		}
		System.out.println(javaExecutable);
		File batch=new File("installer.bat");
		if(!batch.exists()) {
			String batched="@CHCP 65001\r\n@\""+javaExecutable.replace("\"", "\\\"")+"\" -jar twr-installer.jar";
			try {
				FileUtil.transfer(batched, batch);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		File shell=new File("installer.sh");
		if(!shell.exists()) {
			String batched="\""+javaExecutable.replace("\"", "\\\"")+"\" -jar twr-installer.jar";
			try {
				FileUtil.transfer(batched, shell);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		try {
			Process p = (new ProcessBuilder(new String[] { javaExecutable, "-jar", "twr-installer.jar","bootstrap" })).inheritIO().start();
			int exitCode;
			if ((exitCode = p.waitFor()) != 0)
				throw new RuntimeException("Bootstrap application returns non-zero exit code " + exitCode + ". ");
		} catch (Exception e) {
			throw new RuntimeException("Bootstrap application run failed ", e);
		}
	}
}
