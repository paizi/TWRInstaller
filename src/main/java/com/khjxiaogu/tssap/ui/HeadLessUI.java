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
package com.khjxiaogu.tssap.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Scanner;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;

import com.khjxiaogu.tssap.Main;
import com.khjxiaogu.tssap.entity.ChannelItem;
import com.khjxiaogu.tssap.entity.LocalConfig;
import com.khjxiaogu.tssap.entity.PackMeta;
import com.khjxiaogu.tssap.entity.Version;
import com.khjxiaogu.tssap.entity.Versions;
import com.khjxiaogu.tssap.util.LogUtil;
import com.khjxiaogu.tssap.util.ShutdownHandler;

public class HeadLessUI implements UI {
	Runnable closeAction;
	Thread shutdownHook=new Thread(()->{
		if(!ShutdownHandler.isNormally)
			if(closeAction!=null) {
				closeAction.run();
				while(!ShutdownHandler.isNormally) try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
	});
	public HeadLessUI() throws Exception {
		super();
		init();
	}
	public void init() throws Exception {
		Runtime.getRuntime().addShutdownHook(shutdownHook);
		//f.setVisible(true);
	}
	@Override
	public boolean shouldExitImmediate() {
		return true;
	}
	private static String readline() throws IOException {
		String rslt="";
		Scanner sc=new Scanner(System.in);
		rslt=sc.nextLine();
		
		
		while(System.in.available()>0)System.in.read();
		return rslt;
	}
	@Override
	public String[] getUserOperation(LocalConfig config) throws Exception {
		
		System.out.println(Lang.getLang("installer.title")+":");
		System.out.println(Lang.getLang("installer.hint"));
		System.out.println(Lang.getLang("installer.repair")+"[R]");
		System.out.println(Lang.getLang("installer.update")+"[U]");
		System.out.println(Lang.getLang("installer.set_version")+"[C]");
		try {
			while(true) {
				int ch=System.in.read();
				int cp=Character.toLowerCase(ch);
				if(cp=='r') {
					while(System.in.available()>0)System.in.read();
					return new String[] {"repair"};
				}if(cp=='u') {
					while(System.in.available()>0)System.in.read();
					return new String[] {"update"};
				}if(cp=='c') {
					while(System.in.available()>0)System.in.read();
					break;
				}
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}

		
		
		JDialog f2 = new JDialog();
		
		
		System.out.println(Lang.getLang("installer.header.channels"));
	    for(ChannelItem item:config.channels) {
	    	
	    	System.out.println(item.name+":"+item.id);
	    }
	    
	    for(ChannelItem item:config.channels) {
	    	if(item.id.equals(config.selectedChannel)) {
	    		System.out.println(Lang.getLang("installer.header.current",item.id));
	    		break;
	    	}
	    }
	    outer:while(true) {
	    	System.out.println(Lang.getLang("installer.header.channels.prompt"));
		    config.selectedChannel=readline().trim();
		    if(config.selectedChannel.isEmpty())
		    	break outer;
		    for(ChannelItem item:config.channels) {
		    	if(item.id.equals(config.selectedChannel))
		    		break outer;
		    }
		    System.out.println(Lang.getLang("installer.header.channels.error"));
	    }
	    PackMeta meta=Main.getMeta(Main.getSelectedChannel(config));
		Versions vers=Main.fetchVersions(meta);
		System.out.println(Lang.getLang("installer.header.version.list"));
		for(Version item:vers.versions) {
	    	System.out.println(item.versionName);
	    }
		System.out.println(Lang.getLang("installer.header.current",config.selectedVersion));
	    outer:while(true) {
	    	System.out.println(Lang.getLang("installer.header.version.prompt"));
		    config.selectedVersion=readline().trim();
		    if(config.selectedVersion.isEmpty())
		    	break outer;
		    for(Version item:vers.versions) {
		    	if(item.versionName.equals(config.selectedVersion))
		    		break outer;
		    }
		    System.out.println(Lang.getLang("installer.header.version.error"));
	    }
	 
	    return new String[] {"version",config.selectedChannel,config.selectedVersion};
		
	}

	@Override
	public boolean confirm(String title, String prompt) {
		System.out.println(title+":");
		System.out.println(prompt);
		System.out.println(Lang.getLang("installer.header.confirm"));
		try {
			int b = System.in.read();
			boolean rslt=false;
			if(b>0) {
				rslt=b=='y'||b=='Y';
			}
			while(System.in.available()>0)System.in.read();
			return rslt;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public void message(String title, String prompt) {
		System.out.println(title+":");
		System.out.println(prompt);
		System.out.println(Lang.getLang("installer.header.message"));
		try {
			System.in.read();
			while(System.in.available()>0)System.in.read();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	int lastProgress=-1;
	private void printBar(int num) {
		System.out.print("[");
		for(int i=0;i<20;i++) {
			if(i==num)
				System.out.print(">");
			else if(i<num)
				System.out.print("=");
			else
				System.out.print("-");
		}
		System.out.print("]");
	}
	String lastContent;
	@Override
	public void setProgress(String content, float value) {
		if (value >= 0) {
			int nprogress=(int) (value*20);
			if(nprogress!=lastProgress) {
				lastProgress=nprogress;
				printBar(nprogress);
				System.out.println((int) (value*100)+"%");
			}
		} else {
			if (content == null)
				System.out.println("[......]");
			else if(!content.equals(lastContent)) {
				lastContent=content;
				System.out.println("[......]"+content);
			}
			
		}
	}

	@Override
	public void setTitle(String content) {
		System.out.println("======================================");
		System.out.println(content);
		System.out.println("======================================");
	}
	@Override
	public void setCloseAction(Runnable closeAction) {
		this.closeAction = closeAction;
	}
}
