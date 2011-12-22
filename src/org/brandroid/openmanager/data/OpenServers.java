package org.brandroid.openmanager.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.brandroid.utils.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class OpenServers
{
	private static final long serialVersionUID = 6279070404986957630L;
	private ArrayList<OpenServer> mData = new ArrayList<OpenServer>(); 

	public OpenServers() { mData = new ArrayList<OpenServer>(); }
	public OpenServers(JSONArray arr)
	{
		this();
		if(arr == null) return;
		for(int i = 0; i < arr.length(); i++)
			try {
				add(new OpenServer(arr.getJSONObject(i)));
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}
	
	
	public boolean add(OpenServer value)
	{
		if(!value.isValid()) return false;
		return mData.add(value);
	}
	
	public OpenServer remove(int index)
	{
		return mData.remove(index);
	}
	
	public boolean set(int index, OpenServer value)
	{
		if(!value.isValid()) return false;
		if(index >= size())
			return add(value);
		else
		{
			try {
				mData.set(index, value);
			} catch(RuntimeException e) {
				Logger.LogError("Couldn't add server.", e);
				return false;
			}
			return true;
		}
	}
	
	public int size() { return mData.size(); }
	public OpenServer get(int index)
	{
		return mData.get(index);
	}
	
	public JSONArray getJSONArray() {
		JSONArray ret = new JSONArray();
		for(int i = 0; i < mData.size(); i++)
			ret.put(mData.get(i).getJSONObject());
		return ret;
	}
}
