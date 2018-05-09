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
	private FileDownloader fileDownloader; 	//������
	private String downpath;				//������������ļ���URL��
	private File saveFile;					//������ļ�
	private int block;						//Ҫ���صĳ���
	private int downLength;					//�����س���
	private int threadid;					//�߳�id
	private boolean finish = false; 		//�Ƿ����
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
				int startPos = block * (threadid - 1) + downLength;//��ʼλ��
				int endPos = block * threadid;//����λ��
				InputStream inStream = service.getInputStream(downpath,startPos,endPos);
				RandomAccessFile threadFile = new RandomAccessFile(saveFile, "rwd");
				threadFile.seek(startPos);
				Log.i(TAG, threadid + " start download..,startPos = " + startPos );
				byte[] buffer = new byte[1024];
				int offset = 0;
				while( !fileDownloader.isExit()&&(offset = inStream.read(buffer,0,1024)) != -1){
					threadFile.write(buffer, 0, offset);
					downLength += offset;
					Log.i(TAG, threadid+"�Ѿ����أ�" + downLength + "bytes");
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
	 * �����Ƿ����
	 * @return
	 */
	public boolean isFinish() {
		return finish ;
	}
	/**
	 * �Ѿ����ص����ݴ�С
	 * @return �������ֵΪ-1,��������ʧ��
	 */
	public long getDownLength() {
		return downLength;
	}
	
	

}
