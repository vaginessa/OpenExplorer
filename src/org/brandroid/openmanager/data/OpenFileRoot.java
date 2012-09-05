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
import org.brandroid.openmanager.data.OpenPath.OpenPathByteIO;
import org.brandroid.openmanager.fragments.DialogHandler;
import org.brandroid.openmanager.util.FileManager;
import org.brandroid.openmanager.util.RootManager;
import org.brandroid.openmanager.util.RootManager.UpdateCallback;
import org.brandroid.utils.Logger;
import org.brandroid.utils.Utils;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.PatternMatcher;

public class OpenFileRoot
	extends OpenPath
	implements OpenPath.OpenPathUpdateListener,
		OpenPath.NeedsTempFile, OpenPath.OpenPathCopyable,
		OpenPathByteIO
{

	private String mPath;
	private String mName;
	private String mPerms;
	private String mSym;
	private Long mDate = null;
	private Long mSize = null;
	private WeakReference<List<OpenPath>> mChildren = null;
	private boolean mLoaded = false;
	
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
	public boolean isLoaded() {
		return mLoaded;
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
			return new OpenPath[0];
		if(getChildren() != null)
			return getChildren().toArray(new OpenPath[getChildren().size()]);
		else return new OpenPath[0];
	}
	
	private void addChild(OpenPath kid)
	{
		if(getChildren() == null)
		{
			ArrayList<OpenPath> tmp = new ArrayList<OpenPath>();
			mChildren = new WeakReference<List<OpenPath>>(tmp);
		}
		if(!mChildren.get().contains(kid))
			mChildren.get().add(kid);
	}
	
	public void list(final OpenContentUpdater callback) throws IOException {
		mLoaded = false;
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
		String lsopts = getLSOpts();
		if(!RootManager.hasBusybox())
			lsopts = "";
		final String w = "ls -l" + lsopts + " " + path;
		final String md5 = Utils.md5(w);
		UpdateCallback callback2 = new UpdateCallback() {
			@Override
			public void onUpdate() {
				Logger.LogDebug("CF onUpdate");
				//mLoaded = true;
				//callback.doneUpdating();
				//RootManager.Default.setUpdateCallback(null);
			}
			
			private boolean processMessage(String msg)
			{
				String[] parts = msg.split(" +", 7);
				if(parts.length < 4)
				{
					if(buff[0] != null)
					{
						msg = buff[0] + msg;
						parts = msg.split(" +", 4);
					} else buff[0] = msg;
				}
				if(parts.length >= 4)
				{
					OpenFileRoot kid = new OpenFileRoot(getPath(), msg);
					addChild(kid);
					callback.addContentPath(kid);
					return true;
				}
				else if(msg.trim().length() > 0)
				{
					Logger.LogDebug("CF Saving for later: " + msg);
					return false;
				}
				return true;
			}
			
			@Override
			public boolean onReceiveMessage(String msg) {
				boolean isDone = false;
				if(msg.indexOf(md5) > -1)
				{
					isDone = true;
					mLoaded = true;
					msg = msg.substring(0, msg.indexOf(md5));
				}
				Logger.LogDebug("CF Message: (" + w + "): " + msg.length()); //.replace("\n", "\\n"));
				if(msg.indexOf("\n") > -1 || isDone)
				{
					for(String s : msg.split("\n"))
						processMessage(s);
					if(isDone && msg.lastIndexOf("\n") < msg.length() - 1)
					{
						buff[0] = msg.substring(msg.lastIndexOf("\n") + 1);
						if(!buff[0].equals(""))
							return false;
					}
				}
				if(msg != null && !msg.trim().equals(""))
					if(!processMessage(msg))
					{
						isDone = false;
						mLoaded = false;
						buff[0] = msg;
					}
				if(isDone)
				{
					callback.doneUpdating();
					onExit();
				}
				return isDone;
			}
			
			@Override
			public void onExit() {
				Logger.LogDebug("CF onExit");
				RootManager.Default.setUpdateCallback(null);
				mLoaded = true;
			}
		};
		RootManager.Default.write(w + " && echo \"" + md5 + "\"", callback2);
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
		String opts = getLSOpts();
		String cmd = "ls -l" + opts + " " + path;
		String data = execute(cmd, !opts.equals(""));
		while(!data.equals(""))
		{
			String child = data; 
			if(data.indexOf("\n") > -1)
			{
				child = data.substring(0, data.indexOf("\n"));
				data = data.substring(data.indexOf("\n") + 1);
			} else data = "";
			if(child.split(" ").length > 4)
				addChild(new OpenFileRoot(getPath(), child));
			else Logger.LogWarning("Skipping Row while listing: " + child);
		}
		return list();
	}
	
	private String getLSOpts()
	{
		String lsOpts = "";
		if(ShowHiddenFiles)
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

	@SuppressLint("NewApi")
	@Override
	public Boolean canExecute() {
		if(mPerms != null)
			return mPerms.indexOf("x") > -1;
		return Build.VERSION.SDK_INT > 8 ? getFile().canExecute() : true;
	}

	@Override
	public Boolean delete() {
		execute("rm -f " + getPath(), false);
		return true;
	}

	@Override
	public Boolean mkdir() {
		return getFile().mkdir();
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return tempDownload(null).getInputStream();
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return tempDownload(null).getOutputStream();
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
	public OpenFile tempDownload(AsyncTask<?,?,?> task) throws IOException {
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
	public void tempUpload(AsyncTask<?, ?, ?> task) throws IOException {
		OpenFile tmp = getTempFile();
		if(tmp == null) throw new IOException("Unable to download Temp file");
		if(!tmp.exists())
			tmp.create();
		else if(lastModified() <= tmp.lastModified())
			return;
		copyFrom(tmp);
	}
	
	private String execute(final String cmd) { return execute(cmd, false); }
	private String execute(final String cmd, boolean useBusyBox)
	{
		final boolean[] waiting = new boolean[]{true};
		final String[] ret = new String[1];
		String bb = useBusyBox ? RootManager.Default.getBusyBox() : "";
		if(bb == null || !bb.startsWith("/"))
			bb = "";
		else if(!bb.equals(""))
			bb += " ";
		final String md5 = Utils.md5(cmd);
		try {
			RootManager.Default.write(bb + cmd + " && echo \"" + md5 + "\"",
				new UpdateCallback() {
					public void onUpdate() {
						Logger.LogDebug("Done with command: " + cmd);
						waiting[0] = false;
					}
					public boolean onReceiveMessage(String msg) {
						Logger.LogDebug("OpenFileRoot.execute.onReceiveMessage(" + msg + ")");
						ret[0] = (ret[0] == null ? "" : ret[0]) + msg;
						if(msg.indexOf(md5) > -1)
						{
							ret[0] = ret[0].substring(0, ret[0].indexOf(md5));
							waiting[0] = false;
							return true;
						}
						return false;
					}
					public void onExit() {
						waiting[0] = false;
					}
			});
		} catch(Exception e) {
			Logger.LogError("Unable to execute command: " + cmd, e);
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
					public boolean onReceiveMessage(String msg) {
						waiting[0] = false;
						return true;
					}
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
		if(file instanceof OpenFile)
		{
			String cmd = "cp -f " + file.getPath() + " " + getPath();
			String output = execute(cmd, false);
			if(output.indexOf(cmd) > -1)
				return true;
		}
		return copy(file, this);
	}
	@Override
	public byte[] readBytes() {
		OpenFile tmp = getTempFile();
		if(tmp.exists() && tmp.length() > 0 && tmp.lastModified() >= lastModified())
		{
			String ret = execute("cat " + tmp.getPath(), false);
			return ret.getBytes();
		} else {
			String ret = execute("cat " + getPath(), false);
			return ret.getBytes();
		}
		
	}
	@Override
	public void writeBytes(byte[] bytes) {
		String ret = execute("cat > " + getPath() + "\n" + new String(bytes), false);
		Logger.LogDebug("writeBytes response: " + ret);
	}
}
