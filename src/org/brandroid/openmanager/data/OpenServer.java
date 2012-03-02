package org.brandroid.openmanager.data;

import java.util.Hashtable;
import java.util.Iterator;

import org.brandroid.openmanager.R;
import org.brandroid.openmanager.activities.SettingsActivity;
import org.brandroid.utils.Logger;
import org.brandroid.utils.SimpleCrypto;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.text.method.SingleLineTransformationMethod;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Spinner;

public class OpenServer
{
	private String mName = "",
				mType = "ftp",
				mHost = "",
				mPath = "",
				mUser = "",
				mPassword = "";
	//private Hashtable<String, String> mData = new Hashtable<String, String>();
	
	public OpenServer() {
		//mData = new Hashtable<String, String>();
	}
	public OpenServer(JSONObject obj, String decryptPW)
	{
		mName = obj.optString("name");
		mHost = obj.optString("host");
		mPath = obj.optString("dir");
		mUser = obj.optString("user");
		mType = obj.optString("type", mType);
		mPassword = obj.optString("password");
		if(decryptPW != null && decryptPW != "")
			try {
				mPassword = SimpleCrypto.decrypt(decryptPW, mPassword);
			} catch (Exception e) {
				Logger.LogError("Error decrypting password.", e);
			}
		/*mData = new Hashtable<String, String>();
		if(obj != null)
		{
			Iterator keys = obj.keys();
			while(keys.hasNext())
			{
				String key = (String)keys.next();
				setSetting(key, obj.optString(key, obj.opt(key).toString()));
			}
		}*/
	}
	public OpenServer(String host, String path, String user, String password)
	{
		//mData = new Hashtable<String, String>();
		setHost(host);
		setPath(path);
		setUser(user);
		setPassword(password);
	}
	public boolean isValid() { return mHost != null; }
	
	public JSONObject getJSONObject(Boolean encryptPW, Context context) {
		JSONObject ret = new JSONObject();
		try {
			ret.put("name", getName());
			ret.put("host", getHost());
			ret.put("user", getUser());
			ret.put("type", getType());
			if(encryptPW && context != null)
				try {
					ret.put("password", SimpleCrypto.encrypt(SettingsActivity.GetSignatureKey(context), getPassword()));
				} catch (Exception e) {
					ret.put("password", getPassword());
				}
			else
				ret.put("password", getPassword());
			ret.put("dir", getPath());
		} catch(JSONException e) { }
		/*for(String s : mData.keySet())
			try {
				ret.put(s, mData.get(s));
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/
		return ret;
	}
	public String getType() {
		return mType;
	}
	public OpenServer setSetting(String key, String value)
	{
		if(key.equalsIgnoreCase("name"))
			mName = value;
		else if(key.equalsIgnoreCase("host"))
			mHost = value;
		else if(key.equalsIgnoreCase("user"))
			mUser = value;
		else if(key.equalsIgnoreCase("password"))
			mPassword = value;
		else if(key.equalsIgnoreCase("dir"))
			mPath = value;
		else if(key.equalsIgnoreCase("type"))
			mType = value;
		/*else if(key != null && value != null) {
			if(mData == null)
				mData = new Hashtable<String, String>();
			mData.put(key, value);
		}*/
		return this;
	}
	public String getHost()
	{
		return mHost;
	}
	public String getPath()
	{
		return mPath;
	}
	public String getUser()
	{
		return mUser;
	}
	public String getPassword()
	{
		return mPassword;
	}
	public String getName() { return mName != null && !mName.equals("") ? mName : mHost; }
	public OpenServer setHost(String host) {
		mHost = host;
		if(mName == null || mName.equals(""))
			mName = host;
		return this;
	}
	public OpenServer setPath(String path) {
		mPath = path;
		return this;
	}
	public OpenServer setUser(String user) {
		mUser = user;
		return this;
	}
	public OpenServer setPassword(String password) {
		mPassword = password;
		return this;
	}
	public OpenServer setName(String name) {
		mName = name;
		return this;
	}
	public OpenServer setType(String type) {
		mType = type;
		return this;
	}
	public String getString(String key) {
		if(key.equals("name")) return getName();
		if(key.equals("host")) return getHost();
		if(key.equals("dir")) return getPath();
		if(key.equals("path")) return getPath();
		if(key.equals("user")) return getUser();
		if(key.equals("password")) return getPassword();
		if(key.equals("type")) return getType();
		return null;
	}
	
	public static void setupServerDialog(final OpenServer server, final EditText mHost,
			final EditText mUser, final EditText mPassword, final EditText mTextPath,
			final EditText mTextName, final CheckBox mCheckPassword,
			final Spinner mTypeSpinner)
	{
		mHost.setOnFocusChangeListener(new OnFocusChangeListener() {
			public void onFocusChange(View v, boolean hasFocus) {
				if(!hasFocus && server.getName() == "")
					mTextName.setText(mHost.getText());
			}
		});
		mTypeSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id) {
				String[] types = arg0.getResources().getStringArray(R.array.server_types_values);
				if(position >= types.length || position < 0) return;
				String type = types[position];
				server.setType(type);
			}
			@Override
			public void onNothingSelected(AdapterView<?> arg0) { }
		});
		if(mCheckPassword.getVisibility() == View.VISIBLE)
		mCheckPassword.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked)
				{
					mPassword.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
					mPassword.setTransformationMethod(new SingleLineTransformationMethod());
				} else {
					mPassword.setRawInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
					mPassword.setTransformationMethod(new PasswordTransformationMethod());
				}
			}
		});
		mHost.addTextChangedListener(new TextWatcher() {
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			public void afterTextChanged(Editable s) {
				server.setHost(s.toString());
			}
		});
		mUser.addTextChangedListener(new TextWatcher() {
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			public void afterTextChanged(Editable s) {
				server.setUser(s.toString());
			}
		});
		mPassword.addTextChangedListener(new TextWatcher() {
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			public void afterTextChanged(Editable s) {
				server.setPassword(s.toString());
			}
		});
		mTextPath.addTextChangedListener(new TextWatcher() {
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			public void afterTextChanged(Editable s) {
				server.setPath(s.toString());
			}
		});
		mTextName.addTextChangedListener(new TextWatcher() {
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			public void afterTextChanged(Editable s) {
				server.setName(s.toString());
			}
		});
	}
}
