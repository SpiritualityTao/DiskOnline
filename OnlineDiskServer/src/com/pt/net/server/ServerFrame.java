package com.pt.net.server;

import java.awt.BorderLayout;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JLabel;

/**
 * 网盘服务器端界面
 * @author asus
 *
 */
public class ServerFrame extends JFrame{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 5808303999244779308L;
	private JLabel label;
	private FileServer server = new FileServer(8787);
	
	public ServerFrame(String title){
		super(title);
	}
	
	private void init() {
		label = new JLabel("网盘服务端已启动......");
		add(label, BorderLayout.PAGE_START);
		setDefaultCloseOperation(EXIT_ON_CLOSE );
		addWindowListener(new WindowListener() {
			
			@Override
			public void windowOpened(WindowEvent e) {
				new Thread(new Runnable() {
					
					@Override
					public void run() {
						try {
							server.start();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}).start();
			}
			
			@Override
			public void windowIconified(WindowEvent e) {}
			
			@Override
			public void windowDeiconified(WindowEvent e) {}
			
			@Override
			public void windowDeactivated(WindowEvent e) {}
			
			@Override
			public void windowClosing(WindowEvent e) {
			}
			
			@Override
			public void windowClosed(WindowEvent e) {
				server.close();
			}
			
			@Override
			public void windowActivated(WindowEvent e) {}
		});
	}

	public static void main(String[] args) {
		ServerFrame frame= new ServerFrame("网盘服务端 ");
		frame.init();
		frame.setSize(300, 300);
		frame.setVisible(true);
	}

}
