package com.pt.filemanager;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pt.onlinedisk.R;
import com.pt.MyView.Topbar;
import com.pt.net.service.NetService;
import com.pt.utils.MyAdapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class FileManager extends Activity {

	private ListView list;
	private com.pt.MyView.Topbar topbar;
	private TextView tv_path;
	//
	private List<Map<String, Object>> mData;
	private String username;
	// 得到文件系统的根目录
	private String mDir = Environment.getRootDirectory().getParent();
	private String upload_path;				//上传到服务器的位置
	private final static int UPLOAD_SUCCESS = 1;
	
	private Handler mHandler = new Handler(){
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case UPLOAD_SUCCESS:
				String path = msg.getData().getString("path");
				Toast.makeText(getApplicationContext(), path + "路径文件上传完毕", Toast.LENGTH_LONG).show();
				break;

			default:
				break;
			}
		};
	};
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.list);
		Intent intent = getIntent();
		username = intent.getStringExtra("username");
		upload_path = intent.getStringExtra("CurrentPath");
		initView();

		mData = getData();

		MyAdapter adapter = new MyAdapter(this,mData);
		Log.i("BaiduMap", "count=" + adapter.getCount());
		list.setAdapter(adapter);

		topbar.setOnTopbarClickListener(new Topbar.topbarClickListener() {

			public void rightClick() {
				Log.i("BaiduMap", "上传的文件有：");
				final List<String> uploadpath = new ArrayList<String>();
				
				for (int i = 0; i < mData.size(); i++) {
					Log.i("BaiduMap", "i=" + i);
					if (String.valueOf(mData.get(i).get("isUpload")).equals(
							"YES")) {
						Log.i("BaiduMap",
								String.valueOf(mData.get(i).get("info")));
						// 所有要传输文件的路径
						uploadpath.add(String.valueOf(mData.get(i).get("info")));
						
						
					}
				}
				for (int i = 0; i < uploadpath.size(); i++) {
					final NetService service = new NetService(username);
					service.setUploadpath(uploadpath.get(i));
					// 开启多个线程上传
					new Thread(new Runnable() {
						@Override
						public void run() {
							service.conn();
							if(service.upload(upload_path)){
								Message msg = new Message();
								msg.what = UPLOAD_SUCCESS;
								msg.getData().putString("path", upload_path);
								mHandler.sendMessage(msg);
							}
							service.close();
						}
					}).start();
				}
				finish();
			}

			public void leftClick() {

			}
		});
		// 点击item
		list.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {

				// 如果是文件夹则打开
				if ((Integer) (mData.get(position).get("icon")) == R.drawable.icon_folder) {
					mDir = (String) mData.get(position).get("info");
					mData = getData(); // 点击目录时进入子目录
					MyAdapter adapter = new MyAdapter(getApplicationContext(),mData);
					list.setAdapter(adapter);
					tv_path.setText("目前路径：" + mDir);
				} else {
					CheckBox check = ((MyAdapter.ViewHolder) view.getTag()).checkbox;

					if (check.isChecked()) { // 判断CheckBox是否选择
												// ，如果是则取消并修改mData中上传为false
						check.setChecked(false);
						mData.get(position).remove("isUpload");
						mData.get(position).put("checkTag", false);
						// 记录文件是否被点击 标记为上传
						mData.get(position).put("isUpload", "NO");
					} else {
						check.setChecked(true);
						mData.get(position).remove("isUpload");
						mData.get(position).put("checkTag", true);
						mData.get(position).put("isUpload", "YES");
					}
				}
			}
		});
	}

	private void initView() {
		list = (ListView) findViewById(R.id.listview);
		topbar = (Topbar) findViewById(R.id.topbar);
		tv_path = (TextView) findViewById(R.id.tv_path);
		tv_path.setText("目前路径：" + mDir);
		
		topbar.setleftisvisiable(false);
	}

	private List<Map<String, Object>> getData() {
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		Map<String, Object> map = null;
		File f = new File(mDir); // 打开当前目录
		File[] files = f.listFiles(); // 获取当前目录中文件列表

		if (!mDir.equals("/")) { // 不充许进入/sdcard上层目录

			map = new HashMap<String, Object>(); // 加返回上层目录项
			map.put("title", "Back to .." + f.getParent());
			map.put("icon", R.drawable.icon_folder);
			map.put("checkTag", false);
			// 信息是存储点击item之后跳转的目录
			map.put("info", f.getParent());
			list.add(map);
		}
		if (files != null) { // 将目录中文件填加到列表中
			Log.i("BaiduMap", "files=" + files.length);
			for (int i = 0; i < files.length; i++) {

				map = new HashMap<String, Object>();
				// 文件或文件夹的名字
				map.put("title", files[i].getName());
				// 信息是存储点击item之后跳转的路径目录
				map.put("info", files[i].getPath());
				if (files[i].isDirectory()) { // 按不同类型显示不同图标,如果是目录则不显示checkbox
					map.put("icon", R.drawable.icon_folder);
					map.put("filetype", "Directoy");
				} else {
					map.put("icon", R.drawable.other);
					map.put("filetype", "notDirectoy");
				}
				map.put("checkTag", false);
				list.add(map);
			}
		}
		Log.i("BaiduMap", "list.size()" + list.size());
		return list;
	}
}
