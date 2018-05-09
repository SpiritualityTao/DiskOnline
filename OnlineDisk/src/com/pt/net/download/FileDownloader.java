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
	
	private boolean exit;			//停止下载
	
	/* 已下载文件长度 */
	private int downloadSize = 0;

	//缓存各线程的下载长度
	private Map<Integer, Integer> data = new ConcurrentHashMap<Integer, Integer>(); 
	
	private NetService service; 	//网络服务 ，获取输入流
	
	private String downpath;		//下载文件的路径
	
	private int FileSize ;          //文件大小
	
	private File fileSaveDir;		//保存文件的目录
	
	private File saveFile;			//保存的文件路径
	
	private FileDao dao;			//操作数据库
	
	private String filename; 		//文件名
		
	private int threadNum;			//线程数目
	
	private int block ;				//每条线程下载的大小
	
	private DownloadThread[] threads;	//下载线程数组
	
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
		setThreadNum(3);		//设置开启线程的数量
		if(!fileSaveDir.exists()) {
			fileSaveDir.mkdirs();		//构建目录
			Log.i(TAG, fileSaveDir.getAbsolutePath() + "路径目录已构建...");
		}
		this.threads = new DownloadThread[threadNum];		
		Log.i(TAG, "下载路径："+downpath);
		Map<Integer,Integer> logdata = dao.getData(downpath);	//获取下载记录
		if(logdata.size() > 0){		//如果有记录
			for(Map.Entry<Integer, Integer> entry : logdata.entrySet()){
				data.put(entry.getKey(), entry.getValue());
			}
		}
		if(data.size() == threads.length){		
			for (int i = 0; i < this.threads.length; i++) {
				this.downloadSize += this.data.get(i+1);
			}
			Log.i(TAG, "已经下载的长度"+ this.downloadSize);
		}
		Log.i(TAG, "public void initDownThread() ");
		block = FileSize % threads.length == 0 ? FileSize / threads.length : FileSize / threads.length + 1 ;
		
	}

	public int download(DownloadProgressListener listener){
		try {
			RandomAccessFile randout = new RandomAccessFile(saveFile, "rwd");
			//设置要保存文件的大小
			randout.setLength(FileSize);
			randout.close();
			Log.i(TAG, "flag = 1");
			if(data.size() != threads.length){	//没有下载记录或下载线程不一样
				//重置
				data.clear();
				for(int i = 0; i < this.threads.length; i++){
					data.put( i+1 , 0);  	//	线程id从1开始
				}
				downloadSize = 0;
				Log.i(TAG, "flag = 2");
			}
			
			for(int i = 0; i < threads.length; i++){
				int downLength = data.get(i+1);		//取得长度
				if(downLength < this.block && this.downloadSize<this.FileSize){		//该线程没有下载完
					threads[i] = new DownloadThread(this,downpath,saveFile,block,data.get(i+1),i+1);
					threads[i].setPriority(7);
					threads[i].start();
				}else{
					threads[i] = null;
				}
			}
			
			dao.delete(downpath);				//先清除
			dao.save(downpath, data);		//插入各线程的数据
			
			boolean notFinish = true;
			while(notFinish){		
				Thread.sleep(1000);		//每一秒中检测下载是否完成
				notFinish = false;
				for(int i = 0; i< threads.length;i++){
					if (this.threads[i] != null && !this.threads[i].isFinish()) {//如果发现线程未完成下载
						Log.i(TAG, "notFinish()=true");
						notFinish = true;//设置标志为下载没有完成
						if(this.threads[i].getDownLength() == -1){	//如果下载失败,再重新下载
							this.threads[i] = new DownloadThread(this, downpath, this.saveFile, this.block, this.data.get(i+1), i+1);
							this.threads[i].setPriority(7);
							this.threads[i].start();
						}
					}
				}
				if(listener != null)	listener.onDownloadSize(downloadSize);	//在MainActivity中接受，更新进度条
			}
			if(downloadSize >= this.FileSize - threads.length) dao.delete(downpath);//下载完成删除记录
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
	 * 更新指定线程最后下载的位置
	 * @param threadId 线程id
	 * @param pos 最后下载的位置
	 */
	protected synchronized void update(int threadId, int pos) {
		this.data.put(threadId, pos);
		this.dao.update(this.downpath, threadId, pos);
	}

	/**
	 * 累计已下载大小
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


