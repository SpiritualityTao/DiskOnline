package com.pt.net.download;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.pt.net.service.FileDao;
import com.pt.net.service.NetService;

import android.content.Context;
import android.util.Log;

public class FileDownloader {
	
	private static final String TAG = "DownloadTask";

	private Context context;
	
	private boolean exit;			//ֹͣ����
	
	/* �������ļ����� */
	private int downloadSize = 0;

	//������̵߳����س���
	private Map<Integer, Integer> data = new ConcurrentHashMap<Integer, Integer>(); 
	
	private NetService service; 	//������� ����ȡ������
	
	private String downpath;		//�����ļ���·��
	
	private int FileSize ;          //�ļ���С
	
	private File fileSaveDir;		//�����ļ���Ŀ¼
	
	private File saveFile;			//������ļ�·��
	
	private FileDao dao;			//�������ݿ�
	
	private String filename; 		//�ļ���
		
	private int threadNum;			//�߳���Ŀ
	
	private int block ;				//ÿ���߳����صĴ�С
	
	private DownloadThread[] threads;	//�����߳�����
	
	public FileDownloader( Context context, NetService service, String downpath,File saveFile){
		this.context = context;
		this.service = service;
		this.downpath = downpath;
		this.saveFile = saveFile;
		dao = new FileDao(context);
		String savePath = saveFile.getAbsolutePath();
		String saveDir = savePath.substring(0, savePath.lastIndexOf('/'));
		Log.i(TAG, "saveDir="+saveDir);
		fileSaveDir = new File(saveDir);
		dao = new FileDao(context);
		FileSize = service.getFileSize(downpath);
		service.close();
		filename = savePath.substring(savePath.lastIndexOf('/') + 1);
		if (this.FileSize <= 0) throw new RuntimeException("Unkown file size ");
		
		initDownThread();
	}

	public void initDownThread() {
		Log.i( TAG, "initDownThread()");
		setThreadNum(3);		//���ÿ����̵߳�����
		if(!fileSaveDir.exists()) {
			fileSaveDir.mkdirs();		//����Ŀ¼
			Log.i(TAG, fileSaveDir.getAbsolutePath() + "·��Ŀ¼�ѹ���...");
		}
		this.threads = new DownloadThread[threadNum];		
		Log.i(TAG, "����·����"+downpath);
		Map<Integer,Integer> logdata = dao.getData(downpath);	//��ȡ���ؼ�¼
		if(logdata.size() > 0){		//����м�¼
			for(Map.Entry<Integer, Integer> entry : logdata.entrySet()){
				data.put(entry.getKey(), entry.getValue());
			}
		}
		if(data.size() == threads.length){		
			for (int i = 0; i < this.threads.length; i++) {
				this.downloadSize += this.data.get(i+1);
			}
			Log.i(TAG, "�Ѿ����صĳ���"+ this.downloadSize);
		}
		Log.i(TAG, "public void initDownThread() ");
		block = FileSize % threads.length == 0 ? FileSize / threads.length : FileSize / threads.length + 1 ;
		
	}

	public int download(DownloadProgressListener listener){
		try {
			RandomAccessFile randout = new RandomAccessFile(saveFile, "rwd");
			//����Ҫ�����ļ��Ĵ�С
			randout.setLength(FileSize);
			randout.close();
			Log.i(TAG, "flag = 1");
			if(data.size() != threads.length){	//û�����ؼ�¼�������̲߳�һ��
				//����
				data.clear();
				for(int i = 0; i < this.threads.length; i++){
					data.put( i+1 , 0);  	//	�߳�id��1��ʼ
				}
				downloadSize = 0;
				Log.i(TAG, "flag = 2");
			}
			
			for(int i = 0; i < threads.length; i++){
				int downLength = data.get(i+1);		//ȡ�ó���
				if(downLength < this.block && this.downloadSize<this.FileSize){		//���߳�û��������
					threads[i] = new DownloadThread(this,downpath,saveFile,block,data.get(i+1),i+1);
					threads[i].setPriority(7);
					threads[i].start();
				}else{
					threads[i] = null;
				}
			}
			
			dao.delete(downpath);				//�����
			dao.save(downpath, data);		//������̵߳�����
			
			boolean notFinish = true;
			while(notFinish){		
				Thread.sleep(1000);		//ÿһ���м�������Ƿ����
				notFinish = false;
				for(int i = 0; i< threads.length;i++){
					if (this.threads[i] != null && !this.threads[i].isFinish()) {//��������߳�δ�������
						Log.i(TAG, "notFinish()=true");
						notFinish = true;//���ñ�־Ϊ����û�����
						if(this.threads[i].getDownLength() == -1){	//�������ʧ��,����������
							this.threads[i] = new DownloadThread(this, downpath, this.saveFile, this.block, this.data.get(i+1), i+1);
							this.threads[i].setPriority(7);
							this.threads[i].start();
						}
					}
				}
				if(listener != null)	listener.onDownloadSize(downloadSize);	//��MainActivity�н��ܣ����½�����
			}
			if(downloadSize >= this.FileSize - threads.length) dao.delete(downpath);//�������ɾ����¼
		} catch (Exception e){
			try {
				throw new Exception("file download error");
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}finally{
			service.close();
		}
		return downloadSize;
	}
	
	public void setFileSize(int fileSize) {
		FileSize = fileSize;
	}

	public boolean isExit() {
		return this.exit;
	}


	public void exit() {
		this.exit = true;
	}

	public String getUserName(){
		return service.getUsername();
	}
	public void setThreadNum(int threadNum) {
		this.threadNum = threadNum;
	}


	/**
	 * ����ָ���߳�������ص�λ��
	 * @param threadId �߳�id
	 * @param pos ������ص�λ��
	 */
	protected synchronized void update(int threadId, int pos) {
		this.data.put(threadId, pos);
		this.dao.update(this.downpath, threadId, pos);
	}

	/**
	 * �ۼ������ش�С
	 * @param size
	 */
	protected synchronized void append(int size) {
		downloadSize += size;
	}

	
	
	public int getDownloadSize() {
		return downloadSize;
	}

	public void setDownloadSize(int downloadSize) {
		this.downloadSize = downloadSize;
	}

	public int getFileSize() {
		return FileSize;
	}

	public int getThreadNum() {
		return threads.length;
	}

	

	
}


