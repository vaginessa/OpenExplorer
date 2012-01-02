package org.brandroid.openmanager.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.SoftReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import org.apache.commons.net.ftp.FTPFile;
import org.brandroid.openmanager.ftp.FTPManager;
import org.brandroid.openmanager.util.FileManager;
import org.brandroid.openmanager.util.ThumbnailCreator;
import org.brandroid.utils.Logger;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.drawable.Drawable;
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
		String base = path;
		if(base.indexOf("//") > -1)
			base = base.substring(base.indexOf("/", base.indexOf("//") + 2));
		if(!base.endsWith("/"))
			base += "/";
		if(children != null)
			for(FTPFile f : children)
				mChildren.add(new OpenFTP(f, new FTPManager(man, base + f.getName())));
	}
	public OpenFTP(FTPFile file, FTPManager man) { mFile = file; mManager = man; }
	
	public FTPFile getFile() { return mFile; }
	public FTPManager getManager() { return mManager; }

	@Override
	public String getName() {
		if(mFile != null)
			return mFile.getName();
		else return null;
	}

	@Override
	public String getPath() { return getPath(false); }
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
		if(mChildren != null && mChildren.size() > 0)
		{
			OpenPath[] ret = new OpenPath[mChildren.size()];
			mChildren.toArray(ret);
			return ret;
		} else mChildren = new ArrayList<OpenFTP>();
		try {
			FTPFile[] arr = mManager.listFiles(getThreadUpdateCallback());
			if(arr == null)
				return null;
				
			OpenFTP[] ret = new OpenFTP[arr.length];
			String base = getPath();
			if(base.indexOf("//") > -1)
				base = base.substring(base.indexOf("/", base.indexOf("//") + 2) + 1);
			if(!base.endsWith("/"))
				base += "/";
			for(int i = 0; i < arr.length; i++)
			{
				mChildren.add(new OpenFTP(arr[i], new FTPManager(mManager, base + arr[i].getName())));
			}
			mChildren.toArray(ret);
			return ret;
		} catch(IOException e) {
			Logger.LogError("Error listing FTP files.", e);
			return null;
		}
	}

	@Override
	public Boolean isDirectory() {
		if(mChildren != null)
			return true;
		String path = getPath();
		if(path.endsWith("/")) return true;
		path = path.substring(path.indexOf("//") + 2);
		if(path.indexOf("/") == -1)
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
		return getPath(true);
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
		return new OpenFTP(path, null, new FTPManager(mManager, path));
	}
	@Override
	public Boolean isFile() {
		return mFile.isFile();
	}
	@Override
	public Boolean delete() {
		try {
			return mManager.delete(); //mFile.delete();
		} catch(IOException e) {
			Logger.LogError("Error removing FTP file.", e);
			return false;
		}
	}
	@Override
	public Boolean mkdir() {
		return false; //mFile.mkdir();
	}
	public void get(OutputStream stream) throws IOException
	{
		mManager.get(mFile.getName(), stream);
	}
	@Override
	public InputStream getInputStream() throws IOException {
		return mManager.getInputStream();
	}
	@Override
	public OutputStream getOutputStream() throws IOException {
		return mManager.getOutputStream();
	}

	@Override
	public Boolean isHidden() {
		return getName().startsWith(".");
	}
	
	/*
	@Override
	public SoftReference<Bitmap> getThumbnail(int w, int h, Boolean read,
			Boolean write) {
		SoftReference<Bitmap> ret = super.getThumbnail(w, h, read, write);
		Bitmap src = ret.get();
		Bitmap b = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(b);
		Paint p = new Paint();
		c.drawBitmap(src, 0, 0, p);
		p.setARGB(220, 255, 255, 255);
		p.setStyle(Paint.Style.FILL_AND_STROKE);
		p.setTextAlign(Align.CENTER);
		p.setTextSize(12);
		p.setShadowLayer(1f, 0, 0, Color.BLACK);
		c.drawText("FTP", b.getWidth() / 2, b.getHeight() / 2, p);
		return new SoftReference<Bitmap>(b);
	}
	*/
	
	@Override
	public void setPath(String path) {
		mManager.setBasePath(path);
		mFile = new FTPFile();
		mFile.setName(path);
		mChildren = new ArrayList<OpenFTP>();
	}
}
