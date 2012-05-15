package org.brandroid.openmanager.data;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.brandroid.openmanager.R;
import org.brandroid.openmanager.data.OpenNetworkPath.NetworkListener;
import org.brandroid.openmanager.fragments.DialogHandler;
import org.brandroid.utils.Logger;

import android.os.Environment;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.UserInfo;

public abstract class OpenNetworkPath extends OpenPath
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -3829590216951441869L;
	protected UserInfo mUserInfo;
	private int mServersIndex = -1;
	public static final JSch DefaultJSch = new JSch();
	public static int Timeout = 20000;
	protected String mName = null;
	protected int mPort = -1;
	
	public interface NetworkListener
	{
		public static final NetworkListener DefaultListener = new NetworkListener() {
			
			@Override
			public void OnNetworkFailure(OpenNetworkPath np, OpenFile dest, Exception e) {
				Logger.LogWarning("Network Failure for " + np);
			}
			
			@Override
			public void OnNetworkCopyUpdate(int[] progress) {
				
			}
			
			@Override
			public void OnNetworkCopyFinished(OpenNetworkPath np, OpenFile dest) {
				Logger.LogDebug("Network Copy Finished for " + np + " \u2661 " + dest);
			}
		};
		public void OnNetworkCopyFinished(OpenNetworkPath np, OpenFile dest);
		public void OnNetworkCopyUpdate(int... progress);
		public void OnNetworkFailure(OpenNetworkPath np, OpenFile dest, Exception e);
	}
	
	@Override
	public Boolean requiresThread() {
		return true;
	}
	
	public void connect() throws IOException
	{
		Logger.LogVerbose("Connecting OpenNetworkPath");
	}
	public void disconnect() {
		Logger.LogVerbose("Disconnecting OpenNetworkPath");
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
	public void syncTempFileDown() throws IOException
	{
		OpenFile tmp = getTempFile();
		if(!tmp.exists())
			tmp.create();
		else if(lastModified() <= tmp.lastModified())
			return;
		copyTo(tmp, NetworkListener.DefaultListener);
	}
	public void syncTempFileUp() throws IOException
	{
		OpenFile tmp = getTempFile();
		if(!tmp.exists()) return;
		if(lastModified() > tmp.lastModified()) return;
		copyFrom(tmp, NetworkListener.DefaultListener);
	}
	
	public boolean copyFrom(OpenFile f, NetworkListener l) { return false; }
	public boolean copyTo(OpenFile f, NetworkListener l) { return false; }
	
	public abstract boolean isConnected() throws IOException;

	/**
	 * This does not change the actual path of the underlying object, just what is displayed to the user.
	 * @param name New title for OpenPath object
	 */
	public void setName(String name) { mName = name; } 
	public String getName() { return mName; }
	public String getName(String defaultName) { return mName != null ? mName : defaultName; }
	
	public UserInfo getUserInfo() { return mUserInfo; }
	public UserInfo setUserInfo(UserInfo info) { mUserInfo = info; return mUserInfo; }
	
	public int getServersIndex() {
		return mServersIndex;
	}
	public void setServersIndex(int index) { mServersIndex = index; }
	
	public abstract OpenNetworkPath[] getChildren();
	
	@Override
	public String toString() {
		return getName(super.toString());
	}

	public void setPort(int port) {
		mPort = port;
	}
	public int getPort() { return mPort; }
	
	@Override
	public String getDetails(boolean countHiddenChildren, boolean showLongDate)
	{
		String deets = "";
		
		Long last = lastModified();
		if(last != null)
		{
			deets += new SimpleDateFormat(showLongDate ? "MM-dd-yyyy HH:mm" : "MM-dd-yy")
						.format(last);
		}
		
		return deets;
	}
}
