package org.brandroid.openmanager.data;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

public class OpenContent extends OpenPath
{
	private static final long serialVersionUID = 3185620135972000643L;
	private final Uri uri;
	private final Context mContext;
	
	public OpenContent(Uri uri, Context context) {
		this.uri = uri;
		mContext = context;
	}

	@Override
	public String getName() {
		String ret = uri.getLastPathSegment();
		try {
			Cursor cursor = mContext.getContentResolver().query(uri, null, null, null, null);
			cursor.moveToFirst();
			int nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
			if(nameIndex >= 0)
				ret = cursor.getString(nameIndex);
			cursor.close();
		} catch(Exception e) { }
		return ret;
	}
	
	@Override
	public String getExtension() {
		String ext = getName();
		if(ext != null && ext.indexOf(".") > -1)
			ext = ext.substring(ext.lastIndexOf(".") + 1);
		if(ext.length() > 5)
			ext = "..." + ext.substring(ext.length() - 3);
		return ext;
	}

	@Override
	public String getPath() {
		return uri.getPath();
	}

	@Override
	public String getAbsolutePath() {
		return uri.getPath();
	}

	@Override
	public void setPath(String path) {
		
	}

	@Override
	public long length() {
		long ret = 0;
		try {
			Cursor cursor = mContext.getContentResolver().query(uri, null, null, null, null);
			cursor.moveToFirst();
			int sizeIndex = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE);
			if(sizeIndex >= 0)
				ret = cursor.getLong(sizeIndex);
			cursor.close();
		} catch(Exception e) { }
		return ret;
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
	public OpenPath[] list() throws IOException {
		return null;
	}

	@Override
	public OpenPath[] listFiles() throws IOException {
		return null;
	}

	@Override
	public Boolean isDirectory() {
		return false;
	}

	@Override
	public Boolean isFile() {
		return true;
	}

	@Override
	public Boolean isHidden() {
		return false;
	}

	@Override
	public Uri getUri() {
		return uri;
	}

	@Override
	public Long lastModified() {
		long ret = 0;
		try {
			Cursor cursor = mContext.getContentResolver().query(uri, null, null, null, null);
			cursor.moveToFirst();
			int dateIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED);
			if(dateIndex >= 0)
				ret = cursor.getLong(dateIndex);
			cursor.close();
		} catch(Exception e) { }
		return ret;
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
		return true;
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
	public InputStream getInputStream() throws IOException
	{
		return mContext.getContentResolver().openInputStream(uri);
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return mContext.getContentResolver().openOutputStream(uri);
	}

}
