package org.brandroid.openmanager.data;

import java.io.IOException;
import java.io.InputStream;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.UserInfo;

public abstract class OpenNetworkPath extends OpenPath
{
	protected UserInfo mUserInfo;
	private int mServersIndex = -1;
	
	@Override
	public Boolean requiresThread() {
		return true;
	}
	
	public abstract void connect() throws JSchException;
	public abstract void disconnect();
	
	public UserInfo getUserInfo() { return mUserInfo; }
	public UserInfo setUserInfo(UserInfo info) { mUserInfo = info; return mUserInfo; }
	

	
	  static int checkAck(InputStream in) throws IOException{
		    int b=in.read();
		    // b may be 0 for success,
	    //          1 for error,
	    //          2 for fatal error,
	    //          -1
	    if(b==0) return b;
	    if(b==-1) return b;

	    if(b==1 || b==2){
	      StringBuffer sb=new StringBuffer();
	      int c;
	      do {
		c=in.read();
		sb.append((char)c);
	      }
	      while(c!='\n');
	      if(b==1){ // error
		System.out.print(sb.toString());
	      }
	      if(b==2){ // fatal error
		System.out.print(sb.toString());
	      }
	    }
	    return b;
	  }

	public int getServersIndex() {
		return mServersIndex;
	}
	public void setServersIndex(int index) { mServersIndex = index; }
}
