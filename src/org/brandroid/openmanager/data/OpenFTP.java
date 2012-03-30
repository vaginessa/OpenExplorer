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
import org.brandroid.openmanager.adapters.OpenPathDbAdapter;
import org.brandroid.openmanager.ftp.FTPManager;
import org.brandroid.openmanager.util.FileManager;
import org.brandroid.openmanager.util.ThumbnailCreator;
import org.brandroid.utils.Logger;

import android.database.Cursor;
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
	private FTPFile mFile;
	private final FTPManager mManager;
	private final ArrayList<OpenFTP> mChildren; 
	private int mServersIndex = -1;
	private long mMTime = 0;
	private long mSize = 0;
	protected OpenFTP mParent = null;
	
	public OpenFTP(String path, FTPFile[] children, FTPManager man)
	{
		mManager = man;
		mFile = new FTPFile();
		mFile.setName(path);
		mChildren = new ArrayList<OpenFTP>();
		String base = path;
		base = base.replace("://", ":/").replace(":/", "://");
		if(base.indexOf("//") > -1 && base.indexOf("/", base.indexOf("//") + 2) > -1)
			base = base.substring(base.indexOf("/", base.indexOf("//") + 2));
		else base = "";
		if(!base.endsWith("/"))
			base += "/";
		if(children != null)
		{
			for(FTPFile f : children)
			{
				OpenFTP tmp = new OpenFTP(null, f, new FTPManager(man, base + f.getName()));
				tmp.mParent = this;
				mChildren.add(tmp);
			}
			FileManager.setOpenCache(getAbsolutePath(), this);
		}
	}
	public OpenFTP(OpenFTP parent, FTPFile file, FTPManager man) {
		mParent = parent;
		mFile = file;
		mManager = man;
		mChildren = new ArrayList<OpenFTP>();
	}
	
	public OpenFTP(String path, FTPFile[] children, FTPManager manager,
			int size, int modified)
	{
		this(path, children, manager);
		mSize = size;
		mMTime = modified;
	}
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
	}
	
	public String getHost() { return mManager.getHost(); }

	@Override
	public long length() {
		if(mFile != null)
			return mFile.getSize();
		else return mSize;
	}

	@Override
	public OpenFTP getParent() {
		return mParent;
	}

	@Override
	public OpenFTP[] listFiles() throws IOException {
		if(mChildren.size() > 0)
		{
			OpenFTP[] ret = new OpenFTP[mChildren.size()];
			mChildren.toArray(ret);
			return ret;
		}
		FTPFile[] arr = mManager.listFiles();
		if(arr == null)
			return null;
			
		OpenFTP[] ret = new OpenFTP[arr.length];
		String base = getPath();
		if(base.indexOf("//") > -1)
			base = base.substring(base.indexOf("/", base.indexOf("//") + 2) + 1);
		else if(base.indexOf(":/") > -1)
			base = base.substring(base.indexOf("/", base.indexOf(":/") + 2) + 1);
		if(!base.endsWith("/"))
			base += "/";
		for(int i = 0; i < arr.length; i++)
		{
			OpenFTP tmp = new OpenFTP(this, arr[i], new FTPManager(mManager, base + arr[i].getName()));
			mChildren.add(tmp);
		}
		mChildren.toArray(ret);
		FileManager.setOpenCache(getAbsolutePath(), this);
		return ret;
	}
	

	@Override
	public boolean listFromDb()
	{
		Cursor c = mDb.fetchItemsFromFolder(getPath().replace("/" + getName(), ""));
		if(c == null) return false;
		mChildren.clear();
		c.moveToFirst();
		while(!c.isAfterLast())
		{
			String folder = c.getString(OpenPathDbAdapter.getKeyIndex(OpenPathDbAdapter.KEY_FOLDER));
			String name = c.getString(OpenPathDbAdapter.getKeyIndex(OpenPathDbAdapter.KEY_NAME));
			int size = c.getInt(OpenPathDbAdapter.getKeyIndex(OpenPathDbAdapter.KEY_SIZE));
			int modified = c.getInt(OpenPathDbAdapter.getKeyIndex(OpenPathDbAdapter.KEY_MTIME));
			OpenFTP child = new OpenFTP(folder + "/" + name, null, getManager(), size, modified);
			mChildren.add(child);
			c.moveToNext();
		}
		c.close();
		return true;
	}

	@Override
	public Boolean isDirectory() {
		if(mChildren != null && mChildren.size() > 0)
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
		if(mFile != null && mFile.getTimestamp() != null)
			return mFile.getTimestamp().getTimeInMillis();
		else return mMTime;
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
	public OpenPath[] list() throws IOException {
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
		OpenFTP ret = new OpenFTP(path, null, new FTPManager(mManager, path));
		ret.setServersIndex(mServersIndex);
		return ret;
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
		mChildren.clear();
	}
	
	public int getServersIndex() { return mServersIndex; }
	public void setServersIndex(int i) { mServersIndex = i; }
	
	public OpenFTP[] getChildren() {
		return mChildren.toArray(new OpenFTP[0]);
	}
}
