package com.pt.MyView;

import com.pt.onlinedisk.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class Topbar extends RelativeLayout {
	
	private Button leftButton,rightButton;
	private TextView tvtitle;

	private int leftTextColor;
	private Drawable leftBackground;
	private String leftText;
	
	private int rightTextColor;
	private Drawable rightBackground;
	private String rightText;
	
	private int TitleColor;
	private float TitleSize;
	private String title;
	
	//给View控件设置参数
	private LayoutParams leftParams,rightParams,titleParams;
	
	private topbarClickListener listener;
	
	public interface topbarClickListener{
		public void leftClick();
		public void rightClick();
	}
	
	public void setOnTopbarClickListener(topbarClickListener topbarClickListener){
		this.listener = topbarClickListener;
	}
	
	public Topbar(Context context, AttributeSet attrs) {
		super(context, attrs);
		//我们将获取来的自定义的值存到TypedArray中
		TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.Topbar);
		
		
		//从TypedArray取到我们想要的值
		leftTextColor = ta.getColor(R.styleable.Topbar_leftTextColor, 0);
		leftBackground = ta.getDrawable(R.styleable.Topbar_leftBackground);
		leftText = ta.getString(R.styleable.Topbar_leftText);
		
		rightTextColor = ta.getColor(R.styleable.Topbar_rightTextColor, 0);
		rightBackground = ta.getDrawable(R.styleable.Topbar_rightBackground);
		rightText = ta.getString(R.styleable.Topbar_rightText);
		
		TitleColor = ta.getColor(R.styleable.Topbar_titleTextColor, 0);
		TitleSize = ta.getDimension(R.styleable.Topbar_titleTextSize, 0);
		title = ta.getString(R.styleable.Topbar_title);
		
		leftButton = new Button(context);
		rightButton = new Button(context);
		tvtitle = new TextView(context);
		
		leftButton.setTextColor(leftTextColor);
		leftButton.setBackgroundDrawable(leftBackground);
		leftButton.setText(leftText);
		
		rightButton.setTextColor(rightTextColor);
		rightButton.setBackgroundDrawable(rightBackground);
		rightButton.setText(rightText);
		
		tvtitle.setText(title);
		tvtitle.setTextScaleX(TitleSize);
		tvtitle.setTextColor(TitleColor);
		tvtitle.setGravity(Gravity.CENTER);
		
		setBackgroundColor(Color.BLUE);
		
		leftParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 
				ViewGroup.LayoutParams.WRAP_CONTENT);
		leftParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT,TRUE);
		addView(leftButton,leftParams);
		
		rightParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 
				ViewGroup.LayoutParams.WRAP_CONTENT);
		rightParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT,TRUE);
		addView(rightButton, rightParams);
		
		titleParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 
				ViewGroup.LayoutParams.WRAP_CONTENT);
		titleParams.addRule(RelativeLayout.CENTER_IN_PARENT,TRUE);
		addView(tvtitle, titleParams);
		
		leftButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				listener.leftClick();
			}
		});
		
		rightButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				listener.rightClick();
			}
		});
		
	}
	
	/**
	 * 设置左侧的Button是否显示
	 * @param flag
	 */
	public void setleftisvisiable(boolean flag){
		if(flag){
			leftButton.setVisibility(View.VISIBLE);
		}else{
			leftButton.setVisibility(View.GONE);
		}
	}
}
