package org.brandroid.openmanager.adapters;

import org.brandroid.openmanager.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class LinesAdapter extends BaseAdapter
{
	private String[] mLines;
	private final Context mContext;
	private final LayoutInflater inflater;
	private float mTextSize;
	
	public LinesAdapter(Context c, String[] lines)
	{
		mContext = c;
		inflater = (LayoutInflater)c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mLines = lines;
	}
	
	public void setLines(String[] lines)
	{
		mLines = lines;
		notifyDataSetChanged();
	}
	
	public void setTextSize(float size)
	{
		if(size == mTextSize) return;
		mTextSize = size;
		notifyDataSetChanged();
	}

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return mLines.length;
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return mLines[position];
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	private String repeat(String str, int cnt)
	{
		if(cnt <= 0) return "";
		String ret = str;
		while(--cnt > 0)
			ret += str;
		return ret;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View ret = convertView;
		if(ret == null)
			ret = inflater.inflate(R.layout.edit_text_view_row, null);
		TextView txtLine = (TextView)ret.findViewById(R.id.text_line);
		TextView txtData = (TextView)ret.findViewById(R.id.text_data);
		txtLine.setText(repeat(" ", ((Integer)getCount()).toString().length() - ((Integer)position).toString().length())
				+ position);
		txtData.setText(mLines[position]);
		if(mTextSize > 10)
		{
			txtLine.setTextSize(mTextSize-1);
			txtData.setTextSize(mTextSize);
		}
		
		return ret;
	}
	
}