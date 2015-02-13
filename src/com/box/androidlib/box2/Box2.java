package com.box.androidlib.box2;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.parsers.FactoryConfigurationError;

import org.brandroid.openmanager.activities.OpenExplorer;
import org.brandroid.openmanager.util.PrivatePreferences;
import org.brandroid.utils.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;

import com.box.androidlib.BoxSynchronous;
import com.box.androidlib.GetAccessTokenListener;

import android.net.Uri;
import android.net.Uri.Builder;
import android.os.Handler;
import android.widget.Toast;

public class Box2 {
	private static final Handler mHandler = OpenExplorer.getHandler();
	private static final boolean mLoggingEnabled = true;
	private final String mApiKey;
	private AccessToken mAccessToken = new AccessToken();
	private User mUser = null;
	private final ArrayList<String> mRequestLocks = new ArrayList<String>();
	
	public Box2(String apiKey)
	{
		mApiKey = apiKey;
	}
	public static Box2 getInstance(String apiKey)
	{
		return new Box2(apiKey);
	}
	public AccessToken getAccessToken() { return mAccessToken; }
	public void setAccessToken(AccessToken token) { mAccessToken = token; }
	
	public String getApiKey() { return mApiKey; }

    public interface JSONObjectResponseListener {
    	public void onJSONObjectResponse(JSONObject jo);
    	public void onException(Exception e);
    }
    
    public User getUser() {
    	return mUser;
    }

    public JSONObject jsonRequest(final Uri uri) throws IOException
    {
    	return jsonRequest(uri, null, null, null, true);
    }
    public JSONObject jsonRequest(final Uri uri, final String postParams) throws IOException
    {
    	return jsonRequest(uri, postParams, null, null, true);
    }
    public JSONObject jsonRequest(final Uri uri, final AccessToken access) throws IOException
    {
    	return jsonRequest(uri, null, access, null, true);
    }
    public JSONObject jsonRequest(final Uri uri, final String postParams,
    		final AccessToken access, final JSONObjectResponseListener listener,
    		final boolean allowRefresh
    		) throws IOException {
        Uri theUri = uri;
        
        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();
        JSONObject ret = null;
        final String lockKey = uri.toString() + " " + postParams;
        if(mRequestLocks.contains(lockKey)) return null;
        mRequestLocks.add(lockKey);
        try {
            
            HttpURLConnection conn = (HttpURLConnection) (new URL(theUri.toString())).openConnection();
//            conn.setRequestProperty("User-Agent", "");
//            conn.setRequestProperty("Accept-Language", "");
            if(access != null && access.getTokenType("bearer") == "bearer")
            	conn.setRequestProperty("Authorization", "Bearer " + access.getAccessToken());
            conn.setConnectTimeout(BoxConfig.getInstance().getConnectionTimeOut());
            if (mLoggingEnabled) {
            	DevUtils.logcat("URL: " + theUri.toString() + (postParams != null ? "\n-d " + postParams : ""));
//                Iterator<String> keys = conn.getRequestProperties().keySet().iterator();
//                while (keys.hasNext()) {
//                    String key = keys.next();
//                    DevUtils.logcat("Request Header: " + key + " => " + conn.getRequestProperties().get(key));
//                }
            }

            int responseCode = -1;
            try {
            	if(postParams != null)
            	{
	            	conn.setRequestMethod("POST");
	            	conn.setDoOutput(true);
	            	conn.setDoInput(true);
	            	OutputStream os = conn.getOutputStream();
	            	BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
	            	writer.write(postParams);
	            	writer.flush();
	            	writer.close();
	            	os.close();
            	}
                conn.connect();
                responseCode = conn.getResponseCode();
                mRequestLocks.remove(lockKey);
                if(mLoggingEnabled)
                    DevUtils.logcat("Good Response Code: " + responseCode);
//                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream inputStream = conn.getInputStream();
                    br = new BufferedReader(new InputStreamReader(inputStream));
                    String line = "";
                    while((line = br.readLine()) != null)
                    {
                        sb.append(line);
                    }
                    if(mLoggingEnabled)
                        DevUtils.logcat("Response Data: " + sb.toString());
                	JSONParser jp = new JSONParser();
                	try {
                		ret = new JSONObject(sb.toString());
                		if(listener != null)
                			listener.onJSONObjectResponse(ret);
                	} catch(JSONException je) {
                		throw je;
                	}
                    inputStream.close();
//                }
            }
            catch (IOException e) {
                try {
                    responseCode = conn.getResponseCode();
                    if(mLoggingEnabled)
                        DevUtils.logcat("Exception Response Code: " + responseCode);
                }
                catch (NullPointerException ee) {
                    // Honeycomb devices seem to throw a null pointer exception sometimes which traces to HttpURLConnectionImpl.
                }
                // Server returned a 503 Service Unavailable. Usually means a temporary unavailability.
                if (responseCode == HttpURLConnection.HTTP_UNAVAILABLE) {
                    //parser.setStatus(ResponseListener.STATUS_SERVICE_UNAVAILABLE);
                }
                else {
                    throw e;
                }
            }
            finally {
                conn.disconnect();
            	if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED && allowRefresh)
                	if(refreshAccessToken(getAccessToken().getRefreshToken()) != null)
                		return jsonRequest(uri, postParams, access, listener, false);
            }
        }
        catch (JSONException je) {
        	if(listener != null)
        		listener.onException(je);
        }
        catch (final FactoryConfigurationError e) {
            e.printStackTrace();
        }
        return ret;
    }
    
    public final User getAccountInfo() throws IOException
    {
    	return getAccountInfo(getAccessToken());
    }
    
    public final User getAccountInfo(final AccessToken access) throws IOException {
    	Uri uri = new Uri.Builder()
    		.scheme("https")
    		.authority("api.box.com")
    		.appendPath("2.0")
    		.appendPath("users")
    		.appendPath("me")
    		.build();
    	Logger.LogDebug("Requesting account info", new Exception());
    	mUser = new User(jsonRequest(uri, access));
    	Logger.LogDebug("User: " + mUser.toString());
    	return mUser;
    }
    
    public final void getAccountInfo(final AccessToken access, final ResponseListener listener)
    {
    	new Thread() {

            @Override
            public void run() {
                try {
                    final User user = getAccountInfo(access);
                    if(listener != null)
                    mHandler.post(new Runnable() {

                        @Override
                        public void run() {
                        	listener.onComplete(user);
                        }
                    });
                }
                catch (final IOException e) {
                	if(listener != null)
                    mHandler.post(new Runnable() {

                        @Override
                        public void run() {
                            listener.onIOException(e);
                        }
                    });
                }
            }
        }.start();
    }
    public final AccessToken getAccessToken(final String code) throws IOException {
    	Uri url = new Builder()
    		.scheme("https")
    		.authority("app.box.com")
    		.appendPath("api")
    		.appendPath("oauth2")
    		.appendPath("token")
			.build();
    	String postParams = new Builder()
	    	.appendQueryParameter("grant_type", "authorization_code")
			.appendQueryParameter("code", code)
			.appendQueryParameter("client_id", PrivatePreferences.getBoxAPIKey())
			.appendQueryParameter("client_secret", PrivatePreferences.getKey("box_client_secret"))
			.build().getQuery();
    	JSONObject jo = jsonRequest(url, postParams);
    	return new AccessToken(jo);
    }
    public final AccessToken refreshAccessToken(final String refreshToken) throws IOException {
    	Uri url = new Builder()
    		.scheme("https")
    		.authority("app.box.com")
    		.appendPath("api")
    		.appendPath("oauth2")
    		.appendPath("token")
    		.build();
    	String postParams = new Builder()
    		.appendQueryParameter("grant_type", "refresh_token")
    		.appendQueryParameter("refresh_token", refreshToken)
    		.appendQueryParameter("client_id", PrivatePreferences.getBoxAPIKey())
    		.appendQueryParameter("client_secret", PrivatePreferences.getKey("box_client_secret"))
    		.build().getQuery();
    	JSONObject jo = jsonRequest(url, postParams);
    	return new AccessToken(jo);
    }
    public final AccessToken refreshAccessToken() throws IOException
    {
    	mAccessToken = refreshAccessToken(getAccessToken().getRefreshToken());
    	return mAccessToken;
    }
    public final void logout(final String token) throws IOException {
    	Uri url = new Builder()
			.scheme("https")
			.authority("api.box.com")
			.appendPath("oauth2")
			.appendPath("revoke")
			.build();
		String postParams = new Builder()
			.appendQueryParameter("client_id", mApiKey)
			.appendQueryParameter("client_secret", PrivatePreferences.getKey("box_client_secret"))
			.appendQueryParameter("token", token)
			.build().getQuery();
		jsonRequest(url, postParams);
    }
	public ArrayList<JSONParent> listFiles(String folderId) throws IOException {
		ArrayList<JSONParent> ret = new ArrayList<JSONParent>();
		Uri url = new Builder()
			.scheme("https")
			.authority("api.box.com")
			.appendPath("2.0")
			.appendPath("folder")
			.appendPath(folderId)
			.appendPath("items")
			.appendQueryParameter("limit", "1000")
			.build();
		JSONObject json = jsonRequest(url, getAccessToken());
		if(json != null)
		{
			processCollection(json, ret);
			processCollection(json.optJSONObject("path_collection"), ret);
			processCollection(json.optJSONObject("item_collection"), ret);
		}
		return ret;
	}
	
	private void processCollection(JSONObject collection, ArrayList<JSONParent> array)
	{
		JSONArray ret = new JSONArray();
		if(collection != null)
		{
			if(collection.has("entries"))
				ret = collection.optJSONArray("entries");
		}
		for(int i = 0; i < ret.length(); i++)
		{
			JSONObject item = ret.optJSONObject(i);
			if(item != null)
				array.add(item.optString("type","folder") == "folder" ?
						new Folder(item) : new Item(item));
		}
	}

    
    public final void getAccessToken(final String code, final ResponseListener listener) {
    	new Thread() {
    		@Override
    		public void run() {
    			try {
                    final AccessToken at = getAccessToken(code);
                    
                    mHandler.post(new Runnable() {

                        @Override
                        public void run() {
                            listener.onComplete(at);
                        }
                    });
                }
                catch (final IOException e) {
                    mHandler.post(new Runnable() {

                        @Override
                        public void run() {
                            listener.onIOException(e);
                        }
                    });
                }
    		}
    	}.start();
    }
    
	public String getAuthUri() {
		return "https://app.box.com/api/oauth2/authorize?response_type=code&client_id=" +
        		mApiKey + "&state=security_token%3DKnhMJatFipTAnM0nHlZA&redirect_uri=http://127.0.0.1";
	}
	public void logout(final String token, final ResponseListener listener) {
		new Thread() {
    		@Override
    		public void run() {
    			try {
                    logout(token);
                    
                    if(listener != null)
                    mHandler.post(new Runnable() {

                        @Override
                        public void run() {
                            listener.onComplete(null);
                        }
                    });
                }
                catch (final IOException e) {
                	if(listener != null)
                    mHandler.post(new Runnable() {

                        @Override
                        public void run() {
                            listener.onIOException(e);
                        }
                    });
                }
    		}
    	}.start();	
	}
}
