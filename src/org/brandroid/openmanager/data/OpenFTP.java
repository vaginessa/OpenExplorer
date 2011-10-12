package org.brandroid.openmanager.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import org.apache.commons.net.ftp.FTPFile;
import org.brandroid.openmanager.FileManager;
import org.brandroid.openmanager.ftp.FTPManager;
import org.brandroid.utils.Logger;

import android.net.Uri;

public class OpenFTP extends OpenPath
{
	private FTPFile mFile = new FTPFile();
	private FTPManager mManager;
	private ArrayList<OpenFTP> mChildren = null; 
	
	public OpenFTP(String path, FTPFile[] children, FTPManager man)
	{
		mManager = man;
		mFile = new FTPFile();
		mFile.setName(path);
		mChildren = new ArrayList<OpenFTP>();
		if(children != null)
			for(FTPFile f : children)
				mChildren.add(new OpenFTP(f, man));
	}
	public OpenFTP(FTPFile file, FTPManager man) { mFile = file; mManager = man; }
	
	public FTPFile getFile() { return mFile; }
	public FTPManager getManager() { return mManager; }

	@Override
	public String getName() {
		return mFile.getName();
	}

	@Override
	public String getPath() { return getPath(true); }
	public String getPath(boolean bIncludeUser) {
		return mManager.getPath(bIncludeUser);
		//return mFile.getRawListing();
	}

	@Override
	public long length() {
		return mFile.getSize();
	}

	@Override
	public OpenFTP getParent() {
		return null;
		//return new OpenFile(mFile.get());
	}

	@Override
	public OpenPath[] listFiles() {
		FTPFile[] arr = mManager.listFiles();
		if(arr == null)
			return null;
			
		OpenFTP[] ret = new OpenFTP[arr.length];
		for(int i = 0; i < arr.length; i++)
			ret[i] = new OpenFTP(arr[i], mManager);
		return ret;
	}

	@Override
	public Boolean isDirectory() {
		if(mChildren != null)
			return true;
		return mFile.isDirectory();
	}

	@Override
	public Uri getUri() {
		return Uri.parse(mFile.toString());
	}

	@Override
	public Long lastModified() {
		return mFile.getTimestamp().getTimeInMillis();
	}

	@Override
	public Boolean canRead() {
		return mFile.hasPermission(FTPFile.USER_ACCESS, FTPFile.READ_PERMISSION);
	}

	@Override
	public Boolean canWrite() {
		return mFile.hasPermission(FTPFile.USER_ACCESS, FTPFile.WRITE_PERMISSION);
	}

	@Override
	public Boolean canExecute() {
		return false;
	}

	@Override
	public Boolean exists() {
		return mFile != null;
	}
	@Override
	public OpenPath[] list() {
		if(mChildren != null)
		{
			OpenPath[] ret = new OpenPath[mChildren.size()];
			mChildren.toArray(ret);
			return ret;
		}
		return listFiles();
	}
	
	@Override
	public Boolean requiresThread() {
		return true;
	}
	@Override
	public String getAbsolutePath() {
		return mFile.getRawListing();
	}
	@Override
	public OpenPath getChild(String name)
	{
		FTPFile base = mFile;
		if(base != null && !base.isDirectory())
		{
			if(getParent() != null)
				base = getParent().getFile();
		}
		String path = getPath();
		if(!path.endsWith(name))
			path += (path.endsWith("/") ? "" : "/") + name;
		try {
			return new OpenFTP(path, null, new FTPManager(path));
		} catch(MalformedURLException e) {
			Logger.LogError("Error getting FTP child for " + path + " - " + name, e);
		}
		return null; //new OpenFile(new FTPFile(base, name));
	}
	@Override
	public Boolean isFile() {
		return mFile.isFile();
	}
	@Override
	public Boolean delete() {
		return false; //mFile.delete();
	}
	@Override
	public Boolean mkdir() {
		return false; //mFile.mkdir();
	}
	@Override
	public InputStream getInputStream() throws IOException {
		return null; //new FileInputStream(mFile); 
	}
	@Override
	public OutputStream getOutputStream() throws IOException {
		return null; //new FileOutputStream(mFile);
	}

	@Override
	public Boolean isHidden() {
		return mFile.getName().startsWith(".");
	}

}
