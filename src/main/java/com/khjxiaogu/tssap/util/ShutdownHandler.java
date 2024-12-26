package com.khjxiaogu.tssap.util;

public class ShutdownHandler {
	public static boolean isNormally;
	public ShutdownHandler() {
		// TODO Auto-generated constructor stub
	}
	public static void exitNormally() {
		isNormally=true;
		System.exit(0);
	}
}
