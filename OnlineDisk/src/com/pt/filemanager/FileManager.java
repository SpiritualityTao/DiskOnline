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
	// �õ��ļ�ϵͳ�ĸ�Ŀ¼
	private String mDir = Environment.getRootDirectory().getParent();
	private String upload_path;				//�ϴ�����������λ��
	private final static int UPLOAD_SUCCESS = 1;
	
	private Handler mHandler = new Handler(){
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case UPLOAD_SUCCESS:
				String path = msg.getData().getString("path");
				Toast.makeText(getApplicationContext(), path + "·���ļ��ϴ����", Toast.LENGTH_LONG).show();
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
				Log.i("BaiduMap", "�ϴ����ļ��У�");
				final List<String> uploadpath = new ArrayList<String>();
				
				for (int i = 0; i < mData.size(); i++) {
					Log.i("BaiduMap", "i=" + i);
					if (String.valueOf(mData.get(i).get("isUpload")).equals(
							"YES")) {
						Log.i("BaiduMap",
								String.valueOf(mData.get(i).get("info")));
						// ����Ҫ�����ļ���·��
						uploadpath.add(String.valueOf(mData.get(i).get("info")));
						
						
					}
				}
				for (int i = 0; i < uploadpath.size(); i++) {
					final NetService service = new NetService(username);
					service.setUploadpath(uploadpath.get(i));
					// ��������߳��ϴ�
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
		// ���item
		list.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {

				// ������ļ������
				if ((Integer) (mData.get(position).get("icon")) == R.drawable.icon_folder) {
					mDir = (String) mData.get(position).get("info");
					mData = getData(); // ���Ŀ¼ʱ������Ŀ¼
					MyAdapter adapter = new MyAdapter(getApplicationContext(),mData);
					list.setAdapter(adapter);
					tv_path.setText("Ŀǰ·����" + mDir);
				} else {
					CheckBox check = ((MyAdapter.ViewHolder) view.getTag()).checkbox;

					if (check.isChecked()) { // �ж�CheckBox�Ƿ�ѡ��
												// ���������ȡ�����޸�mData���ϴ�Ϊfalse
						check.setChecked(false);
						mData.get(position).remove("isUpload");
						mData.get(position).put("checkTag", false);
						// ��¼�ļ��Ƿ񱻵�� ���Ϊ�ϴ�
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
		tv_path.setText("Ŀǰ·����" + mDir);
		
		topbar.setleftisvisiable(false);
	}

	private List<Map<String, Object>> getData() {
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		Map<String, Object> map = null;
		File f = new File(mDir); // �򿪵�ǰĿ¼
		File[] files = f.listFiles(); // ��ȡ��ǰĿ¼���ļ��б�

		if (!mDir.equals("/")) { // ���������/sdcard�ϲ�Ŀ¼

			map = new HashMap<String, Object>(); // �ӷ����ϲ�Ŀ¼��
			map.put("title", "Back to .." + f.getParent());
			map.put("icon", R.drawable.icon_folder);
			map.put("checkTag", false);
			// ��Ϣ�Ǵ洢���item֮����ת��Ŀ¼
			map.put("info", f.getParent());
			list.add(map);
		}
		if (files != null) { // ��Ŀ¼���ļ���ӵ��б���
			Log.i("BaiduMap", "files=" + files.length);
			for (int i = 0; i < files.length; i++) {

				map = new HashMap<String, Object>();
				// �ļ����ļ��е�����
				map.put("title", files[i].getName());
				// ��Ϣ�Ǵ洢���item֮����ת��·��Ŀ¼
				map.put("info", files[i].getPath());
				if (files[i].isDirectory()) { // ����ͬ������ʾ��ͬͼ��,�����Ŀ¼����ʾcheckbox
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
