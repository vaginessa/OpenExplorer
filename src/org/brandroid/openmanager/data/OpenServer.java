package org.brandroid.openmanager.data;

import java.util.Hashtable;
import java.util.Iterator;

import org.brandroid.utils.Logger;
import org.json.JSONException;
import org.json.JSONObject;

public class OpenServer
{
	private String mName, mHost, mPath, mUser, mPassword;
	//private Hashtable<String, String> mData = new Hashtable<String, String>();
	
	public OpenServer() {
		//mData = new Hashtable<String, String>();
	}
	public OpenServer(JSONObject obj)
	{
		mName = obj.optString("name");
		mHost = obj.optString("host");
		mPath = obj.optString("dir");
		mUser = obj.optString("user");
		mPassword = obj.optString("password");
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
	
	public JSONObject getJSONObject() {
		JSONObject ret = new JSONObject();
		try {
			ret.put("name", getName());
			ret.put("host", getHost());
			ret.put("user", getUser());
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
	public String getString(String key) {
		if(key.equals("name")) return getName();
		if(key.equals("host")) return getHost();
		if(key.equals("dir")) return getPath();
		if(key.equals("path")) return getPath();
		if(key.equals("user")) return getUser();
		if(key.equals("password")) return getPassword();
		return null;
	}
}
