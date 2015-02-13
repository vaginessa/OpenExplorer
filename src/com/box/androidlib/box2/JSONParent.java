package com.box.androidlib.box2;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;

public class JSONParent {
	private final JSONObject joRoot;
	public JSONParent() {
		joRoot = new JSONObject();
	}
	public JSONParent(JSONObject jo) {
		joRoot = jo;
	}
	public String getString(String key, String defReturn)
	{
		if(joRoot != null)
			return joRoot.optString(key, defReturn);
		else return defReturn;
	}
	public Long getLong(String key, Long defReturn)
	{
		if(joRoot != null)
			return joRoot.optLong(key, defReturn);
		else return defReturn;
	}
	public final static JSONParent fromJSON(String jsonString)
	{
		JSONParent ret = null;
		try {
			ret = fromJSON(new JSONObject(jsonString));
		} catch(JSONException e) {
		}
		return ret;
	}
	public final static JSONParent fromJSON(JSONObject jo)
	{
		return new JSONParent(jo);
	}
	public JSONObject getRoot() { return joRoot; }
	public boolean has(String name) {
		return joRoot.has(name);
	}
}