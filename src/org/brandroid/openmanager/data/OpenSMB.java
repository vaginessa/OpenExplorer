package org.brandroid.openmanager.data;

import jcifs.smb.AllocInfo;
import jcifs.smb.Handler;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbAuthException;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbShareInfo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.SoftReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.ArrayList;

import org.brandroid.openmanager.activities.OpenExplorer;
import org.brandroid.openmanager.adapters.OpenPathDbAdapter;
import org.brandroid.openmanager.util.EventHandler.BackgroundWork;
import org.brandroid.openmanager.util.FileManager;
import org.brandroid.openmanager.util.FileManager.SortType;
import org.brandroid.utils.Logger;

import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;

public class OpenSMB extends OpenNetworkPath
{
	private SmbFile mFile;
	private OpenSMB mParent;
	private OpenSMB[] mChildren = null; 
	private Long mSize = null;
	private Long mModified = null;
	private Boolean mHidden = false;
	private Long mDiskSpace = 0l;
	private Long mDiskFreeSpace = 0l;
	
	public OpenSMB(String urlString) throws MalformedURLException
	{
		URL url = new URL(null, urlString, Handler.SMB_HANDLER);
		NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication(url.getUserInfo());
		if(auth.getPassword() == null || auth.getPassword() == "")
		{
			OpenServers servers = OpenServers.DefaultServers;
			OpenServer s = servers.findByUser("smb", url.getHost(), auth.getUsername());
			if(s != null)
			{
				auth.setUsername(s.getUser());
				auth.setPassword(s.getPassword());
			}
		}
		mFile = new SmbFile(url, auth);
		mParent = null;
		mHidden = null;
		mSize = mModified = null;
	}
	public OpenSMB(SmbFile file)
	{
		mFile = file;
		mParent = null;
		mHidden = null;
		mSize = mModified = null;
	}
	public OpenSMB(OpenSMB parent, SmbFile kid)
	{
		mParent = parent;
		mFile = kid;
		try {
			if(kid.isConnected())
			{
				mSize = mFile.length();
				mModified = mFile.lastModified();
				mHidden = mFile.isHidden();
			}
		} catch (SmbException e) {
			Logger.LogError("Error creating SMB", e);
		}
	}
	public OpenSMB(String urlString, long size, long modified) throws MalformedURLException
	{
		URL url = new URL(null, urlString, Handler.SMB_HANDLER);
		NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication(url.getUserInfo());
		if(auth.getUsername() == null || auth.getUsername() == "" || auth.getPassword() == null || auth.getPassword() == "")
		{
			OpenServers servers = OpenServers.DefaultServers;
			OpenServer s = servers.findByUser("smb", url.getHost(), auth.getUsername());
			if(s == null)
				s = servers.findByHost("smb", url.getHost());
			if(s != null)
			{
				auth.setUsername(s.getUser());
				auth.setPassword(s.getPassword());
			}
		}
		mFile = new SmbFile(url, auth);
		mParent = null;
		mSize = size;
		mModified = modified;
	}
	
	@Override
	public void disconnect() {
		super.disconnect();
		mFile.disconnect();
	}

	@Override
	public String getName() {
		String ret = getName(mFile.getPath());
		if(ret.endsWith("/"))
			ret = ret.substring(ret.lastIndexOf("/", ret.lastIndexOf("/") - 1) + 1);
		else
			ret = ret.substring(ret.lastIndexOf("/") + 1);
		if(ret.indexOf("@") > -1)
			ret = ret.substring(ret.indexOf("@") + 1);
		if(ret.equals(""))
			ret = mFile.getName();
		if(ret.equals("") || ret.equals("/"))
			ret = mFile.getServer();
		return ret;
	}

	@Override
	public String getPath() {
		URL url = mFile.getURL();
		String user = url.getUserInfo();
		if(user != null)
		{
			if(user.indexOf(":") > -1)
				user = user.substring(0, user.indexOf(":"));
			if(!user.equals(""))
				user += "@";
		} else user = "";
		return url.getProtocol() + "://" + user + url.getHost() + url.getPath();
	}

	@Override
	public long length() {
		if(mSize != null) return mSize;
		if(isDirectory()) return 0l;
		try {
			mSize = mFile.length();
			return mSize;
		} catch(Exception e) {
			Logger.LogError("Couldn't get SMB length", e);
			return 0l;
		}
	}
	
	@Override
	public String getAbsolutePath() {
		return mFile.getCanonicalPath();
	}

	@Override
	public void setPath(String path) {
		
	}
	
	@Override
	public void setName(String name) {
		mName = name;
	}

	@Override
	public OpenSMB getParent() {
		if(mParent != null)
			return mParent;
		else {
			try {
				if(!Thread.currentThread().equals(OpenExplorer.UiThread))
					return new OpenSMB(new SmbFile(mFile.getParent(), mFile.getAuth()));
				String parent = OpenPath.getParent(getPath());
				if(parent == null) return null;
				if(!parent.startsWith("smb://"))
					parent = "smb://" + parent;
				return new OpenSMB(new SmbFile(parent, mFile.getAuth()));
			} catch (MalformedURLException e) {
				Logger.LogError("Couldn't get SMB Parent.", e);
				return null;
			}
		}
	}

	@Override
	public OpenPath getChild(String name)
	{
		try {
			for(OpenSMB kid : listFiles())
			{
				if(kid.getName().equalsIgnoreCase(name))
					return kid;
			}
		} catch(IOException e) { }
		return null;
	}

	@Override
	public OpenSMB[] list() throws IOException {
		return listFiles();
	}

	@Override
	public OpenSMB[] listFiles() throws IOException {
		if(mChildren != null)
			return mChildren;
		if(Thread.currentThread().equals(OpenExplorer.UiThread))
			return null;
		AllocInfo disk = mFile.getDiskInfo();
		if(disk != null)
		{
			mDiskSpace = disk.getCapacity();
			mDiskFreeSpace = disk.getFree();
		}
		Logger.LogInfo("Listing children under " + getPath());
		SmbFile[] kids = null;
		try {
			kids = mFile.listFiles();
		} catch(SmbAuthException e) {
			Logger.LogWarning("Unable to authenticate. Trying to get password from Servers.");
			mFile.disconnect();
			String path = getServerPath(mFile.getPath());
			URL url = new URL(null, path, Handler.SMB_HANDLER);
			NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication(url.getUserInfo());
			mFile = new SmbFile(mFile.getPath(), auth);
			kids = mFile.listFiles();
		}
		if(kids != null)
		{
			mChildren = new OpenSMB[kids.length];
			for(int i = 0; i < kids.length; i++)
			{
				kids[i].setAuth(mFile.getAuth());
				OpenSMB smb = new OpenSMB(this, kids[i]);
				mChildren[i] = smb;
				FileManager.setOpenCache(smb.getPath(), smb);
			}
		}
		return mChildren;
	}
	
	@Override
	public OpenSMB[] getChildren() { return mChildren; }

	@Override
	public Boolean isDirectory() {
		return mFile.getName().endsWith("/");
	}

	@Override
	public Boolean isFile() {
		return !isDirectory();
	}

	@Override
	public Boolean isHidden() {
		try {
			if(mHidden != null || Thread.currentThread().equals(OpenExplorer.UiThread))
				return mHidden != null ? mHidden : false;
			else
				return mFile.isHidden();
		} catch (Exception e) {
			return true;
		}
	}

	@Override
	public Uri getUri() {
		return Uri.parse(mFile.getPath());
	}

	@Override
	public Long lastModified() {
		if(mModified != null || Thread.currentThread().equals(OpenExplorer.UiThread))
			return mModified != null ? mModified : 0;
		else
			return mFile.getLastModified();
	}

	@Override
	public Boolean canRead() {
		try {
			if(Thread.currentThread().equals(OpenExplorer.UiThread))
				return true;
			else
				return mFile.canRead();
		} catch (SmbException e) {
			return false;
		}
	}

	@Override
	public Boolean canWrite() {
		try {

			if(Thread.currentThread().equals(OpenExplorer.UiThread))
				return true;
			else
				return mFile.canWrite();
		} catch (SmbException e) {
			return false;
		}
	}

	@Override
	public Boolean canExecute() {
		return false;
	}

	@Override
	public Boolean exists() {
		try {
			if(Thread.currentThread().equals(OpenExplorer.UiThread))
				return true;
			else
				return mFile.exists();
		} catch (SmbException e) {
			return false;
		}
	}

	@Override
	public Boolean delete() {
		try {
			mFile.delete();
			return true;
		} catch (SmbException e) {
			return false;
		}
	}

	@Override
	public Boolean mkdir() {
		try {
			mFile.mkdir();
			return true;
		} catch (SmbException e) {
			return false;
		}
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return mFile.getInputStream();
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return mFile.getOutputStream();
	}
	

	public void copyTo(OpenFile dest, BackgroundWork task) throws SmbException {
		mFile.copyTo(dest, task);
	}
	
	public SmbFile getFile() {
		return mFile;
	}

	private String getServerPath(String path)
	{
		OpenServer server = null;
		if(getServersIndex() >= 0)
			server = OpenServers.DefaultServers.get(getServersIndex());
		else {
			Uri uri = Uri.parse(path);
			String user = uri.getUserInfo();
			if(user.indexOf(":") > -1)
				user = user.substring(0, user.indexOf(":"));
			server = OpenServers.DefaultServers.findByPath("smb", uri.getHost(), user, uri.getPath());
			if(server == null)
				server = OpenServers.DefaultServers.findByUser("smb", uri.getHost(), user);
			if(server == null)
				server = OpenServers.DefaultServers.findByHost("smb", uri.getHost());

			Logger.LogVerbose("User: " + user + " :: " + server.getUser() + ":" + server.getPassword().substring(0,1) + server.getPassword().substring(1).replaceAll(".", "*"));
		}
		if(server == null)
			Logger.LogWarning("Couldn't find server for Server Path");
		else if((server.getPassword() == null || server.getPassword().equals("")) && mUserInfo != null)
		{
			if((mUserInfo.getPassword() != null && !mUserInfo.getPassword().equals("")) ||
					mUserInfo.promptPassword("Password for " + server.getHost()))
				path = "smb://" + server.getUser() + ":" + mUserInfo.getPassword() + "@" + server.getHost() + Uri.parse(path).getPath();
		}
		if(server != null)
			path = "smb://" + server.getUser() + ":" + server.getPassword() + "@" + server.getHost() + Uri.parse(path).getPath();
		else
			Logger.LogWarning("Couldn't find server for Server Path");
		return path;
	}

	@Override
	public boolean listFromDb(SortType sort)
	{
		if(!AllowDBCache) return false;
		String parent = getPath(); //.replace("/" + getName(), "");
		if(!parent.endsWith("/"))
			parent += "/";
		Logger.LogDebug("Fetching from folder: " + parent);
		Cursor c = mDb.fetchItemsFromFolder(parent, sort);
		if(c == null) {
			Logger.LogWarning("DB Fetch returned null?");
			return false;
		}
		ArrayList<OpenSMB> arr = new ArrayList<OpenSMB>(); 
		c.moveToFirst();
		while(!c.isAfterLast())
		{
			String folder = c.getString(OpenPathDbAdapter.getKeyIndex(OpenPathDbAdapter.KEY_FOLDER));
			String name = c.getString(OpenPathDbAdapter.getKeyIndex(OpenPathDbAdapter.KEY_NAME));
			int size = c.getInt(OpenPathDbAdapter.getKeyIndex(OpenPathDbAdapter.KEY_SIZE));
			int modified = c.getInt(OpenPathDbAdapter.getKeyIndex(OpenPathDbAdapter.KEY_MTIME));
			OpenSMB child;
			try {
				String path = folder + name;
				/*path = getServerPath(path);*/
				child = new OpenSMB(path, size, modified);
				arr.add(child);
			} catch (MalformedURLException e) { }
			c.moveToNext();
		}
		Logger.LogDebug("listFromDb returning " + arr.size() + " children");
		c.close();
		mChildren = arr.toArray(new OpenSMB[0]);
		return true;
	}
	public long getDiskSpace() {
		if(!Thread.currentThread().equals(OpenExplorer.UiThread))
			try {
				mDiskSpace = mFile.getDiskSpace();
			} catch (SmbException e) { }
		return mDiskSpace;
	}
	public long getDiskFreeSpace() {
		if(!Thread.currentThread().equals(OpenExplorer.UiThread))
			try {
				mDiskFreeSpace = mFile.getDiskFreeSpace();
			} catch (SmbException e) { }
		return mDiskFreeSpace;
	}
}