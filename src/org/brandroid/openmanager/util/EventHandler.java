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

import android.os.AsyncTask;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.util.Log;
import android.view.View;
import android.view.LayoutInflater;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.Context;
import android.widget.ProgressBar;
import android.widget.RemoteViews;
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

import org.brandroid.openmanager.BluetoothActivity;
import org.brandroid.openmanager.OpenExplorer;
import org.brandroid.openmanager.R;
import org.brandroid.openmanager.R.drawable;
import org.brandroid.openmanager.R.id;
import org.brandroid.openmanager.R.layout;
import org.brandroid.openmanager.data.OpenPath;
import org.brandroid.openmanager.data.OpenFile;
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
	private boolean mDeleteFile = false;
	private ProgressBar mExtraProgress;
	private TextView mExtraStatus, mExtraPercent;
	
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
	
	public void setExtraViews(ProgressBar pb, TextView status, TextView percent)
	{
		mExtraProgress = pb;
		mExtraStatus = status;
		mExtraPercent = percent;
	}
	
	public EventHandler(FileManager filemanager) {
		mFileMang = filemanager;
	}
	
	public void setOnWorkerThreadFinishedListener(OnWorkerThreadFinishedListener e) {
		mThreadListener = e;
	}	

	public static CharSequence getResourceString(Context mContext, int... resIds)
	{
		String ret = "";
		for(int resId : resIds)
			ret += (ret == "" ? "" : " ") + mContext.getText(resId);
		return ret;
	}

	public void deleteFile(final ArrayList<OpenPath> path, final Context mContext) {
		final OpenPath[] files = new OpenPath[path.size()];
		path.toArray(files);
		String name;
		
		if(path.size() == 1)
			name = path.get(0).getName();
		else
			name = path.size() + " " + getResourceString(mContext, R.string.s_files);
		
		AlertDialog.Builder b = new AlertDialog.Builder(mContext);
		b.setTitle(getResourceString(mContext, R.string.s_menu_delete) + " " + name)
			.setMessage(((String)mContext.getText(R.string.s_alert_confirm_delete)).replace("xxx", name))
			.setIcon(R.drawable.download)
			.setPositiveButton(getResourceString(mContext, R.string.s_menu_delete), new OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					new BackgroundWork(DELETE_TYPE, mContext, null).execute(files);
				}})
			.setNegativeButton(getResourceString(mContext, R.string.s_cancel), new OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}})
			.create()
			.show();
	}

	public void startSearch(final OpenPath base, final Context mContext)
	{
		LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.input_dialog_layout, null);
		final EditText mQuery = (EditText)view.findViewById(R.id.dialog_input);
		TextView mSubtitle = ((TextView)view.findViewById(R.id.dialog_subtitle));
		mSubtitle.setText(base.getPath());
		mSubtitle.setVisibility(View.VISIBLE);
		view.findViewById(R.id.dialog_message).setVisibility(View.GONE);
		
		new AlertDialog.Builder(mContext)
			.setPositiveButton(getResourceString(mContext, R.string.s_search), new OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					searchFile(base, mQuery.getText().toString(), mContext);
				}
			})
			.setNegativeButton(getResourceString(mContext, R.string.s_cancel), new OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			})
			.setView(view)
			.setTitle(getResourceString(mContext, R.string.s_search))
			.setCancelable(true)
			.setIcon(R.drawable.search)
			.create().show();
	}
	
	public void renameFile(final String path, boolean isFolder, Context mContext) {
		LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.input_dialog_layout, null);
		String name = path.substring(path.lastIndexOf("/") + 1, path.length());
		
		final EditText text = (EditText)view.findViewById(R.id.dialog_input);	
		TextView msg = (TextView)view.findViewById(R.id.dialog_message);
		msg.setText(getResourceString(mContext, R.string.s_alert_rename));
		
		if(!isFolder) {
			TextView type = (TextView)view.findViewById(R.id.dialog_ext);
			type.setVisibility(View.VISIBLE);
			type.setText(path.substring(path.lastIndexOf("."), path.length()));
		}
		
		new AlertDialog.Builder(mContext)
			.setPositiveButton(getResourceString(mContext, R.string.s_menu_rename), new OnClickListener() {
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
			.setNegativeButton(getResourceString(mContext, R.string.s_cancel), new OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			})
			.setView(view)
			.setTitle(getResourceString(mContext, R.string.s_menu_rename) + " " + name)
			.setCancelable(false)
			.setIcon(R.drawable.download)
			.create()
			.show();
	}

	/**
	 * 
	 * @param directory directory path to create the new folder in.
	 */
	public void createNewFolder(final String directory, Context mContext) {
		LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.input_dialog_layout, null);
		
		final EditText text = (EditText)view.findViewById(R.id.dialog_input);
		TextView msg = (TextView)view.findViewById(R.id.dialog_message);
		
		msg.setText(getResourceString(mContext, R.string.s_alert_newfolder));
		
		new AlertDialog.Builder(mContext)
			.setPositiveButton(getResourceString(mContext, R.string.s_create), new OnClickListener() {
			
			public void onClick(DialogInterface dialog, int which) {
				String name = text.getText().toString();
				
				if(name.length() > 0) {
					if(mFileMang != null)
						mFileMang.createDir(directory, name);
					if(mThreadListener != null)
						mThreadListener.onWorkerThreadComplete(MKDIR_TYPE, null);
				} else {
					dialog.dismiss();
				}
			}
		})
		.setNegativeButton(getResourceString(mContext, R.string.s_cancel), new OnClickListener() {
			
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		})
		.setView(view)
		.setTitle(getResourceString(mContext, R.string.s_title_newfolder))
		.setCancelable(false)
		.setIcon(R.drawable.folder).create().show();
	}
	
	public void sendFile(final ArrayList<OpenPath> path, final Context mContext) {
		String name;
		CharSequence[] list = {"Bluetooth", "Email"};
		final OpenPath[] files = new OpenPath[path.size()];
		path.toArray(files);
		final int num = path.size();
		
		if(num == 1)
			name = path.get(0).getName();
		else
			name = path.size() + " " + getResourceString(mContext, R.string.s_files) + ".";
				
		AlertDialog.Builder b = new AlertDialog.Builder(mContext);
		b.setTitle(getResourceString(mContext, R.string.s_title_send).toString().replace("xxx", name))
		 .setIcon(R.drawable.download)
		 .setItems(list, new OnClickListener() {
			
			
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
							mail.putExtra(Intent.EXTRA_STREAM, files[0].getUri());
							mContext.startActivity(mail);
							break;
						}
						
						for(int i = 0; i < num; i++)
							uris.add(files[i].getUri());
						
						mail.setAction(android.content.Intent.ACTION_SEND_MULTIPLE);
						mail.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
						mContext.startActivity(mail);
						break;
				}
			}
		}).create().show();
	}
	
	public void copyFile(ArrayList<OpenPath> files, OpenPath newPath, Context mContext) {
		OpenPath[] array = new OpenPath[files.size()];
		files.toArray(array);
		
		new BackgroundWork(COPY_TYPE, mContext, newPath).execute(array);
	}
	
	public void cutFile(ArrayList<OpenPath> files, OpenPath newPath, Context mContext) {
		mDeleteFile = true;
		
		copyFile(files, newPath, mContext);
	}
	
	public void searchFile(OpenPath dir, String query, Context mContext) {
		new BackgroundWork(SEARCH_TYPE, mContext, dir, query).execute();
	}
	
	public void zipFile(OpenPath into, OpenPath[] files, Context mContext) {
		new BackgroundWork(ZIP_TYPE, mContext, into).execute(files);
	}
	
	public void unzipFile(final OpenPath into, final OpenPath file, final Context mContext) {
		AlertDialog.Builder b = new AlertDialog.Builder(mContext);
		b.setTitle(getResourceString(mContext, R.string.s_title_unzip).toString().replace("xxx", file.getName()))
			 .setMessage(getResourceString(mContext, R.string.s_alert_unzip).toString().replace("xxx", file.getName()))
			 .setIcon(R.drawable.zip)
			 .setPositiveButton(getResourceString(mContext, R.string.s_button_unzip_here), new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						new BackgroundWork(UNZIP_TYPE, mContext, into).execute(file);
					}
				})
			 .setNegativeButton(getResourceString(mContext, R.string.s_button_unzip_else), new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						ArrayList<String> l = new ArrayList<String>();
						l.add(file.getPath());
						mThreadListener.onWorkerThreadComplete(UNZIPTO_TYPE, l);
					}
			 	})
			.create()
			.show();
	}
	
	public void unZipFileTo(OpenPath zipFile, OpenPath toDir, Context mContext) {
		new BackgroundWork(UNZIPTO_TYPE, mContext, toDir).execute(zipFile);
	}
		
	
	/**
	 * Do work on second thread class
	 * @author Joe Berria
	 */
	private class BackgroundWork extends AsyncTask<OpenPath, Integer, Integer> {
		private int mType;
		private ProgressDialog mPDialog;
		private static final int BACKGROUND_NOTIFICATION_ID = 123;
		private Notification mNote;
		private Context mContext;
		private String[] mInitParams = null;
		private OpenPath mIntoPath;
		private ArrayList<OpenPath> mSearchResults = null;
		
		public BackgroundWork(int type, Context context, OpenPath intoPath, String... params) {
			mType = type;
			mContext = context;
			mInitParams = params;
			mIntoPath = intoPath;
		}
		
		protected void onPreExecute() {
			String title = getResourceString(mContext, R.string.s_title_executing).toString();
			switch(mType) {
			case DELETE_TYPE: title = getResourceString(mContext, R.string.s_title_deleting).toString(); break;
			case SEARCH_TYPE: title = getResourceString(mContext, R.string.s_title_searching).toString(); break;
			case COPY_TYPE:
				if(mDeleteFile)
					title = getResourceString(mContext, R.string.s_title_copying).toString(); 
				else
					title = getResourceString(mContext, R.string.s_title_moving).toString();
				break;
			case UNZIP_TYPE:
			case UNZIPTO_TYPE:
				title = getResourceString(mContext, R.string.s_title_unzipping).toString(); 
				break;
			case ZIP_TYPE:
				title = getResourceString(mContext, R.string.s_title_zipping).toString(); 
				break;
			}
			try {
				mPDialog = ProgressDialog.show(mContext, title, getResourceString(mContext, R.string.s_title_wait).toString());
			} catch(Exception e) { }
			try {
				RemoteViews noteView = new RemoteViews(mContext.getPackageName(), R.layout.title_bar);
				noteView.setImageViewResource(R.id.title_icon, R.drawable.icon);
				noteView.setTextViewText(R.id.title_text, title);
				noteView.setProgressBar(R.id.title_progress, 100, 0, true);
				noteView.setViewVisibility(R.id.title_search, View.GONE);
				noteView.setViewVisibility(R.id.title_path, View.GONE);
				Intent intent = new Intent(mContext, OpenExplorer.class);
				PendingIntent pendingIntent = PendingIntent.getActivity(mContext, OpenExplorer.REQUEST_CANCEL, intent, 0);
				NotificationManager mNotifier = (NotificationManager)mContext.getSystemService(Context.NOTIFICATION_SERVICE);
				mNote = new Notification(R.drawable.icon, title, 0);
				mNote.contentView = noteView;
				mNote.contentIntent = pendingIntent;
				mNotifier.notify(BACKGROUND_NOTIFICATION_ID, mNote);
			} catch(Exception e) { }
		}
		
		public ArrayList<OpenPath> searchDirectory(OpenPath dir, String pattern)
		{
			return null;
		}
		
		protected Integer doInBackground(OpenPath... params) {
			int len = params.length;
			int ret = 0;
			
			switch(mType) {
				
				case DELETE_TYPE:
					for(int i = 0; i < len; i++)
						ret += (mFileMang.deleteTarget(params[i]) == 0 ? 1 : 0);
					break;
				case SEARCH_TYPE:
					mSearchResults = searchDirectory(mIntoPath, mInitParams[0]);
					break;
				case COPY_TYPE:
					for(OpenPath file : params)
					{
						try {
							if(copyToDirectory(mIntoPath, file, 0))
							{
								ret++;
								if(mDeleteFile)
									mFileMang.deleteTarget(file);
							}
						} catch (IOException e) {
							Logger.LogError("Couldn't copy file (" + file.getName() + " to " + mIntoPath.getPath() + ")", e);
						}
					}
					break;
				case UNZIPTO_TYPE:
				case UNZIP_TYPE:
					extractZipFiles(mIntoPath, params[0]);
					break;
	
				case ZIP_TYPE:
					mFileMang.createZipFile(mIntoPath, params);
					break;
			}
			
			return ret;
		}
		
		private Boolean copyToDirectory(OpenPath old, OpenPath newDir, int total) throws IOException
		{
			byte[] data = new byte[FileManager.BUFFER];
			int read = 0;
			
			if(old.isDirectory() && newDir.isDirectory() && newDir.canWrite()) {
				OpenPath[] files = old.list();
				OpenPath newFile = newDir.getChild(old.getName());
				
				if(!newFile.mkdir())
					return false;
				
				for(OpenPath file : files)
					total += (int)file.length();
				
				for(int i = 0; i < files.length; i++)
					if(!copyToDirectory(files[i], newFile.getParent(), total))
						return false;
				
			} else if(old.isFile() && newDir.isDirectory() && newDir.canWrite()){
				OpenPath newFile = newDir.getChild(old.getName());
				int size = (int)old.length();
				int pos = 0;

				try {
					BufferedOutputStream o_stream = new BufferedOutputStream(
													newFile.getOutputStream());
					BufferedInputStream i_stream = new BufferedInputStream(
												   old.getInputStream());
					
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
					return false;
					
				} catch (IOException e) {
					Log.e("IOException", e.getMessage());
					return false;
				}
				
			} else if(!newDir.canWrite())
				return false;
			
			return true;
		}
		
		public void extractZipFiles(OpenPath zip, OpenPath directory) {
			byte[] data = new byte[FileManager.BUFFER];
			String name, path, zipDir;
			ZipEntry entry;
			ZipInputStream zipstream;

			if(!directory.mkdir())
				return;
			
			try {
				zipstream = new ZipInputStream(zip.getInputStream());
				
				while((entry = zipstream.getNextEntry()) != null) {
					OpenPath newFile = directory.getChild(entry.getName());
					if(!newFile.mkdir()) continue;
					
					int read = 0;
					FileOutputStream out = (FileOutputStream)newFile.getOutputStream();
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
			
			//mNote.setLatestEventInfo(mContext, , contentText, contentIntent)
			
			if(mExtraProgress != null)
			{
				if(values.length == 0)
					mExtraProgress.setIndeterminate(true);
				else
				{
					mExtraProgress.setIndeterminate(false);
					mExtraProgress.setMax(1000);
					mExtraProgress.setProgress(progA);
					mExtraProgress.setSecondaryProgress(progB);	
				}
			}

			try {
				RemoteViews noteView = mNote.contentView;
				noteView.setProgressBar(R.id.title_progress, 1000, progA, values.length == 0);
				noteView.notify();
			} catch(Exception e) { }
			
			try {
				if(values.length == 0)
					mPDialog.setIndeterminate(true);
				else {
					mPDialog.setIndeterminate(false);
					mPDialog.setMax(1000);
					mPDialog.setProgress(progA);
					mPDialog.setSecondaryProgress(progB);
				}
			} catch(Exception e) { }
		}

		
		protected void onPostExecute(ArrayList<String> result) {
			NotificationManager mNotifier = (NotificationManager)mContext.getSystemService(Context.NOTIFICATION_SERVICE);
			mNotifier.cancel(BACKGROUND_NOTIFICATION_ID);
			
			switch(mType) {
			
			case DELETE_TYPE:		
				if(mPDialog != null)
					mPDialog.dismiss();
				mThreadListener.onWorkerThreadComplete(mType, null);
				
				if(!result.contains("0"))
					Toast.makeText(mContext, getResourceString(mContext, R.string.s_msg_none, R.string.s_msg_deleted), Toast.LENGTH_SHORT).show();
				else if(result.contains("-1"))
					Toast.makeText(mContext, getResourceString(mContext, R.string.s_msg_some,R.string.s_msg_deleted), Toast.LENGTH_SHORT).show();
				else
					Toast.makeText(mContext, getResourceString(mContext, R.string.s_msg_all, R.string.s_msg_deleted), Toast.LENGTH_SHORT).show();
					
				break;
				
			case SEARCH_TYPE:
				if(mPDialog != null)
					mPDialog.dismiss();
				mThreadListener.onWorkerThreadComplete(mType, result);
				break;
				
			case COPY_TYPE:
				if(mPDialog != null)
					mPDialog.dismiss();
				mThreadListener.onWorkerThreadComplete(mType, null);
				
				if(!mDeleteFile) {
					if(result == null || !result.contains("0"))
						Toast.makeText(mContext, getResourceString(mContext, R.string.s_msg_none, R.string.s_msg_copied), Toast.LENGTH_SHORT).show();
					else if(result != null && result.contains("-1"))
						Toast.makeText(mContext, getResourceString(mContext, R.string.s_msg_some, R.string.s_msg_copied), Toast.LENGTH_SHORT).show();
					else
						Toast.makeText(mContext, getResourceString(mContext, R.string.s_msg_all, R.string.s_msg_copied), Toast.LENGTH_SHORT).show();
				} else {
					if(result == null || !result.contains("0"))
						Toast.makeText(mContext, getResourceString(mContext, R.string.s_msg_none, R.string.s_msg_moved), Toast.LENGTH_SHORT).show();
					else if(result != null && result.contains("-1"))
						Toast.makeText(mContext, getResourceString(mContext, R.string.s_msg_some, R.string.s_msg_moved), Toast.LENGTH_SHORT).show();
					else
						Toast.makeText(mContext, getResourceString(mContext, R.string.s_msg_all, R.string.s_msg_moved), Toast.LENGTH_SHORT).show();
				}
				mDeleteFile = false;				
				break;
				
			case UNZIPTO_TYPE:
				if(mPDialog != null)
					mPDialog.dismiss();
				mThreadListener.onWorkerThreadComplete(mType, null);
				break;
				
			case UNZIP_TYPE:
				if(mPDialog != null)
					mPDialog.dismiss();
				mThreadListener.onWorkerThreadComplete(mType, null);
				break;
				
			case ZIP_TYPE:
				if(mPDialog != null)
					mPDialog.dismiss();
				mThreadListener.onWorkerThreadComplete(mType, null);
				break;
			}
		}
	}

}
