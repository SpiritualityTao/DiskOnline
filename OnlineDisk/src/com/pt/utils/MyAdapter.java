package com.pt.utils;

import java.util.List;
import java.util.Map;

import com.pt.onlinedisk.R;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * 自定义ListView Adapter
 * @author 彭韬
 *
 */
public class MyAdapter extends BaseAdapter {

	private LayoutInflater mInflater;
	private List<Map<String,Object>> mData;
	
	public MyAdapter(Context context, List<Map<String,Object>> Data) {
		mInflater = LayoutInflater.from(context);
		mData = Data;
		Log.i("BaiduMap", "MyAdapter");
	}

	@Override
	public int getCount() {

		return mData.size();
	}

	@Override
	public Object getItem(int position) {

		return mData.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	/**
	 * 设置每个列表项显示
	 */
	@Override
	// 设置每个列表项的显示
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder = null;
		if (convertView == null) {
			holder = new ViewHolder();
			convertView = mInflater.inflate(R.layout.item, null); // 设置列表项的布局
			holder.img = (ImageView) convertView.findViewById(R.id.icon);
			holder.title = (TextView) convertView.findViewById(R.id.text);
			holder.checkbox = (CheckBox) convertView
					.findViewById(R.id.checkbox);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		holder.img.setBackgroundResource((Integer) mData.get(position).get(
				"icon")); // 根据位置position设置具体内容
		holder.title.setText((String) mData.get(position).get("title"));
		Log.i("BaiduMap",
				String.valueOf(mData.get(position).get("filetype")));
		if (String.valueOf(mData.get(position).get("filetype")).equals(
				"Directory")) {
			holder.checkbox.setVisibility(View.GONE);
		} else {
			final int pos = position;
			holder.checkbox.setVisibility(View.VISIBLE);
			// 已经判断为文件，从mData中取出 是否上传的标记
			if (String.valueOf(mData.get(position).get("isUpload")).equals(
					"YES")) {
				// 如果是 则checkbox显示为Checked
				// 防止listview缓存机制导致CheckBox是否选中错乱
				holder.checkbox.setChecked((Boolean) mData.get(position).get("checkTag"));
			} else {
				holder.checkbox.setChecked( (Boolean) mData.get(position).get("checkTag"));
			}
		}

		return convertView;
	}
	public final class ViewHolder { // 定义每个列表项所含内容
		public ImageView img; // 显示图片ID
		public TextView title; // 文件目录名
		public CheckBox checkbox; // 多选框
	}
}


