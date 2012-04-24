package org.brandroid.openmanager.activities;

import java.io.IOException;

import org.brandroid.openmanager.R;
import org.brandroid.openmanager.data.OpenFile;
import org.brandroid.openmanager.data.OpenPath;
import org.brandroid.openmanager.fragments.OpenFragment;
import org.brandroid.openmanager.fragments.TextEditorFragment;
import org.brandroid.openmanager.util.FileManager;
import org.brandroid.utils.Logger;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.widget.FrameLayout;
import android.widget.Toast;

public class EditorActivity extends OpenFragmentActivity
{
	private OpenPath file = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		FrameLayout frame = new FrameLayout(this);
		frame.setId(android.R.id.widget_frame);
		setContentView(frame);
		
		try {
			file = FileManager.getOpenCache(getIntent().getDataString(), false, null);
		} catch (IOException e) {
			Logger.LogError("Couldn't edit file.", e);
		}

		if(file == null)
		{
			Toast.makeText(this, R.string.s_error_no_intents, Toast.LENGTH_LONG);
			//finish();
		} else {
			OpenFragment mContentFragment = new TextEditorFragment(file);
			getSupportFragmentManager().beginTransaction()
				.replace(android.R.id.widget_frame, mContentFragment)
				.commit();
		}
	}
}
