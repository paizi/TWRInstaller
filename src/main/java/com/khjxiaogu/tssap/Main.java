package com.khjxiaogu.tssap;

import java.io.File;
import java.io.FileOutputStream;
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
import java.util.Objects;
import java.util.Set;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
	static File localPath=new File("tssap-configs");
	public static void main(String[] args){
		try {
			SimpleDateFormat logdate=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			LogUtil.addLog("=======================================================");
			LogUtil.addLog("started at "+logdate.format(new Date()));
			//prepare user interface components
			DefaultUI.setDefaultUI(new SwingUI());
			Gson gson=new GsonBuilder().setPrettyPrinting().create();
			//load language from jar
			String lang=Locale.getDefault().getLanguage();
			InputStream is=Main.class.getClassLoader().getResourceAsStream("com.khjxiaogu.tssap."+lang+".json");
			if(is==null) {
				is=Main.class.getClassLoader().getResourceAsStream("com.khjxiaogu.tssap.en_us.json");
			}
			Lang.setLang(JsonParser.parseString(FileUtil.readString(is)).getAsJsonObject());
			//load local configurations
			LocalConfig config = null;
			LocalData data= null;
			if(localPath.exists()) {
				File dataFile=new File(localPath,"data.json");
				File configFile=new File(localPath,"config.json");
				if(dataFile.exists()) {
					data=gson.fromJson(FileUtil.readString(dataFile), LocalData.class);
				}
				if(configFile.exists()) {
					config=gson.fromJson(FileUtil.readString(configFile), LocalConfig.class);
				}
			}else {
				localPath.mkdirs();
			}
			if(config==null) {
				//DefaultUI.getDefaultUI().message(Lang.getLang("prompt.no-config.title"), Lang.getLang("prompt.no-config.message"));
				LogUtil.addLog("No config found, skip update.");
				exit();
			}
			if(isEmpty(config.channels)) {
				//DefaultUI.getDefaultUI().message(Lang.getLang("prompt.no-channel.title"), Lang.getLang("prompt.no-channel.message"));
				LogUtil.addLog("No channel found, skip update");
				exit();
			}
			//respect local channel configuration
			ChannelItem selectedChannel=null;
			if(!isEmpty(config.selectedChannel)) {
				if(!isEmpty(config.channels)) {
					for(ChannelItem chan:config.channels) {
						if(config.selectedChannel.equals(chan.id)) {
							selectedChannel=chan;
							break;
						}
					}
				}
			}
			if(isEmpty(selectedChannel)) {//use first channel if no configuration found.
				selectedChannel=config.channels.get(0);
			}
			if(data!=null&&!isEmpty(config.selectedVersion)) {//check if skip needed if local config does not require any update.
				if(Objects.equals(selectedChannel.id, data.cachedChannel)) {
					if(!isEmpty(data.cachedModpack)&&Objects.equals(data.cachedModpack.version,config.selectedVersion!=null)) {
						exit();
					}
				}
			}
			//load metadata
			PackMeta meta=null;
			try (InputStream input=new InflaterInputStream(FileUtil.fetchWithRetry(selectedChannel.url,3))){
				meta=gson.fromJson(FileUtil.readString(input), PackMeta.class);
			}
			if(isEmpty(meta)) {
				exit();
			}
			//load version
			Version remoteVersion=null;
			if(isEmpty(config.selectedVersion)) {//use latest if not version selected
				remoteVersion=meta.latestVersion;
			}else {
				Versions versions=null;
				try (InputStream input=new InflaterInputStream(FileUtil.fetchWithRetry(meta.versionsPath,3))){//load history version if user requires.
					versions=gson.fromJson(FileUtil.readString(input), Versions.class);
				}
				if(isEmpty(versions)|isEmpty(versions.versions)) {
					DefaultUI.getDefaultUI().message(Lang.getLang("prompt.illegal-versions.title"), Lang.getLang("prompt.illegal-versions.message"));
					exit();
				}
				for(Version version:versions.versions) {
					if(Objects.equals(version.versionName, config.selectedVersion)) {
						remoteVersion=version;
						break;
					}
				}
				if(isEmpty(remoteVersion)) {
					DefaultUI.getDefaultUI().message(Lang.getLang("prompt.no-such-version.title"), Lang.getLang("prompt.no-such-version.message"));
					exit();
				}
			}
			if(isEmpty(remoteVersion)) {
				LogUtil.addLog("no latest version found");
				exit();
			}
			if(data==null)
				data=new LocalData();
			if(!isEmpty(data.cachedModpack)&&Objects.equals(remoteVersion.versionName, data.cachedModpack.version)){//remote version matches cache version
				LogUtil.addLog("remote latest matches local latest");
				exit();
			}
			Modpack modpack=null;//finally we get modpack 
			try (InputStream input=new InflaterInputStream(FileUtil.fetchWithRetry(remoteVersion.packFilePath,3))){
				modpack=gson.fromJson(FileUtil.readString(input), Modpack.class);
			}
			if(isEmpty(modpack)) {
				LogUtil.addLog("no modpack found");
				exit();
			}

			TaskList tasks=new TaskList();
			Set<String> addedFiles=new HashSet<>();
			for(ModPackFile mpf:modpack.files) {//check and add new files
				addedFiles.add(mpf.file);
				tasks.addTask(new ModPackInstallTask(mpf));
			}
			if(!isEmpty(data.cachedModpack)) {//delete old file when deleted in new version
				for(ModPackFile mpf:data.cachedModpack.files) {
					if(!addedFiles.contains(mpf.file))
						tasks.addTask(new DeleteOldFileTask(mpf));
				}
			}
			data.cachedModpack=modpack;
			data.cachedChannel=selectedChannel.id;
			tasks.addTask(new UpdateLocalDataTask(new File(localPath,"data.json"), gson.toJson(data)));
			
			tasks.start();
			//create backup
			File packupFolder=new File("tssap-backup");
			packupFolder.mkdirs();
			SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
			Path mainloc = new File("./").toPath();
			File backupFile=new File(packupFolder,sdf.format(new Date()));
			try(ZipOutputStream zos=new ZipOutputStream(new FileOutputStream(backupFile))){
				List<Path> backupExcludes=new ArrayList<>();
				List<Path> backupIncludes=new ArrayList<>();
				if(config.backupExcludes!=null)
					for(String s:config.backupExcludes) {
						backupExcludes.add(new File(s).toPath());
					}
				if(config.backupIncludes!=null)
					for(String s:config.backupIncludes) {
						backupIncludes.add(new File(s).toPath());
					}
				for(AbstractTask task:tasks.getTasks()) {
					if(task instanceof AbstractFileTask) {
						AbstractFileTask ftask=(AbstractFileTask) task;
						if(ftask.getFileData()==null)continue;//nothing to backup
						//check policies if backup of specific files needed
						boolean flag=true;
						Path path=ftask.getFile().toPath();
						for(Path s:backupExcludes) {
							if(path.startsWith(s)) {
								flag=false;
								break;
							}
						}
						if(!flag) {
							for(Path s:backupIncludes) {
								if(path.startsWith(s)) {
									flag=true;
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
			LogUtil.addLog("Update complete, backup saved to "+backupFile.getAbsolutePath());
			System.exit(0);
		}catch(UpdateNotRequiredException e) {
			LogUtil.addLog("update is not required, stopping.");
			System.exit(0);
		}catch(Throwable t) {//must exit program to let game running
			LogUtil.addError("error in updating", t);
			System.exit(0);
		}
		
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
