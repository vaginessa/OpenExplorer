package org.brandroid.openmanager.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import org.apache.http.HttpRequest;
import org.brandroid.utils.Logger;
import org.json.JSONObject;

import android.net.Uri;
import android.util.JsonReader;

public class OpenDropBox extends OpenNetworkPath
{
	private static String DB_KEY = "vjbm22k8blhj61g";
	private static String DB_SECRET = "rxseumehu4gts7r";
	private boolean connected = false;
	private OnAPIResponseListener mListener;
	
	public enum RequestType
	{
		request_token, authorize, access_token, account_info,
		files_get, file_put, files_post,
		metadata, delta, revisions, restore, search,
		shares, media, copy_ref, thumbnails,
		copy, create_folder, delete, move
	}
	
	public void setAPIResponseListener(OnAPIResponseListener l) { mListener = l; }
	
	public interface OnAPIResponseListener
	{
		public void OnResponse(RequestType type, Object result);
		public void OnRequestError(RequestType type, Exception exception, Object... params);
	}
	
	private void make_request(final String url, final RequestType type)
	{
		//new Thread(new Runnable(){public void run() {
				try {
					HttpsURLConnection c = (HttpsURLConnection)new URL(url)
						.openConnection();
					Map<String, List<String>> headers = c.getHeaderFields();
					for(String s : headers.keySet())
					{
						List<String> hdrs = headers.get(s);
						String val = hdrs.toArray(new String[1])[0];
						Logger.LogDebug("DB: " + s + "=" + val);
					}
					InputStream s = c.getInputStream(); 
					
				} catch (MalformedURLException e) {
					if(mListener != null)
						mListener.OnRequestError(type, e, url);
					e.printStackTrace();
				} catch (IOException e2) {
					if(mListener != null)
						mListener.OnRequestError(type, e2, url);
					e2.printStackTrace();
				}
		//}}).start();
	}
	
	private void request_token()
	{
		make_request("https://api.dropbox.com/1/oauth/request_token", RequestType.request_token);
	}

	@Override
	public boolean isConnected() throws IOException {
		return connected;
	}

	@Override
	public OpenNetworkPath[] getChildren() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getPath() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getAbsolutePath() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setPath(String path) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public long length() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public OpenPath getParent() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OpenPath getChild(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OpenPath[] list() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OpenPath[] listFiles() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Boolean isDirectory() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Boolean isFile() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Boolean isHidden() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Uri getUri() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Long lastModified() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Boolean canRead() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Boolean canWrite() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Boolean canExecute() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Boolean exists() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Boolean delete() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Boolean mkdir() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

}
