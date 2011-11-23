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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Stack;
import java.io.File;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.net.ftp.FTPFile;
import org.brandroid.openmanager.data.OpenComparer;
import org.brandroid.openmanager.data.OpenFTP;
import org.brandroid.openmanager.data.OpenPath;
import org.brandroid.openmanager.data.OpenFile;
import org.brandroid.openmanager.data.OpenStack;
import org.brandroid.openmanager.ftp.FTPManager;
import org.brandroid.openmanager.ftp.FTPFileComparer;
import org.brandroid.utils.Logger;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

/**
 * This class is completely modular, which is to say that it has
 * no reference to the any GUI activity. This class could be taken
 * and placed into in other java (not just Android) project and work.
 * <br>
 * <br>
 * This class handles all file and folder operations on the system.
 * This class dictates how files and folders are copied/pasted, (un)zipped
 * renamed and searched. The EventHandler class will generally call these
 * methods and have them performed in a background thread. Threading is not
 * done in this class.  
 * 
 * @author Joe Berria
 *
 */
public class FileManager {
	public static final int BUFFER = 		2048;
	
	private boolean mShowHiddenFiles = false;
	private SortType mSorting = SortType.ALPHA;
	private long mDirSize = 0;
	private ArrayList<OpenPath> mDirContent;
	private OpenStack mPathStack;
	private static Hashtable<String, OpenPath> mOpenCache = new Hashtable<String, OpenPath>();
	
	public static enum SortType {
		NONE,
		ALPHA,
		TYPE,
		SIZE,
		SIZE_DESC,
		DATE,
		DATE_DESC,
		ALPHA_DESC
	}
	
	/**
	 * Constructs an object of the class
	 * <br>
	 * this class uses a stack to handle the navigation of directories.
	 */
	public FileManager() {
		mDirContent = new ArrayList<OpenPath>();
		mPathStack = new OpenStack();
	}
	
	public OpenStack getStack() { return mPathStack; }
	
	public OpenPath peekStack() {
		return mPathStack.peek();
	}
	public void clearStack() {
		mPathStack.clear();
	}
	public OpenPath popStack() {
		return mPathStack.pop();
	}
	public OpenPath pushStack(OpenPath file) {
		return mPathStack.push(file);
	}
	public OpenPath setHomeDir(OpenPath home)
	{
		mPathStack.clear();
		return pushStack(home);
	}

	
	/**
	 * This will determine if hidden files and folders will be visible to the
	 * user.
	 * @param choice	true if user is veiwing hidden files, false otherwise
	 */
	public void setShowHiddenFiles(boolean choice) {
		Logger.LogInfo("Show hidden = " + choice);
		mShowHiddenFiles = choice;
	}
	
	public boolean getShowHiddenFiles() { return mShowHiddenFiles; }
	
	
	public void setSorting(SortType type)
	{
		mSorting = type;
	}
	
	public SortType getSorting() { return mSorting; }
	
	
	/**
	 * 
	 * @param old		the file to be copied
	 * @param newDir	the directory to move the file to
	 * @return
	 */
	public int copyToDirectory(String old, String newDir) {
		File old_file = new File(old);
		File temp_dir = new File(newDir);
		byte[] data = new byte[BUFFER];
		int read = 0;
		
		if(old_file.isFile() && temp_dir.isDirectory() && temp_dir.canWrite()){
			String file_name = old.substring(old.lastIndexOf("/"), old.length());
			File cp_file = new File(newDir + file_name);

			try {
				BufferedOutputStream o_stream = new BufferedOutputStream(
												new FileOutputStream(cp_file));
				BufferedInputStream i_stream = new BufferedInputStream(
											   new FileInputStream(old_file));
				
				while((read = i_stream.read(data, 0, BUFFER)) != -1)
					o_stream.write(data, 0, read);
				
				o_stream.flush();
				i_stream.close();
				o_stream.close();
				
			} catch (FileNotFoundException e) {
				Log.e("FileNotFoundException", e.getMessage());
				return -1;
				
			} catch (IOException e) {
				Log.e("IOException", e.getMessage());
				return -1;
			}
			
		}else if(old_file.isDirectory() && temp_dir.isDirectory() && temp_dir.canWrite()) {
			String files[] = old_file.list();
			String dir = newDir + old.substring(old.lastIndexOf("/"), old.length());
			int len = files.length;
			
			if(!new File(dir).mkdir())
				return -1;
			
			for(int i = 0; i < len; i++)
				copyToDirectory(old + "/" + files[i], dir);
			
		} else if(!temp_dir.canWrite())
			return -1;
		
		return 0;
	}
	
	/**
	 * 
	 * @param zipName
	 * @param toDir
	 * @param fromDir
	 */
	public void extractZipFilesFromDir(OpenPath zip, OpenPath directory) {
		if(!directory.mkdir() && directory.isDirectory()) return;
		extractZipFiles(zip, directory);
	}
	
	/**
	 * 
	 * @param zip_file
	 * @param directory
	 */
	public void extractZipFiles(OpenPath zip, OpenPath directory) {
		byte[] data = new byte[BUFFER];
		ZipEntry entry;
		ZipInputStream zipstream;
		
		directory.mkdir();
		
		try {
			zipstream = new ZipInputStream(zip.getInputStream());
			
			while((entry = zipstream.getNextEntry()) != null) {
				OpenPath newFile = directory.getChild(entry.getName());
				if(!newFile.mkdir())
					continue;
				
				int read = 0;
				FileOutputStream out = null;
				try {
					out = (FileOutputStream)newFile.getOutputStream();
					while((read = zipstream.read(data, 0, BUFFER)) != -1)
						out.write(data, 0, read);
				} catch(Exception e) { Logger.LogError("Error unzipping " + zip.getAbsolutePath(), e); }
				finally {
					zipstream.closeEntry();
					if(out != null)
						out.close();
				}
			}

		} catch (FileNotFoundException e) {
			Logger.LogError("Couldn't find file.", e);
			
		} catch (IOException e) {
			Logger.LogError("Couldn't extract zip.", e);
		}
	}
	
	/**
	 * 
	 * @param path
	 */
	public void createZipFile(OpenPath zip, OpenPath[] files) {
		ZipOutputStream zout = null;
		try {
			zout = new ZipOutputStream(
									  new BufferedOutputStream(
									  new FileOutputStream(((OpenFile)zip).getFile()), BUFFER));
			
			for(OpenPath file : files)
			{
				try {
					zipIt(file, zout);
				} catch(IOException e) {
					Logger.LogError("Error zipping file.", e);
				}
			}

			zout.close();
			
		} catch (FileNotFoundException e) {
			Log.e("File not found", e.getMessage());

		} catch (IOException e) {
			Log.e("IOException", e.getMessage());
		} finally {
			if(zout != null)
				try {
					zout.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
	}
	private void zipIt(OpenPath file, ZipOutputStream zout) throws IOException
	{
		byte[] data = new byte[BUFFER];
		int read;
		
		if(file.isFile()){
			ZipEntry entry = new ZipEntry(file.getName());
			zout.putNextEntry(entry);
			BufferedInputStream instream = new BufferedInputStream(file.getInputStream());
			Log.e("File Manager", "zip_folder file name = " + entry.getName());
			while((read = instream.read(data, 0, BUFFER)) != -1)
				zout.write(data, 0, read);
			
			zout.closeEntry();
			instream.close();
		
		} else if (file.isDirectory()) {
			Log.e("File Manager", "zip_folder dir name = " + file.getPath());
			for(OpenPath kid : file.list())
				zipIt(kid, zout);
		}
	}
	
	/**
	 * 
	 * @param filePath
	 * @param newName
	 * @return
	 */
	public int renameTarget(String filePath, String newName) {
		File src = new File(filePath);
		String ext = "";
		File dest;
		
		if(src.isFile())
			/*get file extension*/
			ext = filePath.substring(filePath.lastIndexOf("."), filePath.length());
		
		if(newName.length() < 1)
			return -1;
	
		String temp = filePath.substring(0, filePath.lastIndexOf("/"));
		
		dest = new File(temp + "/" + newName + ext);
		if(src.renameTo(dest))
			return 0;
		else
			return -1;
	}
	
	/**
	 * 
	 * @param path
	 * @param name
	 * @return
	 */
	public int createDir(String path, String name) {
		int len = path.length();
		
		if(len < 1 || len < 1)
			return -1;
		
		if(path.charAt(len - 1) != '/')
			path += "/";
		
		if (new File(path+name).mkdir())
			return 0;
		
		return -1;
	}
	
	/**
	 * The full path name of the file to delete.
	 * 
	 * @param path name
	 * @return
	 */
	public int deleteTarget(OpenPath target) {
		
		if(target.exists() && target.isFile() && target.canWrite()) {
			target.delete();
			return 0;
		}
		
		else if(target.exists() && target.isDirectory() && target.canRead()) {
			OpenPath[] file_list = null;
			file_list = target.list();
			
			if(file_list != null && file_list.length == 0) {
				target.delete();
				return 0;
				
			} else if(file_list != null && file_list.length > 0) {
				
				for(int i = 0; i < file_list.length; i++)
				{
					OpenPath f = file_list[i];
					if(f.isDirectory())
						deleteTarget(f);
					else if(f.isFile())
						f.delete();
				}
			}
			if(target.exists())
				if(target.delete())
					return 0;
		}	
		return -1;
	}
		
	/**
	 * converts integer from wifi manager to an IP address. 
	 * 
	 * @param des
	 * @return
	 */
	public static String integerToIPAddress(int ip) {
		String ascii_address = "";
		int[] num = new int[4];
		
		num[0] = (ip & 0xff000000) >> 24;
		num[1] = (ip & 0x00ff0000) >> 16;
		num[2] = (ip & 0x0000ff00) >> 8;
		num[3] = ip & 0x000000ff;
		 
		ascii_address = num[0] + "." + num[1] + "." + num[2] + "." + num[3];
		 
		return ascii_address;
	 }
	
	public static OpenPath getOpenCache(String path) { return getOpenCache(path, false); }
	
	public static OpenPath getOpenCache(String path, Boolean bGetNetworkedFiles)
	{
		//Logger.LogDebug("Checking cache for " + path);
		OpenPath ret = mOpenCache.get(path);
		if(ret == null)
		{
			if(path.indexOf("ftp:/") > -1)
			{
				FTPManager man;
				try {
					man = new FTPManager(path);
					ret = new OpenFTP(null, man);
					if(bGetNetworkedFiles)
					{
						FTPFile[] ff = FTPManager.getFTPFiles(path);
						ret = new OpenFTP(path, ff, man);
					}
				} catch (MalformedURLException e) {
					Logger.LogWarning("Bad URL in File Manager - " + path, e);
				}
			}
		}
		if(ret == null)
			ret = setOpenCache(path, new OpenFile(path));
		return ret;
	}
	
	public static OpenPath setOpenCache(String path, OpenPath file)
	{
		mOpenCache.put(path, file);
		return file;
	}
		
	public OpenPath[] getChildren(OpenPath directory)
	{
		//mDirContent.clear();
		if(directory == null) return new OpenPath[0];
		return directory.list();
	}
}

