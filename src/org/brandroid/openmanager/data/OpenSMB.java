package org.brandroid.openmanager.data;

import jcifs.smb.SmbAuthException;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import org.brandroid.openmanager.activities.OpenExplorer;
import org.brandroid.openmanager.adapters.OpenPathDbAdapter;
import org.brandroid.openmanager.util.FileManager;
import org.brandroid.utils.Logger;

import android.database.Cursor;
import android.net.Uri;

public class OpenSMB extends OpenNetworkPath
{
	private final SmbFile mFile;
	private OpenSMB mParent;
	private OpenSMB[] mChildren = null; 
	private Long mSize = null;
	private Long mModified = null;
	private Boolean mHidden = false;
	private Long mDiskSpace = null;
	
	public OpenSMB(String url) throws MalformedURLException
	{
		mFile = new SmbFile(url);
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
		mParent = parent;
	}
	public OpenSMB(String url, long size, long modified) throws MalformedURLException
	{
		mFile = new SmbFile(url);
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
	public OpenPath getParent() {
		if(mParent != null)
			return mParent;
		else {
			try {
				String s = mFile.getParent();
				if(s == null) return null;
				if(s.equals("smb://")) return null;
				SmbFile smb = new SmbFile(s);
				mParent = new OpenSMB(new SmbFile(mFile.getParent()));
				return mParent;
			} catch (MalformedURLException e) {
				e.printStackTrace();
				return null;
			}
		}
	}
	
	public OpenSMB getParentSMB() { return mParent; }

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
		Logger.LogInfo("Listing children under " + getPath());
		SmbFile[] kids = null;
		try {
			kids = mFile.listFiles();
		} catch(SmbAuthException e) {
			Logger.LogWarning("Unable to authenticate. Trying to get password from Servers.");
			String path = getServerPath(mFile.getPath());
			kids = new SmbFile(path).listFiles();
		}
		if(kids != null)
		{
			mChildren = new OpenSMB[kids.length];
			for(int i = 0; i < kids.length; i++)
			{
				mChildren[i] = new OpenSMB(this, kids[i]);
				FileManager.setOpenCache(mChildren[i].getPath(), mChildren[i]);
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
		if(mModified != null) return mModified;
		return mFile.getLastModified();
	}

	@Override
	public Boolean canRead() {
		try {
			return mFile.canRead();
		} catch (SmbException e) {
			return false;
		}
	}

	@Override
	public Boolean canWrite() {
		try {
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
	public SmbFile getFile() {
		return mFile;
	}

	private String getServerPath(String path)
	{
		Uri uri = Uri.parse(path);
		String user = uri.getUserInfo();
		if(user.indexOf(":") > -1)
			user = user.substring(0, user.indexOf(":"));
		Logger.LogInfo("User: " + user);
		OpenServer server = OpenServers.DefaultServers.find(uri.getHost(), user, uri.getPath());
		if(server == null)
			server = OpenServers.DefaultServers.find(uri.getHost(), user);
		if(server == null)
			server = OpenServers.DefaultServers.find(uri.getHost());
		if(server != null)
			path = "smb://" + user + ":" + server.getPassword() + "@" + server.getHost() + uri.getPath();
		else
			Logger.LogWarning("Couldn't find server for Server Path");
		return path;
	}

	@Override
	public boolean listFromDb()
	{
		if(!AllowDBCache) return false;
		String parent = getPath(); //.replace("/" + getName(), "");
		if(!parent.endsWith("/"))
			parent += "/";
		Logger.LogDebug("Fetching from folder: " + parent);
		Cursor c = mDb.fetchItemsFromFolder(parent);
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
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			c.moveToNext();
		}
		Logger.LogDebug("listFromDb returning " + arr.size() + " children");
		c.close();
		mChildren = arr.toArray(new OpenSMB[0]);
		return true;
	}
}
