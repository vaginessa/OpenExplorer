package org.brandroid.openmanager.data;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import org.brandroid.utils.Logger;

import android.database.Cursor;
import android.net.Uri;
import android.view.View;
import android.widget.TextView;

public class OpenCursor extends OpenPath
{
	private static final long serialVersionUID = -8828123354531942575L;
	//private Cursor mCursor;
	private OpenMediaStore[] mChildren = new OpenMediaStore[0];
	private final String mName;
	private String mTitle;
	private Long mTotalSize = 0l;
	private boolean loaded = false;
	private Long mModified = Long.MIN_VALUE;
	public static int LoadedCursors = 0;
	private UpdateBookmarkTextListener mListener = null;
	
	public OpenCursor(String name)
	{
		mName = mTitle = name;
		loaded = false;
	}
	
	public void setUpdateBookmarkTextListener(UpdateBookmarkTextListener listener)
	{
		mListener = listener;
	}
	public boolean hasListener() { return mListener != null; }
	
	public boolean isLoaded() { return loaded; }
	
	public interface UpdateBookmarkTextListener
	{
		void updateBookmarkText(String txt);
	}
	
	public void setCursor(Cursor c)
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
			mModified = Math.max(tmp.lastModified(), mModified);
			if(!tmp.exists()) continue;
			if(!tmp.getFile().exists()) continue;
			kids.add(tmp);
			mTotalSize += tmp.getFile().length();
		}
		mChildren = new OpenMediaStore[kids.size()];
		mChildren = kids.toArray(mChildren);
		if(mListener != null)
			mListener.updateBookmarkText("(" + mChildren.length + ")");
		Logger.LogInfo(getName() + " found " + mChildren.length);
		loaded = true;
		c.close();
	}

	@Override
	public String getName() {
		return mTitle;
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
		return mModified;
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

	public void setName(String name) {
		mTitle = name;
	}

}
