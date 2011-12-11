/*
    Open Explorer, an open source file explorer & text editor
    Copyright (C) 2011 Brandon Bowles <brandroid64@gmail.com>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.brandroid.openmanager;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.prefs.PreferencesFactory;

import org.brandroid.openmanager.data.OpenPath;
import org.brandroid.utils.Logger;
import org.brandroid.utils.Preferences;

import android.app.AlertDialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.Menu;

public class SettingsActivity extends PreferenceActivity
	implements OnPreferenceChangeListener
{
	//keys used for preference file
	public static final String PREF_LIST_KEY =		"pref_dirlist";
	public static final String PREF_BOOKNAME_KEY = 	"pref_bookmarks";
	public static final String PREF_HIDDEN_KEY = 	"pref_hiddenFiles";
	public static final String PREF_THUMB_KEY	=	"pref_thumbnail";
	public static final String PREF_VIEW_KEY =		"pref_view";
	public static final String PREF_SORT_KEY = 		"pref_sorting";
	public static final int MODE_PREFERENCES = 0;
	public static final int MODE_SERVER = 1;
	
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Intent intent = getIntent();
		Bundle config = intent.getExtras();
		if(savedInstanceState != null)
			config = savedInstanceState;
		if(config == null)
			config = new Bundle();
		
		String path = "global";
		if(config.containsKey("path"))
			path = config.getString("path");
		
		int mode = 0;
		if(config.containsKey("mode"))
			mode = config.getInt("mode");
		
		String pathSafe = mode + "_" + path.replaceAll("[^A-Za-z0-9]", "_");
		
		PreferenceManager pm = getPreferenceManager();
		pm.setSharedPreferencesName(pathSafe);
		
		if(mode == MODE_PREFERENCES)
		{
			if(!path.equals("global"))
			{
				PreferenceManager.setDefaultValues(this, pathSafe, PreferenceActivity.MODE_PRIVATE, R.xml.preferences_folders, false);
				addPreferencesFromResource(R.xml.preferences_folders);
				
				Preference pTitle = findPreference("folder_title");
				if(pTitle != null)
					pTitle.setTitle(pTitle.getTitle() + " - " + path);
			} else {
				PreferenceManager.setDefaultValues(this, pathSafe, PreferenceActivity.MODE_PRIVATE, R.xml.preferences, false);
				addPreferencesFromResource(R.xml.preferences);
				
				PreferenceCategory servers = (PreferenceCategory)findPreference("servers");
				if(servers != null)
				{
					for(String sName : PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("servers", "").split(","))
					{
						if(sName.equals("")) continue;
						PreferenceScreen ps = inflatePreferenceScreenFromResource(R.xml.server_prefs);
						ps.setKey("server_modify_" + sName);
						ps.setTitle(sName);
						servers.addItemFromInflater(ps);
					}
				}
			}
		} else if(mode == MODE_SERVER) {
			addPreferencesFromResource(R.xml.server_prefs);
			if(path.equals("new_server"))
			{
				setTitle(getTitle() + " - Add New");
				
			} else {
				Preference pUpdate = findPreference("server_update");
				if(pUpdate != null)
					pUpdate.setEnabled(false);
				setTitle(getTitle() + " - " + path);
				PreferenceManager.setDefaultValues(this, pathSafe, PreferenceActivity.MODE_PRIVATE, R.xml.server_prefs, false);
			}
		}
		setOnChange(getPreferenceScreen());
	}
	
	private void setOnChange(Preference p)
	{
		if(p.getClass().equals(PreferenceScreen.class))
		{
			PreferenceScreen ps = (PreferenceScreen)p;
			for(int i = 0; i < ps.getPreferenceCount(); i++)
				setOnChange(ps.getPreference(i));
		}
		p.setOnPreferenceChangeListener(this);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(requestCode == MODE_SERVER)
		{
			String sPath = data.getStringExtra("path");
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
			String sServers = prefs.getString("servers", "");
			Editor editor = prefs.edit();
			if(sPath.equals("server_add"))
			{
				int server_index = prefs.getInt("server_index", 0);
				sPath = "" + ++server_index;
				sServers += (sServers != "" ? "," : "") + sPath;
				editor
					.putString("servers", sServers)
					.putInt("server_index", server_index);
			} else if(sPath.startsWith("server_modify_"))
				sPath = sPath.replace("server_modify_", "");

			Bundle b = data.getExtras();
			for(String s : b.keySet())
				if(s != "mode" && s != "path")
					editor.putString("server_" + sPath + "_" + s, data.getStringExtra(s));
			
			editor.commit();
		}
	}

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
    		Preference preference) {
    	super.onPreferenceTreeClick(preferenceScreen, preference);
    	if(preference.getKey().equals("pref_global"))
    	{
    		Intent intentGlobal = new Intent(this, SettingsActivity.class);
    		startActivity(intentGlobal);
    		return true;
    	} else if(preference.getKey().equals("server_add"))
    	{
    		Intent intentServer = new Intent(this, SettingsActivity.class);
    		intentServer.putExtra("path", "server_add");
    		intentServer.putExtra("mode", MODE_SERVER);
    		startActivityForResult(intentServer, MODE_SERVER);
    		return true;
    	} else if(preference.getKey().startsWith("server_modify")) {
    		Intent intentServer = new Intent(this, SettingsActivity.class);
    		intentServer.putExtra("path", preference.getKey());
    		intentServer.putExtra("mode", MODE_SERVER);
    		startActivityForResult(intentServer, MODE_SERVER);
    		return true;
    	} else if(preference.getKey().equals("server_update")) {
    		Intent iNew = getIntent();
    		if(iNew == null) iNew = new Intent();
    		for(String s : new String[]{"name","url","user","password","path"})
    			if(findPreference("server_" + s) != null)
    				iNew.putExtra(s, ((EditTextPreference)findPreference("server_" + s)).getText());
    		setResult(RESULT_OK, iNew);
    	}
    	return false;
    }

    public PreferenceScreen inflatePreferenceScreenFromResource(int resId) {
        try {
            Class<PreferenceManager> cls = PreferenceManager.class;
            Method method = cls.getDeclaredMethod("inflateFromResource", Context.class, int.class, PreferenceScreen.class);
            return (PreferenceScreen) method.invoke(getPreferenceManager(), this, resId, null);         
        } catch(Exception e) {
            Logger.LogWarning("Could not inflate preference screen from XML", e);
        }

        return null;
    }
    
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		//getMenuInflater().inflate(R.menu.actbar, menu);
		return true;
	}

	public boolean onPreferenceChange(Preference preference, Object newValue) {
		preference.setSummary(newValue.toString());
		return false;
	}
}
