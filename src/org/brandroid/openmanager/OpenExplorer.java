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
import android.os.Bundle;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.Media;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import java.io.File;
import java.util.ArrayList;

import org.brandroid.openmanager.data.OpenFile;
import org.brandroid.openmanager.data.OpenPath;
import org.brandroid.openmanager.fragments.BookmarkFragment;
import org.brandroid.openmanager.fragments.DialogHandler;
import org.brandroid.openmanager.fragments.DirContentActivity;
import org.brandroid.openmanager.fragments.DirListFragment;
import org.brandroid.openmanager.fragments.TextEditorFragment;
import org.brandroid.openmanager.util.ExecuteAsRootBase;
import org.brandroid.utils.Logger;

public class OpenExplorer extends FragmentActivity implements OnBackStackChangedListener {	
	//menu IDs
	private static final int MENU_DIR = 		0x0;
	private static final int MENU_SEARCH = 		0x1;
	private static final int MENU_MULTI =		0x2;
	private static final int MENU_SETTINGS = 	0x3;
	private static final int MENU_MODE	=		0x4;
	private static final int MENU_SORT = 		0x5;
	private static final int PREF_CODE =		0x6;
	
	private static final int MENU_ID_DELETE = 12;
	private static final int MENU_ID_COPY = 13;
	private static final int MENU_ID_CUT = 14;
	private static final int MENU_ID_SEND = 15;
	
	private static OnSettingsChangeListener mSettingsListener;
	private SharedPreferences mPreferences;
	private SearchView mSearchView;
	private ActionMode mActionMode;
	private ArrayList<OpenPath> mHeldFiles;
	private boolean mBackQuit = false;
	private int mLastBackIndex = -1;
	private BroadcastReceiver storageReceiver;
	
	private Fragment mFavoritesFragment;
	
	private EventHandler mEvHandler;
	private FileManager mFileManger;
	
	private FragmentManager fragmentManager;
	
	private Cursor mPhotoCursor, mVideoCursor;
    
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
        
        fragmentManager = getSupportFragmentManager();
        fragmentManager.addOnBackStackChangedListener(this);
        
        //mTreeFragment = new DirListFragment();
        mFavoritesFragment = new BookmarkFragment();
        
        FragmentTransaction ft = fragmentManager.beginTransaction();
        ft.add(R.id.list_frag, mFavoritesFragment);
        ft.add(R.id.content_frag, new DirContentActivity());
        ft.commit();
        
        /*
        FragmentTransaction trans = fragmentManager.beginTransaction();
        trans.add(R.id.content_frag, new DirContentActivity());
        trans.addToBackStack(null);
        trans.commit();
        */
        //getFragmentManager().findFragmentById(R.id.content_frag);
                        
        mEvHandler = getDirContentFragment().getEventHandlerInst();
        mFileManger = getDirContentFragment().getFileManagerInst();
        
        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mSearchView = new SearchView(this);
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
        
			
			public boolean onQueryTextSubmit(String query) {
				mSearchView.clearFocus();
				mEvHandler.searchFile(mFileManger.peekStack().getPath(), query);
				
				return true;
			}
			
			
			public boolean onQueryTextChange(String newText) {
				return false;
			}
		});
        
        
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
        
        /* read and display the users preferences */
        if(mSettingsListener != null)
        {
	        mSettingsListener.onHiddenFilesChanged(mPreferences.getBoolean(SettingsActivity.PREF_HIDDEN_KEY, false));
			mSettingsListener.onThumbnailChanged(mPreferences.getBoolean(SettingsActivity.PREF_THUMB_KEY, true));
			mSettingsListener.onViewChanged(mPreferences.getString(SettingsActivity.PREF_VIEW_KEY, "list"));
        }
		//mSettingsListener.onSortingChanged(mPreferences.getString(SettingsActivity.PREF_SORT_KEY, "type"));
        refreshCursors();
    }
    
    public Cursor getPhotoCursor() { return mPhotoCursor; }
    public Cursor getVideoCursor() { return mVideoCursor; }
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	unregisterReceiver(storageReceiver);
    }
    
    public void refreshCursors()
    {
        if(mPhotoCursor == null)
        	mPhotoCursor = managedQuery(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        		new String[]{"_id", "_display_name", "_data", "_size", "date_modified"},
				MediaStore.Images.Media.SIZE + " > 0", null,
				MediaStore.Images.Media.DATE_ADDED + " DESC");
        else mPhotoCursor.requery();
        if(mVideoCursor == null)
        	mVideoCursor = managedQuery(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        		new String[]{"_id", "_display_name", "_data", "_size", "date_modified"},
				MediaStore.Video.Media.SIZE + " > 0", null,
				MediaStore.Video.Media.BUCKET_DISPLAY_NAME + " ASC, " +
				MediaStore.Video.Media.DATE_MODIFIED + " DESC");
        else mVideoCursor.requery();
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
    
    public DirContentActivity getDirContentFragment()
    {
    	Fragment frag = fragmentManager.findFragmentById(R.id.content_frag);
    	if(frag != null && frag.getClass().equals(DirContentActivity.class))
    		return (DirContentActivity)fragmentManager.findFragmentById(R.id.content_frag);
    	else {
    		frag = new DirContentActivity();
    		FragmentTransaction ft = fragmentManager.beginTransaction();
    		ft.replace(R.id.content_frag, frag);
    		ft.commit();
    		return (DirContentActivity)frag;
    	}
    }
    
    public void updateTitle(String s)
    {
    	setTitle(getResources().getString(R.string.app_name) + " - " + s);
    }
    
    public void editFile(String path)
    {
    	TextEditorFragment editor = new TextEditorFragment(path);
    	FragmentTransaction ft = fragmentManager.beginTransaction();
    	ft.replace(R.id.content_frag, editor);
    	ft.addToBackStack("edit");
    	ft.commit();
    }
    
    public boolean onCreateOptionsMenu(Menu menu) {
    	getMenuInflater().inflate(R.menu.actbar, menu);
    	return true;
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
	    			DialogHandler dialog = DialogHandler.newDialog(DialogHandler.HOLDINGFILE_DIALOG, this);
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
	    	case MENU_DIR:
	    		mEvHandler.createNewFolder(mFileManger.peekStack().getPath());
	    		return true;
	    		
	    	case R.id.menu_multi:
	    	case MENU_MULTI:
	    		if(mActionMode != null)
	    			return false;
	    		
	    		mActionMode = startActionMode(mMultiSelectAction);
	    		return true;
	    		
	    	case MENU_SORT:
	    		return true;
	    	
	    	case R.id.menu_sort_name_asc:	mSettingsListener.onSortingChanged(FileManager.SortType.ALPHA); return true; 
	    	case R.id.menu_sort_name_desc:	mSettingsListener.onSortingChanged(FileManager.SortType.ALPHA_DESC); return true; 
	    	case R.id.menu_sort_date_asc: 	mSettingsListener.onSortingChanged(FileManager.SortType.DATE); return true;
	    	case R.id.menu_sort_date_desc: 	mSettingsListener.onSortingChanged(FileManager.SortType.DATE_DESC); return true; 
	    	case R.id.menu_sort_size_asc: 	mSettingsListener.onSortingChanged(FileManager.SortType.SIZE); return true; 
	    	case R.id.menu_sort_size_desc: 	mSettingsListener.onSortingChanged(FileManager.SortType.SIZE_DESC); return true; 
	    	case R.id.menu_sort_type: 		mSettingsListener.onSortingChanged(FileManager.SortType.TYPE); return true;
	    	
	    	case R.id.menu_view_grid: mSettingsListener.onViewChanged("grid"); return true;
	    	case R.id.menu_view_list: mSettingsListener.onViewChanged("list"); return true;
	    	case R.id.menu_view_hidden: mSettingsListener.onHiddenFilesChanged(item.isChecked()); return true;
	    	case R.id.menu_view_thumbs: mSettingsListener.onThumbnailChanged(item.isChecked()); return true;
	    	    	
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
	    	case MENU_SETTINGS:
	    		startActivityForResult(new Intent(this, SettingsActivity.class), PREF_CODE);
	    		return true;
	    		
	    	case R.id.menu_search:
	    	case MENU_SEARCH:
	    		item.setActionView(mSearchView);
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
    
	private ActionMode.Callback mMultiSelectAction = new ActionMode.Callback() {
		MultiSelectHandler handler;
		
		
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return false;
		}
		
		
		public void onDestroyActionMode(ActionMode mode) {			
			getDirContentFragment().changeMultiSelectState(false, handler);
			
			mActionMode = null;
			handler = null;
		}
		
		
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			handler = MultiSelectHandler.getInstance(OpenExplorer.this);
			mode.setTitle("Multi-select Options");
			
			menu.add(0, MENU_ID_DELETE, 0, "Delete");
			menu.add(0, MENU_ID_COPY, 0, "Copy");
			menu.add(0, MENU_ID_CUT, 0, "Cut");
			menu.add(0, MENU_ID_SEND, 0, "Send");
			
			getDirContentFragment().changeMultiSelectState(true, handler);
			
			return true;
		}
		
		
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
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
			
			switch(item.getItemId()) {
			case 12: /* delete */
				mEvHandler.deleteFile(mHeldFiles);
				mode.finish();
				return true;
			
			case 13: /* copy */
				getActionBar().setTitle("Holding " + files.size() + " File");
				getDirContentFragment().setCopiedFiles(mHeldFiles, false);
				
				Toast.makeText(OpenExplorer.this, 
							   "Tap the upper left corner to see your held files",
							   Toast.LENGTH_LONG).show();
				mode.finish();
				return true;
				
			case 14: /* cut */
				getActionBar().setTitle("Holding " + files.size() + " File");
				getDirContentFragment().setCopiedFiles(mHeldFiles, true);
				
				Toast.makeText(OpenExplorer.this, 
						   "Tap the upper left corner to see your held files",
						   Toast.LENGTH_LONG).show();
				mode.finish();
				return true;
				
			case 15: /* send */
				ArrayList<Uri> uris = new ArrayList<Uri>();
				Intent mail = new Intent();
				mail.setType("application/mail");

				if(mHeldFiles.size() == 1) {
					mail.setAction(android.content.Intent.ACTION_SEND);
					mail.putExtra(Intent.EXTRA_STREAM, mHeldFiles.get(0).getUri());
					startActivity(mail);
					
					mode.finish();
					return true;
				}
				
				for(int i = 0; i < mHeldFiles.size(); i++)
					uris.add(mHeldFiles.get(i).getUri());
				
				mail.setAction(android.content.Intent.ACTION_SEND_MULTIPLE);
				mail.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
				startActivity(mail);

//				this is for bluetooth
//				mEvHandler.sendFile(mHeldFiles);
				mode.finish();
				return true;
			}
			
			return false;
		}
	};
	
	public static void setOnSettingsChangeListener(OnSettingsChangeListener e) {
    	mSettingsListener = e;
    }
    
    /*
     * used to inform the user when they are holding a file to copy, zip, et cetera
     * When the user does something with the held files (from copy or cut) this is 
     * called to reset the apps title. When that happens we will get rid of the cached
     * held files if there are any.  
     * @param title the title to be displayed
     */
    public void changeActionBarTitle(String title) {
    	if (title.equals(getResources().getString(R.string.app_name)) && mHeldFiles != null) {
	    	mHeldFiles.clear();
	    	mHeldFiles = null;
    	}
    	getActionBar().setTitle(title);
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
    	Fragment fragList = fragmentManager.findFragmentById(R.id.list_frag);
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

	
	public void onBackStackChanged() {
		int i = fragmentManager.getBackStackEntryCount();
		if(i < mLastBackIndex)
			getDirContentFragment().goBack();
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
	
}

