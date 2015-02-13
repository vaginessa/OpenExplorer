package com.box.androidlib.box2;

import org.json.JSONObject;

public class Item extends JSONParent {
	public Item(JSONObject jo) {
		super(jo);
	}
	
	public Long getETag() { return getLong("etag", -1L); }
	public Long getSize() { return getLong("size", -1L); }
	public String getSHA1() { return getString("sha1", ""); }
	public String getName() { return getString("name", null); }
	public Long getId() { return getLong("id", -1L); }
}
