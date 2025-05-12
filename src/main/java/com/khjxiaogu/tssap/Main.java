/*
 * MIT License
 *
 * Copyright (c) 2025 TeamMoeg
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.khjxiaogu.tssap;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
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
import com.khjxiaogu.tssap.entity.Dist;
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
import com.khjxiaogu.tssap.ui.HeadLessUI;
import com.khjxiaogu.tssap.ui.Lang;
import com.khjxiaogu.tssap.ui.SwingUI;
import com.khjxiaogu.tssap.util.FileUtil;
import com.khjxiaogu.tssap.util.JsonUTCDateAdapter;
import com.khjxiaogu.tssap.util.LogUtil;
import com.khjxiaogu.tssap.util.ShutdownHandler;
import com.khjxiaogu.tssap.util.TaskList;

public class Main {
	
	/** The gson. */
	static Gson gson=new GsonBuilder().setPrettyPrinting().registerTypeAdapter(Date.class, new JsonUTCDateAdapter()).create();
	
	/** The local path. */
	static File localPath=new File("tssap-configs");
	
	/** The config file. */
	static File configFile=new File(localPath,"config.json");
	
	/** The data file. */
	static File dataFile=new File(localPath,"data.json");
	
	/**
	 * The main method.
	 *
	 * @param args the arguments
	 */
	public static void main(String[] args){
		//use proxy if necessary
		System.setProperty("java.net.useSystemProxies", "true");
		boolean isBootstrap=false;
		try {
			List<String> largs=new ArrayList<>(Arrays.asList(args));
			isBootstrap=largs.contains("bootstrap");
			LogUtil.init();
			SimpleDateFormat logdate=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			LogUtil.addLog("=================================");
			LogUtil.addLog("started at "+logdate.format(new Date()));
			//load language from jar
			String lang=Locale.getDefault().getLanguage()+"_"+Locale.getDefault().getCountry();
			for(String s:largs) {
				if(s.startsWith("lang:"))
					lang=s.substring(5);
			}
			LogUtil.addLog("display language:"+lang);
			LogUtil.addLog("=================================");
			//prepare user interface components
	

			
			
			InputStream is=Main.class.getClassLoader().getResourceAsStream("com/khjxiaogu/tssap/"+lang.toLowerCase()+".json");
			if(is==null) {
				is=Main.class.getClassLoader().getResourceAsStream("com/khjxiaogu/tssap/en_us.json");
			}
			Lang.setLang(JsonParser.parseString(FileUtil.readString(is)).getAsJsonObject());
			if(GraphicsEnvironment.isHeadless()||largs.contains("headless"))
				DefaultUI.setDefaultUI(new HeadLessUI());
			else
				DefaultUI.setDefaultUI(new SwingUI());
			//DefaultUI.getDefaultUI().setProgress("loading...", -1);
			DefaultUI.getDefaultUI().setTitle(Lang.getLang("title"));
			
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
				case "repair":repairOnly(data,config);
				if(!isBootstrap)
					DefaultUI.getDefaultUI().message(Lang.getLang("prompt.operation_success.title"), Lang.getLang("prompt.operation_success.message"));
				ShutdownHandler.exitNormally();break;
				case "version":config.selectedChannel=opertaion[1];config.selectedVersion=opertaion[2];saveConfig(config);
				case "update":
					default:
				}
			}
			
			defaultUpdate(data,config);
			if(!isBootstrap)
				DefaultUI.getDefaultUI().message(Lang.getLang("prompt.operation_success.title"), Lang.getLang("prompt.operation_success.message"));
			ShutdownHandler.exitNormally();
		}catch(UpdateNotRequiredException e) {
			LogUtil.addLog("update is not required, stopping.");
			if(!isBootstrap)
				DefaultUI.getDefaultUI().message(Lang.getLang("prompt.no_op_needed.title"), Lang.getLang("prompt.no_op_needed.message"));
			ShutdownHandler.exitNormally();
		}catch(Throwable t) {//must exit program to let game running
			if(!isBootstrap)
				DefaultUI.getDefaultUI().message(Lang.getLang("prompt.update_failed.title"), Lang.getLang("prompt.update_failed.message"));
			LogUtil.addError("error in updating", t);
			ShutdownHandler.exitNormally();
		}
		
	}
	
	/**
	 * Default update.
	 *
	 * @param data the data
	 * @param config the config
	 * @throws Exception the exception
	 */
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
		DefaultUI.getDefaultUI().setProgress(Lang.getLang("progress.meta"), -1);
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
	
	/**
	 * Repair only.
	 *
	 * @param data the data
	 * @param config the config
	 * @throws Exception the exception
	 */
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
	
	/**
	 * Fetch versions.
	 *
	 * @param meta the meta
	 * @return the versions
	 */
	public static Versions fetchVersions(PackMeta meta) {
		DefaultUI.getDefaultUI().setProgress(Lang.getLang("progress.meta"), -1);
		try (InputStream input=new InflaterInputStream(FileUtil.fetchWithRetry(meta.versionsPath,3))){//load history version if user requires.
			return gson.fromJson(FileUtil.readString(input), Versions.class);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new Versions();
	}
	
	/**
	 * Fetch modpack.
	 *
	 * @param version the version
	 * @return the modpack
	 * @throws Exception the exception
	 */
	public static Modpack fetchModpack(Version version) throws Exception {
		try (InputStream input=new InflaterInputStream(FileUtil.fetchWithRetry(version.packFilePath,3))){
			return gson.fromJson(FileUtil.readString(input), Modpack.class);
		}catch(IOException ex) {
			reportNetworkFail();
			throw ex;
		}
	}
	
	/**
	 * Pick version.
	 *
	 * @param versions the versions
	 * @param versionName the version name
	 * @return the version
	 */
	public static Version pickVersion(Versions versions,String versionName) {
		for(Version version:versions.versions) {
			if(Objects.equals(version.versionName, versionName)) {
				return version;
			}
		}
		return null;
	}
	
	/**
	 * Gets the meta.
	 *
	 * @param channel the channel
	 * @return the meta
	 * @throws Exception the exception
	 */
	public static PackMeta getMeta(ChannelItem channel) throws Exception {
		DefaultUI.getDefaultUI().setProgress(Lang.getLang("progress.meta"), -1);
		try (InputStream input=new InflaterInputStream(FileUtil.fetchWithRetry(channel.url,3))){
			return gson.fromJson(FileUtil.readString(input), PackMeta.class);
		}catch(IOException ex) {
			reportNetworkFail();
			throw ex;
		}
	}
	
	/** The is network fail reported. */
	private static boolean isNetworkFailReported;
	
	/**
	 * Report network fail.
	 */
	public static void reportNetworkFail() {
		if(!isNetworkFailReported) {
		DefaultUI.getDefaultUI().message(Lang.getLang("prompt.no-network.title"), Lang.getLang("prompt.no-network.message"));
			isNetworkFailReported=true;
		}
	}
	
	/**
	 * Gets the selected channel.
	 *
	 * @param config the config
	 * @return the selected channel
	 */
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
	
	/**
	 * Create task to update mmc-pack if necessary, it would trigger MultiMC download library next start.
	 *
	 * @param tasks the tasks
	 * @param modpack the modpack
	 * @throws Exception the exception
	 */
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
	
	/**
	 * Creates the backup.
	 *
	 * @param tasks the tasks
	 * @param config the config
	 * @return the file
	 * @throws Exception the exception
	 */
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
	
	/**
	 * Compute modpack update tasks.
	 *
	 * @param config the config
	 * @param tasks the tasks
	 * @param modpack the modpack
	 * @param cached the cached
	 * @throws UpdateNotRequiredException the update not required exception
	 */
	public static void updateModpackTask(LocalConfig config,TaskList tasks,Modpack modpack,Modpack cached) throws UpdateNotRequiredException {
		List<String> ignores=new ArrayList<>();
		if(config.updateIgnores!=null)
			ignores.addAll(config.updateIgnores);
		Set<String> addedFiles=new HashSet<>();
		if(modpack==null) {
			DefaultUI.getDefaultUI().message(Lang.getLang("prompt.not-installed.title"), Lang.getLang("prompt.not-installed.message"));
			exit();
		}
		outer:for(ModPackFile mpf:modpack.files) {//check and add new files
			if(mpf.dist==Dist.client&&!config.isClient)continue;
			if(mpf.dist==Dist.server&&config.isClient)continue;
			addedFiles.add(mpf.file);
			for(String s:ignores) {
				if(mpf.file.startsWith(s))
					continue outer;
			}
			tasks.addTask(new ModPackInstallTask(mpf));
		}
		if(!isEmpty(cached)&&!isEmpty(cached.files)) {//delete old file when deleted in new version
			outer:for(ModPackFile mpf:cached.files) {
				if(!addedFiles.contains(mpf.file)) {
					for(String s:ignores) {
						if(mpf.file.startsWith(s))
							continue outer;
					}
					tasks.addTask(new DeleteOldFileTask(mpf));
				}
			}
		}

	}
	
	/**
	 * add task to update local data.
	 *
	 * @param tasks the tasks
	 * @param data the data
	 * @param modpack the modpack
	 * @param selectedChannel the selected channel
	 */
	public static void updateLocalDataTask(TaskList tasks,LocalData data,Modpack modpack,ChannelItem selectedChannel) {
		data.cachedModpack=modpack;
		data.cachedChannel=selectedChannel.id;
		tasks.addTask(new UpdateLocalDataTask(dataFile, gson.toJson(data)));
	}
	
	/**
	 * Load data.
	 *
	 * @return the local data
	 * @throws Exception the exception
	 */
	public static LocalData loadData() throws Exception {
		if(dataFile.exists()) {
			return gson.fromJson(FileUtil.readString(dataFile), LocalData.class);
		}
		return new LocalData();
	}
	
	/**
	 * Load config.
	 *
	 * @return the local config
	 * @throws Exception the exception
	 */
	public static LocalConfig loadConfig() throws Exception {
		if(configFile.exists()) {
			return gson.fromJson(FileUtil.readString(configFile), LocalConfig.class);
		}
		return new LocalConfig();
	}
	
	/**
	 * Save config.
	 *
	 * @param cfg the cfg
	 * @throws Exception the exception
	 */
	public static void saveConfig(LocalConfig cfg) throws Exception {
		FileUtil.transfer(gson.toJson(cfg), configFile);
	}
	
	/**
	 * Checks if is empty.
	 *
	 * @param obj the obj
	 * @return true, if is empty
	 */
	public static boolean isEmpty(Object obj) {
		if(obj==null)return true;
		if(obj instanceof CharSequence)return ((CharSequence)obj).length()==0;
		if(obj instanceof Collection)return ((Collection)obj).size()==0;
		if(obj instanceof Map)return ((Map)obj).size()==0;
		return false;
	}
	
	/**
	 * Exit.
	 *
	 * @throws UpdateNotRequiredException the update not required exception
	 */
	public static void exit() throws UpdateNotRequiredException {
		
		throw new UpdateNotRequiredException();
	}

}
