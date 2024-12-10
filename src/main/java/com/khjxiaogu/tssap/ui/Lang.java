package com.khjxiaogu.tssap.ui;

import com.google.gson.JsonObject;

public class Lang {
	static JsonObject lang;
	
	
	public static void setLang(JsonObject lang) {
		Lang.lang = lang;
	}


	public static String getLang(String key,Object...objects) {
		if(lang.has(key))
			return String.format(lang.get(key).getAsString(), objects);
		return key;
	}
}
