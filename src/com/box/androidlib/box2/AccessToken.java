package com.box.androidlib.box2;

import java.util.Iterator;

import org.json.JSONException;
import org.json.JSONObject;



public class AccessToken extends JSONParent {

	public AccessToken() {
		super();
	}
	public AccessToken(JSONObject jo) {
		super(jo);
	}
	public String getAccessToken() {
		return getString("access_token", null);
	}
	public long getExpiresIn() {
		return getLong("expires_in", -1L);
	}
	public String getTokenType(String defType) { return getString("token_type", defType); }
	public String getTokenType() {
		return getString("token_type", null);
	}
	public String getRefreshToken() {
		return getString("refresh_token", null);
	}
	public AccessToken setAccessToken(String token) {
		try {
			getRoot().put("access_token", token);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return this;
	}
	public AccessToken setRefreshToken(String token) {
		try {
			getRoot().put("refresh_token", token);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return this;
	}
	public JSONObject toJSON() throws JSONException
	{
		return getRoot();
	}
	@Override
	public String toString() {
		String ret = "";
		try {
			ret = toJSON().toString();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return ret;
	}
	public void put(String key, Object value) {
		try {
			getRoot().put(key, value);
		} catch(JSONException je) {
			je.printStackTrace();
		}
	}
}