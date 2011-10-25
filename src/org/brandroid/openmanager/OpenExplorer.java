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

package org.brandroid.openmanager;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.app.ActionBar.Tab;
import android.app.ActionBar.TabListener;
import android.app.actionbarcompat.ActionBarActivity;
import android.app.actionbarcompat.ActionBarHelper;
import android.app.actionbarcompat.ActionBarHelperTab;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentManager.BackStackEntry;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.SearchView;
import android.widget.Toast;
import java.util.ArrayList;

import org.brandroid.openmanager.data.OpenCursor;
import org.brandroid.openmanager.data.OpenFTP;
import org.brandroid.openmanager.data.OpenFile;
import org.brandroid.openmanager.data.OpenMediaStore;
import org.brandroid.openmanager.data.OpenPath;
import org.brandroid.openmanager.fragments.BookmarkFragment;
import org.brandroid.openmanager.fragments.DialogHandler;
import org.brandroid.openmanager.fragments.ContentFragment;
import org.brandroid.openmanager.fragments.TextEditorFragment;
import org.brandroid.openmanager.ftp.FTPManager;
import org.brandroid.openmanager.util.ExecuteAsRootBase;
import org.brandroid.utils.Logger;

public class OpenExplorer
		extends ActionBarActivity
		implements OnBackStackChangedListener {	

	private static final int PREF_CODE =		0x6;
	
	public static final boolean BEFORE_HONEYCOMB = Build.VERSION.SDK_INT < 11;
	
	private static OnSettingsChangeListener mSettingsListener = null;
	private SharedPreferences mPreferences;
	private SearchView mSearchView;
	private ActionMode mActionMode;
	private ArrayList<OpenPath> mHeldFiles;
	private int mLastBackIndex = -1;
	private String mLastPath = "";
	private BroadcastReceiver storageReceiver;
	
	private Fragment mFavoritesFragment;
	
	private EventHandler mEvHandler;
	private FileManager mFileManager;
	
	private FragmentManager fragmentManager;
	
	private Cursor mPhotoCursor, mVideoCursor;
	private OpenCursor mPhotoParent, mVideoParent;
	
	private Boolean mSinglePane = false;
    
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_fragments);

        if(isGTV())
		{
			//showToast("Welcome, GoogleTV user!");
			//getActionBar().hide();
		} // else
        if(findViewById(R.id.title_bar) != null)
        	findViewById(R.id.title_bar).setVisibility(View.GONE);
        
        if(!BEFORE_HONEYCOMB)
        	setTheme(android.R.style.Theme_Holo);
        
        if(findViewById(R.id.list_frag) == null)
        	mSinglePane = true;
        else if(findViewById(R.id.list_frag).getVisibility() == View.GONE)
        	mSinglePane = true;
        
        fragmentManager = getSupportFragmentManager();
        fragmentManager.addOnBackStackChangedListener(this);
        
        //mTreeFragment = new DirListFragment();
        if(mFavoritesFragment == null)
        	mFavoritesFragment = new BookmarkFragment();
        
        //BookmarkFragment.setOnChangeLocationListener(this);

		refreshCursors();
        
        OpenPath path = null;
        if(savedInstanceState != null && savedInstanceState.containsKey("last") && !savedInstanceState.getString("last").equals(""))
        {
        	String last = savedInstanceState.getString("last");
        	if(last.startsWith("/"))
        		path = new OpenFile(last);
        	else if(last.indexOf(":/") > -1)
        		path = new OpenFTP(last, null, new FTPManager());
        	else if(last.equals("Videos"))
        		path = new OpenCursor(getVideoCursor(), "Videos");
        	else if(last.equals("Photos"))
        		path = new OpenCursor(getPhotoCursor(), "Photos");
        	else
        		path = new OpenFile(last);
        	updateTitle(path.getPath());
        } else
        	path = new OpenFile(Environment.getExternalStorageDirectory());
        
        Logger.LogDebug("Creating with " + path.getPath());
        
        FragmentTransaction ft = fragmentManager.beginTransaction();
        ft.replace(mSinglePane ? R.id.content_frag : R.id.list_frag, mFavoritesFragment);
        if(!mSinglePane)
        	ft.replace(R.id.content_frag, new ContentFragment(path));
        ft.commit();
        
        /*
        FragmentTransaction trans = fragmentManager.beginTransaction();
        trans.add(R.id.content_frag, new DirContentActivity());
        trans.addToBackStack(null);
        trans.commit();
        */
        //getFragmentManager().findFragmentById(R.id.content_frag);
                        
        mFileManager = new FileManager();
        mEvHandler = new EventHandler(this, mFileManager);
        
        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        
        handleMediaReceiver();
        
        /* read and display the users preferences */
        //mSettingsListener.onSortingChanged(mPreferences.getString(SettingsActivity.PREF_SORT_KEY, "type"));
    }
    
    public void handleMediaReceiver()
    {
    	storageReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				showToast(action);
				if(action.equals(Intent.ACTION_MEDIA_MOUNTED) || action.equals(Intent.ACTION_MEDIA_UNMOUNTED))
				{
					refreshBookmarks();
				}
			}
		};
		
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
		filter.addAction(Intent.ACTION_MEDIA_EJECT);
		filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
		filter.addDataScheme("file");
		registerReceiver(storageReceiver, filter);
    }
    
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
    	super.onPostCreate(savedInstanceState);
    	/*
    	if(mSettingsListener != null)
        {
	        mSettingsListener.onHiddenFilesChanged(mPreferences.getBoolean(SettingsActivity.PREF_HIDDEN_KEY, false));
			mSettingsListener.onThumbnailChanged(mPreferences.getBoolean(SettingsActivity.PREF_THUMB_KEY, true));
			mSettingsListener.onViewChanged(mPreferences.getString(SettingsActivity.PREF_VIEW_KEY, "list"));
        } */
    }
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	unregisterReceiver(storageReceiver);
    }
    
    public Cursor getPhotoCursor() { if(mPhotoCursor == null) refreshCursors(); return mPhotoCursor; }
    public Cursor getVideoCursor() { if(mVideoCursor == null) refreshCursors(); return mVideoCursor; }
    
    public void refreshCursors()
    {
        if(mPhotoCursor == null)
        	mPhotoCursor = managedQuery(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        		new String[]{"_id", "_display_name", "_data", "_size", "date_modified"},
				MediaStore.Images.Media.SIZE + " > 10000", null,
				MediaStore.Images.Media.DATE_ADDED + " DESC");
        else mPhotoCursor.requery();
        if(mVideoCursor == null)
        	mVideoCursor = managedQuery(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        		new String[]{"_id", "_display_name", "_data", "_size", "date_modified"},
				MediaStore.Video.Media.SIZE + " > 100000", null,
				MediaStore.Video.Media.BUCKET_DISPLAY_NAME + " ASC, " +
				MediaStore.Video.Media.DATE_MODIFIED + " DESC");
        else mVideoCursor.requery();
        mPhotoParent = new OpenCursor(mPhotoCursor, "Photos");
        mVideoParent = new OpenCursor(mVideoCursor, "Videos");
        ensureCursorCache();
    }
    public void ensureCursorCache()
    {
    	ThumbnailCreator.setContext(this);
    	// group into blocks
    	int enSize = 6;
    	ArrayList<OpenPath> buffer = new ArrayList<OpenPath>(enSize);
    	for(OpenMediaStore ms : mPhotoParent.list())
    	{
    		buffer.add(ms);
    		if(buffer.size() == enSize)
    		{
    			OpenMediaStore[] buff = new OpenMediaStore[buffer.size()];
    			buffer.toArray(buff);
    			buffer.clear();
    			new EnsureCursorCacheTask().execute(buff);
    		}
    	}
    	for(OpenMediaStore ms : mVideoParent.list())
    	{
    		buffer.add(ms);
    		if(buffer.size() == enSize)
    		{
    			OpenMediaStore[] buff = new OpenMediaStore[buffer.size()];
    			buffer.toArray(buff);
    			buffer.clear();
    			new EnsureCursorCacheTask().execute(buff);
    		}
    	}
    	if(buffer.size() > 0)
    	{
    		OpenMediaStore[] buff = new OpenMediaStore[buffer.size()];
			buffer.toArray(buff);
			buffer.clear();
			new EnsureCursorCacheTask().execute(buff);
    	}
    }
    
    public void refreshBookmarks()
    {
    	refreshCursors();
    	if(mFavoritesFragment == null)
    	{
			mFavoritesFragment = new BookmarkFragment();
			FragmentTransaction ft = fragmentManager.beginTransaction();
			ft.replace(R.id.list_frag, mFavoritesFragment);
			ft.commit();
    	} else {
    		((BookmarkFragment)mFavoritesFragment).scanBookmarks();
    	}
    }
    public void addTab(final Fragment frag, final String title, Boolean activate)
    {
    	ActionBarHelper bar = getActionBarHelper();
    	ActionBarHelperTab tab = (ActionBarHelperTab) ((Tab) bar.newTab()
				.setText(title))
				.setTabListener(new TabListener() {
					public void onTabSelected(Tab tab, android.app.FragmentTransaction ft2) {
						Logger.LogInfo("onTabSelected");
						FragmentTransaction ft = fragmentManager.beginTransaction();
						ft.replace(R.id.content_frag, frag);
						ft.setBreadCrumbTitle(title);
						ft.addToBackStack("edit");
						ft.commit();
					}
					public void onTabUnselected(Tab tab, android.app.FragmentTransaction ft2) {
						Logger.LogInfo("onTabReselected");
						FragmentTransaction ft = fragmentManager.beginTransaction();
						ft.remove(frag);
						ft.commit();
					}
					public void onTabReselected(Tab tab, android.app.FragmentTransaction ft) {
						Logger.LogInfo("onTabReselected");
					}
				});
		bar.addTab(tab);
		if(activate)
		{
			tab.select();
		}
		/*
		FragmentTransaction ft = fragmentManager.beginTransaction();
		ft.replace(R.id.content_frag, frag);
		ft.addToBackStack("edit");
		ft.commit();
		*/
    }
    
    public ContentFragment getDirContentFragment(Boolean activate)
    {
    	Logger.LogDebug("getDirContentFragment");
    	Fragment ret = fragmentManager.findFragmentById(R.id.content_frag);
    	if(ret == null || !ret.getClass().equals(ContentFragment.class))
   			ret = new ContentFragment(new OpenFile(mLastPath));
    	if(activate)
    	{
    		Logger.LogDebug("Activating content fragment");
    		FragmentTransaction ft = fragmentManager.beginTransaction();
    		ft.replace(R.id.content_frag, ret);
    		ft.commit();
    	}
    	
   		return (ContentFragment)ret;
    }
    
    public void updateTitle(String s)
    {
    	String t = getResources().getString(R.string.app_name) + (s.equals("") ? "" : " - " + s);
    	if(BEFORE_HONEYCOMB)
    	{
    		setTitle(t);
    	} else {
    		getActionBar().setTitle(t);
    	}
    }
    
    public void editFile(OpenPath path)
    {
    	TextEditorFragment editor = new TextEditorFragment(path.getPath());
    	addTab(editor, path.getName(), true);
    }
    
    public boolean onCreateOptionsMenu(Menu menu) {
    	getMenuInflater().inflate(R.menu.actbar, menu);
    	if(!BEFORE_HONEYCOMB)
    	{
    		mSearchView = (SearchView)menu.findItem(R.id.menu_search).getActionView();
	        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
	        	public boolean onQueryTextSubmit(String query) {
					mSearchView.clearFocus();
					mEvHandler.searchFile(mFileManager.peekStack().getPath(), query);
					return true;
				}
				public boolean onQueryTextChange(String newText) {
					return false;
				}
			});
    	}
    	return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	switch(keyCode)
    	{
	    	case KeyEvent.KEYCODE_DPAD_RIGHT:
	    		//fragmentManager.findFragmentById(R.id.content_frag).getView().requestFocus();
	    		break;
	    	case KeyEvent.KEYCODE_DPAD_LEFT:
	        	//fragmentManager.findFragmentById(R.id.list_frag).getView().requestFocus();
	        	break;
    	}
    	return super.onKeyDown(keyCode, event);
    }
    
    public boolean onOptionsItemSelected(MenuItem item)
    {
    	if(item.isCheckable())
    		item.setChecked(item.getGroupId() > 0 ? true : !item.isChecked());
    	
    	switch(item.getItemId())
    	{
	    	case android.R.id.home:
	    		if (mHeldFiles != null) {
	    			//DialogFragment df = 
	    			DialogHandler dialog = DialogHandler.newDialog(DialogHandler.DialogType.HOLDINGFILE_DIALOG, this);
	    			dialog.setHoldingFileList(mHeldFiles);
	    			
	    			FragmentTransaction trans = fragmentManager.beginTransaction();
	    			trans.replace(R.id.content_frag, dialog, "dialog");
	    			//dialog.show(getFragmentManager(), "dialog");
	    			trans.addToBackStack("dialog");
	    			trans.commit();
	    		} else {
	    			//toggleListView();
	    		}
	    		return true;
	    	
	    	case R.id.menu_new_folder:
	    		mEvHandler.createNewFolder(mFileManager.peekStack().getPath());
	    		return true;
	    		
	    	case R.id.menu_multi:
	    		if(mActionMode != null)
	    			return false;
	    		
	    		if(BEFORE_HONEYCOMB)
	    		{
	    			getDirContentFragment(true).changeMultiSelectState(true, MultiSelectHandler.getInstance(OpenExplorer.this));
	    		} else {
	    			
		    		mActionMode = startActionMode(new ActionMode.Callback() {
		    			
		    			MultiSelectHandler handler;
		    			
		    			
		    			public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
		    				return false;
		    			}
		    			public void onDestroyActionMode(ActionMode mode) {
		    				getDirContentFragment(false).changeMultiSelectState(false, handler);
		    				mActionMode = null;
		    				handler = null;
		    			}
		    			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
		    				handler = MultiSelectHandler.getInstance(OpenExplorer.this);
		    				mode.setTitle("Multi-select Options");
		    				mode.getMenuInflater().inflate(R.menu.context_multi, menu);
		    				getDirContentFragment(true).changeMultiSelectState(true, handler);
		    				return true;
		    			}
		    			public boolean onActionItemClicked(ActionMode mode, MenuItem item)
		    			{
		    				ArrayList<String>files = handler.getSelectedFiles();
		    				
		    				//nothing was selected
		    				if(files.size() < 1) {
		    					mode.finish();
		    					return true;
		    				}
		    				
		    				if(mHeldFiles == null)
		    					mHeldFiles = new ArrayList<OpenPath>();
		    				
		    				mHeldFiles.clear();
		    				
		    				for(String s : files)
		    					mHeldFiles.add(FileManager.getOpenCache(s));
		    			
		    				return getDirContentFragment(false).executeMenu(item.getItemId(), mode, mHeldFiles);
		    			}
		    		});
	    		}
	    		return true;
	    		
	    	case R.id.menu_sort_name_asc:	if(mSettingsListener!=null) mSettingsListener.onSortingChanged(FileManager.SortType.ALPHA); return true; 
	    	case R.id.menu_sort_name_desc:	if(mSettingsListener!=null) mSettingsListener.onSortingChanged(FileManager.SortType.ALPHA_DESC); return true; 
	    	case R.id.menu_sort_date_asc: 	if(mSettingsListener!=null) mSettingsListener.onSortingChanged(FileManager.SortType.DATE); return true;
	    	case R.id.menu_sort_date_desc: 	if(mSettingsListener!=null) mSettingsListener.onSortingChanged(FileManager.SortType.DATE_DESC); return true; 
	    	case R.id.menu_sort_size_asc: 	if(mSettingsListener!=null) mSettingsListener.onSortingChanged(FileManager.SortType.SIZE); return true; 
	    	case R.id.menu_sort_size_desc: 	if(mSettingsListener!=null) mSettingsListener.onSortingChanged(FileManager.SortType.SIZE_DESC); return true; 
	    	case R.id.menu_sort_type: 		if(mSettingsListener!=null) mSettingsListener.onSortingChanged(FileManager.SortType.TYPE); return true;
	    	
	    	case R.id.menu_view_grid: if(mSettingsListener!=null) mSettingsListener.onViewChanged("grid"); return true;
	    	case R.id.menu_view_list: if(mSettingsListener!=null) mSettingsListener.onViewChanged("list"); return true;
	    	case R.id.menu_view_hidden: if(mSettingsListener!=null) mSettingsListener.onHiddenFilesChanged(item.isChecked()); return true;
	    	case R.id.menu_view_thumbs: if(mSettingsListener!=null) mSettingsListener.onThumbnailChanged(item.isChecked()); return true;
	    	    	
	    	case R.id.menu_root:
	    		if(!item.isCheckable() || item.isChecked())
	    		{
	    			if(ExecuteAsRootBase.canRunRootCommands())
	    			{
	    				showToast("Root enabled");
	    				item.setTitle("ROOT!");
	    			} else {
	    				item.setEnabled(false).setChecked(false);
	    				showToast("Unable to achieve root.");
	    			}
	    		}
	    		return true;
	    	
	    	case R.id.menu_settings:
	    		startActivityForResult(new Intent(this, SettingsActivity.class), PREF_CODE);
	    		return true;
	    		
	    	case R.id.menu_search:
	    		//item.setActionView(mSearchView);
	    		return true;
    	}
    	
    	return super.onOptionsItemSelected(item);
    }
    
    public boolean onKeyUp(int keyCode, KeyEvent event) {
    	/*
    	if (keyCode == KeyEvent.KEYCODE_BACK) {
    		if (mBackQuit) {
    			return super.onKeyUp(keyCode, event);
    		} else {
    			Toast.makeText(this, "Press back again to quit", Toast.LENGTH_SHORT).show();
    			mBackQuit = true;
    			return true;
    		}    	
    	}*/
    	return super.onKeyUp(keyCode, event);
    }
    
	
	
	public static void setOnSettingsChangeListener(OnSettingsChangeListener e) {
    	mSettingsListener = e;
    }
    
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	if(requestCode == PREF_CODE) {
    		//this could be done better.
    		mSettingsListener.onHiddenFilesChanged(mPreferences.getBoolean(SettingsActivity.PREF_HIDDEN_KEY, false));
    		mSettingsListener.onThumbnailChanged(mPreferences.getBoolean(SettingsActivity.PREF_THUMB_KEY, false));
    		mSettingsListener.onViewChanged(mPreferences.getString(SettingsActivity.PREF_VIEW_KEY, "list"));
    		//mSettingsListener.onSortingChanged(mPreferences.getString(SettingsActivity.PREF_SORT_KEY, "alpha"));
    	}
    }
    
    
    protected void onPause() {
    	super.onPause();
    	Fragment fragList = fragmentManager.findFragmentById(mSinglePane ? R.id.content_frag : R.id.list_frag);
    	String list = "", bookmark = "";
    	if(fragList.getClass().equals(BookmarkFragment.class))
    	{
    		list = ((BookmarkFragment)fragList).getDirListString();
    		bookmark = ((BookmarkFragment)fragList).getBookMarkNameString();
    	}
    	
    	String saved = mPreferences.getString(SettingsActivity.PREF_LIST_KEY, "");
    	String saved_book = mPreferences.getString(SettingsActivity.PREF_BOOKNAME_KEY, "");
    	
    	if (!list.equals(saved)) {
    		SharedPreferences.Editor e = mPreferences.edit();
    		e.putString(SettingsActivity.PREF_LIST_KEY, list);
    		e.commit();
    	}
    	
    	if (!bookmark.equals(saved_book)) {
    		SharedPreferences.Editor e = mPreferences.edit();
    		e.putString(SettingsActivity.PREF_BOOKNAME_KEY, bookmark);
    		e.commit();
    	}
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
    	if(mLastPath == null || mLastPath.equals("")) return;
    	Logger.LogDebug("Saving " + mLastPath);
    	outState.putString("last", mLastPath);
    }


	public void goBack()
	{
		//new FileIOTask().execute(new FileIOCommand(FileIOCommandType.PREV, mFileManager.getLastPath()));
		OpenPath last = mFileManager.popStack();
		if(last == null) return;
		Logger.LogDebug("Going back to " + last.getPath());
		changePath(last, false);
		//updateData(last.list());
	}
	
	public void onBackStackChanged() {
		int i = fragmentManager.getBackStackEntryCount();
		final Boolean isBack = i < mLastBackIndex;
		BackStackEntry entry = null;
		if(i > 0) entry = fragmentManager.getBackStackEntryAt(i - 1);
		Logger.LogDebug("Back Stack " + i + (entry != null ? ": " + entry.getBreadCrumbTitle() : ""));
		if(isBack)
		{
			if(i > 0)
			{
				if(entry != null && entry.getBreadCrumbTitle() != null)
					updateTitle(entry.getBreadCrumbTitle().toString());
				else
					updateTitle("");
			} else if (mSinglePane) updateTitle("");
			else {
				updateTitle("");
				//showToast("Press back again to exit.");
			}
		}
		if(isBack && mFileManager != null && mFileManager.getStack() != null && mFileManager.getStack().size() > 0)
		{
			goBack();
		} else if(mSinglePane && isBack)
		{
			if(findViewById(R.id.list_frag) != null && findViewById(R.id.list_frag).getVisibility() == View.GONE)
			{
				findViewById(R.id.list_frag).setVisibility(View.VISIBLE);
			} else {
				FragmentTransaction ft = fragmentManager.beginTransaction();
				ft.show(mFavoritesFragment);
				ft.commit();
			}
		}
		mLastBackIndex = i;
	}
	
	public boolean isGTV() { return getPackageManager().hasSystemFeature("com.google.android.tv"); }
	public void showToast(final String message)  {
		Logger.LogInfo("Made toast: " + message);
        showToast(message, Toast.LENGTH_SHORT);
    }
	public void showToast(final int iStringResource) { showToast(getResources().getString(iStringResource)); }
	public void showToast(final String message, final int toastLength)  {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(getBaseContext(), message, toastLength).show();
            }
        });
    }

	
	public interface OnSettingsChangeListener {
		
		public void onHiddenFilesChanged(boolean state);
		public void onThumbnailChanged(boolean state);
		public void onViewChanged(String state);
		public void onSortingChanged(FileManager.SortType type);
	}


	public void hideBookmarkTitles() {
		BookmarkFragment bf = ((BookmarkFragment)fragmentManager.findFragmentById(R.id.list_frag));
		bf.hideTitles();
	}

	public void showBookmarkTitles() {
		BookmarkFragment bf = ((BookmarkFragment)fragmentManager.findFragmentById(R.id.list_frag));
		bf.showTitles();
	}
	
	public void toggleBookmarks(Boolean visible)
	{
		if(!mSinglePane) return;
		Fragment frag = fragmentManager.findFragmentById(R.id.list_frag);
		View v = frag.getView();
		v.setVisibility(visible ? View.VISIBLE : View.GONE);
	}

	public void changePath(OpenPath path, Boolean addToStack)
	{
		if(path == null) path = new OpenFile(mLastPath);
		if(!addToStack && path.getPath().equals("/")) return;
		//if(mLastPath.equalsIgnoreCase(path.getPath())) return;
		ContentFragment content = new ContentFragment(path);
		FragmentTransaction ft = fragmentManager.beginTransaction();
		ft.replace(R.id.content_frag, content);
		ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
		if(addToStack)
		{
			int bsCount = fragmentManager.getBackStackEntryCount();
			if(bsCount > 0)
			{
				BackStackEntry entry = fragmentManager.getBackStackEntryAt(bsCount - 1);
				Logger.LogDebug("Changing " + entry.getBreadCrumbTitle() + " to " + path.getPath() + "? " + (entry.getBreadCrumbTitle().toString().equalsIgnoreCase(path.getPath()) ? "No" : "Yes"));
				if(entry.getBreadCrumbTitle().toString().equalsIgnoreCase(path.getPath()))
					return;
			}
			mFileManager.pushStack(path);
			ft.addToBackStack("path");
		} else Logger.LogDebug("Covertly changing to " + path.getPath());
		Logger.LogDebug("Setting path to " + path.getPath());
		ft.setBreadCrumbTitle(path.getPath());
		updateTitle(path.getPath());
		ft.commit();
		if(!BEFORE_HONEYCOMB)
			setLights(!path.getName().equals("Videos"));
		mLastPath = path.getPath();
	}
	public void setLights(Boolean on)
	{
		View root = getCurrentFocus().getRootView();
		int vis = on ? View.STATUS_BAR_VISIBLE : View.STATUS_BAR_HIDDEN;
		if(root.getSystemUiVisibility() != vis)
			root.setSystemUiVisibility(vis);
	}
	public void onChangeLocation(OpenPath path) {
		changePath(path, true);
	}

	public FileManager getFileManager() {
		return mFileManager;
	}

	public EventHandler getEventHandler() {
		return mEvHandler;
	}

	public void setFileManager(FileManager man) {
		mFileManager = man;
	}
	public void setEventHandler(EventHandler handler)
	{
		mEvHandler = handler;
	}
	
	public class EnsureCursorCacheTask extends AsyncTask<OpenPath, Void, Void>
	{
		@Override
		protected Void doInBackground(OpenPath... params) {
			int done = 0;
			for(OpenPath path : params)
			{
				if(path.isDirectory())
				{
					for(OpenPath kid : path.list())
					{
						ThumbnailCreator.generateThumb(kid, 72, 72);
						done++;
					}
				} else {
					ThumbnailCreator.generateThumb(path, 72, 72);
					done++;
				}
			}
			Logger.LogDebug("cursor cache of " + done + " generated.");
			return null;
		}
		
	}
}

