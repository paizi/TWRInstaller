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
