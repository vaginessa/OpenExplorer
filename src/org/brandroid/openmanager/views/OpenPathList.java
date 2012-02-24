package org.brandroid.openmanager.views;

import org.brandroid.openmanager.R;
import org.brandroid.openmanager.adapters.OpenPathAdapter;
import org.brandroid.openmanager.data.OpenPath;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ListAdapter;
import android.widget.ListView;

public class OpenPathList extends ListView
{
	private OpenPath mPathParent = null;
	private OpenPathAdapter mAdapter = null;
	
	public OpenPathList(OpenPath path, Context context)
	{
		super(context);
		setPath(path);
	}
	public OpenPathList(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
	public void setPath(OpenPath path)
	{
		mPathParent = path;
		mAdapter = new OpenPathAdapter(mPathParent, R.layout.list_content_layout, getContext());
		setAdapter(mAdapter);
		invalidateViews();
	}
	
	@Override
	public ListAdapter getAdapter() {
		if(mPathParent == null)
			return super.getAdapter();
		if(mAdapter == null)
			mAdapter = new OpenPathAdapter(mPathParent, R.layout.list_content_layout, getContext());
		return mAdapter;
	}
}
