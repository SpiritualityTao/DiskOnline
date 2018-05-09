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
 * �Զ���ListView Adapter
 * @author ���
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
	 * ����ÿ���б�����ʾ
	 */
	@Override
	// ����ÿ���б������ʾ
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder = null;
		if (convertView == null) {
			holder = new ViewHolder();
			convertView = mInflater.inflate(R.layout.item, null); // �����б���Ĳ���
			holder.img = (ImageView) convertView.findViewById(R.id.icon);
			holder.title = (TextView) convertView.findViewById(R.id.text);
			holder.checkbox = (CheckBox) convertView
					.findViewById(R.id.checkbox);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		holder.img.setBackgroundResource((Integer) mData.get(position).get(
				"icon")); // ����λ��position���þ�������
		holder.title.setText((String) mData.get(position).get("title"));
		Log.i("BaiduMap",
				String.valueOf(mData.get(position).get("filetype")));
		if (String.valueOf(mData.get(position).get("filetype")).equals(
				"Directory")) {
			holder.checkbox.setVisibility(View.GONE);
		} else {
			final int pos = position;
			holder.checkbox.setVisibility(View.VISIBLE);
			// �Ѿ��ж�Ϊ�ļ�����mData��ȡ�� �Ƿ��ϴ��ı��
			if (String.valueOf(mData.get(position).get("isUpload")).equals(
					"YES")) {
				// ����� ��checkbox��ʾΪChecked
				// ��ֹlistview������Ƶ���CheckBox�Ƿ�ѡ�д���
				holder.checkbox.setChecked((Boolean) mData.get(position).get("checkTag"));
			} else {
				holder.checkbox.setChecked( (Boolean) mData.get(position).get("checkTag"));
			}
		}

		return convertView;
	}
	public final class ViewHolder { // ����ÿ���б�����������
		public ImageView img; // ��ʾͼƬID
		public TextView title; // �ļ�Ŀ¼��
		public CheckBox checkbox; // ��ѡ��
	}
}


