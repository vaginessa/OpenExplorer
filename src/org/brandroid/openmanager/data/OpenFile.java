package org.brandroid.openmanager.data;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Hashtable;

import org.brandroid.utils.Logger;

import android.app.AlertDialog.Builder;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.text.format.Formatter;

public class OpenFile extends OpenPath
{
	private File mFile;
	
	public OpenFile(File f) { mFile = f; }
	public OpenFile(String path) { mFile = new File(path); }
	
	public File getFile() { return mFile; }

	@Override
	public String getName() {
		return mFile.getName();
	}

	@Override
	public String getPath() {
		return mFile.getPath();
	}

	@Override
	public long length() {
		return mFile.length();
	}
	
	public long getFreeSpace() {
		StatFs stat = new StatFs(getPath());
		if(stat.getFreeBlocks() > 0)
			return (long)stat.getFreeBlocks() * (long)stat.getBlockSize();
		if(DFInfo.LoadDF().containsKey(getPath()))
			return (long)DFInfo.LoadDF().get(getPath()).getFree();
		return Build.VERSION.SDK_INT > 8 ? mFile.getFreeSpace() * 1024 * 1024 : 0;
	}
	public long getUsableSpace() {
		if(Build.VERSION.SDK_INT > 0)
		{
			StatFs stat = new StatFs(getPath());
			if(stat.getAvailableBlocks() > 0)
				return (long)stat.getAvailableBlocks() * (long)stat.getBlockSize();
		}
		if(DFInfo.LoadDF().containsKey(getPath()))
			return (long)(DFInfo.LoadDF().get(getPath()).getSize() - DFInfo.LoadDF().get(getPath()).getFree());
		return mFile.getUsableSpace();
	}
	public long getTotalSpace() {
		if(Build.VERSION.SDK_INT > 0)
		{
			StatFs stat = new StatFs(getPath());
			if(stat.getBlockCount() > 0)
				return (long)stat.getBlockCount() * (long)stat.getBlockSize();
		}
		if(DFInfo.LoadDF().containsKey(getPath()))
			return (long)DFInfo.LoadDF().get(getPath()).getSize();
		return Build.VERSION.SDK_INT > 8 ? mFile.getTotalSpace() * 1024 * 1024 : length();
	}
	public long getUsedSpace() {
		long ret = length();
		if(isDirectory())
			for(OpenPath kid : list())
				ret += ((OpenFile)kid).getUsedSpace();
		return ret;
	}

	@Override
	public OpenPath getParent() {
		if(mFile.getParent() == null)
			return null;
		return new OpenFile(mFile.getParent());
	}

	@Override
	public OpenPath[] listFiles() {
		File[] arr = mFile.listFiles();
		if((arr == null || arr.length == 0) && !isDirectory() && mFile.getParentFile() != null)
			arr = mFile.getParentFile().listFiles();
		
		if(arr == null)
			return new OpenFile[0];
			
		OpenFile[] ret = new OpenFile[arr.length];
		for(int i = 0; i < arr.length; i++)
			ret[i] = new OpenFile(arr[i]);
		return ret;
	}

	@Override
	public Boolean isDirectory() {
		return mFile.isDirectory();
	}

	@Override
	public Uri getUri() {
		return Uri.fromFile(mFile);
	}

	@Override
	public Long lastModified() {
		return mFile.lastModified();
	}

	@Override
	public Boolean canRead() {
		return mFile.canRead();
	}

	@Override
	public Boolean canWrite() {
		return mFile.canWrite();
	}

	@Override
	public Boolean canExecute() {
		return mFile.canExecute();
	}

	@Override
	public Boolean exists() {
		return mFile.exists();
	}
	@Override
	public OpenPath[] list() {
		return listFiles();
	}
	
	@Override
	public Boolean requiresThread() {
		return false;
	}
	@Override
	public String getAbsolutePath() {
		return mFile.getAbsolutePath();
	}
	@Override
	public OpenPath getChild(String name)
	{
		File base = getFile();
		if(!base.isDirectory())
			base = base.getParentFile();
		return new OpenFile(new File(base, name));
	}
	@Override
	public Boolean isFile() {
		return mFile.isFile();
	}
	@Override
	public Boolean delete() {
		return mFile.delete();
	}
	@Override
	public Boolean mkdir() {
		return mFile.mkdir();
	}
	@Override
	public InputStream getInputStream() throws IOException {
		return new FileInputStream(mFile); 
	}
	@Override
	public OutputStream getOutputStream() throws IOException {
		if(!mFile.exists())
			if(!mFile.createNewFile())
				return null;
		return new FileOutputStream(mFile);
	}
	@Override
	public Boolean isHidden() {
		return mFile.isHidden() || mFile.getName().startsWith(".");
	}
	
	@Override
	public void setPath(String path) {
		mFile = new File(path); 
	}
}
