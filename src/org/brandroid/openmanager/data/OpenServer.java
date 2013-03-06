
package org.brandroid.openmanager.data;

import java.util.Hashtable;
import java.util.Iterator;

import org.brandroid.openmanager.activities.SettingsActivity;
import org.brandroid.utils.Logger;
import org.brandroid.utils.SimpleCrypto;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

public class OpenServer {
    private final JSONObject mData;

    public OpenServer() {
        mData = new JSONObject();
        // mData = new Hashtable<String, String>();
    }

    public OpenServer(JSONObject obj, String key) {
        mData = obj;
        //decryptPW(key);
        /*
         * mData = new Hashtable<String, String>(); if(obj != null) { Iterator
         * keys = obj.keys(); while(keys.hasNext()) { String key =
         * (String)keys.next(); setSetting(key, obj.optString(key,
         * obj.opt(key).toString())); } }
         */
    }

    public OpenServer(String host, String path, String user, String password) {
        mData = new JSONObject();
        // mData = new Hashtable<String, String>();
        setHost(host);
        setPath(path);
        setUser(user);
        setPassword(password);
    }
    
    public void decryptPW(final String key)
    {
        new Thread(new Runnable() {
            public void run() {
                try {
                    String mPassword = mData.optString("password");
                    mPassword = SimpleCrypto.decrypt(key, mPassword);
                    mData.put("password", mPassword);
                } catch (Exception e) {
                    Logger.LogError("Error decrypting password.", e);
                }
            }}).start();
    }

    public boolean isValid() {
        if(mData.optString("type","").equalsIgnoreCase("box"))
            return true;
        return mData != null && mData.has("host");
    }

    private static String[] getNames(JSONObject o) {
        JSONArray a = o.names();
        String[] ret = new String[a.length()];
        for (int i = 0; i < ret.length; i++)
            ret[i] = a.optString(i, a.opt(i).toString());
        return ret;
    }

    public JSONObject getJSONObject(Boolean encryptPW, Context context) {
        JSONObject ret = null;
        try {
            ret = new JSONObject(mData, getNames(mData));
        } catch (JSONException e1) {
            return null;
        }
        try {
            ret.put("dir", getPath());
        } catch (JSONException e) {
        }
        /*
         * for(String s : mData.keySet()) try { ret.put(s, mData.get(s)); }
         * catch (JSONException e) { // TODO Auto-generated catch block
         * e.printStackTrace(); }
         */
        return ret;
    }

    public String getType() {
        return mData.optString("type");
    }

    public OpenServer setSetting(String key, String value) {
        try {
            mData.put(key, value);
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        /*
         * if(key.equalsIgnoreCase("name")) mName = value; else
         * if(key.equalsIgnoreCase("host")) mHost = value; else
         * if(key.equalsIgnoreCase("user")) mUser = value; else
         * if(key.equalsIgnoreCase("password")) mPassword = value; else
         * if(key.equalsIgnoreCase("dir")) mPath = value; else
         * if(key.equalsIgnoreCase("type")) mType = value; else
         * if(key.equalsIgnoreCase("port")) mPort = Integer.parseInt(value);
         */
        /*
         * else if(key != null && value != null) { if(mData == null) mData = new
         * Hashtable<String, String>(); mData.put(key, value); }
         */
        return this;
    }

    public String getHost() {
        return mData.optString("host");
    }

    public String getPath() {
        String mPath = mData.optString("dir");
        return mPath + (mPath.equals("") || mPath.endsWith("/") ? "" : "/");
    }

    public String getUser() {
        return mData.optString("user");
    }

    public String getPassword() {
        return mData.optString("password");
    }

    public String getName() {
        String mName = mData.optString("name");
        return mName != null && !mName.equals("") ? mName : getHost();
    }

    public OpenServer setHost(String host) {
        setSetting("host", host);
        return this;
    }

    public OpenServer setPath(String path) {
        setSetting("path", path);
        return this;
    }

    public OpenServer setUser(String user) {
        setSetting("user", user);
        return this;
    }

    public OpenServer setPassword(String password) {
        setSetting("password", password);
//        final String key = SettingsActivity.GetSignatureKey();
//        new Thread(new Runnable() {
//            public void run() {
//                setSetting("password",
//                        SimpleCrypto.encrypt(key, password));
//            }}).start();
        return this;
    }

    public OpenServer setName(String name) {
        setSetting("name", name);
        return this;
    }

    public OpenServer setType(String type) {
        setSetting("type", type);
        return this;
    }

    public OpenServer setPort(int port) {
        setSetting("port", "" + port);
        return this;
    }

    public boolean has(String name) {
        return mData.has(name);
    }

    public String get(String name, String defValue) {
        return mData.optString(name, defValue);
    }

    public String getString(String key) {
        if (key.equals("name"))
            return getName();
        if (key.equals("host"))
            return getHost();
        if (key.equals("dir"))
            return getPath();
        if (key.equals("path"))
            return getPath();
        if (key.equals("user"))
            return getUser();
        if (key.equals("password"))
            return getPassword();
        if (key.equals("type"))
            return getType();
        if (key.equals("port"))
            return "" + getPort();
        return mData.optString(key);
    }

    public int getPort() {
        try {
            return Integer.parseInt(mData.optString("port"));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    @Override
    public String toString() {
        String ret = getType();
        ret += "://";
        if (!getUser().equals("")) {
            ret += getUser();
            //            if (!getPassword().equals(""))
            //                ret += ":" + getPassword().replaceAll(".", "*");
            ret += "@";
        }
        ret += getHost();
        if (getPort() > 0)
            ret += ":" + getPort();
        ret += getPath();
        return ret;
    }
}
