package com.khjxiaogu.tssap.entity;
/**
 * data structure for a specific file in the modpack
 * */
public class ModPackFile {
	public String file;
	public String hash;
	public String link;
	public long size;
	public Dist dist;
	public boolean compressed;
	public ModPackFile() {
		
	}

}
