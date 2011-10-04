package org.brandroid.openmanager.data;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Comparator;

import org.brandroid.openmanager.FileManager.SortType;
import org.brandroid.utils.Logger;

import android.net.Uri;

public abstract class OpenFace implements Serializable, Comparable<OpenFace>
{
	public static SortType Sorting = SortType.ALPHA;
	
	private static final long serialVersionUID = 332701810738149106L;
	public abstract String getName();
	public abstract String getPath();
	public abstract String getAbsolutePath();
	public abstract long length();
	public abstract OpenFace getParent();
	public abstract OpenFace getChild(String name);
	public abstract OpenFace[] list() throws IOException;
	public abstract OpenFace[] listFiles() throws IOException;
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
	public int compareTo(OpenFace other)
	{
		return compare(this, other);
	}
	public static int compare(OpenFace fa, OpenFace fb)
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
	}
}
