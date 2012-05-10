package org.brandroid.openmanager.util;

public class SortType {
	
	Type mWhich = Type.NONE;
	boolean mFoldersFirst = true;
	
	public SortType(Type which)
	{
		mWhich = which;
	}
	
	public boolean folderFirst() { return mFoldersFirst; }
	public Type getType() { return mWhich; }
	
	public enum Type
	{
		NONE,
		ALPHA,
		TYPE,
		SIZE,
		SIZE_DESC,
		DATE,
		DATE_DESC,
		ALPHA_DESC
	}
	
	public static SortType NONE = new SortType(Type.NONE),
			ALPHA = new SortType(Type.ALPHA),
			TYPE = new SortType(Type.TYPE),
			SIZE = new SortType(Type.SIZE),
			SIZE_DESC = new SortType(Type.SIZE_DESC),
			DATE = new SortType(Type.DATE),
			DATE_DESC = new SortType(Type.DATE_DESC),
			ALPHA_DESC = new SortType(Type.ALPHA_DESC);
}