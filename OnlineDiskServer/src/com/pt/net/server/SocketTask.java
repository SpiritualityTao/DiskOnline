package com.pt.net.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.PushbackInputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.nio.file.FileAlreadyExistsException;
import java.util.Properties;

import javax.annotation.Resource;


import com.pt.entity.UploadFile;
import com.pt.utils.StreamTool;
import com.pt.utils.UserDao;

public class SocketTask implements Runnable {

	public static final String ACTION_UPLOAD = "ACTION_UPLOAD";
	public static final String ACTION_DOWNLOAD = "ACTION_DOWNLOAD";
	public static final String ACTION_LOGIN = "ACTION_LOGIN";
	private static final String ACTION_LOAD_LIST = "ACTION_LOAD_LIST";
	private static final String ACTION_CREATE_FOLDER = "ACTION_CREATE_FOLDER";
	private static final String ACTION_GET_FILESIZE = "ACTION_GET_FILESIZE";
	private static final String ACTION_GET_INPUTSTREAM = "ACTION_GET_INPUTSTREAM";
	private Socket socket = null;
	private boolean quit = false;
	
	public SocketTask(Socket socket) {
		this.socket = socket;
	}

	@Override
	public void run() {
		try {
			System.out.println( "accept connection " + socket.getInetAddress() + ":" + socket.getPort());
			while(!quit ){
				
				PushbackInputStream inStream = new PushbackInputStream(socket.getInputStream());
				//得到客户端发来的第一行协议数据：Action=?;username=?;filepath=?;Content-Length=?;filename=?
				//如果用户初次上传文件，sourceid的值为空。
				String head = StreamTool.readLine(inStream);
				if(head != null ){
					 System.out.println(head);
					//以逗号为基准分段
					String item[] = head.split(";");
					//取出对应的值  
					String Action = item[0].substring(item[0].indexOf("=")+1);
					String username = item[1].substring(item[1].indexOf("=")+1);
					//根据Action  判断是上传还是下载操作
					switch (Action) {
					
					case ACTION_LOAD_LIST:
						String dir = item[2].substring(item[2].indexOf("=")+1);
						loadUserList(username,dir);
						break;
						
					case ACTION_LOGIN:
						
						String password = item[2].substring(item[2].indexOf("=")+1);
						login(username,password);
						break;
						
					case ACTION_CREATE_FOLDER:
						String path = item[2].substring(item[2].indexOf("=")+1);
						String foldername = item[3].substring(item[3].indexOf("=")+1);
						createfolder(username,path,foldername);
						break;
						
					case ACTION_UPLOAD:
						String filepath = item[2].substring(item[2].indexOf("=")+1);
						String filelength = item[3].substring(item[3].indexOf("=")+1);
						String filename = item[4].substring(item[4].indexOf("=")+1);
						System.out.println("upload()");
						UploadFile uploadFile = new UploadFile();
						uploadFile.setFilelength(Integer.valueOf(filelength));
						uploadFile.setFilename(filename);
						uploadFile.setFilepath(filepath);
						upload(username,uploadFile,inStream);
						break;
						
					case ACTION_GET_FILESIZE:
						String lujing = item[2].substring(item[2].indexOf("=")+1);
						getfileSize(lujing);
						break;
					case ACTION_DOWNLOAD:
						String downpath = item[2].substring(item[2].indexOf("=")+1);
						download(downpath);
						break;
						
					case ACTION_GET_INPUTSTREAM:
						String inStreamPath = item[2].substring(item[2].indexOf("=")+1);
						int startPos = Integer.valueOf(item[3].substring(item[3].indexOf("=")+1));
						int endPos = Integer.valueOf(item[4].substring(item[4].indexOf("=")+1));
						getInputStream(inStreamPath,startPos,endPos);
						break;
					}
					head = "";
					//读完流判断是否客户端已经断开连接
					socket.sendUrgentData(0);
				}
			}
		} catch (IOException e) {
			//客户端已经断开，则服务器也断开与客户端的连接
			close();
		}
	}

	private void getInputStream(String inStreamPath, int startPos, int endPos) {
		try {
			OutputStream outStream = socket.getOutputStream();
			RandomAccessFile accessfile = new RandomAccessFile(new File(inStreamPath), "r");
			if(accessfile != null){
				outStream.write("DownThreadStart\r\n".getBytes());
			}
			accessfile.skipBytes(startPos);
			System.out.println("当前偏移量:"+accessfile.getFilePointer());
			byte[] buffer = new byte[endPos - startPos];
			accessfile.read(buffer,0,endPos - startPos);
			System.out.println("当前偏移量:" + accessfile.getFilePointer());
			outStream.write(buffer);
			outStream.flush();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void getfileSize(String downpath) {
		try {
			
			File file = new File(downpath);
			int filesize = 0 ;
			if(file.exists()){
				filesize = (int) file.length();
				System.out.println(filesize);
			}
			OutputStream outStream = socket.getOutputStream();
			PrintWriter output = new PrintWriter(new OutputStreamWriter(outStream,"UTF-8"),true);
			System.out.println("getfileSize");
			output.write(String.valueOf(filesize)+"\r\n");
			output.flush();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void download(String downpath) {
		File file = new File(downpath);
	}

	private void close() {
		if(socket!=null && !socket.isClosed()){
			try {
				quit = true;
				socket.close();
				System.out.println("服务器已断开与"+ socket.getInetAddress() + ":" + socket.getPort()+"的连接");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * 加载客户端文件列表，并以JSON格式发送给客户端 
	 * @param username
	 */
	private void loadUserList(String username,String dir) {
		
		String json = ConstructJson(dir);
		System.out.println(json);
		OutputStream outStream;
		try {
			outStream = socket.getOutputStream();
			PrintWriter output = new PrintWriter(new OutputStreamWriter(outStream,"UTF-8"),true);
			output.write(json);
			output.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String ConstructJson(String dir) {
		File file = new File(dir);
		File[] fileitem = file.listFiles();
		if(fileitem != null && fileitem.length > 0){
			//JSON 数据格式     [{path:"pengtao/haha",type:"folder"},{path:"pengtao/hehe",type:"file"}]
			StringBuilder sb = new StringBuilder();
			sb.append('[');
			for(int i = 0 ; i < fileitem.length;i++ ){
				String filename = fileitem[i].getName();
				String path = dir + "/" + filename;
				String type = null;
				if(fileitem[i].isDirectory()){
					type = "folder";
				}else{
					type = "file";
				}
				sb.append('{');
				sb.append("\"filename\":\"").append(filename).append("\",");
				sb.append("\"path\":\"").append(path).append("\",");
				sb.append("\"type\":\"").append(type).append("\"");
				sb.append("},");
			}
			sb.deleteCharAt(sb.length() - 1);
			sb.append(']');
			sb.append("\r\n");
			return sb.toString();
		}else{
			return "folder is null" + "\r\n";
		}
	}

	/**
	 * 为客户端创建文件夹
	 * @param username			用户名
	 * @param path				路径
	 * @param foldername		文件夹名字
	 */
	private void createfolder(String username, String path, String foldername) {
		File dir = new File(path,foldername);
		System.out.println("正在创建文件夹");
		if( !dir.exists() )
			dir.mkdirs();
		System.out.println("文件路径"+ path + "/" + foldername);
	}

	private void login(String username, String password) {
		try {
			String state ;
			OutputStream outStream = socket.getOutputStream();
			PrintWriter output = new PrintWriter(new OutputStreamWriter(outStream,"UTF-8"),true);
			if(UserDao.isLoginSuccess(username, password, 1)){
				state = "success\r\n";
				output.write(state);
				output.flush();
			}else{
				state = "failed\r\n";
				output.write(state);
				output.flush();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	/**
	 * 文件上传
	 * @param username		上传的用户名
	 * @param uploadFile	上传文件信息
	 * @param inStream		客户端的传输数据
	 * @throws FileAlreadyExistsException
	 */
	private void upload(String username, UploadFile uploadFile, PushbackInputStream inStream) throws FileAlreadyExistsException {
		File file = null;
		String filename = uploadFile.getFilename();
		//服务器端文件存储的形式 = 用户名 + 客户端
		File dir = new File(uploadFile.getFilepath());
		if(!dir.exists())	dir.mkdirs();
		//文件存储在用户客户相应得目录下
		file = new File(dir, filename);
		try {
			//传入客户端的输出流
			OutputStream outStream = socket.getOutputStream();
			if(file.exists()){
				//如果文件存在则抛出异常
				String existfile = "对不起，文件已经存在,上传失败";
			}
			//文件上传的起始位置
			int position = 0;
			//断点上传的记录文件
			File filelog = new File(dir,  filename.substring(0, filename.indexOf("."))+"log");
			
			if(filelog.exists()){   //最近一次没有上传完
				Properties properties = new Properties();
				properties.load(new FileInputStream(filelog));
				//读取最近一次记录的上传长度
				position = Integer.valueOf(properties.getProperty("length"));
			}
			
			//写入文件的输入流
			RandomAccessFile accessFile = new RandomAccessFile(file, "rwd");
			if(position == 0)
				accessFile.setLength(uploadFile.getFilelength());	//设置文件长度
			//设置文件传输的位置
			accessFile.seek(position);
			int len = 0 ;
			int length = position;   //得到实时的已完成文件的长度
			byte[] buffer = new byte[1024];
			//从输入流中读取字节进buffer
			while((len=inStream.read(buffer)) != -1){
				//从读取的buffer中accessFile
				accessFile.write(buffer, 0, len);
				length += len;
				Properties properties = new Properties();
				properties.put("length", String.valueOf(length));
				FileOutputStream logFile = new FileOutputStream(new File(file.getParentFile(), file.getName()+".log"));
				properties.store(logFile, null);//实时记录已经接收的文件长度
				
				logFile.close();
			}
			if(length==accessFile.length()){
				File logFile = new File(file.getParentFile(), file.getName()+".log");
				logFile.delete();
			}
			
			outStream.flush();
			inStream.close();
			accessFile.close();
			outStream.close();
			file = null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	


}
