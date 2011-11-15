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

import org.brandroid.openmanager.OpenExplorer;
import org.brandroid.openmanager.R;
import org.brandroid.openmanager.SettingsActivity;
import org.brandroid.openmanager.OpenExplorer.OnSettingsChangeListener;
import org.brandroid.openmanager.data.BookmarkHolder;
import org.brandroid.openmanager.data.OpenCursor;
import org.brandroid.openmanager.data.OpenFTP;
import org.brandroid.openmanager.data.OpenMediaStore;
import org.brandroid.openmanager.data.OpenPath;
import org.brandroid.openmanager.data.OpenFile;
import org.brandroid.openmanager.fragments.DialogHandler.DialogType;
import org.brandroid.openmanager.fragments.DialogHandler.OnSearchFileSelected;
import org.brandroid.openmanager.ftp.FTPManager;
import org.brandroid.openmanager.util.EventHandler;
import org.brandroid.openmanager.util.FileManager;
import org.brandroid.openmanager.util.IntentManager;
import org.brandroid.openmanager.util.MultiSelectHandler;
import org.brandroid.openmanager.util.ThumbnailCreator;
import org.brandroid.openmanager.util.EventHandler.OnWorkerThreadFinishedListener;
import org.brandroid.openmanager.util.FileManager.SortType;
import org.brandroid.utils.Logger;

import java.io.File;
import java.lang.ref.SoftReference;
import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.StatFs;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.IntentSender.SendIntentException;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.BaseExpandableListAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.net.Uri;

public class ContentFragment extends Fragment implements OnItemClickListener,
															OnSettingsChangeListener,
															OnWorkerThreadFinishedListener{
	
	public static final boolean USE_ACTIONMODE = true;
	private static boolean mMultiSelectOn = false;
	
	private FileManager mFileManager;
	private EventHandler mHandler;
	private MultiSelectHandler mMultiSelect;
	private static OnBookMarkAddListener mBookmarkList;
	
	//private LinearLayout mPathView;
	private LinearLayout mMultiSelectView;
	private GridView mGrid = null;
	private ListView mList = null;
	
	private OpenPath mPath = null;
	private static OpenPath mLastPath = null;
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
	private int mListScrollingState = 0;
	private int mListVisibleStartIndex = 0;
	private int mListVisibleLength = 0; 
	
	public interface OnBookMarkAddListener {
		public void onBookMarkAdd(String path);
	}
	
	public ContentFragment()
	{
		//Logger.LogDebug("Creating empty ContentFragment", new Exception("Creating empty ContentFragment"));
		mPath = mLastPath;
	}
	public ContentFragment(OpenPath path)
	{
		mPath = mLastPath = path;
	}
	
	private int getViewMode() { return ((OpenExplorer)getActivity()).getViewMode(); }
	
	//@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if(savedInstanceState != null && savedInstanceState.containsKey("last"))
			mPath = mLastPath = new OpenFile(savedInstanceState.getString("last"));
		
		if(mPath == null)
			Logger.LogDebug("Creating empty ContentFragment", new Exception("Creating empty ContentFragment"));
		
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
			mHandler = new EventHandler(mFileManager);
			explorer.setEventHandler(mHandler);
		}
		mHandler.setOnWorkerThreadFinishedListener(this);
		
		if(mData2 == null)
			mData2 = new ArrayList<OpenPath>();
		else
			mData2.clear();
		
		OpenPath path = mPath;
		if(path == null) path = mLastPath;
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
		try {
			mShowThumbnails = ((OpenExplorer)getActivity()).getPreferences()
								.getSetting(mPath.getPath(), SettingsActivity.PREF_THUMB_KEY, true);
		} catch(NullPointerException npe) {
			mShowThumbnails = true;
		}

		if(path.getClass().equals(OpenCursor.class) && !OpenExplorer.BEFORE_HONEYCOMB)
			mShowThumbnails = true;
		
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
		
		//mPathView = (LinearLayout)v.findViewById(R.id.scroll_path);
		mGrid = (GridView)v.findViewById(R.id.grid_gridview);
		mList = (ListView)v.findViewById(R.id.list_listview);
		mMultiSelectView = (LinearLayout)v.findViewById(R.id.multiselect_path);

		if(mGrid == null && mList == null)
			Logger.LogError("WTF, where are they?");
		else if (getViewMode() == OpenExplorer.VIEW_GRID)
			updateChosenMode(mGrid);
		else
			updateChosenMode(mList);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		return executeMenu(item.getItemId(), mData2.get(mMenuContextItemIndex));
		//return super.onContextItemSelected(item);
	}
	
	public void updateChosenMode(AbsListView mChosenMode)
	{
		if(getViewMode() == OpenExplorer.VIEW_GRID) {
			mContentAdapter = new FileSystemAdapter(mContext, R.layout.grid_content_layout, mData2);
			mList.setVisibility(View.GONE);
			mGrid.setVisibility(View.VISIBLE);
			mGrid.setAdapter(mContentAdapter);
		} else {
			mContentAdapter = new FileSystemAdapter(mContext, R.layout.list_content_layout, mData2);
			mGrid.setVisibility(View.GONE);
			mList.setVisibility(View.VISIBLE);
			mList.setAdapter(mContentAdapter);
		}
		mChosenMode.setVisibility(View.VISIBLE);
		mChosenMode.setOnItemClickListener(this);
		mChosenMode.setOnCreateContextMenuListener(this);
		mChosenMode.setOnScrollListener(new OnScrollListener() {
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				mListScrollingState = scrollState;
				if(scrollState == 0)
					onScrollStopped(view);
			}
			
			public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount)
			{
				if(firstVisibleItem != mListVisibleStartIndex)
					mListVisibleStartIndex = firstVisibleItem;
				if(visibleItemCount != mListVisibleLength)
					mListVisibleLength = visibleItemCount;
			}
		});
		mChosenMode.setOnItemLongClickListener(new OnItemLongClickListener() {
			//@Override
			public boolean onItemLongClick(AdapterView<?> list, View view ,int pos, long id) {
				if(OpenExplorer.BEFORE_HONEYCOMB || !USE_ACTIONMODE) {
					Logger.LogDebug("Showing context?");
					mMenuContextItemIndex = pos;
					return list.showContextMenu();
				}
				final OpenPath file = mData2.get(pos);
				
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
							mode.getMenuInflater().inflate(R.menu.context_file, menu);
				    		
				    		mActionModeSelected = true;
							return true;
						}

						//@Override
						public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
							//ArrayList<OpenPath> files = new ArrayList<OpenPath>();
							
							//OpenPath file = mLastPath.getChild(mode.getTitle().toString());
							//files.add(file);
							
							if(item.getItemId() != R.id.menu_cut && item.getItemId() != R.id.menu_multi && item.getItemId() != R.id.menu_copy)
							{
								mode.finish();
								mActionModeSelected = false;
							}
							return executeMenu(item.getItemId(), mode, file);
						}
					});
					mActionMode.setTitle(file.getName());
					
					return true;
				}
				
				if(file.isDirectory() && mActionMode == null && !mMultiSelectOn) {
					if(!OpenExplorer.BEFORE_HONEYCOMB && USE_ACTIONMODE)
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
							mode.getMenuInflater().inflate(R.menu.context_dir, menu);
							menu.findItem(R.id.menu_paste).setEnabled(mHoldingFile);
							menu.findItem(R.id.menu_unzip).setEnabled(mHoldingZip);
				        	
				        	mActionModeSelected = true;
							
				        	return true;
						}
						
						//@Override
						public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
							return executeMenu(item.getItemId(), mode, file);
						}
					});
					mActionMode.setTitle(file.getName());
					
					return true;
				}
				
				return false;
			}
		});
		if(OpenExplorer.BEFORE_HONEYCOMB && USE_ACTIONMODE)
			registerForContextMenu(mChosenMode);
	}
	
	protected void onScrollStopped(AbsListView view)
	{
		boolean skipThis = true;
		if(skipThis) return;
		int start = Math.max(0, mListVisibleStartIndex);
		int end = Math.min(mData2.size() - 1, mListVisibleStartIndex + mListVisibleLength);
		int mWidth = 96;
		int mHeight = 96;
		//ThumbnailStruct[] thumbs = ThumbnailStruct[end - start];
		for(int i = start; i < end; i++)
		{
			Object o = view.getItemAtPosition(i);
			if(o != null)
			{
				OpenPath file = (OpenPath)o;
				if(file.getTag() != null && file.getTag().getClass().equals(BookmarkHolder.class))
				{
					BookmarkHolder mHolder = (BookmarkHolder)file.getTag();
					ImageView v = mHolder.getIconView();
					//thumbs[i - start] = new ThumbnailStruct(file, mHolder, mWidth, mHeight);
					new ThumbnailTask().execute(new ThumbnailStruct(file, mHolder, mWidth, mHeight));
				}
			}
			//view.getItemAtPosition(i);
		}
		//Logger.LogDebug("Visible items " + mData2.get(mListVisibleStartIndex).getName() + " - " + mData2.get().getName());
	}
	
	private void finishMode(Object mode)
	{
		if(!OpenExplorer.BEFORE_HONEYCOMB && mode != null)
			((ActionMode)mode).finish();
	}
	
	public boolean executeMenu(final int id, OpenPath file)
	{
		return executeMenu(id, null, file);
	}
	public boolean executeMenu(final int id, Object mode, OpenPath file)
	{
		ArrayList<OpenPath> files = new ArrayList<OpenPath>();
		files.add(file);
		return executeMenu(id, mode, files);
	}
	public boolean executeMenu(final int id, final Object mode, ArrayList<OpenPath> files)
	{
		final OpenPath file = files.get(0);
		final String path = file != null ? file.getPath() : null;
		final OpenPath folder = file != null ? file.getParent() : null;
		String name = file != null ? file.getName() : null;
		
		switch(id) {
			case R.id.menu_multi:
				changeMultiSelectState(!mMultiSelectOn, MultiSelectHandler.getInstance(mContext));
				return true;
			case R.id.menu_bookmark:
				mBookmarkList.onBookMarkAdd(path);
				finishMode(mode);
				return true;
				
			case R.id.menu_delete:
				mHandler.deleteFile(files, getActivity());
				finishMode(mode);
				mContentAdapter.notifyDataSetChanged();
				return true;
				
			case R.id.menu_rename:
				mHandler.renameFile(path, true, getActivity());
				finishMode(mode);
				return true;
				
			case R.id.menu_copy:
			case R.id.menu_cut:
				mCutFile = id == R.id.menu_cut;
				if(mHoldingFileList == null)
					mHoldingFileList = new ArrayList<OpenPath>();
				
				if(!mMultiSelectOn)
					mHoldingFileList.clear();
				mHoldingFileList.add(file);
				mHoldingFile = true;
				if(mHoldingFileList.size() == 1)
					((OpenExplorer)getActivity()).showToast("Tap the upper left corner to see your held files");
				((OpenExplorer)getActivity()).updateTitle("Holding " + (mHoldingFileList.size() == 1 ? name : mHoldingFileList.size() + " files"));
				return false;
				
			case R.id.menu_paste:
				if(mHoldingFile && mHoldingFileList.size() > 0)
					if(mCutFile)
						mHandler.cutFile(mHoldingFileList, path, getActivity());
					else
						mHandler.copyFile(mHoldingFileList, path, getActivity());
				
				mHoldingFile = false;
				mCutFile = false;
				mHoldingFileList.clear();
				mHoldingFileList = null;
				((OpenExplorer)getActivity()).updateTitle(path);
				finishMode(mode);
				return true;
				
			case R.id.menu_zip:
				if(mHoldingFileList == null)
					mHoldingFileList = new ArrayList<OpenPath>();
				mHoldingFileList.add(file);
				final String def = mHoldingFileList.size() == 1 ?
						file.getName() + ".zip" :
						file.getParent().getName() + ".zip";
				
				final DialogBuilder dZip = new DialogBuilder(mContext);
				dZip
					.setMessage("Enter filename of new Zip file:")
					.setDefaultText(def)
					.setIcon(getResources().getDrawable(R.drawable.zip))
					.setTitle("Zip")
					.setCancelable(true)
					.setPositiveButton("OK",
						new OnClickListener() {
							public void onClick(DialogInterface di, int which) {
								if(which != DialogInterface.BUTTON_POSITIVE) return;
								OpenPath[] toZip = new OpenPath[mHoldingFileList.size() + 1];
								toZip[0] = folder.getChild(dZip.getInputText());
								for(int i = 0; i < mHoldingFileList.size(); i++)
									toZip[i + 1] = mHoldingFileList.get(i);
								Logger.LogInfo("Zipping " + (toZip.length == 2 ? toZip[toZip.length - 1].getPath() : (toZip.length - 1) + " files") + " to " + toZip[0].getPath());
								mHandler.zipFile(toZip, getActivity());
								finishMode(mode);
							}
						})
					.create().show();
				return true;
				
			case R.id.menu_unzip:
				mHandler.unZipFileTo(mHoldingZipList.get(0), file, getActivity());
				
				mHoldingZip = false;
				mHoldingZipList.clear();
				mHoldingZipList = null;
				((OpenExplorer)getActivity()).updateTitle("");
				return true;
			
			case R.id.menu_info:
				((OpenExplorer)getActivity()).showFileInfo(file);
				finishMode(mode);
				return true;
				
			case R.id.menu_share:
				
				// TODO: WTF is this?
				Intent mail = new Intent();
				mail.setType("application/mail");
				
				mail.setAction(android.content.Intent.ACTION_SEND);
				mail.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(path)));
				startActivity(mail);
				
				//mode.finish();
				return true;
	
	//			this is for bluetooth
	//			files.add(path);
	//			mHandler.sendFile(files);
	//			mode.finish();
	//			return true;
			}
		return true;
	}
		
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		if(!OpenExplorer.BEFORE_HONEYCOMB && USE_ACTIONMODE) return;
		AdapterContextMenuInfo info = (AdapterContextMenuInfo)menuInfo;
		OpenPath file = mData2.get(info != null ? info.position : mMenuContextItemIndex);
		int menuResId = R.menu.context_file;
		if(file.isDirectory())
			menuResId = R.menu.context_dir;
		if(mMultiSelectOn)
			menuResId = R.menu.context_multi;
		new MenuInflater(mContext).inflate(menuResId, menu);
		if(menuResId == R.menu.context_dir)
		{
			menu.findItem(R.id.menu_paste).setEnabled(mHoldingFile);
			menu.findItem(R.id.menu_unzip).setEnabled(mHoldingZip);
		}
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
			
			IntentManager.startIntent(file, (OpenExplorer)getActivity(), mHandler);
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
			
			DialogHandler dialog = DialogHandler.newDialog(DialogHandler.DialogType.SEARCHRESULT_DIALOG, mContext);
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
			((OpenExplorer)getActivity()).updateTitle("Holding " + name);
			
		} else {
			Logger.LogDebug("Worker thread complete?");
			mContentAdapter.notifyDataSetChanged();
			//changePath(mFileManager.peekStack(), false);
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
	public void onViewChanged(int state) {
		//mViewMode = state;
		((OpenExplorer)getActivity()).setViewMode(state);
		
		View v = getView();
		if(v != null)
		{
			//if(mPathView == null)
			//	mPathView = (LinearLayout)v.findViewById(R.id.scroll_path);
			if(mGrid == null)
				mGrid = (GridView)v.findViewById(R.id.grid_gridview);
			if(mList == null)
				mList = (ListView)v.findViewById(R.id.list_listview);
			if(mMultiSelectView == null)
				mMultiSelectView = (LinearLayout)v.findViewById(R.id.multiselect_path);
		}

		if(mGrid == null && mList == null)
			Logger.LogError("WTF, where are they?");
		else if (getViewMode() == OpenExplorer.VIEW_GRID)
			updateChosenMode(mGrid);
		else
			updateChosenMode(mList);
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
			if(getViewMode() == OpenExplorer.VIEW_GRID)
				mWidth = mHeight = 96;
			
			if(view == null) {
				LayoutInflater in = (LayoutInflater)mContext
										.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				
				view = in.inflate(getViewMode() == OpenExplorer.VIEW_GRID ? R.layout.grid_content_layout : R.layout.list_content_layout, parent, false);
				
				mHolder = new BookmarkHolder(file, mName, view);
				
				view.setTag(mHolder);
				
			} else {
				mHolder = (BookmarkHolder)view.getTag();
				mHolder.cancelTask();
			}

			if(getViewMode() == OpenExplorer.VIEW_LIST) {
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
			} else if(ext.equalsIgnoreCase("jpeg")|| ext.equalsIgnoreCase("png") ||
					  ext.equalsIgnoreCase("jpg") || ext.equalsIgnoreCase("gif") ||
					  ext.equalsIgnoreCase("bmp") ||
					  ext.equalsIgnoreCase("apk") ||
					  ext.equalsIgnoreCase("mp4") || 
					  ext.equalsIgnoreCase("3gp") || 
					  ext.equalsIgnoreCase("avi") ||
					  ext.equalsIgnoreCase("webm")|| 
					  ext.equalsIgnoreCase("m4v"))
			{

				if(mShowThumbnails) {

					Bitmap thumb = ThumbnailCreator.isBitmapCached(file.getPath(), mWidth, mHeight);
					
					if(thumb == null)
					{
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
						
						file.setTag(mHolder);
						
						ThumbnailTask task = new ThumbnailTask();
						mHolder.setTask(task);
						task.execute(new ThumbnailStruct(file, mHolder, mWidth, mHeight));
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
			String deets = ""; //file.getPath() + "\t\t";
			
			if(file.isDirectory() && !file.requiresThread()) {
				deets = file.list().length + " items";
			} else {
				deets = DialogHandler.formatSize(file.length());
			}
			
			deets += " | ";
			
			DateFormat df = new SimpleDateFormat("MM-dd-yyyy HH:mm");
			deets += df.format(file.lastModified());
			
			deets += " | ";
			
			deets += (file.isDirectory()?"d":"-");
			deets += (file.canRead()?"r":"-");
			deets += (file.canWrite()?"w":"-");
			deets += (file.canExecute()?"x":"-");
			
			return deets;
		}
		
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
			{
				BitmapDrawable bd = new BitmapDrawable(mBitmap.get());
				bd.setGravity(Gravity.CENTER);
				Holder.setIconDrawable(bd, this);
			}
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
				if(ret == null) continue;
				//Logger.LogDebug("Getting thumb for " + ret[i].File.getName());
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
	public class DialogBuilder extends Builder
	{
		private View view;
		private EditText mEdit, mEdit2;
		
		public DialogBuilder(Context mContext) {
			super(mContext);
			
			LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			view = inflater.inflate(R.layout.input_dialog_layout, null);
			super.setView(view);
			mEdit = (EditText)view.findViewById(R.id.dialog_input);
			mEdit2 = (EditText)view.findViewById(R.id.dialog_input_top);
			if(mEdit == null)
				mEdit = mEdit2;
		}
		
		public String getInputText() { return mEdit.getText().toString(); }
		public DialogBuilder setPrompt(String s) {
			((EditText)view.findViewById(R.id.dialog_message_top)).setText(s);
			return this;
		}
		
		public DialogBuilder setDefaultText(String s) 
		{
			mEdit.setText(s);
			return this;
		}
		
		@Override
		public DialogBuilder setMessage(CharSequence message) {
			super.setMessage(message);
			return this;
		}
		
		@Override
		public DialogBuilder setTitle(CharSequence title) {
			super.setTitle(title);
			return this;
		}
		
		@Override
		public DialogBuilder setIcon(Drawable icon) {
			super.setIcon(icon);
			return this;
		}
	}
}


