package com.khjxiaogu.tssap.entity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * data structure of a specific version of the modpack
 * */
public class Modpack {
	// list of files
	public List<ModPackFile> files;
	// string version 
	public String version;
	// uid-version pair for mmc libraries
	public Map<String,String> libraries;
	public Modpack() {
	}

}
