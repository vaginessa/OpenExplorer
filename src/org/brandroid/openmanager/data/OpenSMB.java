package org.brandroid.openmanager.data;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.brandroid.utils.Logger;

import android.net.Uri;

public class OpenSMB extends OpenNetworkPath
{
	private final SmbFile mFile;
	private final OpenSMB mParent;
	private OpenSMB[] mChildren = null; 
	
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

	@Override
	public String getName() {
		return getName(mFile.getName());
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
			mChildren[i] = new OpenSMB(this, kids[i]);
		return mChildren;
	}

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

}
