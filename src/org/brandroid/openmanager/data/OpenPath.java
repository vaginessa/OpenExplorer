package org.brandroid.openmanager.data;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.ref.SoftReference;

import org.brandroid.openmanager.adapters.OpenPathDbAdapter;
import org.brandroid.openmanager.util.FileManager;
import org.brandroid.openmanager.util.MimeTypes;
import org.brandroid.openmanager.util.ThumbnailCreator;
import org.brandroid.openmanager.util.FileManager.SortType;
import org.brandroid.utils.Logger;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

public abstract class OpenPath
	implements Serializable, Parcelable, Comparable<OpenPath>
{
	public static SortType Sorting = SortType.DATE_DESC;
	
	private static final long serialVersionUID = 332701810738149106L;
	private Object mTag = null;
	private OpenPathThreadUpdater mUpdater;
	protected static OpenPathDbAdapter mDb = null;
	public static Boolean AllowDBCache = true;
	public abstract String getName();
	public abstract String getPath();
	public abstract String getAbsolutePath();
	public abstract void setPath(String path);
	public abstract long length();
	public abstract OpenPath getParent();
	public abstract OpenPath getChild(String name);
	public OpenPath getChild(int index) throws IOException { return ((OpenPath[])list())[index]; }
	public abstract OpenPath[] list() throws IOException;
	public abstract OpenPath[] listFiles() throws IOException;
	public static String getParent(String path)
	{
		if(path.equals("/")) return null;
		if(path.endsWith("/"))
			path = path.substring(0, path.length() - 1);
		path = path.substring(0, path.lastIndexOf("/") + 1);
		if(path.endsWith("://") || path.endsWith(":/"))
			return null;
		return path;
	}
	public int getChildCount(boolean countHidden) throws IOException {
		if(requiresThread()) return 0;
		if(countHidden)
			return list().length;
		else {
			int ret = 0;
			for(OpenPath kid : list())
				if(!kid.isHidden())
					ret++;
			return ret;
		}
	}
	public abstract Boolean isDirectory();
	public abstract Boolean isFile();
	public abstract Boolean isHidden();
	public abstract Uri getUri();
	public abstract Long lastModified();
	public abstract Boolean canRead();
	public abstract Boolean canWrite();
	public abstract Boolean canExecute();
	public abstract Boolean exists();
	public abstract Boolean requiresThread();
	public abstract Boolean delete();
	public abstract Boolean mkdir();
	public abstract InputStream getInputStream() throws IOException;
	public abstract OutputStream getOutputStream() throws IOException;
	@Override
	public boolean equals(Object o) {
		if(o instanceof OpenPath)
			return getAbsolutePath().equals(((OpenPath)o).getAbsolutePath());
		else
			return super.equals(o);
	}
	public Object getTag() { return mTag; }
	public void setTag(Object o) { mTag = o; }
	public int getListLength() { return -1; }
	public int compareTo(OpenPath other)
	{
		return compare(this, other);
	}
	public SoftReference<Bitmap> getThumbnail(int w, int h) { return ThumbnailCreator.generateThumb(this, w, h); }
	public SoftReference<Bitmap> getThumbnail(int w, int h, Boolean read, Boolean write) { return ThumbnailCreator.generateThumb(this, w, h, read, write); }
	public static int compare(OpenPath fa, OpenPath fb)
	{
		try {
			if(fa == null && fb != null)
				return 1;
			if(fb == null && fa != null)
				return 0;
			if(fb == null || fa == null)
				return 0;
			String a = fa.getName();
			String b = fb.getName();
			Long sa = fa.length();
			Long sb = fb.length();
			Long ma = fa.lastModified();
			Long mb = fb.lastModified();
			if(a == null && b != null)
				return 1;
			if(a == null || b == null)
				return 0;
			switch(Sorting)
			{
				case ALPHA_DESC:
					return b.toLowerCase().compareTo(a.toLowerCase());
				case ALPHA:
					return a.toLowerCase().compareTo(b.toLowerCase());
				case SIZE_DESC:
					if(sa == null && sb != null) return 1;
					if(sa == null || sb == null) return 0;
					return sa.compareTo(sb);
				case SIZE:
					if(sb == null && sa != null) return 1;
					if(sa == null || sb == null) return 0;
					return sb.compareTo(sa);
				case DATE_DESC:
					if(ma == null && mb != null) return 1;
					if(ma == null || mb == null) return 0;
					return ma.compareTo(mb);
				case DATE:
					if(mb == null && ma != null) return 1;
					if(ma == null || mb == null) return 0;
					return mb.compareTo(ma);
				case TYPE:
					String ea = a.substring(a.lastIndexOf(".") + 1, a.length()).toLowerCase();
					String eb = b.substring(b.lastIndexOf(".") + 1, b.length()).toLowerCase();
					return ea.compareTo(eb);
				case NONE:
					return 0;
				default:
					return a.toLowerCase().compareTo(b.toLowerCase());
			}
		} catch(Exception e)
		{
			Logger.LogError("Unable to sort.", e);
			return 0;
		}
	}
	
	private void writeObject(ObjectOutputStream out) throws IOException
	{
		out.writeChars(getPath());
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
	{
		setPath(in.readLine());
	}

	public String getExtension() {
		return getPath().substring(getPath().lastIndexOf(".") + 1);
	}
	
	public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeString(getPath());
    }

    public static final Parcelable.Creator<OpenPath> CREATOR
            = new Parcelable.Creator<OpenPath>() {
        public OpenPath createFromParcel(Parcel in) {
        	String path = in.readString();
        	try {
				return FileManager.getOpenCache(path, false, Sorting);
			} catch (IOException e) {
				return null;
			}
        }

        public OpenPath[] newArray(int size) {
            return new OpenPath[size];
        }
    };
    
	public int getDepth() {
		if(getParent() == null || getParent().getPath().equals(getPath())) return 1;
		return 1 + getParent().getDepth();
	}

	public OpenPath[] getHierarchy()
	{
		OpenPath[] ret = new OpenPath[getDepth()];
		OpenPath tmp = this;
		for(int i = ret.length - 1; i >= 0; i--)
		{
			ret[i] = tmp;
			tmp = tmp.getParent();
		}
		return ret;
	}

	public boolean isTextFile() { return isTextFile(getName()); }
	public static boolean isTextFile(String file) {
		if(file.indexOf(".") == -1) return false;
		String ext = file.substring(file.lastIndexOf(".") + 1);
		if(MimeTypes.Default != null)
			if(MimeTypes.Default.getMimeType(file).startsWith("text/"))
				return true;
		if(ext.equalsIgnoreCase("txt")
				|| ext.equalsIgnoreCase("php")
				|| ext.equalsIgnoreCase("html")
				|| ext.equalsIgnoreCase("htm")
				|| ext.equalsIgnoreCase("xml"))
			return true;
		return false;
	}
	
	public boolean isImageFile() { return isImageFile(getName()); }
	public static boolean isImageFile(String file) {
		String ext = file.substring(file.lastIndexOf(".") + 1);
		if(MimeTypes.Default != null)
			if(MimeTypes.Default.getMimeType(file).startsWith("image/"))
				return true;
		if (ext.equalsIgnoreCase("png") || ext.equalsIgnoreCase("jpg") ||
			ext.equalsIgnoreCase("jpeg")|| ext.equalsIgnoreCase("gif") ||
			ext.equalsIgnoreCase("tiff")|| ext.equalsIgnoreCase("tif"))
			return true;
		
		return false;
	}
	
	public boolean isAPKFile() { return isAPKFile(getName()); }
	public static boolean isAPKFile(String file) {
		String ext = file.substring(file.lastIndexOf(".") + 1);
		
		if (ext.equalsIgnoreCase("apk"))
			return true;
		
		return false;
	}
	public boolean isArchive() {
		return getExtension().equals("zip");
	}
	
	public boolean isVideoFile() { return isVideoFile(getName()); }
	public static boolean isVideoFile(String path)
	{
		if(MimeTypes.Default != null)
			if(MimeTypes.Default.getMimeType(path).startsWith("video/"))
				return true;
		
		String ext = path.substring(path.lastIndexOf(".") + 1);
		if(ext.equalsIgnoreCase("mp4") || 
			  ext.equalsIgnoreCase("3gp") || 
			  ext.equalsIgnoreCase("avi") ||
			  ext.equalsIgnoreCase("webm") || 
			  ext.equalsIgnoreCase("m4v"))
			return true;
		return false;
	}
	
	/**
	 * Indicates the potential existence of a dynamic thumbnail (videos, photos, & apk files)
	 * @return boolean indicating whether a dynamic thumbnail exists
	 */
	public boolean hasThumbnail() {
		return isVideoFile() || isImageFile() || isAPKFile();
	}
	
	public void setThreadUpdateCallback(OpenPathThreadUpdater updater)
	{
		mUpdater = updater;
	}
	public OpenPathThreadUpdater getThreadUpdateCallback() { return mUpdater; }
	
	public interface OpenPathThreadUpdater
	{
		public void update(int progress, int total);
		public void update(String status);
	}

	public String getMimeType() {
		if(MimeTypes.Default != null)
			return MimeTypes.Default.getMimeType(getPath());
		return "*/*";
	}
	
	public OpenPath[] getSiblings() throws IOException
	{
		OpenPath parent = getParent();
		if(parent == null) return new OpenPath[]{this};
		return parent.list();
	}
	
	public OpenPath[] getAncestors(boolean zeroCurrent)
	{
		OpenPath[] ret = new OpenPath[getDepth()];
		OpenPath tmp = this;
		int i = 0;
		while(tmp != null)
		{
			i++;
			ret[zeroCurrent ? i - 1 : ret.length - i] = tmp;
			tmp = tmp.getParent();
		}
		return ret;
	}
	public OpenPath getCommonDenominator(OpenPath other)
	{
		OpenPath[] myAncestors = getAncestors(true);
		for(int i = 0; i < myAncestors.length; i++)
		{
			OpenPath mine = myAncestors[i];
			if(other.getPath().contains(mine.getPath()))
				return mine;
		}
		return null;
	}
	
	
	@Override
	public String toString() {
		return getName();
	}
	
	public final static void setDb(OpenPathDbAdapter openPathDbAdapter) {
		mDb = openPathDbAdapter;
	}
	public boolean addToDb()
	{
		if(mDb == null) return false;
		if(!AllowDBCache) return false;
		return mDb.createItem(this) > 0;
	}
	public boolean listFromDb(SortType sort) { return false; }
	public int deleteFolderFromDb()
	{
		if(!AllowDBCache) return 0;
		if(mDb != null)
			return mDb.deleteFolder(this);
		else return -1;
	}
	
	public final static void flushDbCache()
	{
		if(mDb != null)
			mDb.clear();
	}
}
