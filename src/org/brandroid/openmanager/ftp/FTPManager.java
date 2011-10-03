package org.brandroid.openmanager.ftp;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Hashtable;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileFilter;
import org.brandroid.openmanager.FileManager.SortType;
import org.brandroid.utils.Logger;

public class FTPManager {
	public static Hashtable<String, FTPManager> instances = new Hashtable<String, FTPManager>();
	private static Hashtable<String, FTPFile> fileCache = new Hashtable<String, FTPFile>();
	
	private int mPort = 21;
	private String mHost = "", mUser = "", mPassword = "", mBasePath = "";
	private FTPClient client;
		
	public FTPManager() { }
	public FTPManager(String sFTPPath) throws MalformedURLException
	{
		URL url = new URL(sFTPPath);
		mUser = url.getUserInfo();
		if(mUser.indexOf(":") > -1)
		{
			mPassword = mUser.substring(mUser.indexOf(":")+1);
			mUser = mUser.substring(0,mUser.indexOf(":"));
		}
		mHost = url.getHost();
		mBasePath = url.getPath();
		instances.put("ftp://" + url.getUserInfo() + ":" + url.getHost(), this);
	}
	public FTPManager(URL url)
	{
		mUser = url.getUserInfo();
		if(mUser.indexOf(":") > -1)
		{
			mPassword = mUser.substring(mUser.indexOf(":")+1);
			mUser = mUser.substring(0,mUser.indexOf(":"));
		}
		mHost = url.getHost();
		mBasePath = url.getPath();
		instances.put("ftp://" + url.getUserInfo() + ":" + url.getHost(), this);
	}
	public FTPManager(String host, String user, String password, String basePath)
	{
		mHost = host;
		mUser = user;
		mPassword = password;
		mBasePath = basePath;
	}
	
	@Override
	protected void finalize() throws Throwable {
		if(client.isConnected())
			client.disconnect();
	}
	
	public static FTPFile getFTPFile(String name) { return fileCache.get(name); }
	
	public static FTPManager getInstance(String instanceName)
	{
		if(instances.containsKey(instanceName))
			return instances.get(instanceName);
		else return null;
	}
	public static FTPManager getInstance(URL url) throws MalformedURLException
	{
		FTPManager ret = getInstance("ftp://" + url.getUserInfo() + ":" + url.getHost());
		if(ret == null)
			ret = new FTPManager(url.toString());
		return ret;
	}
	public static void setInstance(String instanceName, FTPManager manager)
	{
		instances.put(instanceName, manager);
	}
	
	public void setHost(String host) { mHost = host; }
	public void setUser(String user) { mUser = user; }
	public void setPassword(String password) { mPassword = password; }
	public void setBasePath(String path) { mBasePath = path; }
	
	public Boolean connect() throws IOException
	{
		if(client == null)
			client = new FTPClient();
		if(!client.isConnected())
		{
			client.connect(mHost);
			client.login(mUser, mPassword);
			client.cwd(mBasePath);
		}
		
		if(client.isConnected()) return true;
		return false;
	}
	
	public FTPFile[] listAll() throws IOException
	{
		FTPFile[] files = listFiles();
		ArrayList<FTPFile> ret = new ArrayList<FTPFile>();
		for(FTPFile f : files)
			if(f.isDirectory())
				ret.add(f);
		for(FTPFile f : files)
			if(!f.isDirectory())
				ret.add(f);
		
		FTPFile[] retf = new FTPFile[ret.size()];
		ret.toArray(retf);
		return retf;
	}
	
	public FTPFile[] listDirectories() throws IOException
	{
		if(connect())
		{
			FTPFile[] ret = client.listDirectories();
			for(FTPFile f : ret)
				fileCache.put(f.getName(), f);
			return ret;
		}
		else return null;
	}
	public FTPFile[] listFiles() throws IOException
	{
		if(connect())
		{
			FTPFile[] ret = client.listFiles();
			for(FTPFile f : ret)
				fileCache.put(f.getName(), f);
			return ret;
		} else return null;
	}
	public InputStream getInputStream(String path) throws IOException
	{
		if(connect())
			return client.retrieveFileStream(path);
		else return null;
	}
}
