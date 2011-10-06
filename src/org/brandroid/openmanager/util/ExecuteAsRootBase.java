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
import android.util.Log;

public abstract class ExecuteAsRootBase
{
	public static boolean canRunRootCommands()
	{
		boolean retval = false;
		Process suProcess;
		
		try
		{
			suProcess = Runtime.getRuntime().exec("su");
			
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
					retval = false;
					exitSu = false;
					Log.d("ROOT", "Can't get root access or denied by user");
				}
				else if (true == currUid.contains("uid=0"))
				{
					retval = true;
					exitSu = true;
					Log.d("ROOT", "Root access granted");
				}
				else
				{
					retval = false;
					exitSu = true;
					Log.d("ROOT", "Root access rejected: " + currUid);
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
			
			retval = false;
			Log.d("ROOT", "Root access rejected [" + e.getClass().getName() + "] : " + e.getMessage());
		}

		return retval;
	}
	
	public final boolean execute()
	{
		return execute(getCommandsToExecute());
	}
	public static boolean execute(String... commands)
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
	
	protected abstract String[] getCommandsToExecute();
}