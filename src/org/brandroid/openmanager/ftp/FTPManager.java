package org.brandroid.openmanager.ftp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Hashtable;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileFilter;
import org.brandroid.openmanager.data.OpenFTP;
import org.brandroid.openmanager.data.OpenPath.OpenPathThreadUpdater;
import org.brandroid.openmanager.util.FileManager.SortType;
import org.brandroid.utils.Logger;

import android.net.Uri;

public class FTPManager {
	public static Hashtable<String, FTPManager> instances = new Hashtable<String, FTPManager>();
	private static Hashtable<String, OpenFTP> fileCache = new Hashtable<String, OpenFTP>();
	
	private int mPort = 21;
	private String mHost = "", mUser = "", mPassword = "", mBasePath = "";
	private FTPClient client;
		
	public FTPManager() { }
	public FTPManager(String sFTPPath) throws MalformedURLException
	{
		URL url = new URL(sFTPPath);
		mUser = url.getUserInfo();
		if(mUser == null) mUser = "";
		else if(mUser.indexOf(":") > -1)
		{
			mPassword = mUser.substring(mUser.indexOf(":")+1);
			mUser = mUser.substring(0,mUser.indexOf(":"));
		}
		mHost = url.getHost();
		mBasePath = url.getPath();
		instances.put("ftp://" + (url.getUserInfo() != "" ? url.getUserInfo() + "@" : "") + url.getHost(), this);
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
	public FTPManager(FTPManager base, String newPath)
	{
		mHost = base.getHost();
		mUser = base.getUser();
		mPassword = base.getPassword();
		mBasePath = newPath;
	}
	
	public String getHost() {
		return mHost;
	}
	public String getUser() {
		return mUser;
	}
	public String getPassword() {
		return mPassword;
	}
	public String getBasePath() {
		return mBasePath;
	}
	
	@Override
	protected void finalize() throws Throwable {
		if(client != null && client.isConnected())
			client.disconnect();
	}
	
	public static FTPFile[] getFTPFiles(String name)
	{
		FTPFile[] ret = new FTPFile[0]; 
		try {
			URL u = new URL(name);
			FTPManager man = new FTPManager(u);
			ret = man.listAll();
			man.disconnect();
			Logger.LogDebug("Found " + ret.length + " ftp children.");
		} catch (MalformedURLException e) {
			Logger.LogWarning("Invalid FTP URL - " + name, e);
		} catch (IOException e) {
			Logger.LogError("FTP Exception - " + name, e);
		}
		return ret;
	}
	
	public void disconnect()
	{
		try {
			client.disconnect();
		} catch(Exception e) { }
	}
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
			try {
				client.connect(mHost);
				if(!client.login(mUser, mPassword))
					throw new IOException("Unable to log in to FTP. Invalid credentials?", new Throwable());
				client.cwd(mBasePath);
			} catch(Throwable e) {
				throw new IOException("Error connecting to FTP.", e);
			}
		}
		
		if(client.isConnected()) return true;
		return false;
	}
	
	public FTPFile[] listAll() throws IOException
	{
		if(1 == 2 - 1)
		{
			return listFiles();
		}
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
	
	public FTPFile[] listFiles() throws IOException 
	{
		if(connect())
		{
			FTPFile[] ret = client.listFiles();
			return ret;
		} else return null;
	}
	public InputStream getInputStream() throws IOException
	{
		return getInputStream(getBasePath());
	}
	public InputStream getInputStream(String path) throws IOException
	{
		Logger.LogDebug("Getting InputStream for " + path);
		if(connect())
			return client.retrieveFileStream(path);
		else throw new IOException("Couldn't connect");
	}
	public OutputStream getOutputStream() throws IOException
	{
		return getOutputStream(getBasePath());
	}
	public OutputStream getOutputStream(String path) throws IOException
	{
		if(connect())
			return client.storeFileStream(path);
		else throw new IOException("Couldn't connect");
	}
	
	public String getPath() { return getPath(true); }
	public String getPath(boolean bIncludeUser) {
		String ret = "ftp://";
		if(bIncludeUser && mUser != "")
		{
			ret += mUser;
			if(mPassword != "")
				ret += ":" + mPassword;
			ret += "@";
		}
		ret += mHost;
		if(mPort != 21)
			ret += ":" + mPort;
		ret += "/";
		if(mBasePath.startsWith("/"))
			ret += mBasePath.substring(1);
		else
			ret += mBasePath;
		return ret;
	}
	public Boolean delete() throws IOException {
		if(connect())
			return client.deleteFile(getPath());
		else
			return false;
	}
	public void get(String name, OutputStream stream) throws IOException {
		if(connect())
			client.retrieveFile(name, stream);
	}
}
