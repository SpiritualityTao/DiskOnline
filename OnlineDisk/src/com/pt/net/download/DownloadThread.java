package com.pt.net.download;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import android.util.Log;

import com.pt.net.service.NetService;

public class DownloadThread extends Thread{

	private static final String TAG = "DownloadTask";
	private FileDownloader fileDownloader; 	//下载器
	private String downpath;				//请求服务器的文件（URL）
	private File saveFile;					//保存的文件
	private int block;						//要下载的长度
	private int downLength;					//已下载长度
	private int threadid;					//线程id
	private boolean finish = false; 		//是否完成
	private NetService service;
	
	public DownloadThread(FileDownloader fileDownloader, String downpath,
			File saveFile, int block, int downLength, int threadid) {
		this.fileDownloader = fileDownloader;
		this.downpath = downpath;
		this.saveFile = saveFile;
		this.block = block;
		this.downLength = downLength;
		this.threadid = threadid;
		service = new NetService(fileDownloader.getUserName());
		service.conn();
	}

	@Override
	public void run() {
		if(downLength < block){
			try {
				int startPos = block * (threadid - 1) + downLength;//开始位置
				int endPos = block * threadid;//结束位置
				InputStream inStream = service.getInputStream(downpath,startPos,endPos);
				RandomAccessFile threadFile = new RandomAccessFile(saveFile, "rwd");
				threadFile.seek(startPos);
				Log.i(TAG, threadid + " start download..,startPos = " + startPos );
				byte[] buffer = new byte[1024];
				int offset = 0;
				while( !fileDownloader.isExit()&&(offset = inStream.read(buffer,0,1024)) != -1){
					threadFile.write(buffer, 0, offset);
					downLength += offset;
					Log.i(TAG, threadid+"已经下载：" + downLength + "bytes");
					fileDownloader.update(this.threadid, downLength);
					fileDownloader.append(offset);
					if(downLength + fileDownloader.getThreadNum() >= block) {
						inStream.close();
						threadFile.close();
						service.close();
						this.finish = true;
						break;
					}
				}
				
				
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * 下载是否完成
	 * @return
	 */
	public boolean isFinish() {
		return finish ;
	}
	/**
	 * 已经下载的内容大小
	 * @return 如果返回值为-1,代表下载失败
	 */
	public long getDownLength() {
		return downLength;
	}
	
	

}
