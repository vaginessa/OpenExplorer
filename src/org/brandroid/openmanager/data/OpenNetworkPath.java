package org.brandroid.openmanager.data;

import java.io.IOException;
import java.io.InputStream;

import org.brandroid.utils.Logger;

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
		
	}
}
