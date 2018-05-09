package com.pt.net.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.pt.onlinedisk.R;
import com.pt.utils.StreamTool;

import android.os.Parcelable;
import android.util.Log;

public class NetService implements Serializable{
	

	private Socket socket;
	private String uploadpath;
	private static final String ACTION_UPLOAD  = "ACTION_UPLOAD";
	private static final String ACTION_LOAD_LIST = "ACTION_LOAD_LIST";
	private static final String ACTION_CREATE_FOLDER = "ACTION_CREATE_FOLDER";
	private static final String ACTION_DOWNLOAD = "ACTION_DOWNLOAD";
	private static final String ACTION_GET_FILESIZE = "ACTION_GET_FILESIZE";
	private static final String TAG = "NetService";
	private static final String ACTION_GET_INPUTSTREAM = "ACTION_GET_INPUTSTREAM";
	private static final String ACTION_LOGIN = "ACTION_LOGIN";
	private String host ;
	private int port;
	private OutputStream outStream;
	private InputStream inStream;

	private String username;
	public String getUploadpath() {
		return uploadpath;
	}

	public void setUploadpath(String uploadpath) {
		this.uploadpath = uploadpath;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public NetService(String username){
		this.username = username;
		host = "172.16.50.56";
		port = 8787;
	}
	public NetService() {
		host = "172.16.50.56";
		port = 8787;
	}

	public String getUsername() {
		return username;
	}
	public void conn(){
		try {
			socket = new Socket(host, port);
			 outStream = socket.getOutputStream();
			 inStream = socket.getInputStream();
			Log.i(TAG, "�ͻ����Ѿ�����");
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public boolean login(String username,String password){
		try {
			String head= "Action=" + ACTION_LOGIN + ";username=" + username + ";password=" + password + "\r\n";
			outStream.write(head.getBytes());
			outStream.flush();
			String state = readString(inStream);
			Log.i(TAG, "state = " + state);
			if(state.equals("success")){
				return true;
			}else{
				return false;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	
	/**
	 * �ϴ��ļ�
	 * @param upload_path 
	 */
	public boolean upload(String upload_path) {
		if(uploadpath !=null){
			try {
				
				File file = new File(uploadpath);
				String filename = file.getName();
				String head = "Action=" + ACTION_UPLOAD  + ";username=" + username + ";filepath=" + upload_path
				+";Content-length="+ file.length() + ";filename=" + filename + "\r\n";
				//�ͻ��˷��͵ĵ�һ�����ݣ�Action=?;username=?;filepath=?;Content-Length=?;filename=?
				outStream.write(head.getBytes());
				RandomAccessFile accessfile = new RandomAccessFile(file, "r");
				accessfile.seek(0);
				byte[] buffer = new byte[1024];
				int size = 0;
				int len = 0;
				while((len = accessfile.read(buffer))!=-1){
					outStream.write(buffer, 0, len);
					size +=len;
					Log.i(TAG, uploadpath+"�ļ������ϴ�");
				}
				if( size == file.length()){
					return true;
				}
				outStream.flush();
				accessfile.close();
				
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return false;
	}

	/**
	 * �����û��Լ����ļ��б�
	 * @param username		�û���
	 * @return 
	 */
	public List<Map<String, Object>> loadMyList(String dir) {
		List<Map<String,Object>> data = new ArrayList<Map<String,Object>>();
		try {
			Log.i(TAG, "loadMyList");
			String head = "ACTION="+ ACTION_LOAD_LIST  + ";username=" + username + ";dir=" + dir + "\r\n";
			outStream.write(head.getBytes());
			outStream.flush();
			Map<String,Object> item;
			String json = readString(inStream);
			Log.i("BaiduMap",json);
			if(!username.equals(dir)){		//����ǲ���Ŀ¼�Ļ������ڵ�һ����ӷ�����һĿ¼��item
					String info = dir.substring(0, dir.lastIndexOf('/'));	
					Log.i("BaiduMap", "info = "+info);
					item = new HashMap<String, Object>();
					item.put("title", "Back to .." + info);
					item.put("icon", R.drawable.icon_folder);
					item.put("filetype","Directoy");
					item.put("checkTag", false);
					item.put("info", info);
					data.add(item);
			}
			if(!json.equals("folder is null")){
				try {
					//��json�ַ���ת��ΪJSON����
					JSONArray array = new JSONArray(json);
					
					for(int i = 0; i< array.length();i++){
						 item = new HashMap<String, Object>();
						JSONObject object = array.getJSONObject(i);
						//��JSONObject ȡ������
						String filename = object.getString("filename");
						String path = object.getString("path");
						String type = object.getString("type");
						item.put("title", filename);
						item.put("info", dir + "/" + filename);		//���item��ת��·��
						Log.i(TAG, "info = "+ dir +"/"+ filename);
						if(type.equals("folder")){
							item.put("checkTag", false);
							item.put("icon", R.drawable.icon_folder);
							item.put("filetype","Directory");
						}else{
							item.put("checkTag", false);
							item.put("icon", R.drawable.other);
							item.put("filetype","notDirectory");
						}
						data.add(item);
					}
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return data;
	}
	
	private String readString(InputStream inStream) {
		String result = "";
		try {
			BufferedReader input = new BufferedReader(new InputStreamReader(inStream,"UTF-8"));
			result = input.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * ������Ϣ�������������ļ���
	 * @param username		�û���
	 * @param path			�ͻ��˵�ǰ��·��
	 * @param foldername	�ļ��е�����
	 */
	public void createfolder(String path, String foldername) {
		try {
			String head = "ACTION=" + ACTION_CREATE_FOLDER + ";username=" 
					+ username + ";path=" + path + ";foldername=" + foldername + "\r\n";
			outStream.write(head.getBytes());
			outStream.flush();
			Log.i(TAG, "createfolder" + foldername);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public int getFileSize(String downpath) {
		Log.i(TAG, "getFileSize:"+downpath);
		int sumsize = 0;
		try {
			String head = "ACTION=" + ACTION_GET_FILESIZE + ";username="+ username + ";downpath=" + downpath + "\r\n";
			outStream.write(head.getBytes());
			outStream.flush();
			BufferedReader input = new BufferedReader(new InputStreamReader(inStream,"UTF-8"));
			sumsize = Integer.parseInt(input.readLine());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Log.i(TAG, "sumsize:" + sumsize);
		return sumsize;
	}

	/**
	 * �õ���������ָ��·���ļ���ָ������λ��
	 * @param downpath		�ļ�·��
	 * @param startPos		����ʼ��λ��
	 * @param endPos		��������λ��
	 * @return
	 */
	public InputStream getInputStream(String downpath, int startPos, int endPos) {
		try {
			String head = "ACTION=" + ACTION_GET_INPUTSTREAM  + ";username=" 
					+ username + ";downpath=" + downpath + ";startPos=" + startPos + ";endPos=" + endPos + "\r\n";
			Log.i(TAG, "getInputStream():" + head);
			outStream.write(head.getBytes());
			outStream.flush();
			
			PushbackInputStream inStream = new PushbackInputStream(socket.getInputStream());
			
			if(inStream != null){
				Log.i(TAG, "inStream != null");
			}
			String info = StreamTool.readLine(inStream);
			
			Log.i(TAG,"info:"+ info);
			
			if(info.equals("DownThreadStart")){
				Log.i(TAG, "inStream.available:"+inStream.available());
				return inStream;
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * �ر�����
	 */
	public void close(){
		try {
			if(socket!=null){
				outStream.close();
				socket.close();
				Log.i(TAG,"�ͻ�����������Ѿ��Ͽ�");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public boolean isConnected(){
		if(socket != null){
			if(socket.isConnected()){
				return true;
			}else{
				return false;
			}
		}
		return false;
	}


		
}
