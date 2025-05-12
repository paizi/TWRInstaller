# TWR Installer
A standalone general purpose modpack versioning and downloading tool
## End User Usage
This is a simple guide for end users.
### Update on game launch
You may add jvm argument: -javaagent=twr-installer.jar in your game launcher or server command line to make it start evert launch.
### Repair Broken Files
1. Run installer.bat/installer.sh it generated.
2. Choose `Repair`.
3. Wait for it verify all files and repair
### Disabling automatic update
1. Run installer.bat/installer.sh it generated.
2. Choose `Select Version` then select a version you would like to stay in.
3. It won't update to any other version anymore.
### Server setup
1. Modify tssap-configs/config.json and set `isClient` to false.
2. Copy jar file, `tssap-configs/config.json` but not `tssap-configs/data.json` to your server folder.
3. Run the application.
4. Choose `Update` to download all files.
### Run in CLI/Headless
run `java -jar twr-installer.jar headless` to run in command line mode.
## Translating
Translation files located in [src/main/resources/com/khjxiaogu/tssap](https://github.com/TeamMoegMC/TWRInstaller/tree/master/src/main/resources/com/khjxiaogu/tssap), create a file named your locale code to add translation.
## Developer Usage
1. Place it in the minecraft folder or ship it in overrides folder.
2. prepare configuration files in `tssap-configs` folder besides it, scheme:
```
{
  "channels": [
    {
      "name": "<name of the branch>",
      "id": "<unique id of the branch>",
      "url": "<meta file url>"
    }
  ],
  "selectedChannel": "<id of the default selected branch>",
  "selectedVersion": "<version within the selected branch, blank for automatic update>",
  "backupIncludes": [
    "<paths starts with to backup when overwritten>",
    "config/"
  ],
  "isClient": <boolean to indicate this is for client or dedicated server>
}
```
3. Add jvm argument: -javaagent=twr-installer.jar in your game launcher to run it and checks for update every single game launch.
## Protocol Scheme
All protocol file json must be compressed with deflate.
### Modpack meta
```
{
  "versionsPath": "<full url path to version list file>",
  "latestVersion": <version scheme>
}
```
### Version list
```
{
  "versions": [<array of version scheme>]
}
```
### Version scheme
```
{
  "versionName": "<name of the version>",
	"versionDate": "<publish utc date in yyyy-MM-dd HH:mm:ss>",
	"packFilePath": "<full url path to modpack scheme>",
	"changelogPath": "<changelog full url path>"
}
```
### Modpack scheme
```
{
	"files": [<array of file scheme>],
	"version": "<version name>",
	"libraries": {"<library uid>":"<library version>"}
}
```
Note that libraries are library uid and version for Multimc automatic updating the mod loader or game version if necessary.
### File scheme
```
{
	"file": "<relative path to file in modpack, like config/frostedheart-client.toml>",
	"hash": "<SHA256 hash for file ignoring byte 09,0A,0D,20>",
	"link": "<Full download link for the file>",
	"size": <file size in byte to show correct progress when download>,
	"dist": "<client or server to mark the side for the file>",
	"compressed": <true if the file is deflated>
}
```
## Building
just run `maven install` and all is done.