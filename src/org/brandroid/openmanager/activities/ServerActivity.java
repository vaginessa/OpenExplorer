package org.brandroid.openmanager.activities;

import org.brandroid.openmanager.R;
import org.brandroid.openmanager.fragments.OpenFragmentActivity;

import android.os.Bundle;

public class ServerActivity extends OpenFragmentActivity
{
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.server);
	}
}
