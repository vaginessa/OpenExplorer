package org.brandroid.openmanager.data;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import org.brandroid.openmanager.adapters.OpenPathDbAdapter;
import org.brandroid.openmanager.util.FileManager;
import org.brandroid.utils.Logger;

import android.database.Cursor;
import android.net.Uri;

public class OpenSMB extends OpenNetworkPath
{
	private final SmbFile mFile;
	private final OpenSMB mParent;
	private OpenSMB[] mChildren = null; 
	private Long mSize = null;
	private Long mModified = null;
	
	public OpenSMB(String url) throws MalformedURLException
	{
		mFile = new SmbFile(url);
		mParent = null;
	}
	public OpenSMB(SmbFile file)
	{
		mFile = file;
		mParent = null;
	}
	public OpenSMB(OpenSMB parent, SmbFile kid)
	{
		mFile = kid;
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
		if(user.indexOf(":") > -1)
			user = user.substring(0, user.indexOf(":"));
		if(!user.equals(""))
			user += "@";
		return url.getProtocol() + "://" + user + url.getHost() + url.getPath();
	}

	@Override
	public long length() {
		if(mSize != null) return mSize;
		if(isDirectory()) return 0l;
		try {
			return mFile.length();
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
		else return null;
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
		SmbFile[] kids = mFile.listFiles();
		mChildren = new OpenSMB[kids.length];
		for(int i = 0; i < kids.length; i++)
		{
			mChildren[i] = new OpenSMB(this, kids[i]);
			FileManager.setOpenCache(mChildren[i].getPath(), mChildren[i]);
		}
		return mChildren;
	}
	
	@Override
	public OpenSMB[] getChildren() { return mChildren; }

	@Override
	public Boolean isDirectory() {
		try {
			return mFile.isDirectory();
		} catch (SmbException e) {
			return false;
		}
	}

	@Override
	public Boolean isFile() {
		try {
			return mFile.isFile();
		} catch (SmbException e) {
			return false;
		}
	}

	@Override
	public Boolean isHidden() {
		try {
			return mFile.isHidden();
		} catch (SmbException e) {
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


	@Override
	public boolean listFromDb()
	{
		Cursor c = mDb.fetchItemsFromFolder(getPath().replace("/" + getName(), ""));
		if(c == null) return false;
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
				child = new OpenSMB(folder + "/" + name, size, modified);
				arr.add(child);
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			c.moveToNext();
		}
		c.close();
		mChildren = arr.toArray(new OpenSMB[0]);
		return true;
	}
}
