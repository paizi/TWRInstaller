package com.khjxiaogu.tssap.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

public class SwingUI implements UI {
	JFrame f = new JFrame("TSSAP bootstrap");
	JProgressBar b;
	public SwingUI() throws Exception {
		super();
		init();
	}
	public void init() throws Exception {
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        JPanel p = new JPanel();
        p.setLayout(new BorderLayout(20,20));
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
        p.setSize(240,80);
        // add progressbar
        p.add(b,BorderLayout.CENTER);
 
        // add panel
        f.add(p);
        f.getContentPane().setPreferredSize(new Dimension(300, 60));
        f.pack();
        
        f.setMinimumSize(f.getSize());
        f.setLocationRelativeTo(null);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setVisible(true);
	}
	@Override
	public boolean confirm(String title,String prompt) {
		return JOptionPane.showConfirmDialog(f, prompt,title,JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION;
	}
	@Override
	public void message(String title,String prompt) {
		JOptionPane.showConfirmDialog(f, prompt,title,JOptionPane.OK_OPTION);
	}
	@Override
	public void setProgress(String content,float value) {
		b.setString(content);
		if(value>=0) {
			b.setStringPainted(true);
			b.setIndeterminate(false);
			b.setValue((int) (value*100));
		}else {
			if(content==null)
				b.setStringPainted(false);
			b.setIndeterminate(true);
		}
	}
}
