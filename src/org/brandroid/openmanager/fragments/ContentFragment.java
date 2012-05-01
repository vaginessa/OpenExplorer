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

import org.brandroid.openmanager.R;
import org.brandroid.openmanager.activities.OpenExplorer;
import org.brandroid.openmanager.adapters.ContentAdapter;
import org.brandroid.openmanager.data.OpenContent;
import org.brandroid.openmanager.data.OpenNetworkPath;
import org.brandroid.openmanager.data.OpenPath;
import org.brandroid.openmanager.data.OpenFile;
import org.brandroid.openmanager.data.OpenSFTP;
import org.brandroid.openmanager.fragments.DialogHandler.OnSearchFileSelected;
import org.brandroid.openmanager.util.EventHandler;
import org.brandroid.openmanager.util.NetworkIOTask;
import org.brandroid.openmanager.util.NetworkIOTask.OnTaskUpdateListener;
import org.brandroid.openmanager.util.FileManager;
import org.brandroid.openmanager.util.IntentManager;
import org.brandroid.openmanager.util.RootManager;
import org.brandroid.openmanager.util.EventHandler.OnWorkerUpdateListener;
import org.brandroid.openmanager.util.FileManager.SortType;
import org.brandroid.openmanager.util.ThumbnailCreator;
import org.brandroid.utils.Logger;
import org.brandroid.utils.MenuBuilder;
import org.brandroid.utils.MenuUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;

import android.net.Uri;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcel;
import android.os.Parcelable;
import android.annotation.TargetApi;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.SearchView;
import android.widget.Toast;
import android.widget.GridView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

public class ContentFragment extends OpenFragment
		implements OnItemClickListener, OnItemLongClickListener,
					OnWorkerUpdateListener, OpenPathFragmentInterface,
					OnTaskUpdateListener
{
	
	//private static MultiSelectHandler mMultiSelect;
	//private LinearLayout mPathView;
	//private SlidingDrawer mMultiSelectDrawer;
	//private GridView mMultiSelectView;
	protected GridView mGrid = null;
	//private View mProgressBarLoading = null;
	
	private OpenPath[] mData; 
	private ArrayList<OpenPath> mData2 = null; //the data that is bound to our array adapter.
	private boolean mShowThumbnails = true;
	private boolean mReadyToUpdate = true;
	private boolean mShowHiddenFiles = false;
	private boolean mFoldersFirst = true;
	private SortType mSorting = SortType.ALPHA;
	private int mListScrollingState = 0;
	private int mListVisibleStartIndex = 0;
	private int mListVisibleLength = 0; 
	private int mListScrollY = 0;
	public static float DP_RATIO = 1;
	public static int mGridImageSize = 128;
	public static int mListImageSize = 36;
	public Boolean mShowLongDate = false;
	private int mTopIndex = 0;
	private OpenPath mTopPath = null;
	private OpenPath mPath = null;
	private OnPathChangeListener mPathListener = null;
	
	private Bundle mBundle;
	
	protected Integer mViewMode = null;
	
	//private static Hashtable<OpenPath, ContentFragment> instances = new Hashtable<OpenPath, ContentFragment>();
	
	public interface OnPathChangeListener
	{
		public void changePath(OpenPath newPath);
	}

	public ContentFragment() {
		if(getArguments() != null && getArguments().containsKey("last"))
		{
			Logger.LogDebug("ContentFragment Restoring to " + getArguments().getString("last"));
			setPath(getArguments().getString("last"));
		}
	}
	private ContentFragment(OpenPath path)
	{
		mPath = path;
	}
	private ContentFragment(OpenPath path, int view)
	{
		mPath = path;
		mViewMode = view;
	}
	private void setPath(String path)
	{
		if(path.startsWith("content://"))
			mPath = new OpenContent(Uri.parse(path), getActivity());
		else
			try {
				mPath = FileManager.getOpenCache(path, false, null);
			} catch (IOException e) {
				mPath = new OpenFile(path);
			}
	}
	public static ContentFragment getInstance(OpenPath path, int mode)
	{
		ContentFragment ret = new ContentFragment(path, mode);
		if(path instanceof OpenFile) return ret;
		Bundle args = ret.getArguments();
		if(args == null)
			args = new Bundle();
		if(path != null)
		{
			args.putString("last", path.getPath());
			ret.setArguments(args);
		} else return null;
		//Logger.LogVerbose("ContentFragment.getInstance(" + path.getPath() + ", " + mode + ")");
		return ret;
	}
	public static ContentFragment getInstance(OpenPath path)
	{
		ContentFragment ret = new ContentFragment(path);
		Bundle args = ret.getArguments();
		if(args == null)
			args = new Bundle();
		if(path != null)
		{
			args.putString("last", path.getPath());
			ret.setArguments(args);
		} else return null;
		//Logger.LogVerbose("ContentFragment.getInstance(" + path.getPath() + ")");
		return ret;
	}

	public static void cancelAllTasks()
	{
		NetworkIOTask.cancelAllTasks();
	}
	
	public int getViewMode() {
		if(mViewMode == null)
			mViewMode = getViewSetting(mPath, "view", getGlobalViewMode());
		return mViewMode;
	}
	public int getGlobalViewMode() {
		String pref = getSetting(null, "pref_view", "list");
		if(pref.equals("list"))
			return OpenExplorer.VIEW_LIST;
		if(pref.equals("grid"))
			return OpenExplorer.VIEW_GRID;
		if(pref.equals("carousel"))
			return OpenExplorer.VIEW_CAROUSEL;
		return OpenExplorer.VIEW_LIST;
	}

	
	private void setViewMode(int mode) {
		mViewMode = mode;
		setViewSetting(mPath, "view", mode);
		Logger.LogVerbose("Content View Mode: " + mode);
		if(mContentAdapter != null)
		{
			if(ContentAdapter.class.equals(mContentAdapter.getClass()))
			{
				mGrid.setAdapter(null);
				mContentAdapter = new ContentAdapter(getExplorer(), mViewMode, mData2);
				((ContentAdapter)mContentAdapter).setViewMode(getViewMode());
				//mContentAdapter = new OpenPathAdapter(mPath, mode, getExplorer());
				mGrid.setAdapter(mContentAdapter);
			}
		}
	}
	
	//@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setHasOptionsMenu(true);
		
		DP_RATIO = getResources().getDimension(R.dimen.one_dp);
		mGridImageSize = (int) (DP_RATIO * getResources().getInteger(R.integer.content_grid_image_size));
		mListImageSize = (int) (DP_RATIO * getResources().getInteger(R.integer.content_list_image_size));
		if(savedInstanceState != null)
			mBundle = savedInstanceState;
		if(getArguments() != null && getArguments().containsKey("last"))
			mBundle = getArguments();
		if(mBundle != null && mBundle.containsKey("last") && (mPath == null || mPath.getPath().equals(mBundle.getString("last"))))
		{
			String last = mBundle.getString("last");
			Logger.LogDebug("Unbundling " + last);
			if(last.startsWith("content://"))
				mPath = new OpenContent(Uri.parse(last), getApplicationContext());
			else if(last.startsWith("/"))
				mPath = new OpenFile(last).setRoot();
			else if(last.equalsIgnoreCase("Photos"))
				mPath = OpenExplorer.getPhotoParent();
			else if(last.equalsIgnoreCase("Videos"))
				mPath = OpenExplorer.getVideoParent();
			else if(last.equalsIgnoreCase("Music"))
				mPath = OpenExplorer.getMusicParent();
			else {
				try {
					mPath = FileManager.getOpenCache(last, false, mSorting);
				} catch (IOException e) {
					Logger.LogWarning("Couldn't get Cache in Fragment", e);
					if(last.startsWith("sftp:/"))
						mPath = new OpenSFTP(last);
					mPath = new OpenFile(last);
				}
			}
		}
		if(mBundle != null && mBundle.containsKey("view"))
			mViewMode = mBundle.getInt("view");
		
		if(mPath == null)
			Logger.LogDebug("Creating empty ContentFragment", new Exception("Creating empty ContentFragment"));
		else Logger.LogDebug("Creating ContentFragment @ " + mPath.getPath());
		
		//OpenExplorer.getEventHandler().setOnWorkerThreadFinishedListener(this);
		
	}
	
	public void notifyDataSetChanged() {
		if(mContentAdapter != null)
			mContentAdapter.notifyDataSetChanged();
	}
	public void refreshData(Bundle savedInstanceState, boolean allowSkips)
	{
		if(!isVisible()) return;
		Logger.LogDebug("Refreshing Data for " + mPath.getPath());
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
		
		if(path instanceof OpenFile && !path.getPath().startsWith("/"))
		{
			if(path.getPath().equals("Photos"))
				path = OpenExplorer.getPhotoParent();
			if(path.getPath().equals("Videos"))
				path = OpenExplorer.getVideoParent();
			if(path.getPath().equals("Music"))
				path = OpenExplorer.getMusicParent();
			if(path.getPath().equals("Downloads"))
				path = OpenExplorer.getDownloadParent();
		}
		mPath = path;
		
		mActionModeSelected = false;
		try {
			mShowHiddenFiles = getViewSetting(path, "show",
						getExplorer() != null ? 
								getExplorer().getSetting(null, "pref_show", false)
								: false);
			mShowThumbnails = getViewSetting(path, "thumbs", 
						getExplorer() != null ?
								getExplorer().getSetting(null, "pref_thumbs", true)
								: true);
			mFoldersFirst = getViewSetting(path, "folders", getExplorer() != null ?
								getExplorer().getSetting(null, "pref_folders", true)
								: true);
		} catch(NullPointerException npe) {
			Logger.LogWarning("Null while getting prefs", npe);
			mShowHiddenFiles = false;
			mShowThumbnails = true;
			mFoldersFirst = true;
		}
		if(getExplorer() != null)
			mSorting = FileManager.parseSortType(
				getViewSetting(path, "sort",
					getExplorer().getSetting(null, "pref_sorting", mSorting != null ? mSorting.toString() : SortType.ALPHA.toString())));
		
		Logger.LogVerbose("View options for " + path.getPath() + " : " + (mShowHiddenFiles ? "show" : "hide") + " + " + (mShowThumbnails ? "thumbs" : "icons") + " + " + mSorting.toString());

		//if(path.getClass().equals(OpenCursor.class) && !OpenExplorer.BEFORE_HONEYCOMB)
		//	mShowThumbnails = true;
		
		if(getActivity() != null && getActivity().getWindow() != null)
			mShowLongDate = getResources().getBoolean(R.bool.show_long_date) //getActivity().getWindow().getWindowManager().getDefaultDisplay().getRotation() % 180 != 0
					&& mPath != null;

		if(!path.requiresThread() && (!allowSkips || path.getListLength() < 300))
			try {
				mData = path.listFiles();
				updateData(mData, allowSkips);
			} catch (IOException e) {
				Logger.LogError("Error getting children from FileManager for " + path, e);
			}
		else {
			mData2.clear();
			mData = new OpenPath[0];
			if(mContentAdapter != null)
				mContentAdapter.notifyDataSetChanged();
			setProgressVisibility(true);
			if(path.listFromDb(mSorting))
			{
				try {
					mData = path.list();
				} catch (IOException e) {
					Logger.LogError("Error listing from path " + path.getPath(), e);
				}
				if(getExplorer() != null)
					getExplorer().sendToLogView("Loaded " + mData.length + " entries from cache", Color.DKGRAY);
			} else if(path instanceof OpenFile)
				mData = ((OpenFile)path).listFiles();
			updateData(mData, allowSkips);
			//cancelAllTasks();
			
		}
		
		//OpenExplorer.setOnSettingsChangeListener(this);
		
			//new FileIOTask().execute(new FileIOCommand(FileIOCommandType.ALL, path));
			
			//if(mGrid != null && savedInstanceState.containsKey("first"))
			
	}
	
	public void runUpdateTask() { runUpdateTask(false); }
	public void runUpdateTask(boolean reconnect)
	{
		String sPath = mPath.getPath();
		NetworkIOTask.cancelTask(sPath);
		if(reconnect && (mPath instanceof OpenNetworkPath))
			((OpenNetworkPath)mPath).disconnect();
		Logger.LogDebug("Running Task for " + sPath);
		final NetworkIOTask task = new NetworkIOTask(this);
		NetworkIOTask.addTask(sPath, task);
		task.execute(mPath);
		new Thread(new Runnable(){
			@Override
			public void run() {
				try { Thread.sleep(30000); } catch (InterruptedException e) { }
				if(task.getStatus() == Status.RUNNING)
					task.doCancel(false);
			}
		}).start();
	}
	

	//@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.content_layout, container, false);
		mGrid = (GridView)v.findViewById(R.id.content_grid);
		//if(mProgressBarLoading == null)
		//	mProgressBarLoading = v.findViewById(R.id.content_progress);
		setProgressVisibility(false);
		super.onCreateView(inflater, container, savedInstanceState);
		//v.setBackgroundResource(R.color.lightgray);
		
		/*
		if (savedInstanceState != null && savedInstanceState.containsKey("location")) {
			String location = savedInstanceState.getString("location");
			if(location != null && !location.equals("") && location.startsWith("/"))
			{
				Logger.LogDebug("Content location restoring to " + location);
				mPath = new OpenFile(location);
				mData = getManager().getChildren(mPath);
				updateData(mData);
			}
			//setContentPath(path, false);
		}
		*/

		return v;
	}
	
	@Override
	public boolean onBackPressed() {
		return false;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if(item == null) return false;
		switch(item.getItemId())
		{
		case R.id.menu_sort_name_asc:	onSortingChanged(FileManager.SortType.ALPHA); return true; 
		case R.id.menu_sort_name_desc:	onSortingChanged(FileManager.SortType.ALPHA_DESC); return true; 
		case R.id.menu_sort_date_asc: 	onSortingChanged(FileManager.SortType.DATE); return true;
		case R.id.menu_sort_date_desc: 	onSortingChanged(FileManager.SortType.DATE_DESC); return true; 
		case R.id.menu_sort_size_asc: 	onSortingChanged(FileManager.SortType.SIZE); return true; 
		case R.id.menu_sort_size_desc: 	onSortingChanged(FileManager.SortType.SIZE_DESC); return true; 
		case R.id.menu_sort_type: 		onSortingChanged(FileManager.SortType.TYPE); return true;
		case R.id.menu_view_hidden:
			onHiddenFilesChanged(!getShowHiddenFiles());
			return true;
		case R.id.menu_view_thumbs:
			onThumbnailChanged(!getShowThumbnails());
			return true;
		case R.id.menu_sort_folders_first:
			onFoldersFirstChanged(!getFoldersFirst());
			return true;
		}
		return false;
	}
	
	@TargetApi(11)
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		menu.clear();
		inflater.inflate(R.menu.main_menu, menu);
		MenuUtils.setMenuVisible(menu, OpenExplorer.IS_DEBUG_BUILD, R.id.menu_debug);
		if(!OpenExplorer.BEFORE_HONEYCOMB && OpenExplorer.USE_ACTION_BAR)
		{
			MenuUtils.setMenuVisible(menu, false, R.id.title_menu);
			try {
			final SearchView mSearchView = (SearchView)menu.findItem(R.id.menu_search).getActionView();
			if(mSearchView != null)
				mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
					@TargetApi(11)
					public boolean onQueryTextSubmit(String query) {
						mSearchView.clearFocus();
						Intent intent = getExplorer().getIntent();
						if(intent == null)
							intent = new Intent();
						intent.setAction(Intent.ACTION_SEARCH);
						Bundle appData = new Bundle();
						appData.putString("path", getExplorer().getDirContentFragment(false).getPath().getPath());
						intent.putExtra(SearchManager.APP_DATA, appData);
						intent.putExtra(SearchManager.QUERY, query);
						getExplorer().handleIntent(intent);
						return true;
					}
					public boolean onQueryTextChange(String newText) {
						return false;
					}
				});
			} catch(NullPointerException e) {
				Logger.LogError("Couldn't set up Search ActionView", e);
			}
		}
		//MenuInflater inflater = new MenuInflater(mContext);
		//if(!OpenExplorer.USE_PRETTY_MENUS||!OpenExplorer.BEFORE_HONEYCOMB)
		if(!(menu instanceof MenuBuilder))
		{
			MenuItem sort = menu.findItem(R.id.menu_sort);
			if(sort != null && sort.getSubMenu() != null && !sort.getSubMenu().hasVisibleItems())
				inflater.inflate(R.menu.menu_sort, sort.getSubMenu());
			MenuItem view = menu.findItem(R.id.menu_view);
			if(view != null && view.getSubMenu() != null && !view.getSubMenu().hasVisibleItems())
				inflater.inflate(R.menu.menu_view, view.getSubMenu());
			MenuItem paste = menu.findItem(R.id.menu_paste);
			if(paste != null && paste.getSubMenu() != null && !paste.getSubMenu().hasVisibleItems())
				inflater.inflate(R.menu.multiselect, paste.getSubMenu());
		}
	}
	
	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		//Logger.LogVerbose("ContentFragment.onPrepareOptionsMenu");
		if(getActivity() == null) return;
		if(menu == null) return;
		if(!isAdded() || isDetached() || !isVisible()) return;
		super.onPrepareOptionsMenu(menu);
		if(OpenExplorer.BEFORE_HONEYCOMB)
			MenuUtils.setMenuVisible(menu, false, R.id.menu_view_carousel);
		
		MenuUtils.setMenuChecked(menu, mFoldersFirst, R.id.menu_sort_folders_first);
		
		switch(getSorting())
		{
		case ALPHA:
			MenuUtils.setMenuChecked(menu, true, R.id.menu_sort_name_asc);
			break;
		case ALPHA_DESC:
			MenuUtils.setMenuChecked(menu, true, R.id.menu_sort_name_desc);
			break;
		case DATE:
			MenuUtils.setMenuChecked(menu, true, R.id.menu_sort_date_asc);
			break;
		case DATE_DESC:
			MenuUtils.setMenuChecked(menu, true, R.id.menu_sort_date_desc);
			break;
		case SIZE:
			MenuUtils.setMenuChecked(menu, true, R.id.menu_sort_size_asc);
			break;
		case SIZE_DESC:
			MenuUtils.setMenuChecked(menu, true, R.id.menu_sort_size_desc);
			break;
		case TYPE:
			MenuUtils.setMenuChecked(menu, true, R.id.menu_sort_type);
			break;
		}
		
		if(OpenExplorer.BEFORE_HONEYCOMB && menu.findItem(R.id.menu_multi) != null)
			menu.findItem(R.id.menu_multi).setIcon(null);
		
		//if(menu.findItem(R.id.menu_context_unzip) != null && getClipboard().getCount() == 0)
		//	menu.findItem(R.id.menu_context_unzip).setVisible(false);
		
		if(getClipboard() == null || getClipboard().size() == 0)
		{
			MenuUtils.setMenuVisible(menu, false, R.id.menu_paste);
		} else {
			MenuItem mPaste = menu.findItem(R.id.menu_paste);
			if(mPaste != null && getClipboard() != null && !isDetached())
				mPaste.setTitle(getString(R.string.s_menu_paste) + " (" + getClipboard().size() + ")");
			if(getClipboard().isMultiselect())
			{
				LayerDrawable d = (LayerDrawable) getResources().getDrawable(R.drawable.ic_menu_paste_multi);
				d.getDrawable(1).setAlpha(127);
				if(menu.findItem(R.id.menu_paste) != null)
					menu.findItem(R.id.menu_paste).setIcon(d);
			}
			if(mPaste != null)
				mPaste.setVisible(true);
		}
		
		int mViewMode = getViewMode();
		MenuUtils.setMenuChecked(menu, true, 0, R.id.menu_view_grid, R.id.menu_view_list, R.id.menu_view_carousel);
		if(mViewMode == OpenExplorer.VIEW_GRID)
			MenuUtils.setMenuChecked(menu, true, R.id.menu_view_grid, R.id.menu_view_list, R.id.menu_view_carousel);
		else if(mViewMode == OpenExplorer.VIEW_LIST)
			MenuUtils.setMenuChecked(menu, true, R.id.menu_view_list, R.id.menu_view_grid, R.id.menu_view_carousel);
		else if(mViewMode == OpenExplorer.VIEW_CAROUSEL)
			MenuUtils.setMenuChecked(menu, true, R.id.menu_view_carousel, R.id.menu_view_grid, R.id.menu_view_list);
		
		MenuUtils.setMenuChecked(menu, getShowHiddenFiles(), R.id.menu_view_hidden);
		MenuUtils.setMenuChecked(menu, getShowThumbnails(), R.id.menu_view_thumbs);
		MenuUtils.setMenuVisible(menu, !OpenExplorer.BEFORE_HONEYCOMB, R.id.menu_view_carousel);
		
		if(RootManager.Default.isRoot())
			MenuUtils.setMenuChecked(menu, true, R.id.menu_root);
		
	}
	
	/*
	@Override
	public void setInitialSavedState(SavedState state) {
		super.setInitialSavedState(state);
		if(state == null) return;
		Bundle b = state.getBundle();
		if(b != null && b.containsKey("last") && mPath == null)
			setPath(b.getString("last"));
		
		Logger.LogVerbose("setInitialSavedState :: " + state.toString());
	}
	*/
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		try {
			super.onSaveInstanceState(outState);
			outState.putInt("view", mViewMode);
			if(mPath != null)
				outState.putString("last", mPath.getPath());
			if(mListVisibleStartIndex > 0)
				outState.putInt("first", mListVisibleStartIndex);
			if(mListScrollY > 0)
				outState.putInt("scroll", mListScrollY);
			if(mGrid != null)
				outState.putParcelable("grid", mGrid.onSaveInstanceState());
		} catch(NullPointerException e) {
			Logger.LogError("Not sure why this is causing NPE crashes", e);
		}

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
		
		if(mBundle == null && savedInstanceState != null)
			mBundle = savedInstanceState;
		
		if(mBundle == null)
			mBundle = new Bundle();
		
		//mPathView = (LinearLayout)v.findViewById(R.id.scroll_path);
		//if(mGrid == null)
		mGrid = (GridView)v.findViewById(R.id.content_grid);
		
		//if(mProgressBarLoading == null) mProgressBarLoading = v.findViewById(R.id.content_progress);
		setProgressVisibility(false);

		if(mGrid == null)
			Logger.LogError("WTF, where are they?");
		else
			//refreshData(null);
			updateGridView();
		
		refreshData(mBundle, true);
		
		if(mGrid != null)
		{
			if(mBundle.containsKey("scroll") && mBundle.getInt("scroll") > 0)
			{
				Logger.LogDebug("Returning Scroll to " + mBundle.getInt("scroll"));
				mGrid.scrollTo(0, mBundle.getInt("scroll"));
			} else if(mBundle.containsKey("grid"))
				mGrid.onRestoreInstanceState(mBundle.getParcelable("grid"));
			if(mBundle.containsKey("first"))
			{
				Logger.LogDebug("Returning first item #" + mBundle.getInt("first"));
				mGrid.setSelection(mBundle.getInt("first"));
			}
		}
	}

	@TargetApi(11)
	public void updateGridView()
	{
		int mLayoutID;
		if(mGrid == null)
			mGrid = (GridView)getView().findViewById(R.id.content_grid);
		if(mGrid == null)
		{
			Logger.LogWarning("This shouldn't happen");
			mGrid = (GridView)((LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.content_grid, null);
			((ViewGroup)getView()).addView(mGrid);
			setupGridView();
		}
		mGrid.invalidateViews();
		mViewMode = getViewMode();
		if(getExplorer() == null) return;
		//mSorting = FileManager.parseSortType(getExplorer().getSetting(mPath, "sort", getExplorer().getPreferences().getSetting("global", "pref_sorting", mSorting.toString())));
		//mShowHiddenFiles = !getExplorer().getSetting(mPath, "hide", getExplorer().getPreferences().getSetting("global", "pref_hide", true));
		//mShowThumbnails = getExplorer().getSetting(mPath, "thumbs", getExplorer().getPreferences().getSetting("global", "pref_thumbs", true));
		
		if(!OpenExplorer.BEFORE_HONEYCOMB)
			getExplorer().invalidateOptionsMenu();
		
		//Logger.LogVerbose("Check View Mode: " + mViewMode);
		if(getViewMode() == OpenExplorer.VIEW_GRID) {
			mLayoutID = R.layout.grid_content_layout;
			int iColWidth = getResources().getDimensionPixelSize(R.dimen.grid_width);
			//Logger.LogVerbose("Grid Widths: " + iColWidth + " :: " + getActivity().getWindowManager().getDefaultDisplay().getWidth());
			mGrid.setColumnWidth(iColWidth);
			//mGrid.setNumColumns(getActivity().getWindowManager().getDefaultDisplay().getWidth() / iColWidth);
		} else {
			mLayoutID = R.layout.list_content_layout;
			int iColWidth = getResources().getDimensionPixelSize(R.dimen.list_width);
			//Logger.LogVerbose("List Widths: " + iColWidth + " :: " + getActivity().getWindowManager().getDefaultDisplay().getWidth());
			mGrid.setColumnWidth(iColWidth);
			//mGrid.setNumColumns(GridView.AUTO_FIT);
		}
		if(mGrid == null) return;
		if(mData2 == null)
			mData2 = new ArrayList<OpenPath>();
		//mContentAdapter = new OpenPathAdapter(mPath, getViewMode(), getExplorer());
		//Logger.LogDebug("Setting up grid w/ " + mData2.size() + " items");
		mContentAdapter = new ContentAdapter(getExplorer(), mLayoutID, mData2);
		((ContentAdapter)mContentAdapter).setViewMode(getViewMode());
		/*
		if(OpenCursor.class.equals(mPath.getClass())) {
			if(mContentAdapter != null && OpenCursorAdapter.class.equals(mContentAdapter.getClass()))
			{
				((OpenCursorAdapter)mContentAdapter)
					.setLayout(mLayoutID)
					.setParent((OpenCursor)mPath);
				//getLoaderManager().initLoader(((OpenCursor)mPath).getCursorType(), null, this);
			}
			else
			{
				mContentAdapter = new OpenCursorAdapter(mContext, (OpenCursor)mPath, 0, (OpenCursor)mPath, mLayoutID);
				mGrid.setAdapter(mContentAdapter);
			}
		} else {
			if(mContentAdapter != null && OpenCursorAdapter.class.equals(mContentAdapter.getClass()))
				((OpenCursorAdapter)mContentAdapter).swapCursor(null);
			mContentAdapter = new OpenArrayAdapter(mContext, mLayoutID, mData2);
		}*/
		mGrid.setAdapter(mContentAdapter);
		mContentAdapter.notifyDataSetChanged();
		setupGridView();
	}
	public void setupGridView()
	{
		//if(mGrid.getTag() == null)
		//	mGrid.setTag(true);
		//else return; // only do the following the first time
		
		//mGrid.setSelector(R.drawable.selector_blue);
		//mGrid.setDrawSelectorOnTop(true);
		mGrid.setVisibility(View.VISIBLE);
		mGrid.setOnItemClickListener(this);
		mGrid.setOnItemLongClickListener(this);
		mGrid.setOnScrollListener(new OnScrollListener() {
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				mListScrollingState = scrollState;
				mListScrollY = view.getScrollY();
				//if(scrollState == 0)
				//	onScrollStopped(view);
			}
			
			public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount)
			{
				if(firstVisibleItem != mListVisibleStartIndex)
					mListVisibleStartIndex = firstVisibleItem;
				if(visibleItemCount != mListVisibleLength)
					mListVisibleLength = visibleItemCount;
			}
		});
		if(!OpenExplorer.USE_PRETTY_CONTEXT_MENUS) //|| !USE_ACTIONMODE)
			registerForContextMenu(mGrid);
	}
	
	/*
	protected void onScrollStopped(AbsListView view)
	{
		boolean skipThis = true;
		if(skipThis) return;
		int start = Math.max(0, mListVisibleStartIndex);
		int end = Math.min(mData2.size() - 1, mListVisibleStartIndex + mListVisibleLength);
		int mWidth = 128, mHeight = 128;
		ThumbnailStruct[] thumbs = new ThumbnailStruct[end - start];
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
					thumbs[i - start] = new ThumbnailStruct(file, mHolder, mWidth, mHeight);
					//new ThumbnailTask().execute(new ThumbnailStruct(file, mHolder, mWidth, mHeight));
				}
			}
			//view.getItemAtPosition(i);
		}
		
		new ThumbnailTask().execute(thumbs);
		//Logger.LogDebug("Visible items " + mData2.get(mListVisibleStartIndex).getName() + " - " + mData2.get().getName());
	}
	*/
	

	
	public void updateData(final OpenPath[] items) { updateData(items, true); }
	private void updateData(final OpenPath[] items, boolean allowSkips)
	{
		if(items == null) return;
		updateData(items,
				!allowSkips || (items.length < 500),
				mFoldersFirst,
				mShowHiddenFiles //getManager().getShowHiddenFiles())
				);
	}
	private void updateData(final OpenPath[] items,
			final boolean doSort,
			final boolean foldersFirst,
			final boolean showHidden) {
		if(!mReadyToUpdate) return;
		if(items == null) return;
		mReadyToUpdate = false;
		
		//new Thread(new Runnable(){public void run() {
		Logger.LogVerbose("updateData on " + items.length + " items : " + (showHidden ? "show" : "hide") + " + " + (foldersFirst ? "folders" : "files") + " + " + (doSort ? mSorting.toString() : "no sort"));

		if(doSort)
		{
			//Logger.LogVerbose("~Sorting by " + mSorting.toString());
			OpenPath.Sorting = mSorting; //getManager().getSorting();
			try {
				Arrays.sort(items);
			} catch(Exception e) {
				//Logger.LogError("Couldn't sort.", e);
			}
		}
		
		if(mData2 == null)
			mData2 = new ArrayList<OpenPath>();
		else
			mData2.clear();
		
		int folder_index = 0;
		if(items != null)
		for(OpenPath f : items)
		{
			if(f == null) continue;
			if(!showHidden && f.isHidden()) continue;
			if(!f.requiresThread())
			{
				if(!f.exists()) continue;
				if(f.isFile() && !(f.length() >= 0)) continue;
			}
			if(foldersFirst && f.isDirectory())
				mData2.add(folder_index++, f);
			else
				mData2.add(f);
		}
		

		setProgressVisibility(false);
		
		//Logger.LogDebug("mData has " + mData2.size());
		if(mContentAdapter != null)
			mContentAdapter.notifyDataSetChanged();
		
		mReadyToUpdate = true;
		
		if(mGrid != null)
			updateGridView();
		
		if(mData2.size() > 0)
		{
			if(!restoreTopPath())
				Logger.LogWarning("Unable to restore top path");
		}
		/*
		mPathView.removeAllViews();
		mBackPathIndex = 0;	
		 */
		//if(getView() != null) getView().invalidate();
		
		//}}).run();
	}
	
	/*
	 * (non-Javadoc)
	 * this will update the data shown to the user after a change to
	 * the file system has been made from our background thread or EventHandler.
	 */
	//@Override
	public void onWorkerThreadComplete(int type, ArrayList<String> results) {
		
		Logger.LogVerbose("Need to refresh!");
		if(type == EventHandler.SEARCH_TYPE) {
			if(results == null || results.size() < 1) {
				Toast.makeText(getApplicationContext(), "Sorry, zero items found", Toast.LENGTH_LONG).show();
				return;
			}
			
			DialogHandler dialog = DialogHandler.newDialog(DialogHandler.DialogType.SEARCHRESULT_DIALOG, getApplicationContext());
			ArrayList<OpenPath> files = new ArrayList<OpenPath>();
			for(String s : results)
				files.add(new OpenFile(s));
			dialog.setHoldingFileList(files);
			dialog.setOnSearchFileSelected(new OnSearchFileSelected() {
				
				//@Override
				public void onFileSelected(String fileName) {
					OpenPath file = null;
					try {
						file = FileManager.getOpenCache(fileName, false, OpenPath.Sorting);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					if(file == null)
						file = new OpenFile(fileName);
					
					if (file.isDirectory()) {
						pushPath(file);
					} else {
						pushPath(file.getParent());
					}
				}
			});
			
			dialog.show(getFragmentManager(), "dialog");
			
		} else if(type == EventHandler.UNZIPTO_TYPE && results != null) {
			String name = new OpenFile(results.get(0)).getName();
			
			getClipboard().add(new OpenFile(results.get(0)));
			getExplorer().updateTitle("Holding " + name);
			
		} else {
			Logger.LogDebug("Worker thread complete (" + type + ")?");
			if(!mPath.requiresThread() || FileManager.hasOpenCache(mPath.getAbsolutePath()))
				try {
					if(mPath.requiresThread())
						mPath = FileManager.getOpenCache(mPath.getAbsolutePath(), false, mSorting);
					updateData(mPath.list());
				} catch (IOException e) {
					Logger.LogWarning("Couldn't update data after thread completion", e);
				}
			else {
				//if(mProgressBarLoading == null) mProgressBarLoading = getView().findViewById(R.id.content_progress);
				new NetworkIOTask(this).execute(mPath);
			}
			
			//changePath(mPath, false);
			if(mContentAdapter != null)
				mContentAdapter.notifyDataSetChanged();
			
			refreshData(null, false);
			//changePath(getManager().peekStack(), false);
		}
	}
	
	@Override
	public void onWorkerProgressUpdate(int pos, int total) {
		
	}
	
	/**
	 * Only to be used to change the path after creation
	 * @param path
	 */
	public void changePath(OpenPath path) {
		mPath = path;
		refreshData(null, false);
	}
	
	private void pushPath(OpenPath path)
	{
		if(mPathListener != null)
			mPathListener.changePath(path);
	}
	
	private void saveTopPath()
	{
		if(mGrid == null) return;
		mTopIndex = mGrid.getFirstVisiblePosition();
		if(mContentAdapter != null)
			mTopPath = (OpenPath)mContentAdapter.getItem(mTopIndex);
		Logger.LogInfo("Top Path saved to " + mTopIndex + (mTopPath != null ? " :: " + mTopPath.getName() : ""));
	}
	
	private Boolean restoreTopPath()
	{
		if(mTopPath != null) 
		{
			Logger.LogDebug("Looking for top path (" + mTopPath.getName() + ")");
			for(int i = 0; i < mContentAdapter.getCount(); i++)
				if(((OpenPath)mContentAdapter.getItem(i)).getName().equals(mTopPath.getName()))
					return restoreTopPath(i);
		}
		if(mTopIndex > 0)
		{
			Logger.LogDebug("Falling back to top index (" + mTopIndex + ")");
			return restoreTopPath(mTopIndex);
		} else return true;
	}
	private Boolean restoreTopPath(int index)
	{
		Logger.LogInfo("Top Path restored to " + index);
		mGrid.setSelection(index);
		mTopIndex = 0;
		mTopPath = null;
		return true;
	}
	
	public void onFoldersFirstChanged(boolean first)
	{
		setFoldersFirst(first);
		refreshData(null, false);
	}
	public void onHiddenFilesChanged()
	{
		onHiddenChanged(!getShowHiddenFiles());
	}
	//@Override
	public void onHiddenFilesChanged(boolean toShow)
	{
		Logger.LogInfo("onHiddenFilesChanged(" + toShow + ")");
		saveTopPath();
		setShowHiddenFiles(toShow);
		//getManager().setShowHiddenFiles(state);
		refreshData(null, false);
	}

	public void onThumbnailChanged() { 
		onThumbnailChanged(!getShowThumbnails());
	}
	//@Override
	public void onThumbnailChanged(boolean state) {
		saveTopPath();
		setShowThumbnails(state);
		refreshData(null, false);
	}
	
	//@Override
	public void onSortingChanged(SortType type) {
		setSorting(type);
		//getManager().setSorting(type);
		refreshData(null, false);
	}
	public void setFoldersFirst(boolean first) {
		mFoldersFirst = first;
		setViewSetting(mPath, "folders", first);
	}
	public void setShowHiddenFiles(boolean show)
	{
		mShowHiddenFiles = show;
		setViewSetting(mPath, "show", show);
	}
	
	public void setSorting(SortType type)
	{
		mSorting = type;
		setViewSetting(mPath, "sort", type.toString());
	}
	
	public void setShowThumbnails(boolean thumbs)
	{
		mShowThumbnails = thumbs;
		setViewSetting(mPath, "thumbs", thumbs);
	}
	
	public void setSettings(SortType sort, boolean thumbs, boolean hidden)
	{
		setSorting(sort);
		setShowThumbnails(thumbs);
		setShowHiddenFiles(hidden);
		refreshData(null, false);
	}

	//@Override
	public void onViewChanged(int state) {
		setViewMode(state);
		//getExplorer().setViewMode(state);
		
		View v = getView();
		if(v != null)
		{
			//if(mPathView == null)
			//	mPathView = (LinearLayout)v.findViewById(R.id.scroll_path);
			if(mGrid == null)
				mGrid = (GridView)v.findViewById(R.id.content_grid);
			/*if(mMultiSelectView == null)
				mMultiSelectView = (GridView)v.findViewById(R.id.multiselect_path);
			if(mMultiSelectView != null)
				setupMultiSelectView();*/
		}

		if(mGrid == null)
			Logger.LogError("WTF, where is it?");
		else updateGridView();
			//refreshData(null);
	}
	
	
	public void setProgressVisibility(boolean visible)
	{
		//if(mProgressBarLoading == null && mGrid != null && mGrid.getParent() != null) mProgressBarLoading = ((View)mGrid.getParent()).findViewById(R.id.content_progress);
		//if(mProgressBarLoading != null && mData.length == 0) mProgressBarLoading.setVisibility(visible ? View.VISIBLE : View.GONE);
		if(getExplorer() != null)
			getExplorer().setProgressVisibility(visible);
	}
	
	public SortType getSorting() {
		return mSorting;
	}
	public boolean getFoldersFirst() { return mFoldersFirst; }
	public boolean getShowHiddenFiles() {
		return mShowHiddenFiles;
	}
	public boolean getShowThumbnails() {
		return mShowThumbnails;
	}
	@Override
	public CharSequence getTitle() {
		return mPath.getName() + ((mPath instanceof OpenFile || mPath instanceof OpenNetworkPath) && mPath.isDirectory() && !mPath.getName().endsWith("/") ? "/" : "");
	}
	
	@Override
	public OpenPath getPath() {
		return mPath;
	}
	
	@Override
	public Drawable getIcon() {
		if(isDetached()) return null;
		if(getActivity() != null && getResources() != null)
			return getResources().getDrawable(ThumbnailCreator.getDefaultResourceId(getPath(), 36, 36));
		return null;
	}
	
}


