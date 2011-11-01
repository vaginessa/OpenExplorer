package org.brandroid.openmanager.data;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.SoftReference;

import org.brandroid.utils.Logger;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;

public class OpenCursor extends OpenPath
{
	private Cursor mCursor;
	private String mName;
	
	public OpenCursor(Cursor c, String name)
	{
		mCursor = c;
		mName = name;
	}

	@Override
	public String getName() {
		return mName;
	}

	@Override
	public String getPath() {
		return mName;
	}

	@Override
	public String getAbsolutePath() {
		return mName;
	}

	@Override
	public long length() {
		return mCursor.getCount();
	}

	@Override
	public OpenPath getParent() {
		return null;
	}

	@Override
	public OpenPath getChild(String name) {
		return null;
	}

	@Override
	public OpenMediaStore[] list() {
		OpenMediaStore[] ret = new OpenMediaStore[(int)length()];
		mCursor.moveToFirst();
		int i = 0;
		while(!mCursor.isAfterLast())
		{
			ret[i++] = new OpenMediaStore(this);
			mCursor.moveToNext();
		}
		return ret;
	}

	@Override
	public OpenMediaStore[] listFiles() {
		return list();
	}

	@Override
	public Boolean isDirectory() {
		return true;
	}

	@Override
	public Boolean isFile() {
		return false;
	}

	@Override
	public Boolean isHidden() {
		return false;
	}

	@Override
	public Uri getUri() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Long lastModified() {
		return null;
	}

	@Override
	public Boolean canRead() {
		return true;
	}

	@Override
	public Boolean canWrite() {
		return false;
	}

	@Override
	public Boolean canExecute() {
		return false;
	}

	@Override
	public Boolean exists() {
		return true;
	}

	@Override
	public Boolean requiresThread() {
		return false;
	}

	@Override
	public Boolean delete() {
		return false;
	}

	@Override
	public Boolean mkdir() {
		return false;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	public Cursor getCursor() {
		return mCursor; 
	}

	@Override
	public void setPath(String path) {
		
	}

}
