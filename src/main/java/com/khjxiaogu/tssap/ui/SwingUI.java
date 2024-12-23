package com.khjxiaogu.tssap.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ListIterator;

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

public class SwingUI implements UI {
	JFrame f = new JFrame("The-Winter-Rescue Installer");
	JProgressBar b;
	Runnable closeAction;
	public SwingUI() throws Exception {
		super();
		init();
	}
	public void init() throws Exception {
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		JPanel p = new JPanel();
		p.setLayout(new BorderLayout(20, 20));
		p.setBorder(new EmptyBorder(15, 15, 15, 15));
		// create a progressbar
		b = new JProgressBar();

		// set initial value
		b.setValue(0);

		b.setAlignmentX(0.5f);
		b.setAlignmentY(0.5f);
		b.setIndeterminate(true);
		b.setStringPainted(false);
		b.setSize(200, 40);
		p.setSize(240, 80);
		// add progressbar
		p.add(b, BorderLayout.CENTER);

		// add panel
		f.add(p);
		f.getContentPane().setPreferredSize(new Dimension(300, 60));
		f.pack();

		f.setMinimumSize(f.getSize());
		f.setLocationRelativeTo(null);
		f.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		f.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				if(confirm(Lang.getLang("prompt.closequery.title"),Lang.getLang("prompt.closequery.message"))) {
					if(closeAction==null)
						System.exit(0);
					else
						closeAction.run();
				}
			}
		});
		//f.setVisible(true);
	}
	@Override
	public String[] getUserOperation(LocalConfig config) {
	    String[] options = new String[] {Lang.getLang("installer.repair"),Lang.getLang("installer.update"),Lang.getLang("installer.set_version")};
	    int response = JOptionPane.showOptionDialog(null, Lang.getLang("installer.hint"), Lang.getLang("installer.title"),
	        JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
	        null, options, options[0]);
		if(response==-1)
			System.exit(0);
		if(response==0)
			return new String[] {"repair"};
		if(response==1)
			return new String[] {"update"};
		
		
		JDialog f2 = new JDialog();
		f2.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		JPanel p = new JPanel();
		p.setBorder(new EmptyBorder(10, 10, 10, 10));

	    final JComboBox<ChannelItem> cb = new JComboBox<>();
	    config.channels.forEach(cb::addItem);
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
		return JOptionPane.showConfirmDialog(f, prompt, title, JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
	}

	@Override
	public void message(String title, String prompt) {
		JOptionPane.showConfirmDialog(f, prompt, title, JOptionPane.DEFAULT_OPTION);
	}

	@Override
	public void setProgress(String content, float value) {
		if(!f.isVisible())f.setVisible(true);
		f.toFront();
		b.setString(content);
		if (value >= 0) {
			b.setStringPainted(true);
			b.setIndeterminate(false);
			b.setValue((int) (value * 100));
		} else {
			if (content == null)
				b.setStringPainted(false);
			else
				b.setStringPainted(true);
			b.setIndeterminate(true);
		}
	}

	@Override
	public void setTitle(String content) {
		f.setTitle(content);
	}
	@Override
	public void setCloseAction(Runnable closeAction) {
		this.closeAction = closeAction;
	}
}
