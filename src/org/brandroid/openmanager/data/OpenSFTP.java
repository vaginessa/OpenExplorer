package org.brandroid.openmanager.data;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.brandroid.utils.Logger;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.UserInfo;

import android.net.Uri;

public class OpenSFTP extends OpenNetworkPath
{
	private long filesize = 0l;
	private boolean isConnected = false;
	private Session mSession = null;
	private ChannelSftp mChannel = null;
	private InputStream in = null;
	private OutputStream out = null;
	private final String mHost, mUser, mRemotePath;
	private UserInfo mUserInfo = null;
	private SftpATTRS mAttrs = null;
	public int Timeout = 30000;
	
	public OpenSFTP(String fullPath)
	{
		Uri uri = Uri.parse(fullPath);
		mHost = uri.getHost();
		mUser = uri.getUserInfo();
		mRemotePath = uri.getPath();
	}
	public OpenSFTP(Uri uri)
	{
		mHost = uri.getHost();
		mUser = uri.getUserInfo();
		mRemotePath = uri.getPath();
	}
	public OpenSFTP(String host, String user, String path, UserInfo info)
	{
		mHost = host;
		mUser = user;
		mRemotePath = path;
		mUserInfo = info;
	}
	public OpenSFTP(OpenSFTP parent, LsEntry child)
	{
		mHost = parent.getHost();
		mUser = parent.getUser();
		mRemotePath = child.getLongname();
		mAttrs = child.getAttrs();
	}
	
	public String getHost() { return mHost; }
	public String getUser() { return mUser; }
	public String getRemotePath() { return mRemotePath; }
	public UserInfo getUserInfo() { return mUserInfo; }
	public UserInfo setUserInfo(UserInfo info)
	{
		mUserInfo = info;
		return info;
	}

	@Override
	public String getName() {
		if(mRemotePath.equals("") || mRemotePath.equals("/"))
			return mHost;
		String ret = mRemotePath.substring(mRemotePath.lastIndexOf("/", mRemotePath.length() - 1) + 1);
		if(ret.equals(""))
			ret = mRemotePath;
		return ret;
	}

	@Override
	public String getPath() {
		return "sftp://" + mUser + "@" + mHost + mRemotePath;
	}

	@Override
	public String getAbsolutePath() {
		return getPath();
	}

	@Override
	public void setPath(String path) {
	}

	@Override
	public long length() {
		return 0;
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
		return listFiles();
	}

	@Override
	public OpenPath[] listFiles() throws IOException {
		List<OpenPath> kids = new ArrayList<OpenPath>(); 
		try {
			connect();
			Logger.LogDebug("Listing Files!");
			Vector vv = mChannel.ls(".");
			for(Object o : vv)
			{
				if(o instanceof LsEntry)
				{
					LsEntry item = (LsEntry)o;
					kids.add(new OpenSFTP(this, item));
				}
			}
		} catch (SftpException e) {
			Logger.LogError("SftpException during listFiles", e);
			throw new IOException("SftpException during listFiles", e);
		} catch (JSchException e) {
			Logger.LogError("JSchException during listFiles", e);
			throw new IOException("JSchException during listFiles", e);
		}
		return kids.toArray(new OpenSFTP[0]);
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
		return null;
	}

	@Override
	public Long lastModified() {
		return null;
	}

	@Override
	public Boolean canRead() {
		return false;
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
		return true;
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
	public void disconnect()
	{
		if(mChannel != null)
			mChannel.disconnect();
		if(mSession != null)
			mSession.disconnect();
		try {
			if(in != null)
				in.close();
		} catch (IOException e) { }
		try {
			if(out != null)
				out.close();
		} catch (IOException e) { }
		isConnected = false;
	}
	
	@Override
	public void connect() throws JSchException
	{
		Logger.LogDebug("Attempting to connect to OpenSFTP " + getName());
		if(mSession != null && mSession.isConnected() && mChannel != null && mChannel.isConnected())
			return;
		disconnect();
		//Logger.LogDebug("Ready for new connection");
		JSch jsch = new JSch();
		//try {
			mSession = jsch.getSession(mUser, mHost, 22);
			mSession.setUserInfo(mUserInfo);
			mSession.setTimeout(Timeout);
			Logger.LogDebug("Connecting session...");
			mSession.connect();
			
			Logger.LogDebug("Session achieved. Opening Channel...");
			//String command = "scp -f " + mRemotePath;
			mChannel = (ChannelSftp)mSession.openChannel("sftp");
			//((ChannelSftp)mChannel);
			
			mChannel.connect();
			
			Logger.LogDebug("Channel open! Ready for action!");
			
			isConnected = true;
		//} catch (JSchException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		//}
	}

	@Override
	public InputStream getInputStream() throws IOException {
		if(in != null)
			return in;

		try {
			connect();
			
			out = mChannel.getOutputStream();
			in = mChannel.getInputStream();
			
			byte[] buf = new byte[1024];
	
			// send '\0'
			buf[0]=0; out.write(buf, 0, 1); out.flush();
		      
			while(true) {
				int c = checkAck(in);
				if(c != 'C') break;
			}
			
			// read '0644 '
			in.read(buf, 0, 5);
			
			filesize = 0l;
			while(true)
			{
				if(in.read(buf,0,1) < 0) break;
				if(buf[0] == ' ') break;
				filesize = filesize * 10l + (long)(buf[0]-'0');
			}
			
			// send '\0'
			buf[0]=0; out.write(buf, 0, 1); out.flush();
			
		} catch (JSchException e) {
			throw new IOException("JSchException while trying to get SCP file (" + mRemotePath + ")");
		}
		return in;
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		if(out != null)
			return out;

		try {
			connect();
			
			out = mChannel.getOutputStream();
			in = mChannel.getInputStream();
			
			if(checkAck(in) != 0) throw new IOException("No ack on getOutputStream");
			
		} catch (JSchException e) {
			throw new IOException("JSchException while trying to get SCP file (" + mRemotePath + ")");
		}
		return out;
	}
}
