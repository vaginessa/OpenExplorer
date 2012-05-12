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
	private Thread mPollingThread;
	private ByteQueue mByteQueue;
    private byte[] mReceiveBuffer;
    private final static int BufferLength = 32 * 1024;

    private static final int NEW_INPUT = 1;
    
	public static final RootManager Default = new RootManager();

    private boolean mIsRunning = false;
    private Handler mMsgHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (!mIsRunning) {
            	Logger.LogWarning("Handler exited");
                return;
            }
            if (msg.what == NEW_INPUT) {
                readFromProcess();
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

       mReceiveBuffer = new byte[BufferLength];
       mByteQueue = new ByteQueue(BufferLength);

       mPollingThread = new Thread() {
           private byte[] mBuffer = new byte[BufferLength];

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
                       mByteQueue.write(mBuffer, 0, read);
                       mMsgHandler.sendMessage(mMsgHandler.obtainMessage(NEW_INPUT));
                   }
               } catch (IOException e) {
            	   Logger.LogError("Polling couldn't write", e);
               } catch (InterruptedException e) {
            	   Logger.LogError("Polling interrupted", e);
				}
           }
       };
	}

    /**
     * Look for new input from the ptty, send it to the terminal emulator.
     */
    private void readFromProcess() {
        int bytesAvailable = mByteQueue.getBytesAvailable();
        mReceiveBuffer = new byte[bytesAvailable];
        int bytesToRead = bytesAvailable; // Math.min(bytesAvailable, mReceiveBuffer.length);
        try {
            int bytesRead = mByteQueue.read(mReceiveBuffer, 0, bytesToRead);
            if(bytesRead == 0) return;
            if(mReceiveBuffer[0] == 0)
            	onUpdate();
            else
            	onReceiveMessage(new String(mReceiveBuffer));
            if(mReceiveBuffer[mReceiveBuffer.length - 1] == 0)
            	onUpdate();
            //mEmulator.append(mReceiveBuffer, 0, bytesRead);
        } catch (InterruptedException e) {
        	Logger.LogError("readFromProcess interrupted?", e);
        }
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
	private void stop()
	{
		if(!mIsRunning) return;
		mIsRunning = false;
		if(mPollingThread != null && mPollingThread.getState() == State.WAITING)
			mPollingThread.stop();
	}
	private void start()
	{
		if(mIsRunning) return;
		mIsRunning = true;
		mPollingThread.start();
	}
	public void write(String cmd)
	{
		try {
			getSuProcess();
		
			start();
		
			is.skip(is.available());
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
			
			start();
			
			if(null != os && null != is)
			{
				//Logger.LogDebug("Writing " + commands.length + " commands.");
				os.writeBytes(cmd + "\n");
				os.flush();
				
				// app crash if this doesn't happen
				//os.writeBytes("exit\n");
				//os.flush();
				
				int retVal = suProcess.waitFor();
				Logger.LogDebug("Root return value: " + retVal);
				
				String line = null;
				while((line = is.readLine()) != null)
				{
					ret.add(line);
					onReceiveMessage(line);
					Logger.LogDebug("Root return line: " + line);
					if(ret.size() > 500) break;
				}
				
				Logger.LogDebug("Root done reading");
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
			if(myProcess == null)
				myProcess = Runtime.getRuntime().exec("su");


			if(os == null)
				os = new DataOutputStream(myProcess.getOutputStream());
			if(is == null)
				is = new DataInputStream(myProcess.getInputStream());
			
			start();
			
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
		mIsRunning = false;
		if(mNotify != null)
			mNotify.onExit();
	}
}