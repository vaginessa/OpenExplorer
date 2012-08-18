package org.brandroid.openmanager.fragments;

import org.brandroid.openmanager.R;
import org.brandroid.openmanager.activities.SettingsActivity;
import org.brandroid.openmanager.data.OpenPath;
import org.brandroid.utils.Logger;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

@TargetApi(11)
public class PreferenceFragmentV11 extends PreferenceFragment
{
	private OpenPath mPath;
	
	public PreferenceFragmentV11()
	{
		super();
	}
	
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
		
		PreferenceScreen ps = getPreferenceScreen();
		String key = null;
		if(getArguments().containsKey("key"))
		{
			Preference p = ps.findPreference(getArguments().getCharSequence("key"));
			ps.removeAll();
			if(p instanceof PreferenceCategory)
			{
				PreferenceCategory pc = (PreferenceCategory)p;
				for(int i = 0; i < pc.getPreferenceCount(); i++)
					ps.addPreference(pc.getPreference(i));
			} else
				ps.addPreference(p);
			setPreferenceScreen(ps);
		}
	}
	
	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
			Preference preference) {
		super.onPreferenceTreeClick(preferenceScreen, preference);
		
		if(((SettingsActivity)getActivity()).onPreferenceTreeClick(preferenceScreen, preference))
			return true;
		
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
