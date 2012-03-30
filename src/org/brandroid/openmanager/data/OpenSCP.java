package org.brandroid.openmanager.data;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.brandroid.utils.Logger;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

import android.net.Uri;

public class OpenSCP extends OpenNetworkPath
{
	private long filesize = 0l;
	private boolean isConnected = false;
	private Session mSession = null;
	private Channel mChannel = null;
	private InputStream in = null;
	private OutputStream out = null;
	private final String mHost, mUser, mRemotePath;
	private UserInfo mUserInfo = null;
	private OpenSCP[] mChildren = null;
	
	public OpenSCP(String host, String user, String path, UserInfo info)
	{
		mHost = host;
		mUser = user;
		mRemotePath = path;
		mUserInfo = info;
	}

	@Override
	public String getName() {
		if(mRemotePath.equals("") || mRemotePath.equals("/"))
			return super.getName(mHost);
		String ret = mRemotePath.substring(mRemotePath.lastIndexOf("/", mRemotePath.length() - 1) + 1);
		if(ret.equals(""))
			ret = mRemotePath;
		return ret;
	}

	@Override
	public String getPath() {
		return "scp://" + mUser + "@" + mHost + mRemotePath;
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
		return new OpenPath[0];
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
		connect("scp -f " + mRemotePath);
	}
	public void connect(String command) throws JSchException
	{
		disconnect();
		JSch jsch = new JSch();
		//try {
			mSession = jsch.getSession(mUser, mHost, 22);
			mSession.setUserInfo(mUserInfo);
			mSession.connect();
			
			//String command = "scp -f " + mRemotePath;
			mChannel = mSession.openChannel("exec");
			((ChannelExec)mChannel).setCommand(command);
			
			mChannel.connect();
			
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
			connect("scp -f " + mRemotePath);
			
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
			connect("scp -p -t " + mRemotePath);
			
			out = mChannel.getOutputStream();
			in = mChannel.getInputStream();
			
			if(checkAck(in) != 0) throw new IOException("No ack on getOutputStream");
			
		} catch (JSchException e) {
			throw new IOException("JSchException while trying to get SCP file (" + mRemotePath + ")");
		}
		return out;
	}
	static int checkAck(InputStream in) throws IOException
	{
		int b=in.read();
		if(b==0) return b;
		if(b==-1) return b;
		
		if(b==1 || b==2){
			StringBuffer sb=new StringBuffer();
			int c;
			do {c=in.read();sb.append((char)c);}while(c!='\n');
			if(b==1){System.out.print(sb.toString());}
			if(b==2){System.out.print(sb.toString());}
		}
		return b;
	}

	@Override
	public OpenNetworkPath[] getChildren() {
		return mChildren;
	}
}
