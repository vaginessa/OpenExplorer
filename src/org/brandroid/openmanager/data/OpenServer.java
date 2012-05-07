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
	private int mPort = 0;
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
		mPort = obj.optInt("port", mPort);
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
		else if(key.equalsIgnoreCase("port"))
			mPort = Integer.parseInt(value);
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
		return mPath + (mPath.equals("") || mPath.endsWith("/") ? "" : "/");
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
	public OpenServer setPort(int port) {
		mPort = port;
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
		if(key.equals("port")) return ""+getPort();
		return null;
	}
	
	public static void setupServerDialog(final OpenServer server, final int iServersIndex,
			final View parentView)
	{
		final EditText mHost = (EditText)parentView.findViewById(R.id.text_server);
		final EditText mUser = (EditText)parentView.findViewById(R.id.text_user);
		final EditText mPassword = (EditText)parentView.findViewById(R.id.text_password);
		final EditText mTextPath = (EditText)parentView.findViewById(R.id.text_path);
		final EditText mTextName = (EditText)parentView.findViewById(R.id.text_name);
		final CheckBox mCheckPassword = (CheckBox)parentView.findViewById(R.id.check_password);
		final Spinner mTypeSpinner = (Spinner)parentView.findViewById(R.id.server_type);
		final EditText mTextPort = (EditText)parentView.findViewById(R.id.text_port);
		final CheckBox mCheckPort = (CheckBox)parentView.findViewById(R.id.check_port);
		if(iServersIndex > -1)
		{
			//mCheckPassword.setVisibility(View.GONE);
			mHost.setText(server.getHost());
			mUser.setText(server.getUser());
			mPassword.setText(server.getPassword());
			mTextPath.setText(server.getPath());
			mTextName.setText(server.getName());
			if(server.getPort() > 0)
			{
				mCheckPort.setChecked(false);
				mTextPort.setText(""+server.getPort());
			} else mCheckPort.setChecked(true);
			String[] types = mTypeSpinner.getResources().getStringArray(R.array.server_types_values);
			int pos = 0;
			for(int i=0; i<types.length; i++)
				if(types[i].equals(server.getType()))
				{
					pos = i;
					break;
				}
			mTypeSpinner.setSelection(pos);
		}

		mHost.setOnFocusChangeListener(new OnFocusChangeListener() {
			public void onFocusChange(View v, boolean hasFocus) {
				if(!hasFocus && server.getName().equals(""))
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
		mTextPort.setEnabled(!mCheckPort.isChecked());
		if(!mCheckPort.isChecked())
			mCheckPort.setText("");
		mTextPort.addTextChangedListener(new TextWatcher() {
			public void onTextChanged(CharSequence s, int start, int before, int count) { }
			public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
			public void afterTextChanged(Editable s) {
				if(!s.toString().equals("") && !s.toString().matches("[^0-9]"))
					server.setPort(Integer.parseInt(s.toString()));
			}
		});
		mCheckPort.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mTextPort.setEnabled(!isChecked);
				if(!isChecked)
					server.setPort(-1);
				else
				{
					try { 
						server.setPort(Integer.parseInt(mTextPort.getText().toString()));
					} catch(Exception e) { Logger.LogWarning("Invalid Port: " + mTextPort.getText().toString()); }
				}
			}
		});
	}
	public int getPort() {
		return mPort;
	}
}
