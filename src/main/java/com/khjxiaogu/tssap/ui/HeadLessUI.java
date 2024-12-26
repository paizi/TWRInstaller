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
	@Override
	public String[] getUserOperation(LocalConfig config) {
		
		System.out.println(Lang.getLang("installer.title")+":");
		System.out.println(Lang.getLang("installer.hint"));
		System.out.println(Lang.getLang("installer.repair")+"[R]");
		System.out.println(Lang.getLang("installer.update")+"[U]");
		//System.out.println(Lang.getLang("installer.set_version")+"[C]");
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
				}/*if(cp=='c') {
					while(System.in.available()>0)System.in.read();
					break;
				}*/
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
	    	if(item.id.equals(config.selectedChannel))
	    		cb.setSelectedItem(item);
	    }
	    cb.setVisible(true);
	    p.add(cb);
	 
	    
	    final JComboBox<Version> cb2 = new JComboBox<Version>();
		Version latest=new Version(){
			@Override
			public String toString() {
				return Lang.getLang("installer.latest");
			}
			
		};
		latest.versionName="";
	    cb2.setPreferredSize(new Dimension(120,20));
	    cb.setPreferredSize(new Dimension(60,20));
	    cb2.addPopupMenuListener(new BoundsPopupMenuListener(true,true,-1,false));
	    cb.addPopupMenuListener(new BoundsPopupMenuListener(true,true,-1,false));
	    cb2.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if(cb2.getSelectedItem()!=null)
				cb2.setToolTipText(cb2.getSelectedItem().toString());
			}
	    	
	    });
		cb2.setVisible(true);
	    p.add(cb2);
	    
	    cb.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				//if(e.getID()==ItemEvent.SELECTED) {
					cb2.removeAllItems();
					cb2.addItem(latest);
					try {
						PackMeta meta=Main.getMeta((ChannelItem) cb.getSelectedItem());
						Versions vers=Main.fetchVersions(meta);
						ListIterator<Version> li = vers.versions.listIterator(vers.versions.size());

						while (li.hasPrevious()) {
						   cb2.addItem(li.previous());
						}
						f.setVisible(false);

					} catch (Exception e1) {
						LogUtil.addError("Error fetching verion", e1);
					}
				//}
			}
	    	
	    });
		try {
			cb2.addItem(latest);
			PackMeta meta=Main.getMeta((ChannelItem) cb.getSelectedItem());
			Versions vers=Main.fetchVersions(meta);
			
			ListIterator<Version> li = vers.versions.listIterator(vers.versions.size());

			while (li.hasPrevious()) {
			   cb2.addItem(li.previous());
			}
			Version selected=Main.pickVersion(vers, config.selectedVersion);
			cb2.setSelectedItem(selected==null?latest:selected);
			f.setVisible(false);
		} catch (Exception e1) {
			LogUtil.addError("Error fetching verion", e1);
		}
	    JButton button=new JButton(Lang.getLang("installer.confirm"));
	    button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				f2.setVisible(false);
			}
	    	
	    });
	    button.setVisible(true);
	    p.add(button);
	    f2.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
	    	
	    });
	    f2.setTitle(Lang.getLang("installer.set_version"));
	    f2.add(p);
	    f2.getContentPane().setPreferredSize(new Dimension(340, 60));
	    f2.pack();
	    f2.setLocationRelativeTo(null);
	    f2.setModal(true);
	    f2.setVisible(true);
	    return new String[] {"version",((ChannelItem)cb.getSelectedItem()).id,((Version)cb2.getSelectedItem()).versionName};
		
	}

	@Override
	public boolean confirm(String title, String prompt) {
		System.out.println(title+":");
		System.out.println(prompt);
		System.out.println("Input y to continue.");
		System.out.println("Input otherwise to cancel.");
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
		System.out.println("Press Enter to continue.");
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
	@Override
	public void setProgress(String content, float value) {
		if (value >= 0) {
			int nprogress=(int) (value*5);
			if(nprogress!=lastProgress) {
				lastProgress=nprogress;
				printBar(nprogress);
				System.out.println((int) (value*100)+"%");
			}
		} else {
			if (content == null)
				System.out.print("[......]");
			else
				System.out.print("[......]"+content);
			
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
