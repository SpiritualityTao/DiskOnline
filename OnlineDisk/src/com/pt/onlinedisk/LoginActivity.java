package com.pt.onlinedisk;

import com.pt.MyView.Topbar;
import com.pt.net.service.NetService;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class LoginActivity extends Activity {

	protected static final int LOGIN_SUCCESS = 1;
	protected static final int LOGIN_FAILED = 2;
	protected static final int NO_CONNECTION = 0;
	protected static final String TAG = "login";
	private Topbar topbar;
	private EditText et_user;	
	private EditText et_pwd;
	private Button btn_login;
	private NetService service;
	
	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case LOGIN_SUCCESS:
				//登陆成功，开启主Activity
				Intent intent = new Intent(LoginActivity.this, MainActivity.class);
				intent.putExtra("username",msg.getData().getString("username"));
				//传入MainActivity
				startActivity(intent);
				break;

			case LOGIN_FAILED:
				//Toast显示错误
				Toast.makeText(getApplicationContext(), R.string.login_error, Toast.LENGTH_SHORT).show();
				break;
				
			case NO_CONNECTION:
				Toast.makeText(getApplicationContext(), R.string.fail_connection,  Toast.LENGTH_SHORT).show();
				break;
			}
		};
	};
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_login);
		service = new NetService();
		initView();
		//点击
		btn_login.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Login();
				Log.i(TAG, "btn_login已被点击");
			}
		});
		
	}

	protected void Login() {
		Log.i(TAG, "Login()");
		new Thread(new Runnable(){
			@Override
			public void run() {
				service.conn();
				if(service.isConnected()){
					Log.i(TAG, "网络连接成功");
					String username = et_user.getText().toString();
					String password = et_pwd.getText().toString();
					Message msg = new Message();
					//将用户名和密码交给NetService操作，连接服务器判断对错
					if(service.login(username, password)){
						msg.what = LOGIN_SUCCESS;
						msg.getData().putString("username", username);
						mHandler.sendMessage(msg);
					}else{
						msg.what = LOGIN_FAILED;
						mHandler.sendMessage(msg);
					}
				}else{
					Log.i(TAG, "网络连接失败");
					Message msg = new Message();
					msg.what = NO_CONNECTION;
					mHandler.sendMessage(msg);
				}
			}
		}).start();
	}

	@Override
	protected void onResume() {
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		if(service != null && service.isConnected())
			service.close();
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		new Thread(new Runnable(){
			@Override
			public void run() {
				service.conn();
			}
		});
	}
	
	private void initView() {
		topbar = (Topbar) findViewById(R.id.login_topbar);
		et_user = (EditText) findViewById(R.id.et_user);
		et_pwd = (EditText) findViewById(R.id.et_pwd);
		btn_login = (Button) findViewById(R.id.btn_Login);
		topbar.setleftisvisiable(false);
	}
}
