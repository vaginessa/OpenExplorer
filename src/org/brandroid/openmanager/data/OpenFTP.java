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

import jcifs.smb.SmbAuthException;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

import org.apache.commons.net.ftp.FTPFile;
import org.brandroid.openmanager.activities.OpenExplorer;
import org.brandroid.openmanager.adapters.OpenPathDbAdapter;
import org.brandroid.openmanager.ftp.FTPManager;
import org.brandroid.openmanager.util.FileManager;
import org.brandroid.openmanager.util.FileManager.SortType;
import org.brandroid.openmanager.util.SimpleUserInfo;
import org.brandroid.openmanager.util.ThumbnailCreator;
import org.brandroid.utils.Logger;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.UserInfo;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.drawable.Drawable;
import android.net.Uri;

public class OpenFTP extends OpenNetworkPath
{
	private FTPFile mFile;
	private final FTPManager mManager;
	private OpenFTP[] mChildren = null; 
	private int mServersIndex = -1;
	private long mMTime = 0;
	private long mSize = 0;
	private String mPath;
	private String mName;
	protected OpenFTP mParent = null;
	private Integer mAttributes = null;
	public final int FLAG_HIDDEN = 1;
	public final int FLAG_DIRECTORY = 2;
	public final int FLAG_SYMBOLIC = 4;
	
	public OpenFTP(String path, FTPFile[] children, FTPManager man)
	{
		mPath = path;
		mManager = man;
		Uri uri = Uri.parse(path);
		String base = uri.getPath();
		String name = uri.getLastPathSegment();
		if(name == null)
			name = uri.getHost();
		mFile = new FTPFile();
		mFile.setName(name);
		if(children != null)
		{
			int i = 0;
			mChildren = new OpenFTP[children.length];
			for(FTPFile f : children)
			{
				String parent = base + (base.endsWith("/") || name.startsWith("/") ? "" : "/") + name;
				if(f.isDirectory() && !parent.endsWith("/"))
					parent += "/";
				OpenFTP tmp = new OpenFTP(null, f, new FTPManager(man, parent));
				tmp.mParent = this;
				mChildren[i++] = tmp;				
			}
			FileManager.setOpenCache(getAbsolutePath(), this);
		}
	}
	public OpenFTP(OpenFTP parent, FTPFile file, FTPManager man) {
		if(man != null)
			mPath = man.getPath();
		else if(parent != null)
			mPath = parent.getPath();
		else mPath = "";
		if(file != null && file.getName() != null && !(mPath.endsWith(file.getName()) || mPath.endsWith(file.getName() + "/")))
		{
			if(!mPath.endsWith("/")) mPath += "/";
			if(file != null)
				mPath += file.getName();
		}
		mAttributes = 0;
		if(file != null && file.isDirectory())
			mAttributes |= FLAG_DIRECTORY;
		if(file != null && file.isSymbolicLink())
			mAttributes |= FLAG_SYMBOLIC;
		mParent = parent;
		mFile = file;
		mManager = man;
		mName = getName();
	}
	
	public OpenFTP(String path, FTPFile[] children, FTPManager manager,
			int size, int modified)
	{
		this(path, children, manager);
		mSize = size;
		mMTime = modified;
	}
	public OpenFTP(OpenFTP parent, FTPFile file, FTPManager man, UserInfo info) {
		this(parent, file, man);
		setUserInfo(info);
	}
	public FTPFile getFile() { return mFile; }
	public FTPManager getManager() { return mManager; }
	
	@Override
	public void disconnect() {
		super.disconnect();
		if(mManager != null)
			mManager.disconnect();
	}
	
	@Override
	public void connect() throws IOException {
		super.connect();
		if(mManager != null)
			mManager.connect();
	}

	public void setName(String name) {
		mName = name;
	}
	@Override
	public String getName() {
		if(mName != null)
			return mName;
		Uri uri = getUri();
		if(uri.getLastPathSegment() != null)
			return uri.getLastPathSegment();
		else return uri.getHost();
	}

	@Override
	public String getPath() { return getPath(false); }
	public String getPath(boolean bIncludeUser) {
		String ret = mPath;
		if(!bIncludeUser)
			ret = ret.replace(getUri().getUserInfo() + "@", "");
		return ret;
	}
	
	public String getHost() { return getUri().getHost(); }

	@Override
	public long length() {
		if(mFile != null && !Thread.currentThread().equals(OpenExplorer.UiThread))
			mSize = mFile.getSize();
		return mSize;
	}

	@Override
	public OpenFTP getParent() {
		if(mParent != null || mParent == null)
			return mParent;
		String parent = OpenPath.getParent(getPath());
		if(parent == null) return null;
		if(parent.length() < 5) return null;
		//return new OpenSMB(new SmbFile(parent, mFile.getAuth()));
		//return new OpenFTP(parent, new FTPFile[]{this.getFile()}, new FTPManager(mManager, parent));
		try {
			return (OpenFTP) FileManager.getOpenCache(parent, false, null);
		} catch (IOException e) {
			return null;
		}
	}

	@Override
	public OpenFTP[] listFiles() throws IOException {
		FTPFile[] arr = mManager.listFiles();
		if(arr == null)
			return null;
			
		mChildren = new OpenFTP[arr.length];
		String base = getPath();
		if(base.indexOf("//") > -1)
			base = base.substring(base.indexOf("/", base.indexOf("//") + 2) + 1);
		else if(base.indexOf(":/") > -1)
			base = base.substring(base.indexOf("/", base.indexOf(":/") + 2) + 1);
		if(!base.endsWith("/"))
			base += "/";
		for(int i = 0; i < arr.length; i++)
		{
			String parent = base + arr[i].getName();
			if(arr[i].isDirectory() && !parent.endsWith("/"))
				parent += "/";
			if(arr[i].getName().equals(".") || arr[i].getName().equals("..")) continue;
			OpenFTP tmp = new OpenFTP(this, arr[i], new FTPManager(mManager, parent));
			mChildren[i] = tmp;
			//FileManager.setOpenCache(tmp.getPath(), tmp);
		}
		//mChildren.toArray(ret);
		FileManager.setOpenCache(getAbsolutePath(), this);
		return mChildren;
	}
	

	@Override
	public boolean listFromDb(SortType sort)
	{
		if(!OpenPath.AllowDBCache) return false;
		String folder = getPath(); //.replace("/" + getName(), "");
		if(!isDirectory())
			folder = folder.replace("/" + getName(), "");
		if(!folder.endsWith("/"))
			folder += "/";
		if(folder.endsWith("//"))
			folder = folder.substring(0, folder.length() - 1);
		Cursor c = mDb.fetchItemsFromFolder(folder, sort);
		if(c == null) return false;
		if(c.getCount() == 0)
		{
			c.close();
			c = mDb.fetchItemsFromFolder(folder.substring(0, folder.length() - 1), sort);
		}
		mChildren = new OpenFTP[c.getCount()];
		c.moveToFirst();
		int i = 0;
		while(!c.isAfterLast())
		{
			//String folder = c.getString(OpenPathDbAdapter.getKeyIndex(OpenPathDbAdapter.KEY_FOLDER));
			String name = c.getString(OpenPathDbAdapter.getKeyIndex(OpenPathDbAdapter.KEY_NAME));
			int size = c.getInt(OpenPathDbAdapter.getKeyIndex(OpenPathDbAdapter.KEY_SIZE));
			int modified = c.getInt(OpenPathDbAdapter.getKeyIndex(OpenPathDbAdapter.KEY_MTIME));
			String path = folder + name;
			int atts = c.getInt(OpenPathDbAdapter.getKeyIndex(OpenPathDbAdapter.KEY_ATTRIBUTES));
			if((atts & FLAG_DIRECTORY) == FLAG_DIRECTORY && !path.endsWith("/"))
				path += "/";
			if(path.endsWith("//"))
				path = path.substring(0, path.length() - 1);
			OpenFTP child = new OpenFTP(path, null, getManager(), size, modified);
			mChildren[i++] = child;
			c.moveToNext();
		}
		c.close();
		return true;
	}

	@Override
	public Boolean isDirectory() {
		if(mAttributes != null && (mAttributes & FLAG_DIRECTORY) == FLAG_DIRECTORY) return true;
		if(mChildren != null && mChildren.length > 0)
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
		return Uri.parse(mPath);
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
			return mChildren;
		return listFiles();
	}
	
	@Override
	public Boolean requiresThread() {
		return true;
	}
	@Override
	public int getAttributes() {
		return mAttributes != null ? mAttributes : 0;
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
		return !isDirectory();
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
	}
	
	@Override
	public OpenFTP[] getChildren() {
		return mChildren;
	}
	public boolean isConnected() throws IOException {
		return mManager.isConnected();
	}
}
