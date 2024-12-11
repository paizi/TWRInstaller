package com.khjxiaogu.tssap.entity;

import java.util.Date;
/**
 * A specific version for the pack
 * */
public class Version {
	public String versionName;
	public Date versionDate;
	public String packFilePath;
	public String changelogPath;
	public Version() {
	}
	@Override
	public String toString() {
		return versionName ;
	}

}
