package com.khjxiaogu.tssap.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

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
		f.setVisible(true);
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
	    cb.setVisible(true);
	    p.add(cb);
	 
	    
	    final JComboBox<Version> cb2 = new JComboBox<Version>();

	    cb2.setVisible(true);
	    p.add(cb2);
	    cb.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if(e.getID()==ItemEvent.SELECTED) {
					cb2.removeAllItems();
					try {
						PackMeta meta=Main.getMeta((ChannelItem) cb.getSelectedItem());
						Versions vers=Main.fetchVersions(meta);
						vers.versions.forEach(cb2::addItem);

					} catch (Exception e1) {
						LogUtil.addError("Error fetching verion", e1);
					}
				}
			}
	    	
	    });
		try {
			PackMeta meta=Main.getMeta((ChannelItem) cb.getSelectedItem());
			Versions vers=Main.fetchVersions(meta);
			vers.versions.forEach(cb2::addItem);

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
	    f2.getContentPane().setPreferredSize(new Dimension(300, 40));
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
