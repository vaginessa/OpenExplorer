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

package org.brandroid.openmanager.fragments;

import org.apache.commons.net.ftp.FTPFile;
import org.brandroid.openmanager.EventHandler;
import org.brandroid.openmanager.FileManager;
import org.brandroid.openmanager.MultiSelectHandler;
import org.brandroid.openmanager.OpenExplorer;
import org.brandroid.openmanager.R;
import org.brandroid.openmanager.SettingsActivity;
import org.brandroid.openmanager.ThumbnailCreator;
import org.brandroid.openmanager.EventHandler.OnWorkerThreadFinishedListener;
import org.brandroid.openmanager.FileManager.SortType;
import org.brandroid.openmanager.OpenExplorer.OnSettingsChangeListener;
import org.brandroid.openmanager.R.color;
import org.brandroid.openmanager.R.drawable;
import org.brandroid.openmanager.R.id;
import org.brandroid.openmanager.R.layout;
import org.brandroid.openmanager.data.BookmarkHolder;
import org.brandroid.openmanager.data.OpenFTP;
import org.brandroid.openmanager.data.OpenFace;
import org.brandroid.openmanager.data.OpenFile;
import org.brandroid.openmanager.fragments.BookmarkFragment.OnChangeLocationListener;
import org.brandroid.openmanager.fragments.DialogHandler.OnSearchFileSelected;
import org.brandroid.openmanager.ftp.FTPManager;
import org.brandroid.utils.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.content.Context;
import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.graphics.drawable.BitmapDrawable;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Toast;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.ImageView;
import android.widget.Button;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.net.Uri;

public class DirContentActivity extends Fragment implements OnItemClickListener,
															OnSettingsChangeListener,
															OnChangeLocationListener,
															OnWorkerThreadFinishedListener{
	private static final int D_MENU_DELETE 	= 0x00;
	private static final int D_MENU_RENAME 	= 0x01;
	private static final int D_MENU_COPY	= 0x02;
	private static final int D_MENU_MOVE	= 0x03;
	private static final int D_MENU_ZIP		= 0x04;
	private static final int D_MENU_PASTE	= 0x05;
	private static final int D_MENU_UNZIP	= 0x0b;
	private static final int D_MENU_BOOK	= 0x0c;
	private static final int D_MENU_INFO	= 0x0d;
	
	private static final int F_MENU_DELETE	= 0x06;
	private static final int F_MENU_RENAME	= 0x07;
	private static final int F_MENU_COPY	= 0x08;
	private static final int F_MENU_MOVE	= 0x09;
	private static final int F_MENU_SEND	= 0x0a;
	private static final int F_MENU_INFO	= 0x0e;
	private static boolean mMultiSelectOn = false;
	
	private FileManager mFileMang;
	private EventHandler mHandler;
	private MultiSelectHandler mMultiSelect;
	private ThumbnailCreator mThumbnail;
	private static OnBookMarkAddListener mBookmarkList;
	
	private LinearLayout mPathView, mMultiSelectView;
	private GridView mGrid = null;
	private ListView mList = null;
	private boolean mShowGrid;
	
	private ArrayList<OpenFace> mData; //the data that is bound to our array adapter.
	private ArrayList<OpenFace> mHoldingFileList; //holding files waiting to be pasted(moved)
	private ArrayList<OpenFace> mHoldingZipList; //holding zip files waiting to be unzipped.
	private Context mContext;
	private FileSystemAdapter mContentAdapter;
	private ActionMode mActionMode;
	private boolean mActionModeSelected;
	private boolean mHoldingFile;
	private boolean mHoldingZip;
	private boolean mCutFile;
	private boolean mShowThumbnails;
	private int mBackPathIndex;
	private boolean mReadyToUpdate = true;
	
	public interface OnBookMarkAddListener {
		public void onBookMarkAdd(String path);
	}
	
	private ActionMode.Callback mFolderOptActionMode = new ActionMode.Callback() {
		
		//@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return false;
		}
		
		//@Override
		public void onDestroyActionMode(ActionMode mode) {
			mActionMode = null;
			mActionModeSelected = false;
		}
		
		//@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			menu.add(0, D_MENU_BOOK, 0, "Bookmark");
			menu.add(0, D_MENU_INFO, 0, "Info");
			menu.add(0, D_MENU_DELETE, 0, "Delete");
			menu.add(0, D_MENU_RENAME, 0, "Rename");
        	menu.add(0, D_MENU_COPY, 0, "Copy");
        	menu.add(0, D_MENU_MOVE, 0, "Cut");
        	menu.add(0, D_MENU_ZIP, 0, "Zip");
        	menu.add(0, D_MENU_PASTE, 0, "Paste into folder").setEnabled(mHoldingFile);
        	menu.add(0, D_MENU_UNZIP, 0, "Unzip here").setEnabled(mHoldingZip);
        	
        	mActionModeSelected = true;
			
        	return true;
		}
		
		//@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			ArrayList<OpenFace> files = new ArrayList<OpenFace>();
			OpenFace file = FileManager.getOpenCache(mFileMang.getCurrentDir() + "/" + mode.getTitle().toString());
			String path = file.getPath();
			String name = file.getName();
			
			switch(item.getItemId()) {
				case D_MENU_BOOK:
					mBookmarkList.onBookMarkAdd(path);
					mode.finish();
					return true;
					
				case D_MENU_DELETE:
					files.add(file);
					mHandler.deleteFile(files);
					mode.finish();
					return true;
					
				case D_MENU_RENAME:
					mHandler.renameFile(path, true);
					mode.finish();
					return true;
					
				case D_MENU_COPY:
					if(mHoldingFileList == null)
						mHoldingFileList = new ArrayList<OpenFace>();
					
					mHoldingFileList.clear();
					mHoldingFileList.add(file);
					mHoldingFile = true;
					mCutFile = false;
					((OpenExplorer)getActivity()).changeActionBarTitle("Holding " + name);
					mode.finish();
					return true;
					
				case D_MENU_MOVE:
					if(mHoldingFileList == null)
						mHoldingFileList = new ArrayList<OpenFace>();
					
					mHoldingFileList.clear();
					mHoldingFileList.add(file);
					mHoldingFile = true;
					mCutFile = true;
					((OpenExplorer)getActivity()).changeActionBarTitle("Holding " + name);
					mode.finish();
					return true;
					
				case D_MENU_PASTE:
					if(mHoldingFile && mHoldingFileList.size() > 0)
						if(mCutFile)
							mHandler.cutFile(mHoldingFileList, path);
						else
							mHandler.copyFile(mHoldingFileList, path);
					
					mHoldingFile = false;
					mCutFile = false;
					mHoldingFileList.clear();
					mHoldingFileList = null;
					((OpenExplorer)getActivity()).changeActionBarTitle("Open Manager");
					mode.finish();
					return true;
					
				case D_MENU_ZIP:
					mHandler.zipFile(path);
					mode.finish();
					return true;
					
				case D_MENU_UNZIP:
					mHandler.unZipFileTo(mHoldingZipList.get(0), file);
					
					mHoldingZip = false;
					mHoldingZipList.clear();
					mHoldingZipList = null;
					((OpenExplorer)getActivity()).changeActionBarTitle("Open Manager");
					mode.finish();
					return true;
				
				case D_MENU_INFO:
					DialogHandler dialog = DialogHandler.newDialog(DialogHandler.FILEINFO_DIALOG, mContext);
					dialog.setFilePath(path);
					dialog.show(getFragmentManager(), "info");
					mode.finish();
					return true;
			}			
			return false;
		}
	};	

	private ActionMode.Callback mFileOptActionMode = new ActionMode.Callback() {
		//@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return false;
		}
		
		//@Override
		public void onDestroyActionMode(ActionMode mode) {
			mActionMode = null;
			mActionModeSelected = false;
		}
		
		//@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			menu.add(0, F_MENU_INFO, 0, "Info");
			menu.add(0, F_MENU_DELETE, 0, "Delete");
    		menu.add(0, F_MENU_RENAME, 0, "Rename");
    		menu.add(0, F_MENU_COPY, 0, "Copy");
    		menu.add(0, F_MENU_MOVE, 0, "Cut");
    		menu.add(0, F_MENU_SEND, 0, "Send");
    		
    		mActionModeSelected = true;
			return true;
		}

		//@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			ArrayList<OpenFace> files = new ArrayList<OpenFace>();
			
			OpenFace file = FileManager.getOpenCache(mFileMang.getCurrentDir() + "/" + mode.getTitle().toString());
			String path = file.getPath();
			String name = file.getName();
			
			switch(item.getItemId()) {
				case F_MENU_DELETE:
					files.add(file);
					mHandler.deleteFile(files);
					mode.finish();
					return true;
					
				case F_MENU_RENAME:
					mHandler.renameFile(path, false);
					mode.finish();
					return true;
					
				case F_MENU_COPY:
					if(mHoldingFileList == null)
						mHoldingFileList = new ArrayList<OpenFace>();
					
					mHoldingFileList.clear();
					mHoldingFileList.add(file);
					mHoldingFile = true;
					mCutFile = false;
					((OpenExplorer)getActivity()).changeActionBarTitle("Holding " + name);				
					mode.finish();
					return true;
					
				case F_MENU_MOVE:
					if(mHoldingFileList == null)
						mHoldingFileList = new ArrayList<OpenFace>();
					
					mHoldingFileList.clear();
					mHoldingFileList.add(file);
					mHoldingFile = true;
					mCutFile = true;
					((OpenExplorer)getActivity()).changeActionBarTitle("Holding " + name);		
					mode.finish();
					return true;
					
				case F_MENU_SEND:
					Intent mail = new Intent();
					mail.setType("application/mail");
					
					mail.setAction(android.content.Intent.ACTION_SEND);
					mail.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(path)));
					startActivity(mail);
					
					mode.finish();
					return true;

//					this is for bluetooth
//					files.add(path);
//					mHandler.sendFile(files);
//					mode.finish();
//					return true;
					
				case F_MENU_INFO:
					DialogHandler dialog = DialogHandler.newDialog(DialogHandler.FILEINFO_DIALOG, mContext);
					dialog.setFilePath(path);
					dialog.show(getFragmentManager(), "info");
					mode.finish();
					return true;
			}
			mActionModeSelected = false;
			return false;
		}
	};
	
	
	//@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mContext = getActivity();
		mFileMang = new FileManager();
		mHandler = new EventHandler(mContext, mFileMang);
		mHandler.setOnWorkerThreadFinishedListener(this);
		
		if (savedInstanceState != null)
			mData = mFileMang.getNextDir(new OpenFile(savedInstanceState.getString("location")), true);
		else
			mData = mFileMang.setHomeDir("/sdcard");
		
		mBackPathIndex = 0;
		mHoldingFile = false;
		mHoldingZip = false;
		mActionModeSelected = false;
		mShowGrid = "grid".equals((PreferenceManager
									.getDefaultSharedPreferences(mContext))
										.getString("pref_view", "list"));
		
		mShowThumbnails = PreferenceManager.getDefaultSharedPreferences(mContext)
							.getBoolean(SettingsActivity.PREF_THUMB_KEY, false);
		
		OpenExplorer.setOnSettingsChangeListener(this);
		BookmarkFragment.setOnChangeLocationListener(this);
	}
	
	 //@Override
	    public void onSaveInstanceState(Bundle outState) {
	    	super.onSaveInstanceState(outState);
	    	
	    	outState.putString("location", mFileMang.getCurrentDir());
	    }
	
	//@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.main_layout, container, false);
		v.setBackgroundResource(R.color.lightgray);
		
		mPathView = (LinearLayout)v.findViewById(R.id.scroll_path);
		mGrid = (GridView)v.findViewById(R.id.grid_gridview);
		mList = (ListView)v.findViewById(R.id.list_listview);
		mMultiSelectView = (LinearLayout)v.findViewById(R.id.multiselect_path);
		
		if (savedInstanceState != null) {
			String location = savedInstanceState.getString("location");
			String[] parts = location.split("/");
			
			for(int i = 2; i < parts.length; i++)
				addBackButton(new OpenFile(parts[i]), false);
		}
		
		updateChosenMode(mShowGrid ? mGrid : mList);
		

		return v;
	}
	
	public void updateChosenMode(AbsListView mChosenMode)
	{
		if(mShowGrid) {
			mContentAdapter = new FileSystemAdapter(mContext, R.layout.grid_content_layout, mData);
			mGrid.setVisibility(View.VISIBLE);
			mList.setVisibility(View.GONE);
		} else {
			mContentAdapter = new FileSystemAdapter(mContext, R.layout.list_content_layout, mData);
			mList.setVisibility(View.VISIBLE);
			mGrid.setVisibility(View.GONE);
		}
		mChosenMode.setOnItemClickListener(this);
		mChosenMode.setAdapter(mContentAdapter);
		mChosenMode.setOnItemLongClickListener(new OnItemLongClickListener() {
			//@Override
			public boolean onItemLongClick(AdapterView<?> list, View view ,int pos, long id) {
				OpenFace file = mData.get(pos);
				
				if(!file.isDirectory() && mActionMode == null && !mMultiSelectOn) {
					mActionMode = getActivity().startActionMode(mFileOptActionMode);
					mActionMode.setTitle(file.getName());
					
					return true;
				}
				
				if(file.isDirectory() && mActionMode == null && !mMultiSelectOn) {
					mActionMode = getActivity().startActionMode(mFolderOptActionMode);
					mActionMode.setTitle(file.getName());
					
					return true;
				}
				
				return false;
			}
		});
	}
	
	//@Override
	public void onItemClick(AdapterView<?> list, View view, int pos, long id) {
		String item_ext = "";
		final OpenFace file = mData.get(pos);
		final String name = file.getName();
		
		//((OpenExplorer)getActivity()).hideBookmarkTitles();
		
		if(mMultiSelectOn) {
			View v;
			
			if (mThumbnail == null)
				v = mMultiSelect.addFile(file.getPath());
			else
				v = mMultiSelect.addFile(file.getPath(), mThumbnail);
			
			if(v == null)
				return;
			
			v.setOnClickListener(new View.OnClickListener() {
				//@Override
				public void onClick(View v) {					
					int ret = mMultiSelect.clearFileEntry(file.getPath());
					mMultiSelectView.removeViewAt(ret);
				}
			});
			
			mMultiSelectView.addView(v);
			return;
		}
		
		if(file.isDirectory() && !mActionModeSelected ) {
			if (mThumbnail != null) {
				mThumbnail.setCancelThumbnails(true);
				mThumbnail = null;
			}
			
			addBackButton(file, true);

		} else if (!file.isDirectory() && !mActionModeSelected ) {
			
			try {
				item_ext = name.substring(name.lastIndexOf("."), name.length());
				
			} catch (StringIndexOutOfBoundsException e) {
				item_ext = "";
			}
			
			/*audio files*/
			if (item_ext.equalsIgnoreCase(".mp3") || 
				item_ext.equalsIgnoreCase(".m4a") ) {
	    		
	    		Intent i = new Intent();
   				i.setAction(android.content.Intent.ACTION_VIEW);
   				i.setDataAndType(file.getUri(), "audio/*");
   				startActivity(i);
			
			}
			
			/* image files*/
			else if(item_ext.equalsIgnoreCase(".jpeg") || 
	    			item_ext.equalsIgnoreCase(".jpg")  ||
	    			item_ext.equalsIgnoreCase(".png")  ||
	    			item_ext.equalsIgnoreCase(".gif")  || 
	    			item_ext.equalsIgnoreCase(".tiff")) {

				Intent picIntent = new Intent();
		    		picIntent.setAction(android.content.Intent.ACTION_VIEW);
		    		picIntent.setDataAndType(file.getUri(), "image/*");
		    		startActivity(picIntent);
	    	}
			
			/*video file selected--add more video formats*/
	    	else if(item_ext.equalsIgnoreCase(".m4v") ||
	    			item_ext.equalsIgnoreCase(".mp4") ||
	    			item_ext.equalsIgnoreCase(".3gp") ||
	    			item_ext.equalsIgnoreCase(".wmv") || 
	    			item_ext.equalsIgnoreCase(".mp4") || 
	    			item_ext.equalsIgnoreCase(".avi") || 
	    			item_ext.equalsIgnoreCase(".ogg") ||
	    			item_ext.equalsIgnoreCase(".wav")) {
	    		
    				Intent movieIntent = new Intent();
		    		movieIntent.setAction(android.content.Intent.ACTION_VIEW);
		    		movieIntent.setDataAndType(file.getUri(), "video/*");
		    		startActivity(movieIntent);	
	    	}
			
			/*pdf file selected*/
	    	else if(item_ext.equalsIgnoreCase(".pdf")) {
	    		
	    		if(file.exists()) {
		    		Intent pdfIntent = new Intent();
		    		pdfIntent.setAction(android.content.Intent.ACTION_VIEW);
		    		pdfIntent.setDataAndType(file.getUri(), "application/pdf");
			    		
		    		try {
		    			startActivity(pdfIntent);
		    		} catch (ActivityNotFoundException e) {
		    			Toast.makeText(mContext, "Sorry, couldn't find a pdf viewer", 
								Toast.LENGTH_SHORT).show();
		    		}
		    	}
	    	}
			
			/*Android application file*/
	    	else if(item_ext.equalsIgnoreCase(".apk")){
	    		
	    		if(file.exists()) {
	    			Intent apkIntent = new Intent();
	    			apkIntent.setAction(android.content.Intent.ACTION_VIEW);
	    			apkIntent.setDataAndType(file.getUri(), 
	    									 "application/vnd.android.package-archive");
	    			startActivity(apkIntent);
	    		}
	    	}
			
			/* HTML XML file */
	    	else if(item_ext.equalsIgnoreCase(".html") || 
	    			item_ext.equalsIgnoreCase(".xml")) {
	    		
	    		if(file.exists()) {
	    			Intent htmlIntent = new Intent();
	    			htmlIntent.setAction(android.content.Intent.ACTION_VIEW);
	    			htmlIntent.setDataAndType(file.getUri(), "text/html");
	    			
	    			try {
	    				startActivity(htmlIntent);
	    			} catch(ActivityNotFoundException e) {
	    				Toast.makeText(mContext, "Sorry, couldn't find a HTML viewer", 
	    									Toast.LENGTH_SHORT).show();
		    			
	    			}
	    		}
	    	}
			
			/* ZIP files */
	    	else if(item_ext.equalsIgnoreCase(".zip")) {
	    		mHandler.unzipFile(file);
	    	}
			
			/* text file*/
	    	else if(item_ext.equalsIgnoreCase(".txt")) {
	    		Boolean bUseIntent = false;
	    		if(!bUseIntent)
	    		{
	    			((OpenExplorer)getActivity()).editFile(file.getAbsolutePath());
	    		} else {
	    			Intent txtIntent = new Intent();
	    			txtIntent.setAction(android.content.Intent.ACTION_VIEW);
	    			txtIntent.setDataAndType(file.getUri(), "text/plain");
	    			
	    			try {
	    				startActivity(txtIntent);
	    			} catch(ActivityNotFoundException e) {
	    				txtIntent.setType("text/*");
	    				startActivity(txtIntent);
	    			}
	    		}
	    	}
			
			/* generic intent */
	    	else {
	    		if(file.exists()) {
		    		Intent generic = new Intent();
		    		generic.setAction(android.content.Intent.ACTION_VIEW);
		    		generic.setDataAndType(file.getUri(), "application/*");
		    		
		    		try {
		    			startActivity(generic);
		    		} catch(ActivityNotFoundException e) {
		    			Toast.makeText(mContext, "Sorry, couldn't find anything " +
		    						   "to open " + file.getName(), 
		    						   Toast.LENGTH_SHORT).show();
			    	}
	    		}
	    	}
		}
	}
	
	/*
	 * (non-Javadoc)
	 * This is a callback function that is called when the user
	 * selects an item (directory) from the dir list on the left.
	 * This will update the contents of the dir on the right
	 * from the path give in the variable name.
	 */
	//@Override
	@SuppressWarnings("unused")
	public void onChangeLocation(final String s)
	{
		if(mActionModeSelected || mMultiSelectOn)
			return;
		
		Logger.LogDebug("Location changed to " + s);
		OpenFace path = FileManager.getOpenCache(s);
		if(path == null)
		{
			Logger.LogWarning("Cache was null still for " + s);
			path = FileManager.setOpenCache(s, new OpenFile(s));
		}
		if(path == null)
		{
			Logger.LogWarning("Invalid path specified - " + s);
			updateData(new ArrayList<OpenFace>());
		} else {

			if (mThumbnail != null) {
				mThumbnail.setCancelThumbnails(true);
				mThumbnail = null;
			}
			
			FragmentTransaction ft = getFragmentManager().beginTransaction();
			ft.setBreadCrumbTitle(path.getPath());
			ft.addToBackStack("path");
			ft.commit();
			
			//getFragmentManager().popBackStackImmediate("Settings", 0);
			
			//if(path.requiresThread())
				new FileIOTask().execute(
						new FileIOCommand(FileIOCommandType.ALL, path));
			//else if(!path.requiresThread())
			//	updateData(mFileMang.getNextDir(path, true));
			/*
			if(!path.exists())
			{
				mData = mFileMang.getNextDir(path, true);
				mContentAdapter.notifyDataSetChanged();
				
				mPathView.removeAllViews();
				mBackPathIndex = 0;	
			}
			*/
		}
	}
	
	private void updateData(final ArrayList<OpenFace> nextDir) {
		if(!mReadyToUpdate) return;
		mReadyToUpdate = false;
		getActivity().runOnUiThread(new Runnable() {
			public void run() {
				mData.clear();
				int folder_index = 0;
				for(OpenFace f : nextDir)
					if(f.isDirectory())
						mData.add(folder_index++, f);
					else
						mData.add(f);
				mContentAdapter.notifyDataSetChanged();
				mReadyToUpdate = true;
			}
		});
	}

	public void goBack()
	{
		updateData(mFileMang.getPreviousDir());
	}
	
	/*
	 * (non-Javadoc)
	 * this will update the data shown to the user after a change to
	 * the file system has been made from our background thread or EventHandler.
	 */
	//@Override
	public void onWorkerThreadComplete(int type, ArrayList<OpenFace> results) {
		
		if(type == EventHandler.SEARCH_TYPE) {
			if(results == null || results.size() < 1) {
				Toast.makeText(mContext, "Sorry, zero items found", Toast.LENGTH_LONG).show();
				return;
			}
			
			DialogHandler dialog = DialogHandler.newDialog(DialogHandler.SEARCHRESULT_DIALOG, mContext);
			dialog.setHoldingFileList(results);
			dialog.setOnSearchFileSelected(new OnSearchFileSelected() {
				
				//@Override
				public void onFileSelected(String fileName) {
					OpenFace file = FileManager.getOpenCache(fileName);
					
					if (file.isDirectory()) {
						updateData(mFileMang.getNextDir(file, true));
					} else {
						updateData(mFileMang.getNextDir(file.getParent(), true));
					}						
				}
			});
			
			dialog.show(getFragmentManager(), "dialog");
			
		} else if(type == EventHandler.UNZIPTO_TYPE && results != null) {
			String name = results.get(0).getName();
			
			if(mHoldingZipList == null)
				mHoldingZipList = new ArrayList<OpenFace>();
			
			mHoldingZipList.add(results.get(0));
			mHoldingZip = true;
			((OpenExplorer)getActivity()).changeActionBarTitle("Holding " + name);
			
		} else {
			updateData(mFileMang.getNextDir(FileManager.getOpenCache(mFileMang.getCurrentDir()), true));
		}
	}
	
	//@Override
	public void onHiddenFilesChanged(boolean state) {
		mFileMang.setShowHiddenFiles(state);
		
		updateData(mFileMang.getNextDir(FileManager.getOpenCache(mFileMang.getCurrentDir()), true));
	}

	//@Override
	public void onThumbnailChanged(boolean state) {
		mShowThumbnails = state;
		mContentAdapter.notifyDataSetChanged();
	}
	
	//@Override
	public void onSortingChanged(SortType type) {
		mFileMang.setSorting(type);
		updateData(mFileMang.getNextDir(FileManager.getOpenCache(mFileMang.getCurrentDir()), true));
	}
	
	public void onSortingChanged(String state) {
		if (state.equals("none"))
			mFileMang.setSorting(SortType.NONE);
		else if (state.equals("alpha"))
			mFileMang.setSorting(SortType.ALPHA);
		else if (state.equals("type"))
			mFileMang.setSorting(SortType.TYPE);
		else if (state.equals("size"))
			mFileMang.setSorting(SortType.SIZE);

		updateData(mFileMang.getNextDir(FileManager.getOpenCache(mFileMang.getCurrentDir()), true));
	}

	//@Override
	public void onViewChanged(String state) {
		if(state.equals("list") && mShowGrid)					
			mShowGrid = false;
		else if (state.equals("grid") && !mShowGrid)
			mShowGrid = true;
		updateChosenMode(mShowGrid ? mGrid : mList);
	}
			
	public void changeMultiSelectState(boolean state, MultiSelectHandler handler) {
		if(state && handler != null) {
			mMultiSelect = handler;
			mMultiSelectOn = state;
			
		} else if (!state && handler != null) {
			mMultiSelect = handler;
			mMultiSelect.cancelMultiSelect();
			mMultiSelectView.removeAllViews();
			mMultiSelectOn = state;
		}
	}
	
	public static void setOnBookMarkAddListener(BookmarkFragment bookmarkFragment) {
		mBookmarkList = bookmarkFragment;
	}
	
	/*
	 * This is a convience function so our applications main activity
	 * (MainActivity.java) class can have access to our event handler
	 * and perform file operations such as delete, rename etc. 
	 * Event handler sits between our view and modal (FileManager)
	 */
	public EventHandler getEventHandlerInst() {
		return mHandler;
	}
	
	/*
	 * See comments for getEventHandlerInst(). Same reasoning.
	 */
	public FileManager getFileManagerInst() {
		return mFileMang;
	}
	
	/*
	 * we need to make a temp arraylist because when the
	 * multiselect actionmode callback is finished our multiselect
	 * object will turn off and clear the data in files
	 */
	public void setCopiedFiles(ArrayList<OpenFace> files, boolean cutFile) {
		ArrayList<OpenFace> temp = new ArrayList<OpenFace>(files);
		
		mHoldingFile = true;
		mCutFile = cutFile;
		mHoldingFileList = temp;
	}
	
	private void addBackButton(OpenFace file, boolean refreshList) {//, int pos) {
		final String bname = file.getName();
		
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		ft.setBreadCrumbTitle(file.getPath());

		if (refreshList)
		{
			if(file.requiresThread())
			{
				new FileIOTask().execute(new FileIOCommand(FileIOCommandType.NEXT, file.getChild(bname)));
			} else
				updateData(mFileMang.getNextDir(file, true));
		}
		
		/*
		Button button = new Button(mContext);
		button.setOnClickListener(new View.OnClickListener() {
			////@Override
			public void onClick(View v) {
				int index = (Integer)v.getTag();
				String path = mFileMang.getCurrentDir();
				String subPath;
				
				if(mActionModeSelected || mMultiSelectOn)
					return;
				
				if (mThumbnail != null) {
					mThumbnail.setCancelThumbnails(true);
					mThumbnail = null;
				}
				
				if(index != (mPathView.getChildCount() - 1)) {
					while(index < mPathView.getChildCount() - 1)
						mPathView.removeViewAt(mPathView.getChildCount() - 1);
					
					mBackPathIndex = index + 1;
					
					subPath = path.substring(0, path.lastIndexOf(bname));					
					updateData(mFileMang.getNextDir(FileManager.getOpenCache(subPath + bname), true));
				}
			}
		});
		
		button.setText(file.getName());
		button.setTag(mBackPathIndex++);
		mPathView.addView(button);
		*/
		if (refreshList)
			mContentAdapter.notifyDataSetChanged();
		
		ft.addToBackStack("path");
		ft.commit();
	}
	
	
	/**
	 * 
	 */
	private class FileSystemAdapter extends ArrayAdapter<OpenFace> {
		private final int KB = 1024;
    	private final int MG = KB * KB;
    	private final int GB = MG * KB;
    	
		private BookmarkHolder mHolder;
		private String mName;
		
		public FileSystemAdapter(Context context, int layout, ArrayList<OpenFace> data) {
			super(context, layout, data);
		}
		
		@Override
		public void notifyDataSetChanged() {
			//Logger.LogDebug("Data set changed.");
			try {
				if(mFileMang != null)
					((OpenExplorer)getActivity()).updateTitle(mFileMang.getCurrentDir());
				super.notifyDataSetChanged();
			} catch(NullPointerException npe) {
				Logger.LogError("Null found while notifying data change.", npe);
			}
		}
		
		////@Override
		public View getView(int position, View view, ViewGroup parent) {
			String ext;
			OpenFace file = mData.get(position);
			OpenFace current = file.getParent();
			mName = file.getName();
			
			if (mThumbnail == null)
				mThumbnail = new ThumbnailCreator(mContext, 72, 72);

			try {
				ext = mName.substring(mName.lastIndexOf('.') + 1, mName.length());
				
			} catch (StringIndexOutOfBoundsException e) { ext = ""; }
			
			if(view == null) {
				LayoutInflater in = (LayoutInflater)mContext
										.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				
				if(mShowGrid)
					view = in.inflate(R.layout.grid_content_layout, parent, false);
				else
					view = in.inflate(R.layout.list_content_layout, parent, false);
				
				mHolder = new BookmarkHolder(mName, mName, view);
				
				if(!mShowGrid) { 
					mHolder.mInfo = (TextView)view.findViewById(R.id.content_info);
					mHolder.mPath = (TextView)view.findViewById(R.id.content_fullpath);
				}
				
				view.setTag(mHolder);
				
			} else {
				mHolder = (BookmarkHolder)view.getTag();
			}
			
			if(!mShowGrid) {
				mHolder.mInfo.setText(getFileDetails());
				mHolder.mPath.setText(getFilePath());
			}
			
			mHolder.mMainText.setText(mName);
			
			/* assign custom icons based on file type */
			if(file.isDirectory()) {
				OpenFace[] lists = null;
				try {
					if(!file.requiresThread())
						lists = file.list();
				} catch (IOException e) {
					Logger.LogError("Error getting lists.", e);
				}
				
				if(file.canRead() && lists != null && lists.length > 0)
					mHolder.mIcon.setImageResource(R.drawable.folder_large_full);
				else
					mHolder.mIcon.setImageResource(R.drawable.folder);
			} else if(ext.equalsIgnoreCase("doc") || ext.equalsIgnoreCase("docx")) {
				mHolder.mIcon.setImageResource(R.drawable.doc);
				
			} else if(ext.equalsIgnoreCase("xls")  || 
					  ext.equalsIgnoreCase("xlsx") ||
					  ext.equalsIgnoreCase("xlsm")) {
				mHolder.mIcon.setImageResource(R.drawable.excel);
				
			} else if(ext.equalsIgnoreCase("ppt") || ext.equalsIgnoreCase("pptx")) {
				mHolder.mIcon.setImageResource(R.drawable.powerpoint);
				
			} else if(ext.equalsIgnoreCase("zip") || ext.equalsIgnoreCase("gzip")) {
				mHolder.mIcon.setImageResource(R.drawable.zip);
				
			} else if (ext.equalsIgnoreCase("rar")) {
				mHolder.mIcon.setImageResource(R.drawable.rar);
				
			//} else if(ext.equalsIgnoreCase("apk")) {
			//	mHolder.mIcon.setImageResource(R.drawable.apk);
				
			} else if(ext.equalsIgnoreCase("pdf")) {
				mHolder.mIcon.setImageResource(R.drawable.pdf);
				
			} else if(ext.equalsIgnoreCase("xml") || ext.equalsIgnoreCase("html")) {
				mHolder.mIcon.setImageResource(R.drawable.xml_html);
				
			} else if(ext.equalsIgnoreCase("mp4") || 
					  ext.equalsIgnoreCase("3gp") || 
					  ext.equalsIgnoreCase("avi") ||
					  ext.equalsIgnoreCase("webm") || 
					  ext.equalsIgnoreCase("m4v")) {
				mHolder.mIcon.setImageResource(R.drawable.movie);
				
			} else if(ext.equalsIgnoreCase("mp3") || ext.equalsIgnoreCase("wav") ||
					  ext.equalsIgnoreCase("wma") || ext.equalsIgnoreCase("m4p") ||
					  ext.equalsIgnoreCase("m4a") || ext.equalsIgnoreCase("ogg")) {
				mHolder.mIcon.setImageResource(R.drawable.music);
			} else if(ext.equalsIgnoreCase("jpeg") || ext.equalsIgnoreCase("png") ||
					  ext.equalsIgnoreCase("apk")  ||
					  ext.equalsIgnoreCase("jpg")  || ext.equalsIgnoreCase("gif")) {

				if(file.length() > 0 && mShowThumbnails) {
					BitmapDrawable thumb = mThumbnail.isBitmapCached(file.getPath());

					if (thumb == null) {
						final Handler handle = new Handler(new Handler.Callback() {
							public boolean handleMessage(Message msg) {
								notifyDataSetChanged();
								
								return true;
							}
						});
										
						mThumbnail.createNewThumbnail(mData, mFileMang.getCurrentDir(), handle);
						
						try {
						if (!mThumbnail.isAlive()) 
							mThumbnail.start();
						} catch(IllegalThreadStateException itse) {
							Logger.LogError("Unable to start thumbnail cache thread.", itse);
						}
						
					} else {
						mHolder.mIcon.setImageDrawable(thumb);
					}
					
				} else {
					mHolder.mIcon.setImageResource(R.drawable.photo);
				}
				
			} else {
				
				OpenFTP f = FTPManager.getFTPFile(mName);
				if(f != null)
				{
					if(f.isDirectory())
						mHolder.mIcon.setImageResource(R.drawable.folder_large_full);
					else
						mHolder.mIcon.setImageResource(R.drawable.unknown);
				} else
					mHolder.mIcon.setImageResource(R.drawable.unknown);
			}
			
			return view;
		}
		
		private String getFilePath() {
			File file = new File(mFileMang.getCurrentDir() + "/" + mName);
			return file.getPath();
		}
		private String getFileDetails() {
			File file = new File(mFileMang.getCurrentDir() + "/" + mName);
			String t = ""; //file.getPath() + "\t\t";
			double bytes;
			String size = "";
			String atrs = " | - ";
			
			if(file.isDirectory()) {
				if(file.canRead())
					size =  file.list().length + " items";
				atrs += " d";
				
			} else {
				bytes = file.length();
				
				if (bytes > GB)
    				size = String.format("%.2f Gb ", (double)bytes / GB);
    			else if (bytes < GB && bytes > MG)
    				size = String.format("%.2f Mb ", (double)bytes / MG);
    			else if (bytes < MG && bytes > KB)
    				size = String.format("%.2f Kb ", (double)bytes/ KB);
    			else
    				size = String.format("%.2f bytes ", (double)bytes);
			}
			
			if(file.canRead())
				atrs += "r";
			if(file.canWrite())
				atrs += "w";
			
			return t + size + atrs;
		}
	}

	public enum FileIOCommandType
	{
		ALL,
		NEXT
	}
	public class FileIOCommand
	{
		public FileIOCommandType Type;
		public OpenFace Path;
		
		public FileIOCommand(FileIOCommandType type, OpenFace path)
		{
			Type = type;
			Path = path;
		}
	}
	
	public class FileIOTask extends AsyncTask<FileIOCommand, Integer, ArrayList<OpenFace>>
	{
		@Override
		protected ArrayList<OpenFace> doInBackground(FileIOCommand... params) {
			ArrayList<OpenFace> ret = new ArrayList<OpenFace>();
			for(FileIOCommand cmd : params)
			{
				if(cmd.Path.requiresThread())
				{
					OpenFace file = (OpenFTP)FileManager.getOpenCache(cmd.Path.getPath(), true);
					try {
						OpenFace[] list = file.list();
						if(list != null)
							for(OpenFace f : list)
								ret.add(f);
					} catch (IOException e) {
						Logger.LogError("Unable to list children for " + cmd.Path);
					}
				} else {
					switch(cmd.Type)
					{
						case NEXT:
							ret.addAll(mFileMang.getNextDir(cmd.Path, false));
							break;
						default:
							ret.addAll(mFileMang.getNextDir(cmd.Path, true));
							break;
					}
				}
			}
			Logger.LogDebug("Found " + ret.size() + " items.");
			return ret;
		}
		
		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			super.onPreExecute();
			//mData.clear();
		}
		
		@Override
		protected void onPostExecute(ArrayList<OpenFace> result)
		{
			mData.clear();
			updateData(result);
		}
		
	}
}


