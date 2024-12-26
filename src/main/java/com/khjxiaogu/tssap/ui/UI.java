package com.khjxiaogu.tssap.ui;

import com.khjxiaogu.tssap.entity.LocalConfig;

public interface UI {
	
	boolean confirm(String title, String prompt);

	void setProgress(String content, float value);

	void message(String title, String prompt);

	void setTitle(String content);

	void setCloseAction(Runnable closeAction);

	String[] getUserOperation(LocalConfig config) throws Exception;

	boolean shouldExitImmediate();

}