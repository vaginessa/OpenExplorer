package org.brandroid.openmanager.activities;

import java.io.IOException;

import org.brandroid.openmanager.R;
import org.brandroid.openmanager.data.OpenFile;
import org.brandroid.openmanager.data.OpenPath;
import org.brandroid.openmanager.fragments.TextEditorFragment;
import org.brandroid.openmanager.util.FileManager;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.widget.Toast;

public class EditorActivity extends OpenFragmentActivity
{

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.text_editor_fragment);
		
		OpenPath file = null;
		try {
			file = FileManager.getOpenCache(getIntent().getDataString(), false, null);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if(file == null)
		{
			Toast.makeText(this, R.string.s_error_no_intents, Toast.LENGTH_LONG);
			finish();
		}
		Fragment mContentFragment = new TextEditorFragment(file);
		getSupportFragmentManager().beginTransaction()
			.replace(android.R.id.widget_frame, mContentFragment)
			.commit();
	}
}
