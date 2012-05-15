package org.brandroid.openmanager.data;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.brandroid.openmanager.util.FileManager;
import org.brandroid.openmanager.util.RootManager;
import org.brandroid.openmanager.util.ShellSession.UpdateCallback;
import org.brandroid.utils.Logger;

import android.net.Uri;
import android.os.PatternMatcher;

public class OpenFileRoot extends OpenPath implements OpenPath.OpenPathUpdateListener {

	private String mPath;
	private String mName;
	private String mPerms;
	private String mSym;
	private Long mDate = null;
	private Long mSize = null;
	private List<OpenPath> mChildren = null;
	
	public OpenFileRoot(OpenPath src)
	{
		mPath = src.getParent().getPath();
		if(!mPath.endsWith("/"))
			mPath += "/";
		mName = src.getName();
		if(src.isDirectory() && !mName.endsWith("/"))
			mName += "/";
		mDate = src.lastModified();
		mSize = src.length();
	}
	public OpenFileRoot(String parent, String listing) {
		//           10        20        30        40        50
		// 01234567890123456789012345678901234567890123456789012345678901234567890
		// drwxrwx--x    1 system   system        2048 Fri May 11 09:40:44 2012 dalvik-cach
		// -rw-r--r-- system   system   48238558 2012-04-27 21:56 com.twodboy.worldofgoofull-1.apk
		mPath = parent;
		if(!mPath.endsWith("/"))
			mPath += "/";
		PatternMatcher pmLong = new PatternMatcher(" [1-2][0-9][0-9][0-9]\\-[0-9]+\\-[0-9]+ ", PatternMatcher.PATTERN_SIMPLE_GLOB);
		boolean bLong = pmLong.match(listing);
		Pattern p = Pattern.compile("[0-9][0-9]\\:[0-9][0-9] " + (bLong ? "[1-2][0-9][0-9][0-9] " : ""));
		Matcher m = p.matcher(listing);
		if(m.matches())
		{
			mName = listing.substring(m.end());
			try {
				mDate = Date.parse(listing.substring(m.start(), m.end() - 1).trim());
				mSize = Long.parseLong(listing.substring(listing.lastIndexOf(" ", m.start()), m.start() - 1).trim());
			} catch(Exception e) { }
			mPerms = listing.split(" ")[0];
		} else {
			String[] parts = listing.split(" +");
			if(parts.length > 5)
			{
				mPerms = parts[0];
				int i = 3;
				try {
					if(parts.length >= 7)
						mSize = Long.parseLong(parts[i++]);
				} catch(NumberFormatException e) { }
				try {
					mDate = Date.parse(parts[i + 1] + " " + parts[i + 2]);
				} catch(Exception e) { }
			}
			mName = parts[parts.length - 1];
			if(mName.indexOf(" -> ") > -1)
			{
				mSym = mName.substring(mName.indexOf(" -> ") + 4);
				mName = mName.substring(0, mName.indexOf(" -> ") - 1).trim();
			}
		}
		if(mPerms != null && mPerms.startsWith("d") && !mName.endsWith("/"))
			mName += "/";
	}
	
	@Override
	public Boolean requiresThread() {
		return true;
	}
	
	@Override
	public Boolean exists() {
		return true;
	}
	
	@Override
	public boolean addToDb() {
		return super.addToDb();
	}
	
	@Override
	public Boolean canRead() {
		if(mPerms != null)
			return mPerms.indexOf("r") > -1;
		return new File(getPath()).canRead();
	}

	@Override
	public String getName() {
		return mName;
	}

	@Override
	public String getPath() {
		return mPath + mName;
	}

	@Override
	public String getAbsolutePath() {
		return getPath();
	}

	@Override
	public void setPath(String path) {
		mPath = path.substring(0, path.lastIndexOf("/"));
		mName = path.substring(path.lastIndexOf("/") + 1);
		OpenFile f = new OpenFile(path);
		mSize = f.length();
		mDate = f.lastModified();
	}

	@Override
	public long length() {
		return mSize != null ? mSize : new File(getPath()).length();
	}

	@Override
	public OpenPath getParent() {
		return FileManager.getOpenCache(mPath);
	}

	@Override
	public OpenPath getChild(String name) {
		try {
			for(OpenPath kid : list())
				if(kid.getName().equals(name))
					return kid;
		} catch(IOException e) { }
		return null;
	}
	
	public List<OpenPath> getChildren() { return mChildren; }

	@Override
	public OpenPath[] list() throws IOException {
		if(mChildren == null)
			return listFiles();
		return mChildren.toArray(new OpenPath[mChildren.size()]);
	}
	
	@Override
	public void list(final OpenContentUpdater callback) throws IOException {
		String path = getPath();
		if(!path.endsWith("/"))
			path += "/";
		Logger.LogDebug("Trying to list " + path + " via Su");
		final String[] buff = new String[]{null};
		RootManager.Default.setUpdateCallback(new UpdateCallback() {
			
			@Override
			public void onUpdate() {
				Logger.LogDebug("CF onUpdate");
				RootManager.Default.setUpdateCallback(null);
			}
			
			@Override
			public void onReceiveMessage(String msg) {
				if(msg.indexOf("\n") > -1)
				{
					while(msg.indexOf("\n") > -1)
					{
						String s = msg.substring(0, msg.indexOf("\n"));
						msg = msg.substring(msg.indexOf("\n") + 1);
						onReceiveMessage(s);
					}
					buff[0] = msg;
					return;
				}
				Logger.LogDebug("CF Message: " + msg);
				if(msg != null && !msg.trim().equals(""))
				{
					String[] parts = msg.split(" +", 7);
					if(parts.length < 7)
					{
						if(buff[0] != null)
						{
							msg = buff[0] + msg;
							parts = msg.split(" +", 7);
						} else buff[0] = msg;
					}
					if(parts.length >= 7)
						callback.add(new OpenFileRoot(getPath(), msg));
				}
			}
			
			@Override
			public void onExit() {
				Logger.LogDebug("CF onExit");
				RootManager.Default.setUpdateCallback(null);
			}
		});
		RootManager.Default.write("ls -" + getLSOpts() + " " + path);
	}

	@Override
	public OpenPath[] listFiles() throws IOException {
		String path = getPath();
		if(!path.endsWith("/"))
			path += "/";
		Logger.LogDebug("Trying to list " + path + " via Su");
		final long[] last = new long[]{new Date().getTime()};
		final String[] buff = new String[]{null};
		mChildren = new ArrayList<OpenPath>();
		RootManager.Default.setUpdateCallback(new UpdateCallback() {
			@Override
			public void onUpdate() {
				Logger.LogDebug("CF onUpdate");
				last[0] = 0;
				RootManager.Default.setUpdateCallback(null);
			}
			@Override
			public void onReceiveMessage(String msg) {
				if(msg.indexOf("\n") > -1)
				{
					for(String s : msg.split("\n"))
						onReceiveMessage(s);
					return;
				}
				last[0] = new Date().getTime();
				Logger.LogDebug("CF Message: " + msg);
				if(msg != null && !msg.trim().equals(""))
				{
					String[] parts = msg.split(" +", 11);
					if(parts.length < 11)
					{
						if(buff[0] != null)
							msg = buff[0] + msg;
						parts = msg.split(" +", 11);
					}
					if(parts.length == 11)
					{
						buff[0] = null;
						OpenFileRoot kid = new OpenFileRoot(getPath(), msg);
						if(!mChildren.contains(kid))
							mChildren.add(kid);
					} else buff[0] = msg;
				}
			}
			@Override
			public void onExit() {
				Logger.LogDebug("CF onExit");
			}
		});
		
		RootManager.Default.write("ls -" + getLSOpts() + " " + path);
		while(new Date().getTime() - last[0] < 1000) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) { }
		}
		return list();
	}
	
	private String getLSOpts()
	{
		String lsOpts = "l";
		if(Sorting.showHidden())
			lsOpts += "A";
		switch(Sorting.getType())
		{
		case ALPHA_DESC:
			lsOpts += "r";
		case ALPHA:
			break;
		case DATE_DESC:
			lsOpts += "r";
		case DATE:
			lsOpts += "t";
			break;
		case SIZE_DESC:
			lsOpts += "r";
		case SIZE:
			lsOpts += "S";
			break;
		case TYPE:
			lsOpts += "X";
			break;
		}
		return lsOpts;
	}
	
	public File getFile() { return new File(getPath()); }

	@Override
	public Boolean isDirectory() {
		if(mPerms != null)
			return mPerms.startsWith("d");
		return getFile().isDirectory();
	}

	@Override
	public Boolean isFile() {
		return !isDirectory();
	}

	@Override
	public Boolean isHidden() {
		return mName.startsWith(".");
	}

	@Override
	public Uri getUri() {
		return Uri.parse(getPath());
	}

	@Override
	public Long lastModified() {
		return mDate != null ? mDate : new File(getPath()).lastModified();
	}

	@Override
	public Boolean canWrite() {
		if(mPerms != null)
			return mPerms.indexOf("w") > -1;
		return getFile().canWrite();
	}

	@Override
	public Boolean canExecute() {
		if(mPerms != null)
			return mPerms.indexOf("x") > -1;
		return getFile().canExecute();
	}

	@Override
	public Boolean delete() {
		return getFile().delete();
	}

	@Override
	public Boolean mkdir() {
		return getFile().mkdir();
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
	public String getDetails(boolean countHiddenChildren, boolean showLongDate)
	{
		String deets = "";
		if(mChildren != null)
			deets = mChildren.size() + " %s | ";
		Long last = lastModified();
		if(last != null && last > 0)
			deets += new SimpleDateFormat(showLongDate ? "MM-dd-yyyy HH:mm" : "MM-dd-yy")
						.format(last);
		return deets;
	}
}
