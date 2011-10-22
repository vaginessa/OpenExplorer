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

import org.brandroid.openmanager.EventHandler;
import org.brandroid.openmanager.FileManager;
import org.brandroid.openmanager.IntentManager;
import org.brandroid.openmanager.MultiSelectHandler;
import org.brandroid.openmanager.OpenExplorer;
import org.brandroid.openmanager.R;
import org.brandroid.openmanager.SettingsActivity;
import org.brandroid.openmanager.ThumbnailCreator;
import org.brandroid.openmanager.EventHandler.OnWorkerThreadFinishedListener;
import org.brandroid.openmanager.FileManager.SortType;
import org.brandroid.openmanager.OpenExplorer.OnSettingsChangeListener;
import org.brandroid.openmanager.data.BookmarkHolder;
import org.brandroid.openmanager.data.OpenCursor;
import org.brandroid.openmanager.data.OpenFTP;
import org.brandroid.openmanager.data.OpenMediaStore;
import org.brandroid.openmanager.data.OpenPath;
import org.brandroid.openmanager.data.OpenFile;
import org.brandroid.openmanager.fragments.DialogHandler.OnSearchFileSelected;
import org.brandroid.openmanager.ftp.FTPManager;
import org.brandroid.utils.Logger;

import java.io.File;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Arrays;

import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Toast;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ZoomButtonsController.OnZoomListener;
import android.net.Uri;

public class ContentFragment extends Fragment implements OnItemClickListener,
															OnSettingsChangeListener,
															OnWorkerThreadFinishedListener{
	
	private static final int F_MENU_DELETE	= 0x06;
	private static final int F_MENU_RENAME	= 0x07;
	private static final int F_MENU_COPY	= 0x08;
	private static final int F_MENU_MOVE	= 0x09;
	private static final int F_MENU_SEND	= 0x0a;
	private static final int F_MENU_INFO	= 0x0e;
	private static boolean mMultiSelectOn = false;
	
	private FileManager mFileManager;
	private EventHandler mHandler;
	private MultiSelectHandler mMultiSelect;
	private static OnBookMarkAddListener mBookmarkList;
	
	private LinearLayout mPathView, mMultiSelectView;
	private GridView mGrid = null;
	private ListView mList = null;
	private boolean mShowGrid;
	
	private OpenPath mPath = null;
	private OpenPath[] mData; 
	private ArrayList<OpenPath> mData2 = null; //the data that is bound to our array adapter.
	private ArrayList<OpenPath> mHoldingFileList; //holding files waiting to be pasted(moved)
	private ArrayList<OpenPath> mHoldingZipList; //holding zip files waiting to be unzipped.
	private Context mContext;
	private FileSystemAdapter mContentAdapter;
	private ActionMode mActionMode = null;
	private boolean mActionModeSelected;
	private boolean mHoldingFile;
	private boolean mHoldingZip;
	private boolean mCutFile;
	private boolean mShowThumbnails;
	private boolean mReadyToUpdate = true;
	private int mMenuContextItemIndex = -1;
	
	public interface OnBookMarkAddListener {
		public void onBookMarkAdd(String path);
	}
	
	public ContentFragment()
	{
		Logger.LogDebug("Creating empty ContentFragment", new Exception("Creating empty ContentFragment"));
	}
	public ContentFragment(OpenPath path)
	{
		mPath = path;
	}
	
	//@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mContext = getActivity();
		
		OpenExplorer explorer = ((OpenExplorer)getActivity());
		mFileManager = explorer.getFileManager();
		if(mFileManager == null)
		{
			mFileManager = new FileManager();
			explorer.setFileManager(mFileManager);
		}
		mHandler = explorer.getEventHandler();
		if(mHandler == null)
		{
			mHandler = new EventHandler(mContext, mFileManager);
			explorer.setEventHandler(mHandler);
		}
		mHandler.setOnWorkerThreadFinishedListener(this);
		
		if(mData2 == null)
			mData2 = new ArrayList<OpenPath>();
		else
			mData2.clear();
		
		OpenPath path = mPath;
		if(path == null)
		{
			if (savedInstanceState != null && savedInstanceState.containsKey("last"))
				path = new OpenFile(savedInstanceState.getString("last"));
			else
				path = new OpenFile(Environment.getExternalStorageDirectory());
		}
		mData = mFileManager.getChildren(path);
		
		mHoldingFile = false;
		mHoldingZip = false;
		mActionModeSelected = false;
		mShowGrid = "grid".equals((PreferenceManager
									.getDefaultSharedPreferences(mContext))
										.getString("pref_view", "list"));
		mShowThumbnails = PreferenceManager.getDefaultSharedPreferences(mContext)
							.getBoolean(SettingsActivity.PREF_THUMB_KEY, true);

		if(path.getClass().equals(OpenCursor.class))
			mShowGrid = mShowThumbnails = true;
		
		OpenExplorer.setOnSettingsChangeListener(this);
		
		updateData(mData);
	}

	//@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.content_layout, container, false);
		//v.setBackgroundResource(R.color.lightgray);
		
		/*
		if (savedInstanceState != null && savedInstanceState.containsKey("location")) {
			String location = savedInstanceState.getString("location");
			if(location != null && !location.equals("") && location.startsWith("/"))
			{
				Logger.LogDebug("Content location restoring to " + location);
				mPath = new OpenFile(location);
				mData = mFileManager.getChildren(mPath);
				updateData(mData);
			}
			//setContentPath(path, false);
		}
		*/

		return v;
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		/*
		if(mPath != null && mPath.getPath() != null)
		{
			Logger.LogDebug("Content location saving to " + mPath.getPath());
			outState.putString("location", mPath.getPath());
		}
		*/
	}
	
	@Override
	public void onViewCreated(View v, Bundle savedInstanceState) {
		super.onViewCreated(v, savedInstanceState);
		
		mPathView = (LinearLayout)v.findViewById(R.id.scroll_path);
		mGrid = (GridView)v.findViewById(R.id.grid_gridview);
		mList = (ListView)v.findViewById(R.id.list_listview);
		mMultiSelectView = (LinearLayout)v.findViewById(R.id.multiselect_path);

		if(mGrid == null && mList == null)
			Logger.LogError("WTF, where are they?");
		else
			updateChosenMode(mShowGrid ? mGrid : mList);
		
		if(OpenExplorer.BEFORE_HONEYCOMB)
			registerForContextMenu(mShowGrid ? mGrid : mList);
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		new MenuInflater(mContext).inflate(R.menu.context, menu);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		executeMenu(item.getItemId(), mData2.get(mMenuContextItemIndex).getName());
		return super.onContextItemSelected(item);
	}
	
	public void updateChosenMode(AbsListView mChosenMode)
	{
		if(mGrid != null && (mShowGrid || mList == null)) {
			mContentAdapter = new FileSystemAdapter(mContext, R.layout.grid_content_layout, mData2);
			mGrid.setVisibility(View.VISIBLE);
			mGrid.setAdapter(mContentAdapter);
			if(mList != null)
				mList.setVisibility(View.GONE);
		} else if(mList != null) {
			mContentAdapter = new FileSystemAdapter(mContext, R.layout.list_content_layout, mData2);
			mList.setVisibility(View.VISIBLE);
			mList.setAdapter(mContentAdapter);
			if(mGrid != null)
				mGrid.setVisibility(View.GONE);
		}
		mChosenMode.setOnItemClickListener(this);
		mChosenMode.setOnItemLongClickListener(new OnItemLongClickListener() {
			//@Override
			public boolean onItemLongClick(AdapterView<?> list, View view ,int pos, long id) {
				if(OpenExplorer.BEFORE_HONEYCOMB)
				{
					mMenuContextItemIndex = pos;
					return false;
				}
				OpenPath file = mData2.get(pos);
				
				if(!file.isDirectory() && mActionMode == null && !mMultiSelectOn) {
					mActionMode = getActivity().startActionMode(new ActionMode.Callback() {
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
							ArrayList<OpenPath> files = new ArrayList<OpenPath>();
							
							OpenPath file = mFileManager.peekStack().getChild(mode.getTitle().toString());
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
										mHoldingFileList = new ArrayList<OpenPath>();
									
									mHoldingFileList.clear();
									mHoldingFileList.add(file);
									mHoldingFile = true;
									mCutFile = false;
									((OpenExplorer)getActivity()).changeActionBarTitle("Holding " + name);				
									mode.finish();
									return true;
									
								case F_MENU_MOVE:
									if(mHoldingFileList == null)
										mHoldingFileList = new ArrayList<OpenPath>();
									
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

//									this is for bluetooth
//									files.add(path);
//									mHandler.sendFile(files);
//									mode.finish();
//									return true;
									
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
					});
					mActionMode.setTitle(file.getName());
					
					return true;
				}
				
				if(file.isDirectory() && mActionMode == null && !mMultiSelectOn) {
					if(!OpenExplorer.BEFORE_HONEYCOMB)
					mActionMode = getActivity().startActionMode(new ActionMode.Callback() {
						
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
							mode.getMenuInflater().inflate(R.menu.context, menu);
							menu.findItem(R.id.menu_paste).setEnabled(mHoldingFile);
							menu.findItem(R.id.menu_unzip).setEnabled(mHoldingZip);
				        	
				        	mActionModeSelected = true;
							
				        	return true;
						}
						
						//@Override
						public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
							if(executeMenu(item.getItemId(), mode.getTitle().toString()))
							{
								mode.finish();
								return true;
							}
							return true;
						}
					});
					mActionMode.setTitle(file.getName());
					
					return true;
				}
				
				return false;
			}
		});
	}
	
	public boolean executeMenu(final int id, String child)
	{
		ArrayList<OpenPath> files = new ArrayList<OpenPath>();
		OpenPath file = mFileManager.peekStack().getChild(child);
		String path = file.getPath();
		String name = file.getName();
		
		switch(id) {
		case R.id.menu_bookmark:
			mBookmarkList.onBookMarkAdd(path);
			return true;
			
		case R.id.menu_delete:
			files.add(file);
			mHandler.deleteFile(files);
			return true;
			
		case R.id.menu_rename:
			mHandler.renameFile(path, true);
			return true;
			
		case R.id.menu_copy:
		case R.id.menu_cut:
			mCutFile = id == R.id.menu_cut;
			if(mHoldingFileList == null)
				mHoldingFileList = new ArrayList<OpenPath>();
			
			mHoldingFileList.clear();
			mHoldingFileList.add(file);
			mHoldingFile = true;
			((OpenExplorer)getActivity()).updateTitle("Holding " + name);
			return false;
			
		case R.id.menu_paste:
			if(mHoldingFile && mHoldingFileList.size() > 0)
				if(mCutFile)
					mHandler.cutFile(mHoldingFileList, path);
				else
					mHandler.copyFile(mHoldingFileList, path);
			
			mHoldingFile = false;
			mCutFile = false;
			mHoldingFileList.clear();
			mHoldingFileList = null;
			((OpenExplorer)getActivity()).updateTitle(path);
			return true;
			
		case R.id.menu_zip:
			mHandler.zipFile(path);
			return true;
			
		case R.id.menu_unzip:
			mHandler.unZipFileTo(mHoldingZipList.get(0), file);
			
			mHoldingZip = false;
			mHoldingZipList.clear();
			mHoldingZipList = null;
			((OpenExplorer)getActivity()).changeActionBarTitle("Open Manager");
			return true;
		
		case R.id.menu_info:
			DialogHandler dialog = DialogHandler.newDialog(DialogHandler.FILEINFO_DIALOG, mContext);
			dialog.setFilePath(path);
			dialog.show(getFragmentManager(), "info");
			return true;
		}
		return true;
	}
	
	//@Override
	public void onItemClick(AdapterView<?> list, View view, int pos, long id) {
		final OpenPath file = (OpenPath)list.getItemAtPosition(pos);
		
		//((OpenExplorer)getActivity()).hideBookmarkTitles();
		
		if(mMultiSelectOn) {
			View v;
			
			//if (mThumbnail == null)
				v = mMultiSelect.addFile(file.getPath());
			//else
			//	v = mMultiSelect.addFile(file.getPath(), mThumbnail);
			
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
			/* if (mThumbnail != null) {
				mThumbnail.setCancelThumbnails(true);
				mThumbnail = null;
			} */
			
			
			//setContentPath(file, true);
			((OpenExplorer)getActivity()).onChangeLocation(file);

		} else if (!file.isDirectory() && !mActionModeSelected ) {
			
			if(file.requiresThread())
			{
				/// TODO: handle networked files
				((OpenExplorer)getActivity()).showToast("Still need to handle this.");
				return;
			}
			
			Intent intent = IntentManager.getIntent(file, (OpenExplorer)getActivity(), mHandler);
			if(intent != null && intent.getDataString() != null && intent.getDataString() != "")
			{
				startActivity(intent);
			}
		}
	}
	
	private void updateData(final OpenPath[] items) {
		if(!mReadyToUpdate) return;
		mReadyToUpdate = false;
		
		OpenPath.Sorting = mFileManager.getSorting();
		Arrays.sort(items);
		
		mData2.clear();
		int folder_index = 0;
		for(OpenPath f : items)
		{
			if(f.isHidden() && !mFileManager.getShowHiddenFiles())
				continue;
			if(f.isDirectory())
				mData2.add(folder_index++, f);
			else
				mData2.add(f);
		}
		//Logger.LogDebug("mData has " + mData2.size());
		if(mContentAdapter != null)
			mContentAdapter.notifyDataSetChanged();
		mReadyToUpdate = true;
		/*
		mPathView.removeAllViews();
		mBackPathIndex = 0;	
		 */
	}
	
	/*
	 * (non-Javadoc)
	 * this will update the data shown to the user after a change to
	 * the file system has been made from our background thread or EventHandler.
	 */
	//@Override
	public void onWorkerThreadComplete(int type, ArrayList<String> results) {
		
		if(type == EventHandler.SEARCH_TYPE) {
			if(results == null || results.size() < 1) {
				Toast.makeText(mContext, "Sorry, zero items found", Toast.LENGTH_LONG).show();
				return;
			}
			
			DialogHandler dialog = DialogHandler.newDialog(DialogHandler.SEARCHRESULT_DIALOG, mContext);
			ArrayList<OpenPath> files = new ArrayList<OpenPath>();
			for(String s : results)
				files.add(new OpenFile(s));
			dialog.setHoldingFileList(files);
			dialog.setOnSearchFileSelected(new OnSearchFileSelected() {
				
				//@Override
				public void onFileSelected(String fileName) {
					OpenPath file = FileManager.getOpenCache(fileName);
					
					if (file.isDirectory()) {
						changePath(mFileManager.pushStack(file));
					} else {
						changePath(mFileManager.pushStack(file.getParent()));
					}						
				}
			});
			
			dialog.show(getFragmentManager(), "dialog");
			
		} else if(type == EventHandler.UNZIPTO_TYPE && results != null) {
			String name = new OpenFile(results.get(0)).getName();
			
			if(mHoldingZipList == null)
				mHoldingZipList = new ArrayList<OpenPath>();
			
			mHoldingZipList.add(new OpenFile(results.get(0)));
			mHoldingZip = true;
			((OpenExplorer)getActivity()).changeActionBarTitle("Holding " + name);
			
		} else {
			Logger.LogDebug("Worker thread complete?");
			changePath(mFileManager.peekStack(), false);
		}
	}
	
	public void changePath(OpenPath path) { changePath(path, true); }
	public void changePath(OpenPath path, Boolean addToStack) {
		if(path == null)
			path = new OpenFile(Environment.getExternalStorageDirectory());
		((OpenExplorer)getActivity()).changePath(path, addToStack);
	}
	
	//@Override
	public void onHiddenFilesChanged(boolean state) {
		mFileManager.setShowHiddenFiles(state);
		OpenPath path = mFileManager.peekStack();
		if(path != null)
			changePath(path);
	}

	//@Override
	public void onThumbnailChanged(boolean state) {
		mShowThumbnails = state;
		OpenPath path = mFileManager.peekStack();
		if(path != null)
			changePath(path);
	}
	
	//@Override
	public void onSortingChanged(SortType type) {
		mFileManager.setSorting(type);
		OpenPath path = mFileManager.peekStack();
		if(path != null)
			changePath(path);
	}
	
	public void onSortingChanged(String state) {
		if (state.equals("none"))
			mFileManager.setSorting(SortType.NONE);
		else if (state.equals("alpha"))
			mFileManager.setSorting(SortType.ALPHA);
		else if (state.equals("type"))
			mFileManager.setSorting(SortType.TYPE);
		else if (state.equals("size"))
			mFileManager.setSorting(SortType.SIZE);

		OpenPath path = mFileManager.peekStack();
		if(path != null)
			changePath(path);
	}

	//@Override
	public void onViewChanged(String state) {
		if(state.equals("list") && mShowGrid)					
			mShowGrid = false;
		else if (state.equals("grid") && !mShowGrid)
			mShowGrid = true;
		
		View v = getView();
		if(v != null)
		{
			if(mPathView == null)
				mPathView = (LinearLayout)v.findViewById(R.id.scroll_path);
			if(mGrid == null)
				mGrid = (GridView)v.findViewById(R.id.grid_gridview);
			if(mList == null)
				mList = (ListView)v.findViewById(R.id.list_listview);
			if(mMultiSelectView == null)
				mMultiSelectView = (LinearLayout)v.findViewById(R.id.multiselect_path);
		}

		if(mGrid == null && mList == null)
			Logger.LogError("WTF, where are they?");
		else
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
		return ((OpenExplorer)getActivity()).getEventHandler();
	}
	
	/*
	 * See comments for getEventHandlerInst(). Same reasoning.
	 */
	public FileManager getFileManagerInst() {
		return ((OpenExplorer)getActivity()).getFileManager();
	}
	
	/*
	 * we need to make a temp arraylist because when the
	 * multiselect actionmode callback is finished our multiselect
	 * object will turn off and clear the data in files
	 */
	public void setCopiedFiles(ArrayList<OpenPath> files, boolean cutFile) {
		ArrayList<OpenPath> temp = new ArrayList<OpenPath>(files);
		
		mHoldingFile = true;
		mCutFile = cutFile;
		mHoldingFileList = temp;
	}
	
	/**
	 * 
	 */
	public class FileSystemAdapter extends ArrayAdapter<OpenPath> {
		private final int KB = 1024;
    	private final int MG = KB * KB;
    	private final int GB = MG * KB;
    	
		private BookmarkHolder mHolder;
		private String mName;
		private ThumbnailCreator mThumbnail;
		
		public FileSystemAdapter(Context context, int layout, ArrayList<OpenPath> data) {
			super(context, layout, data);
			mThumbnail = new ThumbnailCreator(mContext, handler);
		}
		
		@Override protected void finalize() throws Throwable { mThumbnail.setCancelThumbnails(true); };
		
		@Override
		public void notifyDataSetChanged() {
			//Logger.LogDebug("Data set changed.");
			try {
				//if(mFileManager != null)
				//	((OpenExplorer)getActivity()).updateTitle(mFileManager.peekStack().getPath());
				super.notifyDataSetChanged();
			} catch(NullPointerException npe) {
				Logger.LogError("Null found while notifying data change.", npe);
			}
		}
		
		private final Handler handler = new Handler(new Handler.Callback() {
			public boolean handleMessage(Message msg) {
				notifyDataSetChanged();
				return true;
			}
		});
		
		////@Override
		public View getView(int position, View view, ViewGroup parent)
		{
			final OpenPath file = super.getItem(position);
			final String mName = file.getName();
			final String ext = mName.substring(mName.lastIndexOf(".") + 1);
			
			int mWidth = 36, mHeight = 36;
			if(mShowGrid)
				mWidth = mHeight = 72;
			
			if(view == null) {
				LayoutInflater in = (LayoutInflater)mContext
										.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				
				view = in.inflate(mShowGrid ? R.layout.grid_content_layout : R.layout.list_content_layout, parent, false);
				
				mHolder = new BookmarkHolder(file, mName, view);
				
				view.setTag(mHolder);
				
			} else {
				mHolder = (BookmarkHolder)view.getTag();
			}

			if(!mShowGrid) {
				mHolder.setInfo(getFileDetails(file));
				mHolder.setPath(file.getPath());
			}
			
			if(file.getClass().equals(OpenMediaStore.class))
				mHolder.showPath(true);
			else
				mHolder.showPath(false);
			
			mHolder.setText(mName);
			
			/* assign custom icons based on file type */
			if(file.isDirectory()) {
				OpenPath[] lists = null;
				if(!file.requiresThread())
					lists = file.list();
				
				if(file.canRead() && lists != null && lists.length > 0)
					mHolder.setIconResource(R.drawable.folder_large_full);
				else
					mHolder.setIconResource(R.drawable.folder);
			} else if(ext.equalsIgnoreCase("doc") || ext.equalsIgnoreCase("docx")) {
				mHolder.setIconResource(R.drawable.doc);
				
			} else if(ext.equalsIgnoreCase("xls")  || 
					  ext.equalsIgnoreCase("xlsx") ||
					  ext.equalsIgnoreCase("xlsm")) {
				mHolder.setIconResource(R.drawable.excel);
				
			} else if(ext.equalsIgnoreCase("ppt") || ext.equalsIgnoreCase("pptx")) {
				mHolder.setIconResource(R.drawable.powerpoint);
				
			} else if(ext.equalsIgnoreCase("zip") || ext.equalsIgnoreCase("gzip")) {
				mHolder.setIconResource(R.drawable.zip);
				
			} else if (ext.equalsIgnoreCase("rar")) {
				mHolder.setIconResource(R.drawable.rar);
				
			//} else if(ext.equalsIgnoreCase("apk")) {
			//	mHolder.setIconResource(R.drawable.apk);
				
			} else if(ext.equalsIgnoreCase("pdf")) {
				mHolder.setIconResource(R.drawable.pdf);
				
			} else if(ext.equalsIgnoreCase("xml") || ext.equalsIgnoreCase("html")) {
				mHolder.setIconResource(R.drawable.xml_html);
				
			} else if(ext.equalsIgnoreCase("mp3") || ext.equalsIgnoreCase("wav") ||
					  ext.equalsIgnoreCase("wma") || ext.equalsIgnoreCase("m4p") ||
					  ext.equalsIgnoreCase("m4a") || ext.equalsIgnoreCase("ogg")) {
				mHolder.setIconResource(R.drawable.music);
			} else if(ext.equalsIgnoreCase("jpeg") || ext.equalsIgnoreCase("png") ||
					  ext.equalsIgnoreCase("apk")  ||
					  ext.equalsIgnoreCase("jpg")  || ext.equalsIgnoreCase("gif") ||
					  ext.equalsIgnoreCase("mp4") || 
					  ext.equalsIgnoreCase("3gp") || 
					  ext.equalsIgnoreCase("avi") ||
					  ext.equalsIgnoreCase("webm") || 
					  ext.equalsIgnoreCase("m4v"))
			{

				if(mShowThumbnails) {
					if(ext.equalsIgnoreCase("mp4") || 
						  ext.equalsIgnoreCase("3gp") || 
						  ext.equalsIgnoreCase("avi") ||
						  ext.equalsIgnoreCase("webm") || 
						  ext.equalsIgnoreCase("m4v")) {
						mHolder.setIconResource(R.drawable.movie);
					} else if(ext.equals("apk")) {
						mHolder.setIconResource(R.drawable.apk);
					} else {
						mHolder.setIconResource(R.drawable.photo);
					}
					
					Bitmap thumb = mThumbnail.isBitmapCached(file.getPath());

					if (thumb == null) {
						
										
						//mThumbnail.createNewThumbnail(mData2, file.getParent().getPath(), handle);
						//mThumbnail.createNewThumbnail(file, mWidth, mHeight);
						//thumb = ThumbnailCreator.generateThumb(file, mWidth, mHeight).get();
						new ThumbnailTask().execute(new ThumbnailStruct(file, mHolder, mWidth, mHeight));
						
						/*
						try {
							if (!mThumbnail.isAlive()) 
								mThumbnail.start();
						} catch(IllegalThreadStateException itse) {
							Logger.LogError("Unable to start thumbnail cache thread.", itse);
						}
						*/
						
					}
					if(thumb != null)
					{
						BitmapDrawable bd = new BitmapDrawable(thumb);
						bd.setGravity(Gravity.CENTER);
						mHolder.setIconDrawable(bd);
					}
				
				} else if(ext.equalsIgnoreCase("mp4") || 
					  ext.equalsIgnoreCase("3gp") || 
					  ext.equalsIgnoreCase("avi") ||
					  ext.equalsIgnoreCase("webm") || 
					  ext.equalsIgnoreCase("m4v")) {
					mHolder.setIconResource(R.drawable.movie);
				} else if(ext.equals("apk")) {
					mHolder.setIconResource(R.drawable.apk);
				} else {
					mHolder.setIconResource(R.drawable.photo);
				}
				
			} else if(file.getPath() != null && file.getPath().indexOf("ftp:/") > -1) {
				
				OpenFTP f = FTPManager.getFTPFile(mName);
				if(f != null)
				{
					if(f.isDirectory())
						mHolder.setIconResource(R.drawable.folder);
					else
						mHolder.setIconResource(R.drawable.unknown);
				} else
					mHolder.setIconResource(R.drawable.unknown);
			} else
				mHolder.setIconResource(R.drawable.unknown);
			
			return view;
		}
		
		private String getFilePath(String name) {
			return mFileManager.peekStack().getChild(name).getPath();
		}
		private String getFileDetails(OpenPath file) {
			//OpenPath file = mFileManager.peekStack().getChild(name); 
			String t = ""; //file.getPath() + "\t\t";
			double bytes;
			String size = "";
			String atrs = " | - ";
			
			if(file.isDirectory() && !file.requiresThread()) {
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
		
		public class ThumbnailStruct
		{
			public OpenPath File;
			public int Width = 0, Height = 0;
			public BookmarkHolder Holder;
			private SoftReference<Bitmap> mBitmap; 
			//public Handler Handler;
			public ThumbnailStruct(OpenPath path, BookmarkHolder holder, int width, int height)
			{
				File = path;
				Holder = holder;
				//Handler = handler;
				Width = width;
				Height = height;
			}
			public void setBitmap(SoftReference<Bitmap> thumb)
			{
				mBitmap = thumb;
			}
			public void updateHolder()
			{
				if(Holder != null && mBitmap != null && mBitmap.get() != null)
					Holder.setIconDrawable(new BitmapDrawable(mBitmap.get()));
			}
		}
		
		public class ThumbnailTask extends AsyncTask<ThumbnailStruct, Void, ThumbnailStruct[]>
		{
			private int iPending = 0;
			
			public ThumbnailTask() {
				
			}
			
			@Override
			protected ThumbnailStruct[] doInBackground(ThumbnailStruct... params) {
				ThumbnailStruct[] ret = new ThumbnailStruct[params.length];
				for(int i = 0; i < params.length; i++)
				{
					ret[i] = params[i];
					ret[i].setBitmap(ThumbnailCreator.generateThumb(ret[i].File, ret[i].Width, ret[i].Height));
				}
				return ret;
			}
			
			@Override
			protected void onPostExecute(ThumbnailStruct[] result) {
				super.onPostExecute(result);
				for(ThumbnailStruct t : result)
					t.updateHolder();
			}
			
		}
	}

	public enum FileIOCommandType
	{
		ALL
	}
	public class FileIOCommand
	{
		public FileIOCommandType Type;
		public OpenPath Path;
		
		public FileIOCommand(FileIOCommandType type, OpenPath path)
		{
			Type = type;
			Path = path;
		}
	}
	
	public class FileIOTask extends AsyncTask<FileIOCommand, Integer, OpenPath[]>
	{
		@Override
		protected OpenPath[] doInBackground(FileIOCommand... params) {
			ArrayList<OpenPath> ret = new ArrayList<OpenPath>();
			for(FileIOCommand cmd : params)
			{
				if(cmd.Path.requiresThread())
				{
					OpenFTP file = (OpenFTP)FileManager.getOpenCache(cmd.Path.getPath(), true);
					OpenPath[] list = file.list();
					if(list != null)
						for(OpenPath f : list)
							ret.add(f);
				} else {
					for(OpenPath f : cmd.Path.list())
						ret.add(f);
				}
				mFileManager.pushStack(cmd.Path);
			}
			Logger.LogDebug("Found " + ret.size() + " items.");
			OpenPath[] ret2 = new OpenPath[ret.size()];
			ret.toArray(ret2);
			return ret2;
		}
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			//mData.clear();
		}
		
		@Override
		protected void onPostExecute(OpenPath[] result)
		{
			mData2.clear();
			updateData(result);
		}
		
	}
}


