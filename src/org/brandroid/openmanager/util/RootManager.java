/*
    Open Explorer, an open source file explorer & text editor
    Copyright (C) 2011 Brandon Bowles <brandroid64@gmail.com>

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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.brandroid.utils.Logger;

import android.util.Log;

public class RootManager
{
	private static boolean rootRequested = false;
	private static boolean rootEnabled = false;
	
	public static RootManager Default = new RootManager();
	
	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		exitRoot();
	}
	
	public Process getSuProcess() throws IOException
	{
		if(!isRoot()) return null;
		return Runtime.getRuntime().exec("su -c sh");
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
		Process suProcess = null;
		if(suProcess != null) // exit su
		{
			try {
				DataOutputStream os = new DataOutputStream(suProcess.getOutputStream());
				os.writeBytes("exit\n");
				os.flush();
				os.close();
			} catch(Exception e) { }
		}
	}
	public HashSet<String> execute(String... commands)
	{
		if(!isRoot()) {
			Logger.LogWarning("Root execute without request?");
			return null;
		}
		HashSet<String> ret = new HashSet<String>();
		DataOutputStream os = null;
		DataInputStream is = null;
		try {
			Process suProcess = getSuProcess();

			os = new DataOutputStream(suProcess.getOutputStream());
			is = new DataInputStream(suProcess.getInputStream());
			
			if(null != os && null != is)
			{
				//Logger.LogDebug("Writing " + commands.length + " commands.");
				for (String cmd : commands)
				{
					os.writeBytes(cmd + "\n");
					os.flush();
				}
				
				os.writeBytes("exit\n");
				os.flush();
				
				int retVal = suProcess.waitFor();
				//Logger.LogDebug("Root return value: " + retVal);
				
				String line = null;
				while((line = is.readLine()) != null)
				{
					ret.add(line);
					//Logger.LogDebug("Root return line: " + line);
				}
				
				//Logger.LogDebug("Root done reading");
			} else {
				Logger.LogWarning("One of the streams was null.");
				return null;
			}
		} catch(Exception e) {
			Logger.LogError("Error executing commands [" + commands[0] + "]", e);
		} finally {
			if(os != null)
				try {
					os.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			if(is != null)
				try {
					is.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
		return ret;
	}
	public boolean requestRoot()
	{
		if(rootRequested) return rootEnabled;
		try
		{
			Process suProcess = Runtime.getRuntime().exec("su");
			
			DataOutputStream os = new DataOutputStream(suProcess.getOutputStream());
			DataInputStream osRes = new DataInputStream(suProcess.getInputStream());
			
			if (null != os && null != osRes)
			{
				// Getting the id of the current user to check if this is root
				os.writeBytes("id\n");
				os.flush();

				String currUid = osRes.readLine();
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

				if (exitSu)
				{
					os.writeBytes("exit\n");
					os.flush();
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
		
		rootRequested = false;

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

				os.writeBytes("exit\n");
				os.flush();

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
}