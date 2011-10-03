package org.brandroid.openmanager.ftp;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Hashtable;

import org.brandroid.utils.Logger;

public class FTPManager {
	public static Hashtable<String, FTPManager> instances = new Hashtable<String, FTPManager>();
	
	private String mHost = "", mUser = "", mPassword = "", mBasePath = "";
	private FTPConnection mConnection = null;
	
	public FTPManager() { }
	public FTPManager(String host, String user, String password, String basePath)
	{
		mHost = host;
		mUser = user;
		mPassword = password;
		mBasePath = basePath;
	}
	
	public static FTPManager getInstance(String instanceName)
	{
		if(instances.containsKey(instanceName))
			return instances.get(instanceName);
		else return null;
	}
	public static void setInstance(String instanceName, FTPManager manager)
	{
		instances.put(instanceName, manager);
	}
	
	public void setHost(String host) { mHost = host; }
	public void setUser(String user) { mUser = user; }
	public void setPassword(String password) { mPassword = password; }
	public void setBasePath(String path) { mBasePath = path; }
	
	public String[] list()
	{
		return getConnection().listFiles().split("\n");
	}
	
	public String getFTPPath()
	{
		return "ftp://" + (mUser != "" ? mUser + (mPassword != "" ? ":" + mPassword : "") + "@" : "") + mHost + "/" + mBasePath;
	}
	public FTPConnection getConnection()
	{
		if(mConnection == null)
			mConnection = new FTPConnection(getFTPPath());
		return mConnection;
	}
}
