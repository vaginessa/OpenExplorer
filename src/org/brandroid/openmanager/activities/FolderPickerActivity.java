package org.brandroid.openmanager.activities;

import org.brandroid.openmanager.R;
import org.brandroid.openmanager.adapters.ContentAdapter;
import org.brandroid.openmanager.adapters.ContentTreeAdapter;
import org.brandroid.openmanager.data.OpenFile;
import org.brandroid.openmanager.data.OpenPath;
import org.brandroid.utils.MenuUtils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.TextView;

public class FolderPickerActivity extends FragmentActivity
	implements OnItemClickListener, OnClickListener
{
	private OpenPath mPath;
	private boolean pickDirOnly = true;
	private String mDefaultName;
	private GridView mGrid;
	private BaseAdapter mAdapter;
	private TextView mSelection, mTitle;
	private EditText mPickName;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();
		if(intent == null)
			intent = new Intent();
		if(savedInstanceState != null)
		{
			if(savedInstanceState.containsKey("start"))
			{
				mPath = (OpenPath)savedInstanceState.getParcelable("start");
				intent.putExtra("picker", savedInstanceState.getParcelable("start"));
			} else mPath = OpenFile.getExternalMemoryDrive(true);
			if(savedInstanceState.containsKey("name"))
			{
				mDefaultName = savedInstanceState.getString("name");
				intent.putExtra("name", mDefaultName);
			}
		}
		setContentView(R.layout.picker);
		mGrid = (GridView)findViewById(android.R.id.list);
		mGrid.setNumColumns(getResources().getInteger(R.integer.max_grid_columns));
		mSelection = (TextView)findViewById(R.id.pick_path);
		mPickName = (EditText)findViewById(R.id.pick_filename);
		mTitle = (TextView)findViewById(android.R.id.title);
		setPath(mPath);
		mGrid.setOnItemClickListener(this);
		MenuUtils.setViewsOnClick(this, this, android.R.id.button1, android.R.id.button2);
	}
	
	private void setPath(OpenPath path)
	{
		if(path == null)
			path = OpenFile.getExternalMemoryDrive(true);
		mPath = path;
		mTitle.setText(path.getPath());
		mTitle.setVisibility(View.VISIBLE);
		mSelection.setText(path.getPath());
		if(mPath.isFile())
		{
			mDefaultName = mPath.getName();
			mPickName.setText(mDefaultName);
			mPath = mPath.getParent();
		}
		mAdapter = new ContentTreeAdapter(this, mPath);
		mGrid.setAdapter(mAdapter);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		OpenPath path = (OpenPath)mAdapter.getItem(position);
		setPath(path);
	}
	
	private void returnPath()
	{
		Intent intent = getIntent();
		if(intent == null) intent = new Intent();
		OpenPath ret = mPath;
		if(!pickDirOnly)
			ret = ret.getChild(mPickName.getText().toString());
		intent.setData(ret.getUri());
		intent.putExtra("path", (Parcelable)ret);
		setResult(RESULT_OK, intent);
		finish();
	}

	@Override
	public void onClick(View v) {
		switch(v.getId())
		{
		case android.R.id.button1:
			returnPath();
			break;
		case android.R.id.button2:
			setResult(RESULT_CANCELED);
			finish();
			break;
		}
	}
}
