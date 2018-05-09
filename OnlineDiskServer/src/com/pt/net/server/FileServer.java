package com.pt.net.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileServer {
	
	private int port;				//�˿�
	private ServerSocket server;	//�����׽���
	private ExecutorService service;
	private boolean quit = false; 	//�Ƿ�ֹͣ����ı�־
	public FileServer(int port) {
		this.port = port;
		//�����̳߳أ���CPU*40�����߳���
		service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()*40);
	}
	
	/**
	 * ��ȥservice
	 * @throws IOException
	 */
	public void start() throws IOException {
		server = new ServerSocket(port);
		while(!quit){
			//�����߳�
			Socket socket = server.accept();
			service.execute(new SocketTask(socket));
		}
	}
	
	public void stop(){
		if(service != null)
			quit = true;
	}
	
	public void close() {
		try {
			server.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
