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
				//�õ��ͻ��˷����ĵ�һ��Э�����ݣ�Action=?;username=?;filepath=?;Content-Length=?;filename=?
				//����û������ϴ��ļ���sourceid��ֵΪ�ա�
				String head = StreamTool.readLine(inStream);
				if(head != null ){
					 System.out.println(head);
					//�Զ���Ϊ��׼�ֶ�
					String item[] = head.split(";");
					//ȡ����Ӧ��ֵ  
					String Action = item[0].substring(item[0].indexOf("=")+1);
					String username = item[1].substring(item[1].indexOf("=")+1);
					//����Action  �ж����ϴ��������ز���
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
					//�������ж��Ƿ�ͻ����Ѿ��Ͽ�����
					socket.sendUrgentData(0);
				}
			}
		} catch (IOException e) {
			//�ͻ����Ѿ��Ͽ����������Ҳ�Ͽ���ͻ��˵�����
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
			System.out.println("��ǰƫ����:"+accessfile.getFilePointer());
			byte[] buffer = new byte[endPos - startPos];
			accessfile.read(buffer,0,endPos - startPos);
			System.out.println("��ǰƫ����:" + accessfile.getFilePointer());
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
				System.out.println("�������ѶϿ���"+ socket.getInetAddress() + ":" + socket.getPort()+"������");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * ���ؿͻ����ļ��б�����JSON��ʽ���͸��ͻ��� 
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
			//JSON ���ݸ�ʽ     [{path:"pengtao/haha",type:"folder"},{path:"pengtao/hehe",type:"file"}]
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
	 * Ϊ�ͻ��˴����ļ���
	 * @param username			�û���
	 * @param path				·��
	 * @param foldername		�ļ�������
	 */
	private void createfolder(String username, String path, String foldername) {
		File dir = new File(path,foldername);
		System.out.println("���ڴ����ļ���");
		if( !dir.exists() )
			dir.mkdirs();
		System.out.println("�ļ�·��"+ path + "/" + foldername);
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
	 * �ļ��ϴ�
	 * @param username		�ϴ����û���
	 * @param uploadFile	�ϴ��ļ���Ϣ
	 * @param inStream		�ͻ��˵Ĵ�������
	 * @throws FileAlreadyExistsException
	 */
	private void upload(String username, UploadFile uploadFile, PushbackInputStream inStream) throws FileAlreadyExistsException {
		File file = null;
		String filename = uploadFile.getFilename();
		//���������ļ��洢����ʽ = �û��� + �ͻ���
		File dir = new File(uploadFile.getFilepath());
		if(!dir.exists())	dir.mkdirs();
		//�ļ��洢���û��ͻ���Ӧ��Ŀ¼��
		file = new File(dir, filename);
		try {
			//����ͻ��˵������
			OutputStream outStream = socket.getOutputStream();
			if(file.exists()){
				//����ļ��������׳��쳣
				String existfile = "�Բ����ļ��Ѿ�����,�ϴ�ʧ��";
			}
			//�ļ��ϴ�����ʼλ��
			int position = 0;
			//�ϵ��ϴ��ļ�¼�ļ�
			File filelog = new File(dir,  filename.substring(0, filename.indexOf("."))+"log");
			
			if(filelog.exists()){   //���һ��û���ϴ���
				Properties properties = new Properties();
				properties.load(new FileInputStream(filelog));
				//��ȡ���һ�μ�¼���ϴ�����
				position = Integer.valueOf(properties.getProperty("length"));
			}
			
			//д���ļ���������
			RandomAccessFile accessFile = new RandomAccessFile(file, "rwd");
			if(position == 0)
				accessFile.setLength(uploadFile.getFilelength());	//�����ļ�����
			//�����ļ������λ��
			accessFile.seek(position);
			int len = 0 ;
			int length = position;   //�õ�ʵʱ��������ļ��ĳ���
			byte[] buffer = new byte[1024];
			//���������ж�ȡ�ֽڽ�buffer
			while((len=inStream.read(buffer)) != -1){
				//�Ӷ�ȡ��buffer��accessFile
				accessFile.write(buffer, 0, len);
				length += len;
				Properties properties = new Properties();
				properties.put("length", String.valueOf(length));
				FileOutputStream logFile = new FileOutputStream(new File(file.getParentFile(), file.getName()+".log"));
				properties.store(logFile, null);//ʵʱ��¼�Ѿ����յ��ļ�����
				
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
