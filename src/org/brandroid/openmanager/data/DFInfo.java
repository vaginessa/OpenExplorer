package org.brandroid.openmanager.data;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Hashtable;

import org.brandroid.utils.Logger;

public class DFInfo
{
	private String mPath;
	private int mSize, mUsed, mFree, mBlocksize;
	private static Hashtable<String, DFInfo> mDefault = null;
	
	public String getPath() { return mPath; }
	public int getSize() { return mSize; }
	public int getUsed() { return mUsed; }
	public int getFree() { return mFree; }
	public int getBlockSize() { return mBlocksize; }
	
	public DFInfo(String path, int size, int used, int free, int blocksize)
	{
		mPath = path;
		mSize = size;
		mUsed = used;
		mFree = free;
		mBlocksize = blocksize;
	}
	public static int getSize(String s)
	{
		int level = 0;
		if(s.endsWith("K"))
			level = 1;
		else if(s.endsWith("M"))
			level = 2;
		else if(s.endsWith("G"))
			level = 3;
		else if(s.endsWith("T"))
			level = 4;
		try {
			double sz = Integer.parseInt(s.replaceAll("[^0-9\\.]", ""));
			sz *= (1024 ^ level);
			return (int)Math.floor(sz);
		} catch(Exception e) { Logger.LogError("Unable to get size from [" + s + "]", e); return -1; }
	}
	public static Hashtable<String, DFInfo> LoadDF()
	{
		if(mDefault != null) return mDefault;
		Process dfProc;
		DataInputStream is = null;
		DataOutputStream os = null;
		mDefault = new Hashtable<String, DFInfo>();
		try {
			dfProc = Runtime.getRuntime().exec("df");
			is = new DataInputStream(dfProc.getInputStream());
			os = new DataOutputStream(dfProc.getOutputStream());
			os.writeBytes("df\n");
			os.flush();
			String sl;
			while((sl = is.readLine()) != null)
			{
				sl = sl.replaceAll("  *", " ");
				Logger.LogInfo("DF: " + sl);
				if(!sl.startsWith("/"))
					continue;
				try {
					String[] slParts = sl.split(" ");
					DFInfo item = new DFInfo(slParts[0], getSize(slParts[1]), getSize(slParts[2]), getSize(slParts[3]), getSize(slParts[4]));
					Logger.LogInfo("DF: Added " + item.getPath() + " - " + item.getFree() + "/" + item.getSize());
					mDefault.put(slParts[0], item);
				} catch(ArrayIndexOutOfBoundsException e) { Logger.LogWarning("DF: Unable to add " + sl); }
			}
		} catch (IOException e) {
			Logger.LogError("DF: Couldn't get Drive sizes.", e);
		} finally {
			try {
				if(os != null)
					os.close();
				if(is != null)
					is.close();
			} catch (IOException e) {
				Logger.LogWarning("DF: Couldn't close drive size input stream.", e);
			}
		}
		return mDefault;
	}
	
}
