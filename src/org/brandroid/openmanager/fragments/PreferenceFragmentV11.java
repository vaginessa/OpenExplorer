package org.brandroid.openmanager.fragments;

import org.brandroid.openmanager.R;

import android.os.Bundle;
import android.preference.PreferenceFragment;

public class PreferenceFragmentV11 extends PreferenceFragment
{
	public PreferenceFragmentV11()
	{
		super();
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
	}
}
