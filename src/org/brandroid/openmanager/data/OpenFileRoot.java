package org.brandroid.openmanager.data;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.brandroid.openmanager.data.OpenNetworkPath.NetworkListener;
import org.brandroid.openmanager.fragments.DialogHandler;
import org.brandroid.openmanager.util.FileManager;
import org.brandroid.openmanager.util.RootManager;
import org.brandroid.openmanager.util.RootManager.UpdateCallback;
import org.brandroid.utils.Logger;

import android.net.Uri;
import android.os.Environment;
import android.os.PatternMatcher;

public class OpenFileRoot
	extends OpenPath
	implements //OpenPath.OpenPathUpdateListener,
		OpenPath.NeedsTempFile, OpenPath.OpenPathCopyable
{

	private String mPath;
	private String mName;
	private String mPerms;
	private String mSym;
	private Long mDate = null;
	private Long mSize = null;
	private WeakReference<List<OpenPath>> mChildren = null;
	
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
	
	public List<OpenPath> getChildren() { return mChildren != null ? mChildren.get() : null; }

	@Override
	public OpenPath[] list() throws IOException {
		if(mChildren == null)
			return listFiles();
		return getChildren().toArray(new OpenPath[getChildren().size()]);
	}
	
	private void addChild(OpenPath kid)
	{
		if(getChildren() == null)
		{
			ArrayList<OpenPath> tmp = new ArrayList<OpenPath>();
			mChildren = new WeakReference<List<OpenPath>>(tmp);
		} else {
			if(!mChildren.get().contains(kid))
				mChildren.get().add(kid);
		}
		
	}
	
	public void list(final OpenContentUpdater callback) throws IOException {
		if(getChildren() != null)
		{
			for(OpenPath kid : getChildren())
				callback.addContentPath(kid);
			return;
		}
		String path = getPath();
		if(!path.endsWith("/"))
			path += "/";
		Logger.LogDebug("Trying to list " + path + " via Su with Callback");
		final String[] buff = new String[]{null};
		final String w = "ls -" + getLSOpts() + " " + path;
		RootManager proc = new RootManager();
		UpdateCallback callback2 = new UpdateCallback() {
			
			@Override
			public void onUpdate() {
				Logger.LogDebug("CF onUpdate");
				callback.doneUpdating();
				RootManager.Default.setUpdateCallback(null);
			}
			
			private void processMessage(String msg)
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
				{
					OpenFileRoot kid = new OpenFileRoot(getPath(), msg);
					addChild(kid);
					callback.addContentPath(kid);
				}
				else if(msg.trim().length() > 0)
					Logger.LogDebug("CF Saving for later: " + msg);
			}
			
			@Override
			public void onReceiveMessage(String msg) {
				Logger.LogDebug("CF Message: (" + w + "): " + msg.length()); //.replace("\n", "\\n"));
				if(msg.indexOf("\n") > -1)
				{
					RootManager.Default.onUpdate();
					while(msg.indexOf("\n") > -1)
					{
						String s = msg.substring(0, msg.indexOf("\n"));
						msg = msg.substring(msg.indexOf("\n") + 1);
						processMessage(s);
					}
					buff[0] = msg;
					return;
				}
				if(msg != null && !msg.trim().equals(""))
					processMessage(msg);
			}
			
			@Override
			public void onExit() {
				Logger.LogDebug("CF onExit");
				RootManager.Default.setUpdateCallback(null);
			}
		};
		RootManager.Default.write(w, callback2);
	}

	@Override
	public OpenPath[] listFiles() throws IOException {
		String path = getPath();
		if(!path.endsWith("/"))
			path += "/";
		Logger.LogDebug("Trying to list " + path + " via Su");
		if(mChildren != null)
			mChildren.clear();
		else mChildren = new WeakReference<List<OpenPath>>(new ArrayList<OpenPath>());
		String cmd = "ls -l " + path;
		if(RootManager.Default.getBusyBox() != "")
			cmd = "ls -l" + getLSOpts() + " " + path;
		String data = execute(cmd, true);
		while(data.indexOf("\n") > -1)
		{
			addChild(new OpenFileRoot(getPath(), data.substring(0, data.indexOf("\n"))));
			data = data.substring(data.indexOf("\n") + 1);
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
		execute("rm " + getPath(), false);
		return true;
	}

	@Override
	public Boolean mkdir() {
		return getFile().mkdir();
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return tempDownload().getInputStream();
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return tempDownload().getOutputStream();
	}

	@Override
	public String getDetails(boolean countHiddenChildren, boolean showLongDate)
	{
		String deets = "";
		if(getChildren() != null)
			deets = getChildren().size() + " %s | ";
		else if(isFile())
			deets = DialogHandler.formatSize(length()) + " | ";
		Long last = lastModified();
		if(last != null && last > 0)
			deets += new SimpleDateFormat(showLongDate ? "MM-dd-yyyy HH:mm" : "MM-dd-yy")
						.format(last);
		return deets;
	}

	public String getTempFileName()
	{
		return getPath().replaceAll("[^A-Za-z0-9\\.]", "-");
	}
	public OpenFile getTempFile()
	{
		OpenFile root = OpenFile.getTempFileRoot();
		if(root != null)
			return root.getChild(getTempFileName());
		return null;
	}
	
	@Override
	public OpenFile tempDownload() throws IOException {
		OpenFile tmp = getTempFile();
		if(tmp == null) throw new IOException("Unable to download Temp file");
		if(!tmp.exists())
			tmp.create();
		else if(lastModified() <= tmp.lastModified())
			return tmp;
		copyTo(tmp);
		return tmp;
	}
	@Override
	public void tempUpload() throws IOException {
		OpenFile tmp = getTempFile();
		if(tmp == null) throw new IOException("Unable to download Temp file");
		if(!tmp.exists())
			tmp.create();
		else if(lastModified() <= tmp.lastModified())
			return;
		copyFrom(tmp);
	}
	
	private String execute(final String cmd, boolean useBusyBox)
	{
		final boolean[] waiting = new boolean[]{true};
		final String[] ret = new String[1];
		final String bb = useBusyBox ? RootManager.Default.getBusyBox() : "";
		try {
			RootManager.Default.write(bb + cmd,
				new UpdateCallback() {
					public void onUpdate() {
						Logger.LogDebug("Done with command: " + cmd);
						waiting[0] = false;
					}
					public void onReceiveMessage(String msg) {
						ret[0] = (ret[0] == null ? "" : ret[0]) + msg;
					}
					public void onExit() { }
			});
		} catch(Exception e) {
			return null;
		}
		try {
			while(waiting[0]) { Thread.sleep(50); }
		} catch(InterruptedException e) { }
		return ret[0];
	}
	
	public static boolean copy(final OpenPath src, final OpenPath dest) {
		final boolean[] waiting = new boolean[]{true};
		try {
			RootManager.Default.write("cp -f " + src.getPath() + " " + dest.getPath(),
				new UpdateCallback() {
					public void onUpdate() {
						Logger.LogDebug("Done copying " + src.getPath() + " to " + dest.getPath());
						waiting[0] = false;
					}
					public void onReceiveMessage(String msg) { }
					public void onExit() { }
			});
		} catch(Exception e) {
			return false;
		}
		try {
			while(waiting[0]) { Thread.sleep(50); }
		} catch(InterruptedException e) { }
		return true;
	}
	public boolean copyTo(OpenFile dest) { return copy(this, dest); }
	@Override
	public boolean copyFrom(OpenPath file) {
		return copy(file, this);
	}
}
