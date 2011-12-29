package org.brandroid.openmanager.data;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.util.Comparator;

import org.brandroid.openmanager.util.FileManager;
import org.brandroid.openmanager.util.ThumbnailCreator;
import org.brandroid.openmanager.util.FileManager.SortType;
import org.brandroid.utils.Logger;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;

public abstract class OpenPath implements Serializable, Parcelable, Comparable<OpenPath>
{
	public static SortType Sorting = SortType.ALPHA;
	
	private static final long serialVersionUID = 332701810738149106L;
	private Object mTag = null;
	public abstract String getName();
	public abstract String getPath();
	public abstract String getAbsolutePath();
	public abstract void setPath(String path);
	public abstract long length();
	public abstract OpenPath getParent();
	public abstract OpenPath getChild(String name);
	public abstract OpenPath[] list();
	public abstract OpenPath[] listFiles();
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
	public Object getTag() { return mTag; }
	public void setTag(Object o) { mTag = o; }
	public int compareTo(OpenPath other)
	{
		return compare(this, other);
	}
	public SoftReference<Bitmap> getThumbnail(int w, int h) { return ThumbnailCreator.generateThumb(this, w, h); }
	public SoftReference<Bitmap> getThumbnail(int w, int h, Boolean read, Boolean write) { return ThumbnailCreator.generateThumb(this, w, h, read, write); }
	public static int compare(OpenPath fa, OpenPath fb)
	{
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
		if(a == null && b != null)
			return 1;
		if(a == null || b == null)
			return 0;
		try {
			switch(Sorting)
			{
				case ALPHA_DESC:
					return b.toLowerCase().compareTo(a.toLowerCase());
				case ALPHA:
					return a.toLowerCase().compareTo(b.toLowerCase());
				case SIZE_DESC:
					return sb.compareTo(sa);
				case SIZE:
					return sa.compareTo(sb);
				case DATE_DESC:
					return fb.lastModified().compareTo(fa.lastModified());
				case DATE:
					return fa.lastModified().compareTo(fb.lastModified());
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
        	if(new File(path).exists())
        		return new OpenFile(path);
        	return FileManager.getOpenCache(path);
        }

        public OpenPath[] newArray(int size) {
            return new OpenPath[size];
        }
    };
    
	public int getDepth() {
		if(getParent() == null || getParent().getPath().equals(getPath())) return 1;
		return 1 + getParent().getDepth();
	}


	public boolean isTextFile() { return isTextFile(getName()); }
	public static boolean isTextFile(String file) {
		String ext = file.substring(file.lastIndexOf(".") + 1);
		if(ext.equalsIgnoreCase("txt") || ext.equalsIgnoreCase("php")
				|| ext.equalsIgnoreCase("html") || ext.equalsIgnoreCase("htm")
				|| ext.equalsIgnoreCase("xml"))
			return true;
		return false;
	}
	
	public boolean isImageFile() { return isImageFile(getName()); }
	public static boolean isImageFile(String file) {
		String ext = file.substring(file.lastIndexOf(".") + 1);
		
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
	
	public boolean isVideoFile() { return isVideoFile(getName()); }
	public static boolean isVideoFile(String path)
	{
		String ext = path.substring(path.lastIndexOf(".") + 1);
		if(ext.equalsIgnoreCase("mp4") || 
			  ext.equalsIgnoreCase("3gp") || 
			  ext.equalsIgnoreCase("avi") ||
			  ext.equalsIgnoreCase("webm") || 
			  ext.equalsIgnoreCase("m4v"))
			return true;
		return false;
	}
}
