package com.box.androidlib.box2;

import org.json.JSONObject;

public class User extends JSONParent {
	
	public User(JSONObject jo) {
		super(jo);
	}
	public long getId() { return getLong("id", -1L); }
	public String getName() { return getString("name", null); }
	public String getLogin() { return getString("login", null); }
	public Long getSpaceAvailable() { return getLong("space_amount", -1L); }
	public Long getSpaceUsed() { return getLong("space_used", -1L); }
}
