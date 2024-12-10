package com.khjxiaogu.tssap.util;

public class LogUtil {
	public static void addLog(Object data) {
		System.out.println(data);
	}
	public static void addError(String message,Throwable ex) {
		ex.printStackTrace();
		System.out.println(message);
		
	}
}
