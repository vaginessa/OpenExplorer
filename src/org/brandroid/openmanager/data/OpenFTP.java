package org.brandroid.openmanager.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;

import org.apache.commons.net.ftp.FTPFile;
import org.brandroid.openmanager.ftp.FTPManager;

import android.net.Uri;

public class OpenFTP extends OpenFace
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
	public String getPath() {
		return mManager.getPath();
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
	public OpenFace[] listFiles() throws IOException {
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
	public OpenFace[] list() throws IOException {
		if(mChildren != null)
		{
			OpenFace[] ret = new OpenFace[mChildren.size()];
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
	public OpenFace getChild(String name)
	{
		FTPFile base = mFile;
		if(!base.isDirectory())
			base = getParent().getFile();
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
