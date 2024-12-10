package com.khjxiaogu.tssap.entity;

import java.util.List;

public class LocalConfig {
	public List<ChannelItem> channels;
	public String selectedChannel;
	public String selectedVersion;
	public List<String> backupIncludes;
	public List<String> backupExcludes;
	public List<String> updateIgnores;
	public boolean isClient;
}
