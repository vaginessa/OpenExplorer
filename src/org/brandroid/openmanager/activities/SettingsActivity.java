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

import org.brandroid.billing.BillingService;
import org.brandroid.billing.PurchaseObserver;
import org.brandroid.openmanager.R;
import org.brandroid.openmanager.R.xml;
import org.brandroid.openmanager.data.OpenServer;
import org.brandroid.openmanager.data.OpenServers;
import org.brandroid.utils.Logger;
import org.brandroid.utils.Preferences;
import org.brandroid.utils.SimpleCrypto;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.brandroid.billing.ResponseHandler;

import org.brandroid.billing.Consts;
import org.brandroid.billing.BillingService.RequestPurchase;
import org.brandroid.billing.BillingService.RestoreTransactions;
import org.brandroid.billing.Consts.PurchaseState;
import org.brandroid.billing.Consts.ResponseCode;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.RemoteException;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.method.PasswordTransformationMethod;
import android.text.method.ReplacementTransformationMethod;
import android.text.method.TransformationMethod;
import android.util.Log;
import android.view.Menu;
import android.view.animation.TranslateAnimation;
import android.widget.EditText;

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
	private BillingService mBillingService;
	private DonationObserver mDonationObserver;
	private Handler mHandler;
	
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
				
				Preference preference = pm.findPreference("pref_stats");
				if(preference != null) { // "Help improve..."
					preference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
						public boolean onPreferenceClick(Preference preference) {
				    		try {
					    		if(mBillingService == null)
					    		{
					    			mBillingService = new BillingService();
					    			mBillingService.setContext(SettingsActivity.this);
					    		}
					    		if (mBillingService.checkBillingSupported())
					    		{
					    			if(mBillingService.requestPurchase("donate_01", null))
					    				Logger.LogDebug("Donation success!");
					    			else
					    				Logger.LogWarning("Donation fail?");
					    		} else
					    			Logger.LogWarning("Billing not supported");
				    		} catch(Exception e) {
				    			Logger.LogError("Error using billing service.", e);
				    		}
				    		return false;
						}
					});
				} else Logger.LogWarning("Couldn't find donation button");
				
				refreshServerList();
			}
		} else if(mode == MODE_SERVER) {
			addPreferencesFromResource(R.xml.server_prefs);
			//SharedPreferences sp = Preferences.getPreferences(getApplicationContext(), "servers");
			//String servers = sp.getString("servers", "");
			OpenServers servers = LoadDefaultServers(); //new OpenServers(prefs.getJSON("global", "servers", new JSONObject()));
			if(path.equals("server_add"))
			{
				setTitle(getTitle() + " - Add New");
				getPreferenceScreen().findPreference("server_delete").setEnabled(false);
			} else {
				path = path.replace("server_modify_", "");
				OpenServer server = new OpenServer();
				int index = 0;
				try {
					index = Integer.parseInt(path);
					server = servers.get(index);
					path = server.getName();
				} catch(NumberFormatException e) {
					Logger.LogWarning("Couldn't parseInt " + path);
				}
				setTitle(getTitle() + " - " + path);
				Preference p;
				if(server != null)
					for(String s : new String[]{"name","host","user","password","dir"})
						if((p = getPreferenceManager().findPreference("server_" + s)) != null)
						{
							String val = server.getString(s);
							if(val != null)
							{
								getIntent().putExtra(s, val);
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
		
		mHandler = new Handler();
		mDonationObserver = new DonationObserver(mHandler);
		mBillingService = new BillingService();
		mBillingService.setContext(this);
		ResponseHandler.register(mDonationObserver);
        if (!mBillingService.checkBillingSupported()) {
        	Logger.LogWarning("Billing not supported.");
            //showDialog(DIALOG_CANNOT_CONNECT_ID);
        }
	}
	
	/**
     * A {@link PurchaseObserver} is used to get callbacks when Android Market sends
     * messages to this application so that we can update the UI.
     */
    private class DonationObserver extends PurchaseObserver {
        public DonationObserver(Handler handler) {
            super(SettingsActivity.this, handler);
        }

        @Override
        public void onBillingSupported(boolean supported) {
            if (Consts.DEBUG) {
                //Log.i(TAG, "supported: " + supported);
            }
            if (supported) {
                //restoreDatabase();
                //mBuyButton.setEnabled(true);
                //mEditPayloadButton.setEnabled(true);
            } else {
                //showDialog(DIALOG_BILLING_NOT_SUPPORTED_ID);
            }
        }

        @Override
        public void onPurchaseStateChange(PurchaseState purchaseState, String itemId,
                int quantity, long purchaseTime, String developerPayload) {
        	Logger.LogDebug("onPurchaseStateChange() itemId: " + itemId + " " + purchaseState);
            /*

            if (developerPayload == null) {
                logProductActivity(itemId, purchaseState.toString());
            } else {
                logProductActivity(itemId, purchaseState + "\n\t" + developerPayload);
            }

            if (purchaseState == PurchaseState.PURCHASED) {
                mOwnedItems.add(itemId);
            }
            mCatalogAdapter.setOwnedItems(mOwnedItems);
            mOwnedItemsCursor.requery();
            */
        }

        @Override
        public void onRequestPurchaseResponse(RequestPurchase request,
                ResponseCode responseCode) {
        	Logger.LogDebug(request.mProductId + ": " + responseCode);
        	if (responseCode == ResponseCode.RESULT_OK) {
                Logger.LogInfo("purchase was successfully sent to server");
                //logProductActivity(request.mProductId, "sending purchase request");
            } else if (responseCode == ResponseCode.RESULT_USER_CANCELED) {
                Logger.LogInfo("user canceled purchase");
                //logProductActivity(request.mProductId, "dismissed purchase dialog");
            } else {
                Logger.LogInfo("purchase failed");
                //logProductActivity(request.mProductId, "request purchase returned " + responseCode);
            }
        }

        @Override
        public void onRestoreTransactionsResponse(RestoreTransactions request,
                ResponseCode responseCode) {
        	if (responseCode == ResponseCode.RESULT_OK) {
                Logger.LogDebug("completed RestoreTransactions request");
                // Update the shared preferences so that we don't perform
                // a RestoreTransactions again.
                /*SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
                SharedPreferences.Editor edit = prefs.edit();
                edit.putBoolean(DB_INITIALIZED, true);
                edit.commit();*/
            } else {
                Logger.LogDebug("RestoreTransactions error: " + responseCode);
            }
        }
    }
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if(mBillingService != null)
			mBillingService.unbind();
	}
	
	private void refreshServerList() {
		PreferenceCategory mPrefServers = (PreferenceCategory)findPreference("servers");
		if(mPrefServers != null)
		{
			//for(int i = mPrefServers.getPreferenceCount() - 1; i > 0; i--)
			//	mPrefServers.removePreference(mPrefServers.getPreference(i));
			OpenServers servers = LoadDefaultServers(); //new OpenServers(prefs.getSetting("global", "servers", new JSONObject()));
			for(int i = 0; i < servers.size(); i++)
			{
				OpenServer server = servers.get(i);
				//Logger.LogDebug("Checking server [" + sName + "]");
				//if(sName.equals("")) continue;
				Preference p = mPrefServers.findPreference("server_modify_" + i);
				if(p == null) p = new Preference(this);
				//PreferenceScreen ps = inflatePreferenceScreenFromResource(R.xml.server_prefs);
				p.setKey("server_modify_" + i);
				p.setTitle(server.getName());
				mPrefServers.addPreference(p);
			}
			for(int i = mPrefServers.getPreferenceCount() - 1; i > servers.size(); i--)
				mPrefServers.removePreference(mPrefServers.getPreference(i)); // remove the rest
		}
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		ResponseHandler.register(mDonationObserver);
	}

	@Override
	protected void onStop() {
		super.onStop();
		ResponseHandler.unregister(mDonationObserver);
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
			OpenServer server = null;
			int index = 0;
			if(sPath.equals("server_add"))
			{
				server = new OpenServer();
				index = servers.size();
				sPath = index + "";
				servers.add(server);
			} else {
				try {
					index = Integer.parseInt(sPath.replaceAll("[^0-9]", ""));
					server = servers.get(index);
					if(resultCode == RESULT_FIRST_USER) // delete
					{
						servers.remove(index);
						SaveToDefaultServers(servers, getApplicationContext());
						refreshServerList();
						return;
					} else
						sPath = index + "";
				} catch(NumberFormatException e) {
					Logger.LogWarning("Couldn't parseInt " + sPath);
				}
			}

			if(server == null)
				server = new OpenServer();

			Bundle b = data.getExtras();
			for(String s : b.keySet())
				if(s != "mode")
				{
					if(b.get(s) == null) continue;
					server.setSetting(s, b.get(s).toString());
				}
			
			if(server.getPath() == null || (b.containsKey("dir") && !b.getString("dir").equalsIgnoreCase(server.getPath())))
				server.setPath(b.getString("dir"));
			
			servers.set(index, server);
			SaveToDefaultServers(servers, getApplicationContext());
			refreshServerList();
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
    		//OpenServer server = new OpenServer();
    		Preference p;
    		for(String s : new String[]{"name","host","url","user","password","dir"})
    			if((p = preferenceScreen.findPreference("server_" + s)) != null)
    			{
    				if(iNew.hasExtra(s)) continue;
    				//server.setSetting(s, ((EditTextPreference)getPreferenceManager().findPreference("server_" + s)).getText());
    				Logger.LogDebug("Found " + s + " = " + ((EditTextPreference)p).getEditText().getText().toString());
    				iNew.putExtra(s, ((EditTextPreference)p).getEditText().getText().toString());
    			}
    		//server.setSetting("name", server.getString("name", server.getHost()));
    		//Logger.LogDebug("Saving " + server.getName() + " (" + server.getHost() + ") - " + server.getJSONObject().toString());
    		//OpenServers servers = LoadDefaultServers(); //new OpenServers(prefs.getJSON("global", "servers", new JSONObject()));
    		//servers.addServer(server.getString("name", "Server"), server);
    		//SaveToDefaultServers(servers, getApplicationContext());
    		//prefs.setSetting("global", "servers", servers.getJSONObject());
    		setResult(RESULT_OK, iNew);
    		finish();
    	} else if(preference.getKey().equals("server_delete")) {
    		setResult(RESULT_FIRST_USER, getIntent());
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
		if(EditTextPreference.class.equals(preference.getClass()) && ((EditTextPreference)preference).getEditText() != null && ((EditTextPreference)preference).getEditText().getTransformationMethod() != null)
			preference.setSummary(((EditTextPreference)preference).getEditText().getTransformationMethod().getTransformation(newValue.toString(), ((EditTextPreference)preference).getEditText()));
		else
			preference.setSummary(newValue.toString());
		if(preference.getKey().equals("server_host") && (!getIntent().hasExtra("name") || getIntent().getStringExtra("name") == null))
			onPreferenceChange(getPreferenceScreen().findPreference("server_name"), newValue);
		//preference.getExtras().putString("value", newValue.toString());
		Intent intent = getIntent();
		intent.putExtra(preference.getKey().replace("server_", ""), newValue.toString());
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
			String data = servers.getJSONArray(true, context).toString();
			//data = SimpleCrypto.encrypt(GetSignatureKey(context), data);
			w.write(data);
			w.close();
		} catch(IOException e) {
			Logger.LogError("Couldn't save OpenServers.", e);
		} catch (Exception e) {
			Logger.LogError("Problem encrypting servers?", e);
		} finally {
			try {
				if(w != null)
					w.close();
			} catch(IOException e2)
			{
				Logger.LogError("Couldn't close writer during error", e2);
			}
		}
	}
	public OpenServers LoadDefaultServers() { return LoadDefaultServers(getApplicationContext()); }
	public static OpenServers LoadDefaultServers(Context context)
	{
		File f = new File(context.getFilesDir().getPath(), "servers.json");
		Reader r = null;
		try {
			//getApplicationContext().openFileInput("servers.json"); //, Context.MODE_PRIVATE);
			if(!f.exists() && !f.createNewFile())
			{
				Logger.LogWarning("Couldn't create default servers file (" + f.getPath() + ")");
				return new OpenServers();
			} else {
				//Logger.LogDebug("Created default servers file (" + f.getPath() + ")");
				r = new BufferedReader(new FileReader(f));
				char[] chars = new char[256];
				StringBuilder sb = new StringBuilder();
				while(r.read(chars) > 0)
					sb.append(chars);
				r.close();
				if(sb.length() == 0)
					return new OpenServers();
				String data = sb.toString();
				return new OpenServers(new JSONArray(data), GetSignatureKey(context));
			}
		} catch (IOException e) {
			Logger.LogError("Error loading default server list.", e);
			return new OpenServers();
		} catch (JSONException e) {
			Logger.LogError("Error decoding JSON for default server list.", e);
			return new OpenServers();
		} finally {
			if(r != null)
				try {
					r.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
		}
		
	}
	
	public static String GetSignatureKey(Context context)
	{
		String ret = "";
		try {
			Signature[] sigs = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES).signatures;
			for(Signature sig : sigs)
				ret += sig.toCharsString();
		} catch (NameNotFoundException e) {
			Logger.LogError("No Package for Signature?", e);
		}
		return ret;
	}
}
