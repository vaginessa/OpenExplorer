package org.brandroid.openmanager.data;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import android.database.Cursor;
import android.net.Uri;

public class OpenCursor extends OpenPath
{
	private static final long serialVersionUID = -8828123354531942575L;
	//private Cursor mCursor;
	private OpenMediaStore[] mChildren;
	private String mName;
	private Long mTotalSize = 0l;
	
	public OpenCursor(Cursor c, String name)
	{
		//mCursor = c;
		if(c == null) return;
		ArrayList<OpenMediaStore> kids = new ArrayList<OpenMediaStore>(c.getCount());
		//mChildren = new OpenMediaStore[(int)c.getCount()];
		c.moveToFirst();
		for(int i = 0; i < c.getCount(); i++)
		{
			c.moveToPosition(i);
			OpenMediaStore tmp = new OpenMediaStore(this, c);
			if(!tmp.exists()) continue;
			if(!tmp.getFile().exists()) continue;
			kids.add(tmp);
			mTotalSize += tmp.getFile().length();
		}
		mChildren = new OpenMediaStore[kids.size()];
		mChildren = kids.toArray(mChildren);
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
		return mChildren.length; // mCursor.getCount();
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
		return mChildren;
		/*
		OpenMediaStore[] ret = new OpenMediaStore[(int)length()];
		mCursor.moveToFirst();
		int i = 0;
		while(!mCursor.isAfterLast())
		{
			if(!mCursor.isBeforeFirst())
				ret[i++] = new OpenMediaStore(this);
			mCursor.moveToNext();
		}
		return ret;
		*/
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
	
	@Override
	public void setPath(String path) {
		
	}
	
	public long getTotalSize()
	{
		return mTotalSize;
	}

}
