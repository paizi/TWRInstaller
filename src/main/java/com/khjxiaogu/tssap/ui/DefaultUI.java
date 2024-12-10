package com.khjxiaogu.tssap.ui;

public class DefaultUI {
	private static UI defUI;

	public static UI getDefaultUI() {
		return defUI;
	}

	public static void setDefaultUI(UI defUI) {
		DefaultUI.defUI = defUI;
	}
	
}
