package org.brandroid.openmanager.data;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;

import android.net.Uri;

public abstract class OpenFace implements Serializable
{
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
}
