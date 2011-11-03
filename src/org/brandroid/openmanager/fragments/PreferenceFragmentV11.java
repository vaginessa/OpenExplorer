package org.brandroid.openmanager.fragments;

import org.brandroid.openmanager.R;
import org.brandroid.openmanager.data.OpenPath;
import org.brandroid.utils.Logger;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

public class PreferenceFragmentV11 extends PreferenceFragment
{
	private OpenPath mPath;
	
	public PreferenceFragmentV11(OpenPath path)
	{
		super();
		mPath = path;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.preferences);
		PreferenceManager pm = getPreferenceManager();
		pm.setSharedPreferencesName("global");
		PreferenceManager.setDefaultValues(getActivity(), "global", R.xml.preferences, PreferenceActivity.MODE_PRIVATE, false);
		
		PreferenceScreen root = getPreferenceScreen();
		root.getPreference(0).setTitle(root.getPreference(0).getTitle() + " - Global");
		int iCount = root.getPreferenceCount();
		if(mPath != null && mPath.getPath() != "")
		{
			addPreferencesFromResource(R.xml.preferences);
			pm.setSharedPreferencesName(mPath.getPath());
			root.getPreference(iCount).setTitle(root.getPreference(iCount).getTitle() + " - " + mPath.getPath());
			PreferenceManager.setDefaultValues(getActivity(), mPath.getPath(), PreferenceActivity.MODE_PRIVATE, R.xml.preferences, false);
		}
	}
	
	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
			Preference preference) {
		super.onPreferenceTreeClick(preferenceScreen, preference);
		if(preference.getKey().equals("server_prefs"))
		{
			getFragmentManager()
				.beginTransaction()
				.replace(R.id.content_frag, new ServerSettings())
				.addToBackStack(null)
				.commit();
			return true;
		}
		return false;
	}
	
	public class ServerSettings extends PreferenceFragment
	{
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.server_prefs);
		}
	}
}
