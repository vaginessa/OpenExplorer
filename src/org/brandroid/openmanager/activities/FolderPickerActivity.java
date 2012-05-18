package org.brandroid.openmanager.activities;

import org.brandroid.openmanager.R;
import org.brandroid.openmanager.data.OpenFile;
import org.brandroid.openmanager.data.OpenPath;
import org.brandroid.openmanager.fragments.PickerFragment;
import org.brandroid.openmanager.fragments.PickerFragment.OnOpenPathPickedListener;
import org.brandroid.utils.MenuUtils;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.TextView;

public class FolderPickerActivity extends FragmentActivity
	implements OnItemClickListener, OnClickListener
{
	private OpenPath mPath;
	private boolean pickDirOnly = true;
	private String mDefaultName;
	private TextView mSelection, mTitle;
	private EditText mPickName;
	private FragmentManager mFragmentManager;
	
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
		setContentView(R.layout.picker_widget);
		mSelection = (TextView)findViewById(R.id.pick_path);
		mPickName = (EditText)findViewById(R.id.pick_filename);
		mTitle = (TextView)findViewById(android.R.id.title);
		mFragmentManager = getSupportFragmentManager();
		setPath(mPath);
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
		PickerFragment frag = new PickerFragment(this, mPath);
		frag.setShowSelection(false);
		frag.setOnOpenPathPickedListener(new OnOpenPathPickedListener() {
			@Override
			public void onOpenPathPicked(OpenPath path) {
				setPath(path);
			}
		});
		mFragmentManager
			.beginTransaction()
			.replace(R.id.picker_widget, frag)
			.setBreadCrumbTitle(mPath.getPath())
			.commit();
	}
	
	public PickerFragment getSelectedFragment()
	{
		return (PickerFragment)mFragmentManager.findFragmentById(R.id.picker_widget);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		OpenPath path = getSelectedFragment().getPath();
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
