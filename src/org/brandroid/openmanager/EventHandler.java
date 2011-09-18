/*
    Open Manager For Tablets, an open source file manager for the Android system
    Copyright (C) 2011  Joe Berria <nexesdevelopment@gmail.com>

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

package org.brandroid.openmanager;

import android.os.AsyncTask;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.util.Log;
import android.view.View;
import android.view.LayoutInflater;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Context;
import android.widget.Toast;
import android.widget.EditText;
import android.widget.TextView;
import android.net.Uri;

import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.brandroid.utils.Logger;


public class EventHandler {
	public static final int SEARCH_TYPE =		0x00;
	public static final int COPY_TYPE =			0x01;
	public static final int UNZIP_TYPE =		0x02;
	public static final int UNZIPTO_TYPE =		0x03;
	public static final int ZIP_TYPE =			0x04;
	public static final int DELETE_TYPE = 		0x05;
	public static final int RENAME_TYPE =		0X06;
	public static final int MKDIR_TYPE = 		0x07;
	
	private OnWorkerThreadFinishedListener mThreadListener;
	private FileManager mFileMang;
	private Context mContext;
	private boolean mDeleteFile = false;
	
	public interface OnWorkerThreadFinishedListener {
		/**
		 * This callback is called everytime our background thread
		 * completes its work.
		 * 
		 * @param type specifying what work it did (e.g SEARCH, DELETE ...)
		 * 			   you may pass null if you do not want to report the results.
		 * @param results the results of the work
		 */
		public void onWorkerThreadComplete(int type, ArrayList<String> results);
	}
	
	
	public EventHandler(Context context, FileManager filemanager) {
		mFileMang = filemanager;
		mContext = context;
	}
	
	public void setOnWorkerThreadFinishedListener(OnWorkerThreadFinishedListener e) {
		mThreadListener = e;
	}
	
	public void deleteFile(final ArrayList<String> path) {
		final String[] files;
		String name;
		
		if(path.size() == 1)
			name = path.get(0).substring(path.get(0).lastIndexOf("/") + 1,
										 path.get(0).length());
		else
			name = path.size() + " files";
		
		files =  buildStringArray(path);
		
		AlertDialog.Builder b = new AlertDialog.Builder(mContext);
		b.setTitle("Deleting " + name)
		 .setMessage("Deleting " + name + " cannot be undone.\nAre you sure" +
					 " you want to continue?")
		 .setIcon(R.drawable.download)
		 .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
			
			public void onClick(DialogInterface dialog, int which) {
				
				new BackgroundWork(DELETE_TYPE).execute(files);
			}
		})
		.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
			
		}).create().show();
	}
	
	public void renameFile(final String path, boolean isFolder) {
		LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.input_dialog_layout, null);
		String name = path.substring(path.lastIndexOf("/") + 1, path.length());
		
		final EditText text = (EditText)view.findViewById(R.id.dialog_input);	
		TextView msg = (TextView)view.findViewById(R.id.dialog_message);
		msg.setText("Please type the new name you want to call this file.");
		
		if(!isFolder) {
			TextView type = (TextView)view.findViewById(R.id.dialog_ext);
			type.setVisibility(View.VISIBLE);
			type.setText(path.substring(path.lastIndexOf("."), path.length()));
		}
		
		new AlertDialog.Builder(mContext)
		.setPositiveButton("Rename", new DialogInterface.OnClickListener() {
			
			public void onClick(DialogInterface dialog, int which) {
				String name = text.getText().toString();
				
				if(name.length() > 0) {
					mFileMang.renameTarget(path, name);
					mThreadListener.onWorkerThreadComplete(RENAME_TYPE, null);
				} else {
					dialog.dismiss();
				}
			}
		})
		.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		})
		.setView(view)
		.setTitle("Rename " + name)
		.setCancelable(false)
		.setIcon(R.drawable.download).create().show();
	}

	/**
	 * 
	 * @param directory directory path to create the new folder in.
	 */
	public void createNewFolder(final String directory) {
		LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.input_dialog_layout, null);
		
		final EditText text = (EditText)view.findViewById(R.id.dialog_input);
		TextView msg = (TextView)view.findViewById(R.id.dialog_message);
		
		msg.setText("Type the name of the folder you would like to create.");
		
		new AlertDialog.Builder(mContext)
		.setPositiveButton("Create", new DialogInterface.OnClickListener() {
			
			public void onClick(DialogInterface dialog, int which) {
				String name = text.getText().toString();
				
				if(name.length() > 0) {
					mFileMang.createDir(directory, name);
					mThreadListener.onWorkerThreadComplete(MKDIR_TYPE, null);
				} else {
					dialog.dismiss();
				}
			}
		})
		.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		})
		.setView(view)
		.setTitle("Create a new Folder")
		.setCancelable(false)
		.setIcon(R.drawable.folder).create().show();
	}
	
	public void sendFile(final ArrayList<String> path) {
		String name;
		CharSequence[] list = {"Bluetooth", "Email"};
		final String[] files = buildStringArray(path);
		final int num = path.size();
		
		if(num == 1)
			name = path.get(0).substring(path.get(0).lastIndexOf("/") + 1,
										 path.get(0).length());
		else
			name = path.size() + " files.";
				
		AlertDialog.Builder b = new AlertDialog.Builder(mContext);
		b.setTitle("Sending " + name)
		 .setIcon(R.drawable.download)
		 .setItems(list, new DialogInterface.OnClickListener() {
			
			
			public void onClick(DialogInterface dialog, int which) {
				switch(which) {
					case 0:
						Intent bt = new Intent(mContext, BluetoothActivity.class);
						
						bt.putExtra("paths", files);
						mContext.startActivity(bt);
						break;
						
					case 1:
						ArrayList<Uri> uris = new ArrayList<Uri>();
						Intent mail = new Intent();
						mail.setType("application/mail");
						
						if(num == 1) {
							mail.setAction(android.content.Intent.ACTION_SEND);
							mail.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(files[0])));
							mContext.startActivity(mail);
							break;
						}
						
						for(int i = 0; i < num; i++)
							uris.add(Uri.fromFile(new File(files[i])));
						
						mail.setAction(android.content.Intent.ACTION_SEND_MULTIPLE);
						mail.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
						mContext.startActivity(mail);
						break;
				}
			}
		}).create().show();
	}
	
	public void copyFile(ArrayList<String> files, String newPath) {
		int len = files.size() + 1;
		String[] array = new String[len];
		
		array[0] = newPath;		//a convenient way to pass the dest dir to our background thread
		for(int i = 1; i < len; i++)
			array[i] = files.get(i - 1);
		
		new BackgroundWork(COPY_TYPE).execute(array);
	}
	
	public void cutFile(ArrayList<String> files, String newPath) {
		mDeleteFile = true;
		
		copyFile(files, newPath);
	}
	
	public void searchFile(String dir, String query) {
		new BackgroundWork(SEARCH_TYPE).execute(dir, query);
	}
	
	public void zipFile(String path) {
		new BackgroundWork(ZIP_TYPE).execute(path);
	}
	
	public void unzipFile(String path) {
		final String oPath = path;
		final String zipFile = path.substring(path.lastIndexOf("/") + 1, path.length());
		final String zipPath = path.substring(0, path.lastIndexOf(zipFile));
		
		AlertDialog.Builder b = new AlertDialog.Builder(mContext);
		b.setTitle("Unzip file " + zipFile)
		 .setMessage("Would you like to unzip " + zipFile +
				 	 " here or some other folder?")
		 .setIcon(R.drawable.zip)
		 .setPositiveButton("Unzip here", new DialogInterface.OnClickListener() {
			
			
			public void onClick(DialogInterface dialog, int which) {
				new BackgroundWork(UNZIP_TYPE).execute(zipFile, zipPath);
			}
		})
		 .setNegativeButton("Unzip else where", new DialogInterface.OnClickListener() {
			
			
			public void onClick(DialogInterface dialog, int which) {
				ArrayList<String> l = new ArrayList<String>();
				l.add(oPath);
				mThreadListener.onWorkerThreadComplete(UNZIPTO_TYPE, l);
			}
			
		}).create().show();
	}
	
	public void unZipFileTo(String zipFile, String toDir) {
		String name = zipFile.substring(zipFile.lastIndexOf("/") + 1, 
										zipFile.length());
		String orgDir = zipFile.substring(0, zipFile.lastIndexOf("/"));
		
		new BackgroundWork(UNZIPTO_TYPE).execute(name, toDir, orgDir);
	}
	
	private String[] buildStringArray(ArrayList<String> array) {
		int len = array.size();
		String[] a = new String[len];
				
		for(int i = 0; i < len; i++)
			a[i] = array.get(i);
		
		return a;
	}
		
	
	/**
	 * Do work on second thread class
	 * @author Joe Berria
	 */
	private class BackgroundWork extends AsyncTask<String, Integer, ArrayList<String>> {
		private int mType;
		private ProgressDialog mPDialog;
		
		public BackgroundWork(int type) {
			mType = type;
		}
		
		
		protected void onPreExecute() {
			switch(mType) {
			case DELETE_TYPE:
				mPDialog = ProgressDialog.show(mContext, "Deleting", 
											   "Please Wait...");
				break;
				
			case SEARCH_TYPE:
				mPDialog = ProgressDialog.show(mContext, "Searching", 
				   							   "Please Wait...");
				break;
				
			case COPY_TYPE:
				if(mDeleteFile)
					mPDialog = ProgressDialog.show(mContext, "Copying", 
					   							   "Please Wait...");
				else
					mPDialog = ProgressDialog.show(mContext, "Moving", 
					   							   "Please Wait...");
				mPDialog.setMax(0);
				break;
				
			case UNZIP_TYPE:
			case UNZIPTO_TYPE:
				mPDialog = ProgressDialog.show(mContext, "Unzipping", 
				   							   "Please Wait...");
				break;
								
			case ZIP_TYPE:
				mPDialog = ProgressDialog.show(mContext, "Zipping Folder", 
				   							   "Please Wait...");
				break;
			}
		}
		
		
		protected ArrayList<String> doInBackground(String... params) {
			ArrayList<String> results = null;
			int len = params.length;
			
			switch(mType) {
			
			case DELETE_TYPE:
				if(results == null)
					 results = new ArrayList<String>();
				
				for(int i = 0; i < len; i++) {
					int ret = mFileMang.deleteTarget(params[i]);
					results.add(ret + "");
				}
				
				return results;
				
			case SEARCH_TYPE:
				results = mFileMang.searchInDirectory(params[0], params[1]);
				
				return results;
				
			case COPY_TYPE:
				//the first index is our dest path.
				String dir = params[0];
				int ret = 0;
				
				if(results == null)
					 results = new ArrayList<String>();
				
				for(int i = 1; i < len; i++) {
					ret = copyToDirectory(params[i], dir, 0);
					results.add(ret + "");
					
					if(mDeleteFile) {
						mFileMang.deleteTarget(params[i]);
						results.add(ret + "");
					}
					
				}
					
				return results;
				
			case UNZIP_TYPE:
				String file = params[0];
				String folder = params[1];
				
				extractZipFiles(file, folder);
				return null;
				
			case UNZIPTO_TYPE:
				String name = params[0];
				String toDir = params[1];
				String fromDir = params[2];
				
				mFileMang.extractZipFilesFromDir(name, toDir, fromDir);
				return null;
				
			case ZIP_TYPE:				
				mFileMang.createZipFile(params[0]);
				return null;
			}
			
			return null;
		}
		
		private Integer copyToDirectory(String old, String newDir, int total)
		{
			File old_file = new File(old);
			File new_dir = new File(newDir);
			byte[] data = new byte[FileManager.BUFFER];
			int read = 0;
			
			if(old_file.isDirectory() && new_dir.isDirectory() && new_dir.canWrite()) {
				String files[] = old_file.list();
				String dir = newDir + old.substring(old.lastIndexOf("/"), old.length());
				
				if(!new File(dir).mkdir())
					return -1;
				
				for(String file : files)
					total += (int)file.length();
				
				for(int i = 0; i < files.length; i++)
					if(copyToDirectory(files[i], dir, total) == -1)
						return -1;
				
			} else if(old_file.isFile() && new_dir.isDirectory() && new_dir.canWrite()){
				String file_name = old.substring(old.lastIndexOf("/"), old.length());
				File cp_file = new File(newDir + file_name);
				int size = (int)old_file.length();
				int pos = 0;

				try {
					BufferedOutputStream o_stream = new BufferedOutputStream(
													new FileOutputStream(cp_file));
					BufferedInputStream i_stream = new BufferedInputStream(
												   new FileInputStream(old_file));
					
					while((read = i_stream.read(data, 0, FileManager.BUFFER)) != -1)
					{
						o_stream.write(data, 0, read);
						pos += FileManager.BUFFER;
						publishProgress(pos, size);
					}
					
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
				
			} else if(!new_dir.canWrite())
				return -1;
			
			return 0;
		}
		
		public void extractZipFiles(String zip_file, String directory) {
			byte[] data = new byte[FileManager.BUFFER];
			String name, path, zipDir;
			ZipEntry entry;
			ZipInputStream zipstream;
			
			if(zip_file.contains("/")) {
				path = zip_file;
				name = path.substring(path.lastIndexOf("/") + 1, 
									  path.length() - 4);
				zipDir = directory + name + "/";
				
			} else {
				path = directory + zip_file;
				name = path.substring(path.lastIndexOf("/") + 1, 
			 			  			  path.length() - 4);
				zipDir = directory + name + "/";
			}

			new File(zipDir).mkdir();
			
			try {
				zipstream = new ZipInputStream(new FileInputStream(path));
				
				while((entry = zipstream.getNextEntry()) != null) {
					String buildDir = zipDir;
					String[] dirs = entry.getName().split("/");
					
					if(dirs != null && dirs.length > 0) {
						for(int i = 0; i < dirs.length - 1; i++) {
							buildDir += dirs[i] + "/";
							new File(buildDir).mkdir();
						}
					}
					
					int read = 0;
					FileOutputStream out = new FileOutputStream(
											zipDir + entry.getName());
					while((read = zipstream.read(data, 0, FileManager.BUFFER)) != -1)
						out.write(data, 0, read);
					
					zipstream.closeEntry();
					out.close();
				}

			} catch (FileNotFoundException e) {
				e.printStackTrace();
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		@Override
		protected void onProgressUpdate(Integer... values) {
			int current = 0,
				size	= 0,
				total	= 0;
			if(values.length > 0)
				current = size = total = values[0];
			if(values.length > 1)
				size = total = values[1];
			if(values.length > 2)
				total = values[2];
			
			int progA = (int)(((float)current / (float)size) * 1000f);
			int progB = (int)(((float)current / (float)total) * 1000f);
			
			//Logger.LogInfo("onProgressUpdate(" + current + ", " + size + ", " + total + ")-(" + progA + "," + progB + ")");

			if(values.length == 0)
				mPDialog.setIndeterminate(true);
			else {
				mPDialog.setIndeterminate(false);
				mPDialog.setMax(1000);
				mPDialog.setProgress(progA);
				mPDialog.setSecondaryProgress(progB);
			}
		}

		
		protected void onPostExecute(ArrayList<String> result) {
			switch(mType) {
			
			case DELETE_TYPE:				
				mPDialog.dismiss();
				mThreadListener.onWorkerThreadComplete(mType, null);
				
				if(!result.contains("0"))
					Toast.makeText(mContext, "File(s) could not be deleted", Toast.LENGTH_SHORT).show();
				else if(result.contains("-1"))
					Toast.makeText(mContext, "Some file(s) were not deleted", Toast.LENGTH_SHORT).show();
				else
					Toast.makeText(mContext, "File(s) successfully deleted", Toast.LENGTH_SHORT).show();
					
				break;
				
			case SEARCH_TYPE:
				mPDialog.dismiss();
				mThreadListener.onWorkerThreadComplete(mType, result);
				break;
				
			case COPY_TYPE:
				mPDialog.dismiss();
				mThreadListener.onWorkerThreadComplete(mType, null);
				
				if(!mDeleteFile) {
					if(!result.contains("0"))
						Toast.makeText(mContext, "File(s) could not be copied", Toast.LENGTH_SHORT).show();
					else if(result.contains("-1"))
						Toast.makeText(mContext, "Some file(s) were not copied", Toast.LENGTH_SHORT).show();
					else
						Toast.makeText(mContext, "File(s) successfully copied", Toast.LENGTH_SHORT).show();
				} else {
					if(!result.contains("0"))
						Toast.makeText(mContext, "File(s) could not be moved", Toast.LENGTH_SHORT).show();
					else if(result.contains("-1"))
						Toast.makeText(mContext, "Some file(s) were not moved", Toast.LENGTH_SHORT).show();
					else
						Toast.makeText(mContext, "File(s) successfully moved", Toast.LENGTH_SHORT).show();
				}
				mDeleteFile = false;				
				break;
				
			case UNZIPTO_TYPE:
				mPDialog.dismiss();
				mThreadListener.onWorkerThreadComplete(mType, null);
				break;
				
			case UNZIP_TYPE:
				mPDialog.dismiss();
				mThreadListener.onWorkerThreadComplete(mType, null);
				break;
				
			case ZIP_TYPE:
				mPDialog.dismiss();
				mThreadListener.onWorkerThreadComplete(mType, null);
				break;
			}
		}
	}
}
