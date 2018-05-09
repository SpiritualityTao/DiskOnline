package com.pt.net.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileServer {
	
	private int port;				//端口
	private ServerSocket server;	//服务套接字
	private ExecutorService service;
	private boolean quit = false; 	//是否停止服务的标志
	public FileServer(int port) {
		this.port = port;
		//开启线程池，（CPU*40）个线程数
		service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()*40);
	}
	
	/**
	 * 开去service
	 * @throws IOException
	 */
	public void start() throws IOException {
		server = new ServerSocket(port);
		while(!quit){
			//阻塞线程
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
