package com.khjxiaogu.tssap.ui;

public interface UI {
	
	boolean confirm(String title, String prompt);

	void setProgress(String content, float value);

	void message(String title, String prompt);

}