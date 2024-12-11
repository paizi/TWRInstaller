package com.khjxiaogu.tssap.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class LogUtil {
	static PrintStream file;
	public static void init() {
		new File("logs").mkdirs();
		File latestlog=new File("logs/tssap_latest.log");
		File oldlog=new File("logs/tssap_previous.log");
		try {
			Files.move(latestlog.toPath(), oldlog.toPath(), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			file=new PrintStream(latestlog);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	public static void addLog(Object data) {
		System.out.println(data);
		if(file!=null)file.println(data);
	}
	public static void addError(String message,Throwable ex) {
		ex.printStackTrace();
		System.out.println(message);
		if(file!=null) {
			ex.printStackTrace(file);
			file.println(message);
		}
		
	}
}
