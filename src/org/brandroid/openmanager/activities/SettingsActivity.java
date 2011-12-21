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

package org.brandroid.openmanager.activities;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.Random;

import org.brandroid.openmanager.R;
import org.brandroid.openmanager.R.xml;
import org.brandroid.openmanager.data.OpenServer;
import org.brandroid.openmanager.data.OpenServers;
import org.brandroid.utils.Logger;
import org.brandroid.utils.Preferences;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
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
	
	private Preferences prefs;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Intent intent = getIntent();
		if(intent == null) intent = new Intent();
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
		//if(mode == MODE_SERVER)
		//	path = pathSafe = "servers";
		
		if(!config.containsKey("path"))
			config.putString("path", path);
		
		PreferenceManager pm = getPreferenceManager();
		pm.setSharedPreferencesName(pathSafe);
		//getPreferences(MODE_PRIVATE);
		prefs = new Preferences(getApplicationContext());
		
		if(mode == MODE_PREFERENCES)
		{
			if(!path.equals("global")) // folder preferences
			{
				PreferenceManager.setDefaultValues(this, pathSafe, PreferenceActivity.MODE_PRIVATE, R.xml.preferences_folders, false);
				addPreferencesFromResource(R.xml.preferences_folders);
				
				Preference pTitle = findPreference("folder_title");
				if(pTitle != null)
					pTitle.setTitle(pTitle.getTitle() + " - " + path);
			} else { // global preferences
				PreferenceManager.setDefaultValues(this, pathSafe, PreferenceActivity.MODE_PRIVATE, R.xml.preferences, false);
				addPreferencesFromResource(R.xml.preferences);
				
				PreferenceCategory mPrefServers = (PreferenceCategory)findPreference("servers");
				if(mPrefServers != null)
				{
					OpenServers servers = LoadDefaultServers(); //new OpenServers(prefs.getSetting("global", "servers", new JSONObject()));
					for(String sName : servers.keySet())
					{
						OpenServer server = servers.get(sName);
						Logger.LogDebug("Checking server [" + sName + "]");
						if(sName.equals("")) continue;
						Preference p = new Preference(this);
						//PreferenceScreen ps = inflatePreferenceScreenFromResource(R.xml.server_prefs);
						p.setKey("server_modify_" + sName);
						p.setTitle(sName);
						mPrefServers.addPreference(p);
					}
				}
			}
		} else if(mode == MODE_SERVER) {
			addPreferencesFromResource(R.xml.server_prefs);
			//SharedPreferences sp = Preferences.getPreferences(getApplicationContext(), "servers");
			//String servers = sp.getString("servers", "");
			OpenServers servers = LoadDefaultServers(); //new OpenServers(prefs.getJSON("global", "servers", new JSONObject()));
			if(path.equals("server_add"))
			{
				setTitle(getTitle() + " - Add New");
			} else {
				path = path.replace("server_modify_", "");
				setTitle(getTitle() + " - " + path);
				OpenServer server = servers.get(path);
				if(server != null)
					for(String s : new String[]{"name","url","user","password","path"})
						if(findPreference(s) != null)
						{
							Preference p = findPreference(s);
							String val = server.getString(s, "");
							if(val != "")
							{
								((EditTextPreference)p).setText(val);
								p.setSummary(val);
								p.setDefaultValue(val);
							}
						}
			}
			//PreferenceManager.setDefaultValues(this, "servers", PreferenceActivity.MODE_PRIVATE, R.xml.server_prefs, true);
			//setIntent(intent);
		}
		setOnChange(getPreferenceScreen(), false);
	}
	
	@Override
	protected void onStop() {
		super.onStop();
	}
	
	private void setOnChange(Preference p, Boolean bSetSummaries)
	{
		if(p.getClass().equals(PreferenceScreen.class))
		{
			PreferenceScreen ps = (PreferenceScreen)p;
			for(int i = 0; i < ps.getPreferenceCount(); i++)
				setOnChange(ps.getPreference(i), bSetSummaries);
		}
		p.setOnPreferenceChangeListener(this);
		
		if(bSetSummaries && p.getClass().equals(EditTextPreference.class))
			if(((EditTextPreference)p).getText() != null && !"".equals(((EditTextPreference)p).getText()))
			{
				String txt = ((EditTextPreference)p).getText();
				p.setSummary(txt);
				p.setDefaultValue(txt);
			}
		
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if(requestCode == MODE_SERVER && data != null)
		{
			String sPath = data.getStringExtra("path");
			Preferences prefs = new Preferences(getApplicationContext());
			OpenServers servers = LoadDefaultServers(); //new OpenServers(prefs.getJSON("global", "servers", new JSONObject()));
			String name = sPath;
			if(data.hasExtra("name"))
				name = data.getStringExtra("name");
			OpenServer server = servers.get(name);
			if(server == null)
				server = new OpenServer();

			Bundle b = data.getExtras();
			for(String s : b.keySet())
				if(s != "mode" && s != "path")
				{
					if(b.get(s) == null) continue;
					server.setSetting(s, b.get(s).toString());
				}
			
			servers.addServer(name, server);
			SaveToDefaultServers(servers, getApplicationContext());
			//prefs.setSetting("global", "servers", servers.getJSONObject());
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
    		OpenServer server = new OpenServer();
    		server.setSetting("name", "Server " + new Random().nextInt());
    		for(String s : new String[]{"name","url","user","password","path"})
    			if(findPreference("server_" + s) != null)
    				server.setSetting(s, ((EditTextPreference)findPreference("server_" + s)).getText());
    			else if(findPreference(s) != null)
    				server.setSetting(s, ((EditTextPreference)findPreference(s)).getText());
    		OpenServers servers = LoadDefaultServers(); //new OpenServers(prefs.getJSON("global", "servers", new JSONObject()));
    		servers.addServer(server.getString("name", "Server"), server);
    		SaveToDefaultServers(servers, getApplicationContext());
    		//prefs.setSetting("global", "servers", servers.getJSONObject());
    		setResult(RESULT_OK);
    		finish();
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
		Intent intent = getIntent();
		intent.putExtra(preference.getKey(), newValue.toString());
		setIntent(intent);
		return false;
	}
	

	public static void SaveToDefaultServers(OpenServers servers, Context context)
	{		
		File f = new File(context.getFilesDir().getPath(), "servers.json");
		Writer w = null;
		try {
			f.delete();
			f.createNewFile();
			w = new BufferedWriter(new FileWriter(f));
			String data = servers.getJSONObject().toString();
			w.write(data);
			w.close();
		} catch(IOException e) {
			Logger.LogError("Couldn't save OpenServers.", e);
			try {
				if(w != null)
					w.close();
			} catch(IOException e2)
			{
				Logger.LogError("Couldn't close writer during error", e2);
			}
		}
	}
	public OpenServers LoadDefaultServers()
	{
		File f = new File(getApplicationContext().getFilesDir().getPath(), "servers.json");
		Reader r = null;
		try {
			//getApplicationContext().openFileInput("servers.json"); //, Context.MODE_PRIVATE);
			if(f.createNewFile())
			{
				Logger.LogWarning("Couldn't create default servers file (" + f.getPath() + ")");
				return new OpenServers();
			} else {
				Logger.LogWarning("Created default servers file (" + f.getPath() + ")");
				r = new BufferedReader(new FileReader(f));
				char[] chars = new char[256];
				StringBuilder sb = new StringBuilder();
				while(r.read(chars) > 0)
					sb.append(chars);
				r.close();
				return new OpenServers(new JSONObject(sb.toString()));
			}
		} catch (IOException e) {
			Logger.LogError("Error loading default server list.", e);
			if(r != null)
				try {
					r.close();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			return new OpenServers();
		} catch (JSONException e) {
			if(r != null)
				try {
					r.close();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			Logger.LogError("Error translating default server list into JSON.", e);
			return new OpenServers();
		}
		
	}
}
