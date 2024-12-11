package com.khjxiaogu.tssap;

import java.io.File;
import java.lang.instrument.Instrumentation;

public class PreMain {
	public static void premain(String options, Instrumentation inst) {
		String javaExecutable = System.getProperty("java.home") + File.separator + "bin" + File.separator;
		if (System.getProperty("os.name").startsWith("Win")) {
			javaExecutable = javaExecutable + "javaw.exe";
		} else {
			javaExecutable = javaExecutable + "java";
		}
		System.out.println(javaExecutable);
		
		try {
			Process p = (new ProcessBuilder(new String[] { javaExecutable, "-jar", "tssap-bootstrap.jar","bootstrap" })).inheritIO().start();
			int exitCode;
			if ((exitCode = p.waitFor()) != 0)
				throw new RuntimeException("Bootstrap application returns non-zero exit code " + exitCode + ". ");
		} catch (Exception e) {
			throw new RuntimeException("Bootstrap application run failed ", e);
		}
	}
}
