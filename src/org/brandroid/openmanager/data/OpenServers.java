package org.brandroid.openmanager.data;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import org.brandroid.utils.Logger;
import org.json.JSONException;
import org.json.JSONObject;

public class OpenServers
{
	private static final long serialVersionUID = 6279070404986957630L;
	private Hashtable<String, OpenServer> mData = new Hashtable<String, OpenServer>();

	public OpenServers() { super(); }
	public OpenServers(JSONObject obj)
	{
		super();
		if(obj == null) return;
		if(obj.keys() == null) return;
		Iterator keys = obj.keys();
		try {
			while(keys.hasNext())
			{
				String s = (String) keys.next();
				Logger.LogDebug("OpenServers added " + s);
				mData.put(s, new OpenServer(obj.optJSONObject(s)));
			}
			Logger.LogDebug("Done making OpenServers");
		} catch(NoSuchElementException e) {
			
		}
	}
	
	
	public OpenServers addServer(String key, OpenServer value)
	{
		mData.put(key, value);
		return this;
	}
	
	public OpenServers removeServer(String key)
	{
		mData.remove(key);
		return this;
	}
	
	public OpenServer get(String key)
	{
		return mData.get(key);
	}
	
	public boolean containsKey(String key)
	{
		return mData.containsKey(key);
	}

	public JSONObject getJSONObject() {
		JSONObject ret = new JSONObject();
		for(String s : mData.keySet())
			try {
				if(s != null)
					ret.put(s, mData.get(s).getJSONObject());
			} catch (JSONException e) {
				Logger.LogError("Couldn't put " + s + " into OpenServers", e);
			}
		return ret;
	}
	public Set<String> keySet() {
		return mData.keySet();
	}
}
