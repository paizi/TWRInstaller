package com.khjxiaogu.tssap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.khjxiaogu.tssap.entity.ChannelItem;
import com.khjxiaogu.tssap.entity.LocalConfig;
import com.khjxiaogu.tssap.entity.LocalData;
import com.khjxiaogu.tssap.entity.ModPackFile;
import com.khjxiaogu.tssap.entity.Modpack;
import com.khjxiaogu.tssap.entity.PackMeta;
import com.khjxiaogu.tssap.entity.Version;
import com.khjxiaogu.tssap.entity.Versions;
import com.khjxiaogu.tssap.task.AbstractFileTask;
import com.khjxiaogu.tssap.task.AbstractTask;
import com.khjxiaogu.tssap.task.DeleteOldFileTask;
import com.khjxiaogu.tssap.task.ModPackInstallTask;
import com.khjxiaogu.tssap.task.UpdateLocalDataTask;
import com.khjxiaogu.tssap.ui.DefaultUI;
import com.khjxiaogu.tssap.ui.Lang;
import com.khjxiaogu.tssap.ui.SwingUI;
import com.khjxiaogu.tssap.util.FileUtil;
import com.khjxiaogu.tssap.util.LogUtil;
import com.khjxiaogu.tssap.util.TaskList;

public class Main {
	static Gson gson=new GsonBuilder().setPrettyPrinting().setDateFormat("yyyy-MM-dd HH:mm:ss").create();
	static File localPath=new File("tssap-configs");
	static File configFile=new File(localPath,"config.json");
	static File dataFile=new File(localPath,"data.json");
	public static void main(String[] args){
		try {
			LogUtil.init();
			SimpleDateFormat logdate=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			LogUtil.addLog("=======================================================");
			LogUtil.addLog("started at "+logdate.format(new Date()));
			//prepare user interface components
			DefaultUI.setDefaultUI(new SwingUI());
			DefaultUI.getDefaultUI().setProgress("loading...", -1);
			boolean isBootstrap=false;
			if(args.length>0)
				isBootstrap=args[0].equals("bootstrap");
			//load language from jar
			String lang=Locale.getDefault().getLanguage()+"_"+Locale.getDefault().getCountry();
			LogUtil.addLog("display language:"+lang);
			InputStream is=Main.class.getClassLoader().getResourceAsStream("com/khjxiaogu/tssap/"+lang.toLowerCase()+".json");
			if(is==null) {
				is=Main.class.getClassLoader().getResourceAsStream("com/khjxiaogu/tssap/en_us.json");
			}
			Lang.setLang(JsonParser.parseString(FileUtil.readString(is)).getAsJsonObject());
			DefaultUI.getDefaultUI().setTitle(Lang.getLang("title"));
			DefaultUI.getDefaultUI().setProgress(Lang.getLang("progress.meta"), -1);
			//load local configurations
			LocalConfig config = loadConfig();
			LocalData data= loadData();
			if(!localPath.exists()) {
				localPath.mkdirs();
			}
			//config validity check. During bootstrap mode, this would fail sliently
			if(config==null) {
				LogUtil.addLog("No config found, skip update.");
				if(!isBootstrap)
					DefaultUI.getDefaultUI().message(Lang.getLang("prompt.no-config.title"), Lang.getLang("prompt.no-config.message"));
				
				exit();
			}
			if(isEmpty(config.channels)) {
				LogUtil.addLog("No channel found, skip update");
				if(!isBootstrap)
					DefaultUI.getDefaultUI().message(Lang.getLang("prompt.no-channel.title"), Lang.getLang("prompt.no-channel.message"));
				
				exit();
			}
			//system base loaded, run logics
			if(!isBootstrap) {
				String[] opertaion=DefaultUI.getDefaultUI().getUserOperation(config);
				switch(opertaion[0]) {
				case "repair":repairOnly(data,config);System.exit(0);break;
				case "version":config.selectedChannel=opertaion[1];config.selectedVersion=opertaion[2];saveConfig(config);
				case "update":
					default:
				}
			}
			
			defaultUpdate(data,config);
			
			System.exit(0);
		}catch(UpdateNotRequiredException e) {
			LogUtil.addLog("update is not required, stopping.");
			System.exit(0);
		}catch(Throwable t) {//must exit program to let game running
			LogUtil.addError("error in updating", t);
			System.exit(0);
		}
		
	}
	public static void defaultUpdate(LocalData data,LocalConfig config) throws Exception {
		//respect local channel configuration
		ChannelItem selectedChannel=getSelectedChannel(config);
		if(!isEmpty(data.cachedChannel)&&!isEmpty(config.selectedVersion)) {//check if skip needed if local config does not require any update.
			if(Objects.equals(selectedChannel.id, data.cachedChannel)) {
				if(!isEmpty(data.cachedModpack)&&Objects.equals(data.cachedModpack.version,config.selectedVersion!=null)) {
					exit();
				}
			}
		}
		//load metadata
		PackMeta meta=getMeta(selectedChannel);
		
		if(isEmpty(meta)) {
			exit();
		}
		//load version
		Version remoteVersion=null;
		if(isEmpty(config.selectedVersion)) {//use latest if not version selected
			remoteVersion=meta.latestVersion;
		}else {
			Versions versions=fetchVersions(meta);
			if(isEmpty(versions.versions)) {
				DefaultUI.getDefaultUI().message(Lang.getLang("prompt.illegal-versions.title"), Lang.getLang("prompt.illegal-versions.message"));
				exit();
			}
			remoteVersion=pickVersion(versions,config.selectedVersion);
			if(isEmpty(remoteVersion)) {
				DefaultUI.getDefaultUI().message(Lang.getLang("prompt.no-such-version.title"), Lang.getLang("prompt.no-such-version.message"));
				exit();
			}
		}
		if(isEmpty(remoteVersion)) {
			LogUtil.addLog("no latest version found");
			exit();
		}
		if(!isEmpty(data.cachedModpack)&&Objects.equals(remoteVersion.versionName, data.cachedModpack.version)){//remote version matches cache version
			LogUtil.addLog("remote latest matches local latest");
			exit();
		}
		Modpack modpack=fetchModpack(remoteVersion);//finally we get modpack 

		if(isEmpty(modpack)) {
			LogUtil.addLog("no modpack found");
			exit();
		}
		TaskList tasks=new TaskList();
		//create tasks 
		updateModpackTask(config,tasks,modpack,data.cachedModpack);
		updateLibraryTask(tasks,modpack);
		updateLocalDataTask(tasks,data,modpack,selectedChannel);
		//begin task multi-threaded
		tasks.start();
		DefaultUI.getDefaultUI().setProgress(Lang.getLang("progress.backup"), -1);
		//create backup
		File backupFile=createBackup(tasks,config);
		LogUtil.addLog("Install complete, backup saved to "+backupFile.getAbsolutePath());
	}
	public static void repairOnly(LocalData data,LocalConfig config) throws Exception {
		TaskList tasks=new TaskList();
		//create tasks 
		updateModpackTask(config,tasks,data.cachedModpack,data.cachedModpack);
		updateLibraryTask(tasks,data.cachedModpack);
		//begin task multi-threaded
		tasks.start();
		DefaultUI.getDefaultUI().setProgress(Lang.getLang("progress.backup"), -1);
		//create backup
		File backupFile=createBackup(tasks,config);
		LogUtil.addLog("Repair complete, backup saved to "+backupFile.getAbsolutePath());
	}
	public static Versions fetchVersions(PackMeta meta) {
		try (InputStream input=new InflaterInputStream(FileUtil.fetchWithRetry(meta.versionsPath,3))){//load history version if user requires.
			return gson.fromJson(FileUtil.readString(input), Versions.class);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new Versions();
	}
	public static Modpack fetchModpack(Version version) throws Exception {
		try (InputStream input=new InflaterInputStream(FileUtil.fetchWithRetry(version.packFilePath,3))){
			return gson.fromJson(FileUtil.readString(input), Modpack.class);
		}
	}
	public static Version pickVersion(Versions versions,String versionName) {
		for(Version version:versions.versions) {
			if(Objects.equals(version.versionName, versionName)) {
				return version;
			}
		}
		return null;
	}
	public static PackMeta getMeta(ChannelItem channel) throws Exception {
		try (InputStream input=new InflaterInputStream(FileUtil.fetchWithRetry(channel.url,3))){
			return gson.fromJson(FileUtil.readString(input), PackMeta.class);
		}
	}
	
	public static ChannelItem getSelectedChannel(LocalConfig config) {
		if(!isEmpty(config.selectedChannel)) {
			if(!isEmpty(config.channels)) {
				for(ChannelItem chan:config.channels) {
					if(config.selectedChannel.equals(chan.id)) {
						return chan;
					}
				}
			}
		}
		//use first channel if no configuration found.
		return config.channels.get(0);
		
	}
	public static void updateLibraryTask(TaskList tasks,Modpack modpack) throws Exception {
		File mmcPack=new File("../mmc-pack.json");
		if(mmcPack.exists()&&!isEmpty(modpack.libraries)) {
			boolean changed=false;
			JsonElement mmcPackJson=JsonParser.parseString(FileUtil.readString(mmcPack));
			try {
				JsonArray ja=mmcPackJson.getAsJsonObject().get("components").getAsJsonArray();
				outer:for(Entry<String, String> lib:modpack.libraries.entrySet()) {
					for(JsonElement e:ja) {
						if(e.isJsonObject()) {
							JsonElement uid=e.getAsJsonObject().get("uid");
							JsonElement version=e.getAsJsonObject().get("version");
							if(uid==null||version==null)continue;
							if(lib.getKey().equals(uid.getAsString())) {
								if(!lib.getValue().equals(version.getAsString())) {
									e.getAsJsonObject().addProperty("version", lib.getValue());
									changed=true;
								}
								continue outer;
							}
						}
					}
					JsonObject newLib=new JsonObject();
					newLib.addProperty("uid", lib.getKey());
					newLib.addProperty("version", lib.getValue());
					ja.add(newLib);
					changed=true;
				}
				mmcPackJson.getAsJsonObject().add("components", ja);
				tasks.addTask(new UpdateLocalDataTask(mmcPack, gson.toJson(mmcPackJson)).setOptional(true));
			}catch(Exception ex) {
				LogUtil.addError("can not update mmc pack libraries",ex);
			}
		}
	}
	public static File createBackup(TaskList tasks,LocalConfig config) throws Exception {
		File packupFolder=new File("tssap-backup");
		packupFolder.mkdirs();
		SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
		Path mainloc = new File("./").toPath();
		File backupFile=new File(packupFolder,sdf.format(new Date())+".zip");
		try(ZipOutputStream zos=new ZipOutputStream(new FileOutputStream(backupFile))){
			for(AbstractTask task:tasks.getTasks()) {
				if(task instanceof AbstractFileTask) {
					AbstractFileTask ftask=(AbstractFileTask) task;
					//LogUtil.addLog(ftask.getBackupEntry());
					if(ftask.getFileData()==null||ftask.getBackupEntry()==null)continue;//nothing to backup
					//check policies if backup of specific files needed
					boolean flag=false;
					String path=ftask.getBackupEntry();
					if(config.backupIncludes!=null)
					for(String s:config.backupIncludes) {
						if(path.startsWith(s)) {
							flag=true;
							break;
						}
					}
					
					if(flag) {
						if(config.backupExcludes!=null)
							for(String s:config.backupExcludes) {
								if(path.startsWith(s)) {
									flag=false;
									break;
								}
							}
					}
					//write backup
					if(flag) {
						zos.putNextEntry(new ZipEntry(ftask.getBackupEntry()));
						zos.write(ftask.getFileData());
					}
					
				}
			}
			
		}
		return backupFile;
	}
	public static void updateModpackTask(LocalConfig config,TaskList tasks,Modpack modpack,Modpack cached) {
		
		Set<String> addedFiles=new HashSet<>();
		for(ModPackFile mpf:modpack.files) {//check and add new files
			if(mpf.client&&!config.isClient)continue;
			if(mpf.server&&config.isClient)continue;
			addedFiles.add(mpf.file);
			tasks.addTask(new ModPackInstallTask(mpf));
		}
		if(!isEmpty(cached)&&!isEmpty(cached.files)) {//delete old file when deleted in new version
			for(ModPackFile mpf:cached.files) {
				if(!addedFiles.contains(mpf.file))
					tasks.addTask(new DeleteOldFileTask(mpf));
			}
		}

	}
	public static void updateLocalDataTask(TaskList tasks,LocalData data,Modpack modpack,ChannelItem selectedChannel) {
		data.cachedModpack=modpack;
		data.cachedChannel=selectedChannel.id;
		tasks.addTask(new UpdateLocalDataTask(dataFile, gson.toJson(data)));
	}
	public static LocalData loadData() throws Exception {
		if(dataFile.exists()) {
			return gson.fromJson(FileUtil.readString(dataFile), LocalData.class);
		}
		return new LocalData();
	}
	
	public static LocalConfig loadConfig() throws Exception {
		if(configFile.exists()) {
			return gson.fromJson(FileUtil.readString(configFile), LocalConfig.class);
		}
		return new LocalConfig();
	}
	public static void saveConfig(LocalConfig cfg) throws Exception {
		FileUtil.transfer(gson.toJson(cfg), configFile);
	}
	public static boolean isEmpty(Object obj) {
		if(obj==null)return true;
		if(obj instanceof CharSequence)return ((CharSequence)obj).length()==0;
		if(obj instanceof Collection)return ((Collection)obj).size()==0;
		if(obj instanceof Map)return ((Map)obj).size()==0;
		return false;
	}
	public static void exit() throws UpdateNotRequiredException {
		
		throw new UpdateNotRequiredException();
	}

}
