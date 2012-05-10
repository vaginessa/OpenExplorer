/*
    Open Explorer, an open source file explorer & text editor
    Copyright (C) 2011, 2012 Brandon Bowles <brandroid64@gmail.com>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.brandroid.openmanager.util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.Thread.State;
import java.util.HashSet;
import org.brandroid.openmanager.util.ShellSession.UpdateCallback;
import org.brandroid.utils.ByteQueue;
import org.brandroid.utils.Logger;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class RootManager implements UpdateCallback
{
	private static boolean rootRequested = false;
	private static boolean rootEnabled = false;
	private static final boolean singleProcess = true; 
	private Process myProcess = null;
	private UpdateCallback mNotify = null;
	private DataInputStream is;
	private DataOutputStream os;
	private Thread mWatcherThread;
	private Thread mPollingThread;
	private ByteQueue mByteQueue;
    private byte[] mReceiveBuffer;

    private static final int NEW_INPUT = 1;
    private static final int PROCESS_EXITED = 2;
    
	public static RootManager Default = new RootManager();

    private boolean mIsRunning = false;
    private Handler mMsgHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (!mIsRunning) {
                return;
            }
            if (msg.what == NEW_INPUT) {
                readFromProcess();
            } else if (msg.what == PROCESS_EXITED) {
            	onExit();
            }
        }
    };
    
	@Override
	protected void finalize() throws Throwable {
		exitRoot();
		try {
			if(os != null)
				os.close();
		} catch(Exception e) { }
		try {
			if(is != null)
				is.close();
		} catch(Exception e) { }
		super.finalize();
	}

	@Override
	public void onUpdate() {
		if(mNotify != null)
			mNotify.onUpdate();
	}

	@Override
	public void onReceiveMessage(String msg) {
		if(msg.indexOf("\n") > -1)
			for(String s : msg.replace("\r", "").split("\n"))
				onReceiveMessage(s);
		if(mNotify != null)
			mNotify.onReceiveMessage(msg);
	}
	
	public RootManager setUpdateCallback(UpdateCallback notify)
	{
		mNotify = notify;
		return this;
	}
	
	public RootManager()
	{
        mWatcherThread = new Thread() {
            @Override
            public void run() {
				Logger.LogInfo("waiting for: " + myProcess);
				int result = -1;
				try {
					while(myProcess == null) { }
					result = myProcess.waitFor();
				} catch (InterruptedException e) { }
				Logger.LogInfo("Subprocess exited: " + result);
				mMsgHandler.sendMessage(mMsgHandler.obtainMessage(PROCESS_EXITED, result));
            }
       };

       mReceiveBuffer = new byte[4 * 1024];
       mByteQueue = new ByteQueue(4 * 1024);

       mPollingThread = new Thread() {
           private byte[] mBuffer = new byte[4096];

           @Override
           public void run() {
               try {
                   while(true) {
                       int read = is.read(mBuffer);
                       if (read == -1) {
                           // EOF -- process exited
                    	   onExit();
                           return;
                       }
                       onReceiveMessage(new String(mBuffer));
                   }
               } catch (IOException e) {
               }
           }
       };
	}

    /**
     * Look for new input from the ptty, send it to the terminal emulator.
     */
    private void readFromProcess() {
        int bytesAvailable = mByteQueue.getBytesAvailable();
        int bytesToRead = Math.min(bytesAvailable, mReceiveBuffer.length);
        try {
            int bytesRead = mByteQueue.read(mReceiveBuffer, 0, bytesToRead);
            onReceiveMessage(new String(mReceiveBuffer));
            //mEmulator.append(mReceiveBuffer, 0, bytesRead);
        } catch (InterruptedException e) {
        }

        onUpdate();
    }
    
	public Process getSuProcess() throws IOException
	{
		if(rootRequested && !rootEnabled) return null;
		if(myProcess == null || !singleProcess)
		{
			myProcess = new ProcessBuilder()
							.command("su", "-c sh")
							.redirectErrorStream(true)
							.start();
			is = new DataInputStream(myProcess.getInputStream());
			os = new DataOutputStream(myProcess.getOutputStream());
			//myProcess = Runtime.getRuntime().exec("su -c sh");
		}
		return myProcess;
	}
	public boolean isRoot()
	{
		return rootEnabled;
	}
	public boolean isRootRequested() { return rootRequested; }
	
	@SuppressWarnings("unused")
	public void exitRoot()
	{
		rootEnabled = false;
		rootRequested = false;
		if(myProcess != null) // exit su
		{
			try {
				if(os == null)
					os = new DataOutputStream(myProcess.getOutputStream());
				os.writeBytes("exit\n");
				os.flush();
				os.close();
				myProcess.destroy();
			} catch(Exception e) { }
		}
	}
	public void write(String cmd)
	{
		try {
			getSuProcess();
		
			if(mPollingThread.getState() == State.NEW)
				mPollingThread.start();
			if(mWatcherThread.getState() == State.NEW)
				mWatcherThread.start();
		
			if(os == null)
				os = new DataOutputStream(myProcess.getOutputStream());

			Logger.LogDebug("Writing to process: " + cmd);
			os.writeBytes(cmd + "\n");
			os.flush();
		} catch (IOException e) {
			Logger.LogError("Error writing to Root output stream.", e);
		}
	}
	public HashSet<String> execute(String cmd)
	{
		if(!isRoot()) {
			Logger.LogWarning("Root execute without request?");
			return null;
		}
		HashSet<String> ret = new HashSet<String>();
		try {
			Process suProcess = getSuProcess();

			if(os == null)
				os = new DataOutputStream(suProcess.getOutputStream());
			if(is == null)
				is = new DataInputStream(suProcess.getInputStream());
			
			if(mPollingThread.getState() == State.NEW)
				mPollingThread.start();
			if(mWatcherThread.getState() == State.NEW)
				mWatcherThread.start();
			
			if(null != os && null != is)
			{
				//Logger.LogDebug("Writing " + commands.length + " commands.");
				os.writeBytes(cmd + "\n");
				os.flush();
				
				// app crash if this doesn't happen
				//os.writeBytes("exit\n");
				//os.flush();
				
				//int retVal = suProcess.waitFor();
				//Logger.LogDebug("Root return value: " + retVal);
				
				String line = null;
				while((line = is.readLine()) != null)
				{
					ret.add(line);
					onReceiveMessage(line);
					Logger.LogDebug("Root return line: " + line);
					if(ret.size() > 500) break;
				}
				
				//Logger.LogDebug("Root done reading");
			} else {
				Logger.LogWarning("One of the streams was null.");
				return null;
			}
		} catch(Exception e) {
			Logger.LogError("Error executing commands [" + cmd + "]", e);
		} finally {
		}
		return ret;
	}
	@SuppressWarnings("deprecation")
	public boolean requestRoot()
	{
		if(rootRequested) return rootEnabled;
		try
		{
			Process suProcess = myProcess != null ? myProcess :
				Runtime.getRuntime().exec("su");
			
			if(mPollingThread.getState() == State.RUNNABLE)
				mPollingThread.start();
			if(mWatcherThread.getState() == State.RUNNABLE)
				mWatcherThread.start();
			
			if(os == null)
				os = new DataOutputStream(suProcess.getOutputStream());
			if(is == null)
				is = new DataInputStream(suProcess.getInputStream());
			
			if (null != os && null != is)
			{
				// Getting the id of the current user to check if this is root
				os.writeBytes("id\n");
				os.flush();

				String currUid = is.readLine();
				boolean exitSu = false;
				if (null == currUid)
				{
					rootEnabled = false;
					Logger.LogDebug("Can't get root access or denied by user");
				}
				else if (true == currUid.contains("uid=0"))
				{
					rootEnabled = true;
					exitSu = true;
					Logger.LogDebug("Root access granted");
				}
				else
				{
					rootEnabled = false;
					exitSu = true;
					Logger.LogDebug("Root access rejected: " + currUid);
				}

				if(!rootEnabled)
				{
					mPollingThread.stop();
					mWatcherThread.stop();
				}
				if (exitSu)
				{
					//os.writeBytes("exit\n");
					//os.flush();
				}
			}
		}
		catch (Exception e)
		{
			// Can't get root !
			// Probably broken pipe exception on trying to write to output stream (os) after su failed, meaning that the device is not rooted
			
			rootEnabled = false;
			Logger.LogError("Root access rejected [" + e.getClass().getName() + "]", e);
		}
		
		rootRequested = true;

		return rootEnabled;
	}
	
	public static boolean tryExecute(String... commands)
	{
		boolean retval = false;
		
		try
		{
			//ArrayList<String> commands = getCommandsToExecute();
			if (null != commands && commands.length > 0)
			{
				Process suProcess = Runtime.getRuntime().exec("su");

				DataOutputStream os = new DataOutputStream(suProcess.getOutputStream());

				// Execute commands that require root access
				for (String currCommand : commands)
				{
					os.writeBytes(currCommand + "\n");
					os.flush();
				}

				//os.writeBytes("exit\n");
				//os.flush();

				try
				{
					int suProcessRetval = suProcess.waitFor();
					if (255 != suProcessRetval)
					{
						// Root access granted
						retval = true;
					}
					else
					{
						// Root access denied
						retval = false;
					}
				}
				catch (Exception ex)
				{
					Log.e("OpenManager","Error executing root action", ex);
				}
			}
		}
		catch (IOException ex)
		{
			Log.w("OpenManager", "Can't get root access", ex);
		}
		catch (SecurityException ex)
		{
			Log.w("OpenManager", "Can't get root access", ex);
		}
		catch (Exception ex)
		{
			Log.w("OpenManager", "Error executing internal operation", ex);
		}
		
		return retval;
	}

	@Override
	public void onExit() {
		if(mNotify != null)
			mNotify.onExit();
	}
}