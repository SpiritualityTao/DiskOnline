package com.pt.onlinedisk;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.pt.onlinedisk.R;
import com.pt.MyView.RoundImageView;
import com.pt.filemanager.FileManager;
import com.pt.net.download.DownloadProgressListener;
import com.pt.net.download.FileDownloader;
import com.pt.net.service.NetService;
import com.pt.utils.MyAdapter;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	
	protected static final int CREATE_FOLDER = 3;
	
	protected static final int UPDATE_LIST = 1;
	
	protected static final int DOWNLOADING = 2;
	
	protected static final int REQUEST_EX = 0;
	
	private ImageButton btn_upload;				//上传图标按钮
	
	private ImageButton create_folder;			//创建文件夹
	
	private ImageButton btn_download;
	
	private RoundImageView image_head;			//头像
	
	private TextView tv_username;
	
	private NotificationManager manager = null;  //消息通知栏
	
	private RemoteViews remoteViews;
	
	private Notification.Builder builder;
	
	private List<Map<String, Object>> mData;	
	
	private ListView listview;	
	
	private NetService service ;				//网络服务
	
	private String currentpath;  				//记录当前路径
	
	private String username ;
	
	protected ArrayList<String> downloadpath;	//要下载的路径
	
	private static final String TAG = "OnDiskClient";
	
	private Handler mHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case UPDATE_LIST:	//更新文件列表
				MyAdapter mAdapter = new MyAdapter(getApplicationContext(), mData);
				listview.setAdapter(mAdapter);
				break;
				
			case DOWNLOADING:	//下载
			    //从Message中取得值
				int taskid = msg.getData().getInt("TaskId");
				int size = msg.getData().getInt("size");
				int filesize = msg.getData().getInt("filesize");
				String filename = msg.getData().getString("filename");
				
				if(size < filesize){	//设置Notification
					Notification notification = builder.getNotification();
					remoteViews = new RemoteViews(getPackageName(), R.layout.notification);
					remoteViews.setProgressBar(R.id.notificationProgress, filesize, size, false);
					remoteViews.setImageViewResource(R.id.notificationImage, R.drawable.ic_launcher);
					remoteViews.setTextViewText(R.id.notificationTitle, filename);
					remoteViews.setTextViewText(R.id.notificationPercent, (int)((float)size/(float)filesize*100) + "%");
					notification.contentView = remoteViews;
					manager.notify(taskid, notification);
				}else{  //文件下载完成
					manager.cancel(taskid);
					Toast.makeText(getApplicationContext(), filename+ "文件下载完成！！", Toast.LENGTH_LONG).show();
					
				}
				break;
				
			case CREATE_FOLDER:		//创建文件夹
				String file = msg.getData().getString("filename");
				Toast.makeText(getApplicationContext(), file + "文件夹已被创建", Toast.LENGTH_LONG).show();
				new Thread(new Runnable() {
					
					@Override
					public void run() {
						mData = service.loadMyList(currentpath);
						Message msg = new Message();
						msg.what = UPDATE_LIST;
						mHandler.sendMessage(msg);
					}
				});
				break;
			}
		};
	};
	

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_main);
		
		init();
		
		//listview item点击
		listview.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				//判断是否为文件夹
				if((Integer) (mData.get(position).get("icon")) == R.drawable.icon_folder){
					currentpath = (String) mData.get(position).get("info");
					//开启线程取得List，并用Handler更新UI
				    new Thread(new Runnable() {
						@Override
						public void run() {
							mData = service.loadMyList(currentpath);
							Message msg = new Message();
							msg.what = UPDATE_LIST;
							mHandler.sendMessage(msg);
						}
					}).start();
				}else{	//不是则CheckBox变更，刷新下载标志
					CheckBox check = ((MyAdapter.ViewHolder) view.getTag()).checkbox;
					if(check.isChecked()){   		//如果被勾选
						check.setChecked(false);
						mData.get(position).remove("isDownload");
						mData.get(position).put("checkTag", false);
						// 记录文件是否被点击 标记为下载
						mData.get(position).put("isDownload", "NO");
					}else{
						check.setChecked(true);
						mData.get(position).remove("isDownload");
						mData.get(position).put("checkTag", true);
						// 记录文件是否被点击 标记为下载
						mData.get(position).put("isDownload", "YES");
					}
				}
			}
		});

		// 创建文件夹
		create_folder.setOnClickListener(new MyOnClickListener());
		// 头像点击
		image_head.setOnClickListener(new MyOnClickListener());
		
		btn_download.setOnClickListener(new MyOnClickListener());
		// 上传
		btn_upload.setOnClickListener(new MyOnClickListener());
	}

	/**
	 * 初始化View以及Service
	 */
	private void init() {
		create_folder = (ImageButton) findViewById(R.id.create_folder);
		listview = (ListView) findViewById(R.id.main_listview);
		image_head = (RoundImageView) findViewById(R.id.image_head);
		btn_upload = (ImageButton) findViewById(R.id.ib_upload);
		btn_download = (ImageButton) findViewById(R.id.ib_download);
		tv_username = (TextView) findViewById(R.id.tv_username);
		manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		initNotification();
		
		Intent intent = getIntent();
		//从Intent中得到已经成功设置用户名的NetService
	    username = intent.getStringExtra("username");
	    tv_username.setText(username);
		service = new NetService(username);
		//得到用户名
		currentpath = username;
		 Log.i( TAG , "onCreate()："+currentpath);
		 
	}

	
	private void initNotification() {
		CharSequence title = "正在下载...";
		builder = new Notification.Builder(MainActivity.this);
		builder.setTicker(title);
		builder.setSmallIcon(R.drawable.ic_launcher);
		builder.setWhen(System.currentTimeMillis());
		builder.setAutoCancel(true);
		builder.setContentIntent(PendingIntent.getActivity(MainActivity.this, 0, new Intent(Intent.ACTION_DELETE), 0));	
	}

	/**
	 * 新建文件夹对话框
	 */
	protected void dialog() {
		final EditText et = new EditText(this);
		 AlertDialog.Builder builder = new AlertDialog.Builder(this)
				.setTitle("新建文件夹")
				.setIcon(R.drawable.ic_launcher)
				.setView(et);
		  builder.setPositiveButton("新建", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				new Thread(new Runnable() {
					@Override
					public void run() {
					   Log.i(TAG,"新建键已经点击");
						service.createfolder(currentpath ,et.getText().toString());
						Message msg = new Message();
						msg.what = CREATE_FOLDER;
						msg.getData().putString("filename", et.getText().toString());
						mHandler.sendMessage(msg);
					}
				}).start();
				
			}
		});
		  builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					
				}
			});
		  builder.create().show();
	}

	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		if (resultCode == RESULT_OK) {
			if (requestCode == REQUEST_EX) {
				Uri uri = intent.getData(); // 接收用户所选文件的路径
				TextView text = (TextView) findViewById(R.id.text);
				text.setText("select: " + uri); // 在界面上显示路径
			}
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
	}
	
	@Override
	protected void onResume() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				service.conn();
				Log.i(TAG, "onResume");
			    mData = service.loadMyList(currentpath);
			    Message msg = new Message();
			    msg.what = UPDATE_LIST;
			    mHandler.sendMessage(msg);
			    
			}
		}).start();
		
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		if(service != null)
			service.close();
		super.onPause();
	}
	

	private final class MyOnClickListener implements OnClickListener{

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.create_folder:	//创建文件夹
				dialog();
				break;

			case R.id.image_head:        
				Log.i(TAG, "头像已被点击");
				break;
				
			case R.id.ib_upload:		//上传文件
				Intent intent = new Intent(MainActivity.this, FileManager.class);
				intent.putExtra("username", username);
				intent.putExtra("CurrentPath", currentpath);
				startActivity(intent);
				break;
				
			case R.id.ib_download: 		//狭隘文件
				Log.i("Download:", "下载的文件有");
				downloadpath = new ArrayList<String>();
				for(int i = 0 ; i < mData.size() ; i++){	//遍历item  已勾选的则下载
					String isDownload = String.valueOf(mData.get(i).get("isDownload"));
					if(isDownload.equals("YES")){
						//取得所有勾选的文件的路径
						String path = String.valueOf(mData.get(i).get("info"));
						downloadpath.add(path);
						Log.i(TAG, path);
					}
				}
				
				String SDCardPath = null ;
				if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
					SDCardPath = Environment.getExternalStorageDirectory().getAbsolutePath();
					Log.i(TAG, SDCardPath);
				}else{
					Toast.makeText(getApplicationContext(), R.string.sdcarderror, 1).show();
				}
				
				for(int i = 0 ; i < downloadpath.size() ; i++){
				    NetService service = new NetService(username);
				    File saveFile = new File(SDCardPath + "/" + downloadpath.get(i) );
				    Log.i(TAG, "文件下载路径是："+SDCardPath + "/" + downloadpath.get(i) );
					download(service,saveFile,downloadpath.get(i));

				}
				downloadpath = null;
				break;
			}
		}

		private void download(NetService service, File saveFile,
				String downpath) {
			DownTask task = new DownTask(service,saveFile,downpath);
			new Thread(task).start();
		}
		
		/**
		 * 下载任务
		 * @author 韬
		 *
		 */
		private class DownTask implements Runnable{

			private Notification notification;
			private NetService service;
			private FileDownloader downloader;
			private File saveFile ;
			private String downpath;
			private boolean finish = false;
			
			public DownTask(NetService service, File saveFile, String downpath) {
				this.service = service;
				this.downpath = downpath;
				this.saveFile = saveFile;
				notification = new Notification();
			}
			@Override
			public void run() {
				service.conn();
				downloader = new FileDownloader(getApplicationContext(), 
						service, downpath,saveFile );
				downloader.download(new DownloadProgressListener() {
					
					@Override
					public void onDownloadSize(int size) {
						int filesize = downloader.getFileSize();
						Message msg = new Message();
						msg.what = DOWNLOADING;
						msg.getData().putInt("size", size);	
						msg.getData().putInt("filesize", filesize);	
						msg.getData().putInt("TaskId", getTaskId());
						msg.getData().putString("filename", saveFile.getName());
						msg.getData().putBoolean("isFinish", finish);
						mHandler.sendMessage(msg);
					}
				});
				service.close();
			}	
			
			/**
			 * 退出下载
			 */
			public void exit(){
				if(downloader!=null) {
					downloader.exit();
				}
			}
		}
		
	}

	
}
