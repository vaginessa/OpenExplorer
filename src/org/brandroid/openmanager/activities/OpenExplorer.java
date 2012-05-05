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

package org.brandroid.openmanager.activities;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.NetworkInfo.State;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Parcelable;
import android.os.StatFs;
import android.provider.MediaStore;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTP.OnFTPCommunicationListener;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentManager.BackStackEntry;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.util.LruCache;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.InputType;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.PasswordTransformationMethod;
import android.text.method.SingleLineTransformationMethod;
import android.text.style.ForegroundColorSpan;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.InflateException;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewStub;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.TextView.BufferType;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.RejectedExecutionException;

import jcifs.smb.ServerMessageBlock;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFile.OnSMBCommunicationListener;

import org.brandroid.openmanager.R;
import org.brandroid.openmanager.adapters.ArrayPagerAdapter;
import org.brandroid.openmanager.adapters.OpenBookmarks;
import org.brandroid.openmanager.adapters.OpenClipboard;
import org.brandroid.openmanager.adapters.OpenPathDbAdapter;
import org.brandroid.openmanager.adapters.PagerTabsAdapter;
import org.brandroid.openmanager.adapters.ArrayPagerAdapter.OnPageTitleClickListener;
import org.brandroid.openmanager.adapters.IconContextMenu.IconContextItemSelectedListener;
import org.brandroid.openmanager.adapters.OpenBookmarks.BookmarkType;
import org.brandroid.openmanager.adapters.OpenClipboard.OnClipboardUpdateListener;
import org.brandroid.openmanager.adapters.IconContextMenu;
import org.brandroid.openmanager.adapters.IconContextMenuAdapter;
import org.brandroid.openmanager.data.FTPManager;
import org.brandroid.openmanager.data.OpenContent;
import org.brandroid.openmanager.data.OpenCursor;
import org.brandroid.openmanager.data.OpenFTP;
import org.brandroid.openmanager.data.OpenFile;
import org.brandroid.openmanager.data.OpenMediaStore;
import org.brandroid.openmanager.data.OpenNetworkPath;
import org.brandroid.openmanager.data.OpenPath;
import org.brandroid.openmanager.data.OpenPathArray;
import org.brandroid.openmanager.data.OpenSFTP;
import org.brandroid.openmanager.data.OpenSmartFolder;
import org.brandroid.openmanager.data.OpenZip;
import org.brandroid.openmanager.data.OpenSmartFolder.SmartSearch;
import org.brandroid.openmanager.fragments.CarouselFragment;
import org.brandroid.openmanager.fragments.DialogHandler;
import org.brandroid.openmanager.fragments.ContentFragment;
import org.brandroid.openmanager.fragments.LogViewerFragment;
import org.brandroid.openmanager.fragments.OpenFragment;
import org.brandroid.openmanager.fragments.OperationsFragment;
import org.brandroid.openmanager.fragments.OpenFragment.OnFragmentTitleLongClickListener;
import org.brandroid.openmanager.fragments.OpenPathFragmentInterface;
import org.brandroid.openmanager.fragments.PreferenceFragmentV11;
import org.brandroid.openmanager.fragments.SearchResultsFragment;
import org.brandroid.openmanager.fragments.TextEditorFragment;
import org.brandroid.openmanager.interfaces.OpenApp;
import org.brandroid.openmanager.util.BetterPopupWindow;
import org.brandroid.openmanager.util.EventHandler;
import org.brandroid.openmanager.util.EventHandler.OnWorkerUpdateListener;
import org.brandroid.openmanager.util.FileManager.SortType;
import org.brandroid.openmanager.util.MimeTypes;
import org.brandroid.openmanager.util.OpenInterfaces.OnBookMarkChangeListener;
import org.brandroid.openmanager.util.MimeTypeParser;
import org.brandroid.openmanager.util.OpenInterfaces;
import org.brandroid.openmanager.util.RootManager;
import org.brandroid.openmanager.util.FileManager;
import org.brandroid.openmanager.util.SimpleHostKeyRepo;
import org.brandroid.openmanager.util.SimpleUserInfo;
import org.brandroid.openmanager.util.SimpleUserInfo.UserInfoInteractionCallback;
import org.brandroid.openmanager.util.ThumbnailCreator;
import org.brandroid.openmanager.util.ThumbnailCreator.OnUpdateImageListener;
import org.brandroid.openmanager.views.OpenPathList;
import org.brandroid.openmanager.views.OpenViewPager;
import org.brandroid.utils.CustomExceptionHandler;
import org.brandroid.utils.DiskLruCache;
import org.brandroid.utils.ImageUtils;
import org.brandroid.utils.Logger;
import org.brandroid.utils.LoggerDbAdapter;
import org.brandroid.utils.MenuBuilder;
import org.brandroid.utils.MenuItemImpl;
import org.brandroid.utils.MenuUtils;
import org.brandroid.utils.Preferences;
import org.brandroid.utils.SubmitStatsTask;

import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.DownloadCache;
import com.android.gallery3d.data.ImageCacheService;
import com.android.gallery3d.util.ThreadPool;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.viewpagerindicator.PageIndicator;
import com.viewpagerindicator.TabPageIndicator;

import org.xmlpull.v1.XmlPullParserException;

public class OpenExplorer
		extends OpenFragmentActivity
		implements OnBackStackChangedListener, OnClipboardUpdateListener,
			OnWorkerUpdateListener,
			OnPageTitleClickListener, LoaderCallbacks<Cursor>, OnPageChangeListener,
			OpenApp
	{

	private static final int PREF_CODE =		0x6;
	private static final int REQ_SPLASH = 7;
	private static final int REQ_INTENT = 8;
	public static final int VIEW_LIST = 0;
	public static final int VIEW_GRID = 1;
	public static final int VIEW_CAROUSEL = 2;
	
	public static final boolean BEFORE_HONEYCOMB = Build.VERSION.SDK_INT < 11;
	public static boolean CAN_DO_CAROUSEL = false;
	public static boolean USE_ACTION_BAR = false;
	public static boolean USE_ACTIONMODE = false;
	public static boolean USE_SPLIT_ACTION_BAR = true;
	public static boolean IS_DEBUG_BUILD = false;
	public static boolean LOW_MEMORY = false;
	public static final boolean SHOW_FILE_DETAILS = false;
	public static boolean USE_PRETTY_MENUS = true;
	public static boolean USE_PRETTY_CONTEXT_MENUS = true;
	
	private static MimeTypes mMimeTypes;
	private Object mActionMode;
	private static OpenClipboard mClipboard;
	private int mLastBackIndex = -1;
	private OpenPath mLastPath = null;
	private BroadcastReceiver storageReceiver = null;
	private Handler mHandler = new Handler();  // handler for the main thread
	//private int mViewMode = VIEW_LIST;
	//private static long mLastCursorEnsure = 0;
	private static boolean mRunningCursorEnsure = false;
	private Boolean mSinglePane = false;
	private Boolean mStateReady = true;
	
	private static LogViewerFragment mLogFragment = null;
	private static boolean mLogViewEnabled = true;
	private OpenViewPager mViewPager;
	private static ArrayPagerAdapter mViewPagerAdapter;
	private static final boolean mViewPagerEnabled = true; 
	private ExpandableListView mBookmarksList;
	private OpenBookmarks mBookmarks;
	private BetterPopupWindow mBookmarksPopup;
	private static OnBookMarkChangeListener mBookmarkListener;
	protected MenuBuilder mMainMenu = null;
	private IconContextMenu mOpenMenu = null;
	private ViewGroup mToolbarButtons = null;
	
	private static boolean bRetrieveDimensionsForPhotos = Build.VERSION.SDK_INT >= 10;
	private static boolean bRetrieveExtraVideoDetails = Build.VERSION.SDK_INT > 8;
	private static boolean bRetrieveCursorFiles = Build.VERSION.SDK_INT > 10;
	
	private static final FileManager mFileManager = new FileManager();
	private static final EventHandler mEvHandler = new EventHandler(mFileManager);
	
	private FragmentManager fragmentManager;
	
	private final static OpenCursor
			mPhotoParent = new OpenCursor("Photos"),
			mVideoParent = new OpenCursor("Videos"),
			mMusicParent = new OpenCursor("Music"),
			mApkParent = new OpenCursor("Apps");
	private final static OpenSmartFolder
			mDownloadParent = new OpenSmartFolder("Downloads");

	
	public boolean isViewPagerEnabled() { return mViewPagerEnabled; }
	
	private void loadPreferences()
	{
		Preferences prefs = getPreferences();

		//mViewPagerEnabled = prefs.getBoolean("global", "pref_pagers", true);
		//USE_ACTIONMODE = getPreferences().getBoolean("global", "pref_actionmode", false);
		
		Preferences.Pref_Intents_Internal = prefs.getBoolean("global", "pref_intent_internal", true);
		Preferences.Pref_Text_Internal = prefs.getBoolean("global", "pref_text_internal", true);
		Preferences.Pref_Zip_Internal = prefs.getBoolean("global", "pref_zip_internal", true);
		
		USE_PRETTY_MENUS = prefs.getBoolean("global", "pref_fancy_menus", USE_PRETTY_MENUS);
		USE_PRETTY_CONTEXT_MENUS = prefs.getBoolean("global", "pref_fancy_context", USE_PRETTY_CONTEXT_MENUS);
		
		String s = prefs.getString("global", "pref_location_ext", null);
		if(s != null && new OpenFile(s).exists())
			OpenFile.setExternalMemoryDrive(new OpenFile(s));
		else prefs.setSetting("global", "pref_location_ext", OpenFile.getExternalMemoryDrive(true).getPath());
		
		s = prefs.getString("global", "pref_location_int", null);
		if(s != null && new OpenFile(s).exists())
			OpenFile.setInternalMemoryDrive(new OpenFile(s));
		else prefs.setSetting("global", "pref_location_int", OpenFile.getInternalMemoryDrive().getPath());
	}
	
	public void onCreate(Bundle savedInstanceState)
	{
		Thread.setDefaultUncaughtExceptionHandler(new CustomExceptionHandler());
		
		if(getPreferences().getBoolean("global", "pref_fullscreen", false))
		{
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
								 WindowManager.LayoutParams.FLAG_FULLSCREEN);
		} //else getWindow().addFlags(WindowManager.LayoutParams.FLAG
		else {
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}
		
		loadPreferences();
		
		if(getPreferences().getBoolean("global", "pref_hardware_accel", true) && !BEFORE_HONEYCOMB)
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED, WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);

		if(BEFORE_HONEYCOMB)
		{
			requestWindowFeature(Window.FEATURE_NO_TITLE);
			USE_ACTION_BAR = false;
		} else if(!BEFORE_HONEYCOMB) {
			USE_ACTION_BAR = true;
			requestWindowFeature(Window.FEATURE_ACTION_BAR);
			ActionBar ab = getActionBar();
			if(ab != null)
			{
				if(Build.VERSION.SDK_INT >= 14)
					ab.setHomeButtonEnabled(true);
				ab.setDisplayUseLogoEnabled(true);
				try {
					ab.setCustomView(R.layout.title_bar);
					ab.setDisplayShowCustomEnabled(true);
					ab.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
					ViewGroup cv = (ViewGroup)ab.getCustomView();
					if(cv.findViewById(R.id.title_menu) != null)
						cv.findViewById(R.id.title_menu).setVisibility(View.GONE);
					if(cv.findViewById(R.id.title_paste) != null)
						cv.removeView(cv.findViewById(R.id.title_paste));
					//ab.getCustomView().findViewById(R.id.title_icon).setVisibility(View.GONE);
				} catch(InflateException e) {
					Logger.LogWarning("Couldn't set up ActionBar custom view", e);
				}
			} else USE_ACTION_BAR = false;
		}
		
		setupLoggingDb();
		handleExceptionHandler();
		getMimeTypes();
		setupFilesDb();
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_fragments);
		getWindow().setBackgroundDrawableResource(R.drawable.background_holo_dark);
		
		try {
			upgradeViewSettings();
		} catch(Exception e) { }
		//try {
			showWarnings();
		//} catch(Exception e) { }
		
		mEvHandler.setUpdateListener(this);
		
		mClipboard = new OpenClipboard(this);
		mClipboard.setClipboardUpdateListener(this);
		
		try {
			/*
			Signature[] sigs = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNATURES).signatures;
			for(Signature sig : sigs)
				if(sig.toCharsString().indexOf("4465627567") > -1) // check for "Debug" in signature
					IS_DEBUG_BUILD = true;
			*/
			IS_DEBUG_BUILD = (getPackageManager().getActivityInfo(getComponentName(), 0)
					.applicationInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) ==
						ApplicationInfo.FLAG_DEBUGGABLE;
			if(IS_DEBUG_BUILD && Build.MANUFACTURER.equals("RIM"))
				IS_DEBUG_BUILD = false;
		} catch (NameNotFoundException e1) { }

		handleNetworking();
		
		refreshCursors();

		try {
			if(isGTV() && !getPreferences().getBoolean("global", "welcome", false))
			{
				showToast("Welcome, GoogleTV user!");
				getPreferences().setSetting("global", "welcome", true);
			}
		} catch(Exception e) { Logger.LogWarning("Couldn't check for GTV", e); }
		
		try {
			if(getPreferences().getSetting("global", "pref_root", false) ||
					Preferences.getPreferences(getApplicationContext(), "global").getBoolean("pref_root", false))
				RootManager.Default.requestRoot();
			else if(RootManager.Default.isRoot())
				RootManager.Default.exitRoot();
		} catch(Exception e) { Logger.LogWarning("Couldn't get root.", e); }
		
		if(!BEFORE_HONEYCOMB)
		{
			if(Build.VERSION.SDK_INT < 15)
			{
				getActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.actionbar_shadow));
				setViewVisibility(false, false, R.id.title_underline);
			}
			//if(USE_ACTION_BAR)
			//	setViewVisibility(false, false, R.id.title_bar, R.id.title_underline, R.id.title_underline_2);
		}
		if(BEFORE_HONEYCOMB || !USE_ACTION_BAR)
		{
			ViewStub mTitleStub = (ViewStub)findViewById(R.id.title_stub);
			if(mTitleStub != null)
			{
				mTitleStub.inflate();
				setViewVisibility(true, false, R.id.title_underline);
			}
			ViewStub mBaseStub = (ViewStub)findViewById(R.id.base_stub);
			if(mBaseStub != null)
				mBaseStub.inflate();
		}
		setViewVisibility(false, false, R.id.title_paste);
		setOnClicks(
				R.id.title_icon, R.id.title_menu,
				R.id.title_paste, R.id.title_paste_icon, R.id.title_paste_text
				//,R.id.title_sort, R.id.title_view, R.id.title_up
				);
		IconContextMenu.clearInstances();
		
		if(findViewById(R.id.list_frag) == null)
			mSinglePane = true;
		else if(findViewById(R.id.list_frag).getVisibility() == View.GONE)
			mSinglePane = true;

		OpenPath path = mLastPath;
		if(savedInstanceState == null || path == null)
		{	
			String start = getPreferences().getString("global", "pref_start", "External");
	
			if(savedInstanceState != null && savedInstanceState.containsKey("last") && !savedInstanceState.getString("last").equals(""))
				start = savedInstanceState.getString("last");

			if(start.startsWith("/"))
				path = new OpenFile(start);
			else if(start.startsWith("ftp:/"))
				path = new OpenFTP(start, null, new FTPManager());
			else if(start.startsWith("sftp:/"))
				path = new OpenSFTP(start);
			else if(start.equals("Videos"))
				path = mVideoParent;
			else if(start.equals("Photos"))
				path = mPhotoParent;
			else if(start.equals("Music"))
				path = mMusicParent;
			else if(start.equals("Downloads"))
				path = mDownloadParent;
			else if(start.equals("External") && !checkForNoMedia(OpenFile.getExternalMemoryDrive(false)))
				path = OpenFile.getExternalMemoryDrive(false);
			else if(start.equals("Internal") || start.equals("External"))
				path = OpenFile.getInternalMemoryDrive();
			else
				path = new OpenFile(start);
			if(path == null || !path.exists())
				path = OpenFile.getInternalMemoryDrive();
			if(path == null || !path.exists())
				path = new OpenFile("/");
		}
		
		if(checkForNoMedia(path))
			showToast(R.string.s_error_no_media, Toast.LENGTH_LONG);
		
		mLastPath = path;
		
		boolean bAddToStack = true;

		int mViewMode = getSetting(path, "view", 0);

		if(mViewPagerEnabled && findViewById(R.id.content_pager_frame_stub) != null)
			((ViewStub)findViewById(R.id.content_pager_frame_stub)).inflate();
		
		if(fragmentManager == null)
		{
			fragmentManager = getSupportFragmentManager();
			fragmentManager.addOnBackStackChangedListener(this);
		}
		
		mLogFragment = new LogViewerFragment();
		if(findViewById(R.id.frag_log) != null)
		{
			fragmentManager.beginTransaction().add(R.id.frag_log, mLogFragment, "log").commit();
			findViewById(R.id.frag_log).setVisibility(View.GONE);
		}

		FragmentTransaction ft = fragmentManager.beginTransaction();

		Logger.LogDebug("Creating with " + path.getPath());
		if(path instanceof OpenFile)
			new PeekAtGrandKidsTask().execute((OpenFile)path);

		initPager();
		if(handleIntent(getIntent()))
		{
			path = mLastPath = null;
			bAddToStack = false;
		}
		
		/*if(findViewById(R.id.content_frag) != null && !mViewPagerEnabled)
		{
			if(bAddToStack)
			{
				if(fragmentManager.getBackStackEntryCount() == 0 ||
						!mLastPath.getPath().equals(fragmentManager.getBackStackEntryAt(fragmentManager.getBackStackEntryCount() - 1).getBreadCrumbTitle()))
					ft
						.addToBackStack("path")
					.replace(R.id.content_frag, mContentFragment)
					.setBreadCrumbTitle(path.getPath());
			else Logger.LogWarning("Damn it dog, stay!");
			} else {
				ft.replace(R.id.content_frag, mContentFragment);
				ft.disallowAddToBackStack();
			}
			updateTitle(mLastPath.getPath());
		} else */
		if(mViewPager != null && mViewPagerAdapter != null && path != null)
		{
			//mViewPagerAdapter.add(mContentFragment);
			mLastPath = null;
			changePath(path, bAddToStack, true);
			setCurrentItem(mViewPagerAdapter.getCount() - 1, false);
			restoreOpenedEditors();
		}

		ft.commit();

		setupBaseBarButtons();
		initBookmarkDropdown();
		
		setOnClicks(R.id.menu_view, R.id.menu_sort);
		
		//updateTitle(mLastPath.getPath());
		if(!BEFORE_HONEYCOMB)
			invalidateOptionsMenu();
		
		handleMediaReceiver();

		if(!getPreferences().getBoolean("global", "pref_splash", false))
			showSplashIntent(this, getPreferences().getString("global", "pref_start", "Internal"));
	}
	
	private void handleNetworking()
	{
		FileManager.DefaultUserInfo = new SimpleUserInfo();
		SimpleUserInfo.setInteractionCallback(new UserInfoInteractionCallback() {
			
			public boolean promptPassword(String message) {
				String mPassword = null;
				View view = ((LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE))
						.inflate(R.layout.prompt_password, null);
				TextView tv = (TextView)view.findViewById(android.R.id.message);
				tv.setText(message);
				final EditText text1 = ((EditText)view.findViewById(android.R.id.text1));
				final CheckBox checkpw = (CheckBox)view.findViewById(android.R.id.checkbox);
				checkpw.setOnCheckedChangeListener(new OnCheckedChangeListener(){
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if(isChecked)
					{
						text1.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
						text1.setTransformationMethod(new SingleLineTransformationMethod());
					} else {
						text1.setRawInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
						text1.setTransformationMethod(new PasswordTransformationMethod());
					}
				}});
				
				AlertDialog dlg = new AlertDialog.Builder(getApplicationContext())
					.setTitle(R.string.s_prompt_password)
					.setView(view)
					.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							String mPassword = text1.getText().toString();
							onPasswordEntered(mPassword);
							onYesNoAnswered(true);
						}
					})
					.setNegativeButton(android.R.string.no, new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							onYesNoAnswered(false);
						}
					})
					.create();
				dlg.show();
				return true;
			}
			
			@Override
			public void onYesNoAnswered(boolean yes) {
			}
			
			@Override
			public void onPasswordEntered(String password) {
			}

			@Override
			public boolean promptYesNo(final String message) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						AlertDialog dlg = new AlertDialog.Builder(getApplicationContext())
							.setMessage(message)
							.setPositiveButton(android.R.string.yes, new OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									onYesNoAnswered(true);
								}
							})
							.setNegativeButton(android.R.string.no, new OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									onYesNoAnswered(false);
								}
							})
							.create();
						dlg.show();
					}
				});
				return true;
			}
		});
		try {
			OpenSFTP.DefaultJSch.setHostKeyRepository(
					new SimpleHostKeyRepo(
						OpenSFTP.DefaultJSch,
						FileManager.DefaultUserInfo,
						Preferences.getPreferences(getApplicationContext(), "hosts")));
			OpenNetworkPath.Timeout = getPreferences().getSetting("global", "server_timeout", 20) * 1000;
		} catch (JSchException e) {
			Logger.LogWarning("Couldn't set Preference-backed Host Key Repository", e);
		}
	}

	private MimeTypes getMimeTypes()
	{
		return getMimeTypes(this);
	}
	
	public static MimeTypes getMimeTypes(Context c)
	{
		if(mMimeTypes != null) return mMimeTypes;
		if(MimeTypes.Default != null) return MimeTypes.Default;
		MimeTypeParser mtp = null;
		try {
			mtp = new MimeTypeParser(c, c.getPackageName());
		} catch (NameNotFoundException e) {
			//Should never happen
		}

		XmlResourceParser in = c.getResources().getXml(R.xml.mimetypes);
		
		try {
			 mMimeTypes = mtp.fromXmlResource(in);
		} catch (XmlPullParserException e) {
			Logger.LogError("Couldn't parse mimeTypes.", e);
			throw new RuntimeException("PreselectedChannelsActivity: XmlPullParserException");
		} catch (IOException e) {
			Logger.LogError("PreselectedChannelsActivity: IOException", e);
			throw new RuntimeException("PreselectedChannelsActivity: IOException");
		}
		MimeTypes.Default = mMimeTypes;
		return mMimeTypes;
	}
	
	private void setCurrentItem(final int page, final boolean smooth)
	{
		try {
			//if(!Thread.currentThread().equals(UiThread))
				mViewPager.post(new Runnable() {
					public void run() {
						if(mViewPager.getCurrentItem() != page)
							mViewPager.setCurrentItem(page, smooth);
					}
				});
			//else if(mViewPager.getCurrentItem() != page)
			//	mViewPager.setCurrentItem(page, smooth);
		} catch(Exception e) {
			Logger.LogError("Couldn't set ViewPager page to " + page, e);
		}
	}

	private void handleExceptionHandler()
	{
		if(Logger.checkWTF())
		{
			if(!getSetting(null, "pref_autowtf", false))
				showWTFIntent();
			else if(isNetworkConnected())
				new SubmitStatsTask(this).execute(
						Logger.getCrashReport(true));
		}
	}

	private void showWTFIntent() {
		Intent intent = new Intent(this, WTFSenderActivity.class);
		startActivity(intent);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		Logger.LogDebug("New Intent! " + intent.toString());
		setIntent(intent);
		handleIntent(intent);
	}

	public void showMenu() {
		if(USE_PRETTY_MENUS)
			showMenu(R.menu.main_menu, findViewById(R.id.title_menu));
		else
			openOptionsMenu();
		//showMenu(R.menu.main_menu_top);
	}
	public boolean showMenu(int menuId, final View from)
	{
		Logger.LogVerbose("showMenu(0x" + Integer.toHexString(menuId) + "," + (from != null ? from.toString() : "NULL") + ")");
		//if(mMenuPopup == null)
		if(menuId == R.id.title_menu || menuId == R.menu.main_menu)
			showContextMenu(mMainMenu, from instanceof CheckedTextView ? null : from);
		else if (menuId == R.id.menu_sort
				|| menuId == R.menu.menu_sort
				|| menuId == R.menu.menu_sort_flat)
		{
			if(from != null && !USE_SPLIT_ACTION_BAR)
			{
				from.setOnCreateContextMenuListener(this);
				if(from.showContextMenu())
					return true;
				if(showContextMenu(R.menu.menu_sort_flat, findViewById(R.id.title_menu)) != null)
					return true;
				from.setOnCreateContextMenuListener(null);
			}
			if(showContextMenu(R.menu.menu_sort_flat, from) != null)
				return true;
			else if(from != null && from.showContextMenu()) return true;
			//else showToast("Oops");
		}
		if(menuId == R.id.menu_view
				|| menuId == R.menu.menu_view
				|| menuId == R.menu.menu_view_flat)
		{
			if(from != null && !USE_SPLIT_ACTION_BAR)
			{
				from.setOnCreateContextMenuListener(this);
				if(from.showContextMenu())
					return true;
				if(showContextMenu(R.menu.menu_view_flat, findViewById(R.id.title_menu)) != null)
					return true;
				from.setOnCreateContextMenuListener(null);
			}
			if(showContextMenu(R.menu.menu_view_flat, from) != null)
				return true;
			else if(from != null && from.showContextMenu()) return true;
			//else showToast("Oops");
		} else if(menuId == R.menu.text_view)
		{
			if(from != null && !USE_SPLIT_ACTION_BAR)
			{
				from.setOnCreateContextMenuListener(this);
				if(from.showContextMenu())
					return true;
				if(showContextMenu(R.menu.text_view, findViewById(R.id.title_menu)) != null)
					return true;
			}
			if(showContextMenu(R.menu.text_view_flat, from) != null)
				return true;
		}
		else if(from != null && !(from instanceof CheckedTextView) && 
				showContextMenu(menuId, from instanceof CheckedTextView ? null : from) != null)
			return true;
		if(IS_DEBUG_BUILD && BEFORE_HONEYCOMB)
			showToast("Invalid option (" + menuId + ")" + (from != null ? " under " + from.toString() + " (" + from.getLeft() + "," + from.getTop() + ")" : ""));
		return false;
	}

	/*
	 * This should only be used with the "main" menu
	 */
	public void showMenu(MenuBuilder menu, final View from)
	{
		if(from != null){
			if(showContextMenu(menu, from) == null)
				openOptionsMenu();
		} else openOptionsMenu();
	}
	
	public IconContextMenu showContextMenu(int menuId, final View from)
	{
		try {
			if(menuId == R.id.menu_sort || menuId == R.menu.menu_sort)
				menuId = R.menu.menu_sort;
			else if(menuId == R.id.menu_view || menuId == R.menu.menu_view)
				menuId = R.menu.menu_view;
			else if(menuId == R.id.title_menu || menuId == R.menu.main_menu)
				menuId = R.menu.main_menu;
			else if(menuId == R.menu.menu_sort_flat)
				menuId = R.menu.menu_sort_flat;
			else if(menuId == R.menu.menu_view_flat)
				menuId = R.menu.menu_view_flat;
			else if(menuId == R.menu.text_view)
				menuId = R.menu.text_view;
			else if(menuId == R.menu.text_view_flat)
				menuId = R.menu.text_view_flat;
			else {
				Logger.LogWarning("Unknown menuId (" + menuId + ")!");
				return null;
			}
			Logger.LogDebug("Trying to show context menu 0x" + Integer.toHexString(menuId) + (from != null ? " under " + from.toString() + " (" + from.getLeft() + "," + from.getTop() + ")" : "") + ".");
			if(menuId == R.menu.context_file ||
				menuId == R.menu.main_menu ||
				menuId == R.menu.menu_sort ||
				menuId == R.menu.menu_view ||
				menuId == R.menu.text_view ||
				menuId == R.menu.text_view_flat ||
				menuId == R.menu.menu_sort_flat ||
				menuId == R.menu.menu_view_flat)
			{
				//IconContextMenu icm1 = new IconContextMenu(getApplicationContext(), menu, from, null, null);
				//MenuBuilder menu = IconContextMenu.newMenu(this, menuId);
				mOpenMenu = IconContextMenu.getInstance(this, menuId, from, null, null);
				onPrepareOptionsMenu(mOpenMenu.getMenu());
				mOpenMenu.setAnchor(from);
				if(mOpenMenu.getMenu().findItem(R.id.menu_context_copy) != null)
				{
					mOpenMenu.setNumColumns(2);
					//icm.setPopupWidth(getResources().getDimensionPixelSize(R.dimen.popup_width) / 2);
					mOpenMenu.setTextLayout(R.layout.context_item);
				} else mOpenMenu.setNumColumns(1);
				mOpenMenu.setOnIconContextItemSelectedListener(new IconContextItemSelectedListener() {
					public void onIconContextItemSelected(MenuItem item, Object info, View view) {
						//showToast(item.getTitle().toString());
						if(item.getItemId() == R.id.menu_sort)
							showMenu(R.menu.menu_sort, view);
						else if(item.getItemId() == R.id.menu_view)
							showMenu(R.menu.menu_view, view);
						else
							onClick(item.getItemId(), item, view);
						//mOpenMenu.dismiss();
						//mMenuPopup.dismiss();
					}
				});
				/*
				if(menuId == R.menu.menu_sort || menuId == R.menu.menu_sort_flat)
					mOpenMenu.setTitle(getString(R.string.s_menu_sort) + " (" + getDirContentFragment(false).getPath().getPath() + ")");
				else if(menuId == R.menu.menu_view || menuId == R.menu.menu_view_flat)
					mOpenMenu.setTitle(getString(R.string.s_view) + " (" + getDirContentFragment(false).getPath().getPath() + ")");
				*/
				mOpenMenu.show();
				return mOpenMenu;
			} else {
				showMenu(menuId, from);
			}
		} catch(Exception e) {
			Logger.LogWarning("Couldn't show icon context menu" + (from != null ? " under " + from.toString() + " (" + from.getLeft() + "," + from.getTop() + ")" : "") + ".", e);
			if(from != null)
				return showContextMenu(menuId, null);
		}
		Logger.LogWarning("Not sure what happend with " + menuId + (from != null ? " under " + from.toString() + " (" + from.getLeft() + "," + from.getTop() + ")" : "") + ".");
		return null;
	}
	public IconContextMenu showContextMenu(MenuBuilder menu, final View from)
	{
		Logger.LogDebug("Trying to show context menu " + menu.toString() + (from != null ? " under " + from.toString() + " (" + from.getLeft() + "," + from.getTop() + ")" : "") + ".");
		try {
			if(mToolbarButtons != null)
				for(int i = menu.size() - 1; i >= 0; i--)
				{
					MenuItem item = menu.getItem(i);
					if(mToolbarButtons.findViewById(item.getItemId()) != null)
						menu.removeItemAt(i);
				}
			mOpenMenu = new IconContextMenu(this, menu, from, null, null);
			if(menu.findItem(R.id.menu_context_bookmark) != null)
				mOpenMenu.setNumColumns(2);
			else mOpenMenu.setNumColumns(1);
			mOpenMenu.setOnIconContextItemSelectedListener(new IconContextItemSelectedListener() {
				public void onIconContextItemSelected(MenuItem item, Object info, View view) {
					//showToast(item.getTitle().toString());
					int id = item.getItemId();
					if(id == R.id.menu_sort || id == R.id.menu_view)
					{
						view.setOnCreateContextMenuListener(OpenExplorer.this);
						if(view.showContextMenu())
							return;
					}
					onClick(id, item, view);
					//if(mOpenMenu != null)
					//	mOpenMenu.dismiss();
					//mMenuPopup.dismiss();
				}
			});
			mOpenMenu.show();
		} catch(Exception e) {
			Logger.LogWarning("Couldn't show icon context menu.", e);
		}
		return mOpenMenu;
	}
	
	
	/*
	 * Returns true if the Intent was "Handled"
	 */
	public boolean handleIntent(Intent intent)
	{
		if(Intent.ACTION_SEARCH.equals(intent.getAction()))
		{
			OpenPath searchIn = new OpenFile("/");
			Bundle bundle = intent.getBundleExtra(SearchManager.APP_DATA);
			if(bundle != null && bundle.containsKey("path"))
				try {
					searchIn = FileManager.getOpenCache(bundle.getString("path"), false, null);
				} catch (IOException e) {
					searchIn = new OpenFile(bundle.getString("path"));
				}
			SearchResultsFragment srf = new SearchResultsFragment(searchIn, intent.getStringExtra(SearchManager.QUERY));
			if(mViewPagerEnabled && mViewPagerAdapter != null)
			{
				mViewPagerAdapter.add(srf);
				setViewPageAdapter(mViewPagerAdapter);
				setCurrentItem(mViewPagerAdapter.getCount() - 1, true);
			} else {
				getSupportFragmentManager().beginTransaction()
					.replace(R.id.content_frag, srf)
					.commit();
			}
		}
		else if ((Intent.ACTION_VIEW.equals(intent.getAction()) || Intent.ACTION_EDIT.equals(intent.getAction()))
				&& intent.getData() != null)
		{
			OpenPath path = null;
			if(intent.getDataString().startsWith("content://"))
				path = new OpenContent(intent.getData(), this);
			else
				try {
					path = FileManager.getOpenCache(intent.getDataString(), false, null);
				} catch (IOException e) {
					Logger.LogError("Couldn't get file from cache.", e);
				}
			if(path == null)
				path = new OpenFile(intent.getDataString());
			if(path != null) // && path.exists() && (path.isTextFile() || path.length() < 500000))
			{
				editFile(path);
				return true;
			}
		}else if(intent.hasExtra("state"))
		{
			Bundle state = intent.getBundleExtra("state");
			onRestoreInstanceState(state);
		}
		return false;
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		if(outState == null) return;
		mStateReady = false;
		if(mLogFragment != null)
			try {
				//fragmentManager.beginTransaction().remove(mLogFragment).disallowAddToBackStack().commitAllowingStateLoss();
			} catch(Exception e) { }
		try {
			Fragment f = fragmentManager.findFragmentByTag("ops");
			//if(f != null)
			//	fragmentManager.beginTransaction().remove(f).disallowAddToBackStack().commitAllowingStateLoss();
		} catch(Exception e) { }
		super.onSaveInstanceState(outState);
		if(mViewPagerAdapter != null)
		{
			Parcelable p = mViewPagerAdapter.saveState();
			if(p != null)
			{
				Logger.LogDebug("<-- Saving Fragments: " + p.toString());
				outState.putParcelable("oe_fragments", p);
				outState.putInt("oe_frag_index", mViewPager.getCurrentItem());
			}
		}
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle state) {
		super.onRestoreInstanceState(state);
		mStateReady = true;
		if(state != null && state.containsKey("oe_fragments"))
		{
			mViewPagerAdapter.restoreState(state, getClassLoader());
			setViewPageAdapter(mViewPagerAdapter);
			setCurrentItem(state.getInt("oe_frag_index"), false);
		}
	}
	
	@Override
	public boolean onSearchRequested() {
		Bundle appData = new Bundle();
		ContentFragment cf = getDirContentFragment(false);
		if(cf == null) return false;
		OpenPath path = cf.getPath();
		appData.putString("path", path.getPath());
		startSearch(null, false, appData, false);
		return true;
	}

	private void upgradeViewSettings() {
		if(getSetting(null, "pref_up_views", false)) return;
		setSetting(null, "pref_up_views", true);
		getPreferences().upgradeViewSettings();
	}

	private void initBookmarkDropdown()
	{
		if(mBookmarksList == null)
			mBookmarksList = new ExpandableListView(this);
		if(findViewById(R.id.list_frag) != null) {
			ViewGroup leftNav = ((ViewGroup)findViewById(R.id.list_frag));
			leftNav.removeAllViews();
			leftNav.addView(mBookmarksList);
		} else {
			View anchor = null;
			if(anchor == null)
				anchor = findViewById(R.id.title_icon);
			if(anchor == null)
				anchor = findViewById(android.R.id.home);
			if(anchor == null && USE_ACTION_BAR && !BEFORE_HONEYCOMB && getActionBar() != null && getActionBar().getCustomView() != null)
				anchor = getActionBar().getCustomView();
			if(anchor == null)
				anchor = findViewById(R.id.title_bar);
			if(anchor == null)
				anchor = findViewById(R.id.base_bar);
			if(anchor == null)
				anchor = findViewById(R.id.base_row);
			mBookmarksPopup = new BetterPopupWindow(this, anchor);
			mBookmarksPopup.setContentView(mBookmarksList);
		}
		mBookmarks = new OpenBookmarks(this, mBookmarksList);
		for(int i = 0; i < mBookmarksList.getCount(); i++)
			mBookmarksList.expandGroup(i);
	}
	
	private void initPager()
	{
		mViewPager = ((OpenViewPager)findViewById(R.id.content_pager));
		TabPageIndicator indicator = null;
		if(mViewPagerEnabled && mViewPager != null)
		{
			setViewVisibility(false, false, R.id.content_frag, R.id.title_text, R.id.title_path, R.id.title_bar_inner, R.id.title_underline_2);
			setViewVisibility(true, false, R.id.content_pager, R.id.content_pager_indicator);
			mViewPager.setOnPageChangeListener(this);
			//mViewPager.setOnPageIndicatorChangeListener(this);
			View indicator_frame = findViewById(R.id.content_pager_indicator);
			try {
				//LayoutAnimationController lac = new LayoutAnimationController(AnimationUtils.makeInAnimation(getApplicationContext(), false));
				if(indicator_frame != null)
					indicator_frame.setAnimation(AnimationUtils.makeInAnimation(getApplicationContext(), false));
			} catch(Resources.NotFoundException e)
			{
				Logger.LogError("Couldn't load pager animation.", e);
			}
			indicator = (TabPageIndicator)findViewById(R.id.content_pager_indicator);
			if(indicator != null)
				mViewPager.setIndicator(indicator);
			else
				Logger.LogError("Couldn't find indicator!");
			//mViewPager = new ViewPager(getApplicationContext());
			//((ViewGroup)findViewById(R.id.content_frag)).addView(mViewPager);
			//findViewById(R.id.content_frag).setId(R.id.fake_content_id);
		} else {
			//mViewPagerEnabled = false;
			mViewPager = null; //(ViewPager)findViewById(R.id.content_pager);
			setViewVisibility(false, false, R.id.content_pager, R.id.content_pager_indicator);
			setViewVisibility(true, false, R.id.content_frag, R.id.title_text, R.id.title_path, R.id.title_bar_inner, R.id.title_underline_2);
		}

		if(mViewPager != null && mViewPagerEnabled)
		{
			Logger.LogVerbose("Setting up ViewPager");
			mViewPagerAdapter = //new PagerTabsAdapter(this, mViewPager, indicator);
					new ArrayPagerAdapter(this, mViewPager);
			mViewPagerAdapter.setOnPageTitleClickListener(this);
			setViewPageAdapter(mViewPagerAdapter);
		}

	}
	
	private boolean checkForNoMedia(OpenPath defPath)
	{
		if(defPath == null) return true;
		if(defPath instanceof OpenFile)
		{
			StatFs sf = new StatFs(defPath.getPath());
			if(sf.getBlockCount() == 0)
				return true;
			else return false;
		} else {
			try {
				return defPath.list() == null || defPath.list().length == 0;
			} catch(IOException e)
			{
				Logger.LogError("Error Checking for Media.", e);
				return true;
			}
		}
	}

	public void setViewVisibility(final boolean visible, final boolean allowAnimation, int... ids) {
		for(int id : ids)
		{
			final View v = findViewById(id);
			if(v != null && visible != (v.getVisibility() == View.VISIBLE))
			{
				if(allowAnimation)
				{
					Animation anim;
					if(visible)
						anim = AnimationUtils.makeInAnimation(getApplicationContext(), false);
					else
						anim = AnimationUtils.makeOutAnimation(getApplicationContext(), false);
					anim.setAnimationListener(new Animation.AnimationListener() {
						@Override
						public void onAnimationStart(Animation animation) {
							if(visible)
								v.setVisibility(View.VISIBLE);
						}
						
						@Override
						public void onAnimationRepeat(Animation animation) {
						}
						
						@Override
						public void onAnimationEnd(Animation animation) {
							if(!visible)
								v.setVisibility(View.GONE);
							else
								v.setVisibility(View.VISIBLE);
						}
					});
					v.startAnimation(anim);
				} else
					v.setVisibility(visible ? View.VISIBLE : View.GONE);
			}
		}
	}

	private boolean setViewPageAdapter(PagerAdapter adapter) { return setViewPageAdapter(adapter, false); }
	private boolean setViewPageAdapter(PagerAdapter adapter, boolean reload)
	{
		if(adapter == null) adapter = mViewPager.getAdapter();
		if(mViewPager != null)
		{
			try {
				if(!adapter.equals(mViewPager.getAdapter()) || reload)
					mViewPager.setAdapter(adapter);
				else {
					mViewPager.notifyDataSetChanged();
				}
				return true;
			} catch(IndexOutOfBoundsException e) {
				Logger.LogError("Why is this happening?", e);
				return false;
			} catch(IllegalStateException e) {
				Logger.LogError("Error trying to set ViewPageAdapter", e);
				return false;
			} catch(Exception e) {
				Logger.LogError("Please stop!", e);
				return false;
			}
		}
		return false;
	}
	
	@SuppressWarnings("deprecation")
	public static void launchTranslator(Activity a)
	{
		new Preferences(a).setSetting("warn", "translate", true);
		String lang = DialogHandler.getLangCode();
		Uri uri = Uri.parse("http://brandroid.org/translation_helper.php?lang=" + lang + "&full=" + Locale.getDefault().getDisplayLanguage() + "&wid=" + a.getWindowManager().getDefaultDisplay().getWidth());
		//Intent intent = new Intent(a, WebViewActivity.class);
		//intent.setData();
		//a.startActivity(intent);
		WebViewFragment web = new WebViewFragment().setUri(uri);
		if(!(a instanceof FragmentActivity))
		{
			Intent intent = new Intent(Intent.ACTION_VIEW, uri);
			a.startActivity(intent);
		} else if(a.findViewById(R.id.frag_log) != null)
		{
			((FragmentActivity)a).getSupportFragmentManager()
				.beginTransaction()
				.replace(R.id.frag_log, web)
				.addToBackStack("trans")
				.commit();
			a.findViewById(R.id.frag_log).setVisibility(View.VISIBLE);
			LayoutParams lp = a.findViewById(R.id.frag_log).getLayoutParams();
			lp.width = a.getResources().getDimensionPixelSize(R.dimen.bookmarks_width) * 2;
			a.findViewById(R.id.frag_log).setLayoutParams(lp);
		} else {
			web.setShowsDialog(true);
			web.show(((FragmentActivity)a).getSupportFragmentManager(), "trans");
			if(web.getDialog() != null)
				web.getDialog().setTitle(R.string.button_translate);
			web.setUri(uri);
		}
	}
	private void showWarnings()
	{
		// Non-viewpager disabled
		
		if(checkLanguage() > 0 && !getPreferences().getBoolean("warn", "translate", false))
		{
			//runOnUiThread(new Runnable(){public void run() {
				int msg = checkLanguage() == 1 ? R.string.alert_translate_google : R.string.alert_translate_none;
				new AlertDialog.Builder(OpenExplorer.this)
					.setCancelable(true)
					.setMessage(getString(msg, Locale.getDefault().getDisplayLanguage()))
					.setPositiveButton(R.string.button_translate,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									launchTranslator(OpenExplorer.this);
								}
							})
					.setNegativeButton(android.R.string.no,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									getPreferences().setSetting("warn", "translate", true);
								}
							})
					.create()
					.show();
			//}});
		}
	}
	
	private int checkLanguage()
	{
		String lang = DialogHandler.getLangCode();
		if(lang.equals("EN")) return 0;
		return ",ES,FR,KO,HE,DE,RU,".indexOf(","+DialogHandler.getLangCode()+",") == -1 ? 2 : 1;
	}
	
	public static void showSplashIntent(Context context, String start)
	{
		Intent intent = new Intent(context, SplashActivity.class);
		intent.putExtra("start", start);
		if(context instanceof OpenExplorer)
			((OpenExplorer)context).startActivityForResult(intent, REQ_SPLASH);
		else if(context instanceof Activity)
			((Activity)context).startActivityForResult(intent, REQ_SPLASH);
		else if(context instanceof FragmentActivity)
			((FragmentActivity)context).startActivityForResult(intent, REQ_SPLASH);
		else
			context.startActivity(intent);
	}
	
	@SuppressWarnings("unused")
	public void updatePagerTitle(int page)
	{
		TextView tvLeft = null; // (TextView)findViewById(R.id.title_left);
		TextView tvRight = null; //(TextView)findViewById(R.id.title_right);
		String left = "";
		SpannableStringBuilder ssb = new SpannableStringBuilder();
		for(int i = 0; i < page; i++)
		{
			OpenFragment f = mViewPagerAdapter.getItem(i);
			if(f instanceof ContentFragment)
			{
				OpenPath p = ((ContentFragment)f).getPath();
				left += p.getName();
				if(p.isDirectory() && !left.endsWith("/"))
					left += "/";
			}
		}
		SpannableString srLeft = new SpannableString(left);
		srLeft.setSpan(new ForegroundColorSpan(Color.GRAY), 0, left.length(), Spanned.SPAN_COMPOSING);
		ssb.append(srLeft);
		//ssb.setSpan(new ForegroundColorSpan(Color.GRAY), 0, left.length(), Spanned.SPAN_COMPOSING);
		OpenFragment curr = mViewPagerAdapter.getItem(page);
		if(curr instanceof ContentFragment)
		{
			OpenPath pCurr = ((ContentFragment)curr).getPath();
			ssb.append(pCurr.getName());
			if(pCurr.isDirectory())
				ssb.append("/");
		}
		String right = "";
		for(int i = page + 1; i < mViewPagerAdapter.getCount(); i++)
		{
			OpenFragment f = mViewPagerAdapter.getItem(i);
			if(f instanceof ContentFragment)
			{
				OpenPath p = ((ContentFragment)f).getPath();
				right += p.getName();
				if(p.isDirectory() && !right.endsWith("/"))
					right += "/";
			}
		}
		SpannableString srRight = new SpannableString(right);
		srRight.setSpan(new ForegroundColorSpan(Color.GRAY), 0, right.length(), Spanned.SPAN_COMPOSING);
		ssb.append(srRight);
		updateTitle(ssb);
	}
	
	@Override
	public void onLowMemory() {
		super.onLowMemory();
		LOW_MEMORY = true;
		showToast(R.string.s_msg_low_memory);
		ThumbnailCreator.flushCache(getApplicationContext(), false);
		FileManager.clearOpenCache();
		EventHandler.cancelRunningTasks();
	}
	
	public void setBookmarksPopupListAdapter(ListAdapter adapter)
	{
		mBookmarksList.setAdapter(adapter);
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		Logger.LogVerbose("OpenExplorer.onStart");
		setupLoggingDb();
		submitStats();
		//new Thread(new Runnable(){public void run() {refreshCursors();}}).start();;
		//refreshCursors();
		mBookmarks.scanBookmarks();
		mStateReady = true;
	}
	
	/*
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		try {
			super.onSaveInstanceState(outState);
			List<Fragment> frags = mViewPagerAdapter.getFragments();
			String[] paths = new String[frags.size()];
			for(int i = 0; i < frags.size(); i++)
			{
				Fragment f = frags.get(i);
				if(f instanceof OpenPathFragmentInterface)
					paths[i] = ((OpenPathFragmentInterface)f).getPath().toString();
			}
			outState.putStringArray("paths", paths);
			Logger.LogDebug("-->Saving fragments: " + paths.length);
		} catch(Exception e) {
			Logger.LogError("Couldn't save main state", e);
		}
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		if(savedInstanceState != null && savedInstanceState.containsKey("paths"))
		{
			String[] paths = savedInstanceState.getStringArray("paths");
			Logger.LogDebug("<--Restoring fragments: " + paths.length);
			for(int i = 0; i < paths.length; i++)
			{
				String path = paths[i];
				if(path == null) continue;
				try {
					OpenPath file = FileManager.getOpenCache(path, false, null);
					if(file == null) file = new OpenFile(path);
					if(file.isTextFile())
						mViewPagerAdapter.add(new TextEditorFragment(file));
				} catch (IOException e) {
					Logger.LogError("Couldn't get Path while restoring state", e);
				}
				
			}
			mViewPagerAdapter.notifyDataSetChanged();
		}
	}
	*/
	
	public void sendToLogView(final String txt, final int color)
	{
		Logger.LogVerbose(txt);
		if(mLogViewEnabled && mLogFragment != null && !mLogFragment.isVisible())
		{
			final View logview = findViewById(R.id.frag_log);
			if(logview != null)
			{
				logview.post(new Runnable(){
					public void run() {
						logview.setVisibility(View.VISIBLE);
						OpenFragment f = (OpenFragment)fragmentManager.findFragmentById(R.id.frag_log);
						if(f == null || !(f instanceof LogViewerFragment))
							fragmentManager.beginTransaction()
								.replace(R.id.frag_log, mLogFragment)
								.commit();
						LayoutParams lp = logview.getLayoutParams();
						lp.width = getResources().getDimensionPixelSize(R.dimen.bookmarks_width);
						logview.setLayoutParams(lp);
				}});
			} //else mLogFragment.show(fragmentManager, "log");
			else {
				mViewPager.postDelayed(new Runnable(){
					public void run() {
						if(mViewPagerAdapter.getItemPosition(mLogFragment) > -1)
						{
							mViewPagerAdapter.add(mLogFragment);
							mViewPager.setAdapter(mViewPagerAdapter);
						}
					}}, 500);
			}
		}
		if(mLogFragment != null)
			mLogFragment.print(txt, color);
	}
	private void setupLoggingDb()
	{
		FTP.setCommunicationListener(new OnFTPCommunicationListener() {
			
			@Override
			public void onDisconnect(FTP file) {
				sendToLogView("FTP Disconnect " + getFTPString(file), Color.GRAY);
			}
			
			@Override
			public void onConnectFailure(FTP file) {
				sendToLogView("FTP Failure " + getFTPString(file), Color.RED);
			}
			
			@Override
			public void onConnect(FTP file) {
				sendToLogView("FTP Connect " + getFTPString(file), Color.GREEN);
			}
			
			@Override
			public void onBeforeConnect(FTP file) {
				//sendToLogView("FTP Before Connect " + getFTPString(file));
			}

			@Override
			public void onSendCommand(FTP file, String message) {
				if(message.startsWith("PASS "))
					message = "PASS " + message.substring(6).replaceAll(".", "*");
				sendToLogView("Command: " + message.replace("\n", ""), Color.BLACK); // + getFTPString(file), Color.BLACK);
			}
			
			private String getFTPString(FTP file)
			{
				if(file != null && file.getSocket() != null && file.getRemoteAddress() != null)
					return " @ " + file.getRemoteAddress().getHostName();
				return "";
			}

			@Override
			public void onReply(String line) {
				sendToLogView("Reply: " + line, Color.BLUE);
			}
		});
		JSch.setLogger(new com.jcraft.jsch.Logger() {
			@Override
			public void log(int level, String message) {
				switch(level)
				{
					case com.jcraft.jsch.Logger.DEBUG:
						sendToLogView("SFTP - " + message, Color.GREEN);
						break;
					case com.jcraft.jsch.Logger.INFO:
						sendToLogView("SFTP - " + message, Color.BLUE);
						break;
					case com.jcraft.jsch.Logger.WARN:
						sendToLogView("SFTP - " + message, Color.YELLOW);
						break;
					case com.jcraft.jsch.Logger.ERROR:
						sendToLogView("SFTP - " + message, Color.RED);
						break;
					case com.jcraft.jsch.Logger.FATAL:
						sendToLogView("SFTP - " + message, Color.MAGENTA);
						break;
					default:
						sendToLogView("SFTP (" + level + ") - " + message, Color.BLACK);
						break;
				}		
			}
			
			@Override
			public boolean isEnabled(int level) {
				return true;
			}
		});
		if(Logger.isLoggingEnabled())
		{
			if(getPreferences().getBoolean("global", "pref_stats", true))
			{
				if(!Logger.hasDb())
					Logger.setDb(new LoggerDbAdapter(getApplicationContext()));
			} else if(!IS_DEBUG_BUILD)
				Logger.setLoggingEnabled(false);
		}
		SmbFile.setSMBCommunicationListener(new OnSMBCommunicationListener() {

			@Override
			public void onBeforeConnect(SmbFile file) {
				sendToLogView("SMB Connecting: " + file.getPath(), Color.GREEN);
			}

			@Override
			public void onConnect(SmbFile file) {

				//sendToLogView("SMB Connected: " + file.getPath(), Color.GREEN);
			}

			@Override
			public void onConnectFailure(SmbFile file) {
				sendToLogView("SMB Connect Failure: " + file.getPath(), Color.RED);
			}

			@Override
			public void onDisconnect(SmbFile file) {
				sendToLogView("SMB Disconnect: " + file.getPath(), Color.DKGRAY);
			}

			@Override
			public void onSendCommand(SmbFile file, Object... commands) {
				URL url = file.getURL();
				String s = "Command: smb://" + url.getHost() + url.getPath(); 
				for(Object o : commands)
				{
					if(o instanceof ServerMessageBlock)
					{
						ServerMessageBlock blk = (ServerMessageBlock)o;
						String tmp = blk.toString();
						if(tmp.indexOf("[") > -1)
							s += " -> " + tmp.substring(0, tmp.indexOf("["));
						else
							s += " -> " + tmp; 
					} else s += " -> " + o.toString();
				}
				sendToLogView(s, Color.BLACK);
			}
			
		});
	}
	
	private void setupFilesDb()
	{
		OpenPath.setDb(new OpenPathDbAdapter(getApplicationContext()));
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		if(Logger.isLoggingEnabled() && Logger.hasDb())
			Logger.closeDb();
	}
	
	public boolean isNetworkConnected()
	{
		ConnectivityManager conman = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		if(!conman.getBackgroundDataSetting()) return false;
		NetworkInfo ni = conman.getActiveNetworkInfo();
		if(ni == null) return false;
		if(!ni.isAvailable() || !ni.isConnected()) return false;
		if(ni.getState() == State.CONNECTING) return false;
		return true;
	}
	
	@SuppressWarnings("deprecation")
	private void submitStats()
	{
		if(!Logger.isLoggingEnabled()) return;
		setupLoggingDb();
		if(IS_DEBUG_BUILD) return;
		long lastSubmit = getPreferences().getSetting("flags", "last_stat_submit", 0l);
		if(new Date().getTime() - lastSubmit < 6000)
			return;
		lastSubmit = new Date().getTime();
		if(!isNetworkConnected()) return;
		
		String logs = Logger.getDbLogs(false);
		if(logs == null || logs == "") logs = "[]";
		//if(logs != null && logs != "") {
			Logger.LogDebug("Found " + logs.length() + " bytes of logs.");
			new SubmitStatsTask(this).execute(logs);
		//} else Logger.LogWarning("Logs not found.");
	}
	
	
	public void handleRefreshMedia(final String path, boolean keepChecking, final int retries)
	{
		if(!keepChecking || retries <= 0)
		{
			refreshBookmarks();
			return;
		}
		final Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			public void run() {
				Logger.LogDebug("Check " + retries + " for " + path);
				try {
					StatFs sf = new StatFs(path);
					if(sf.getBlockCount() == 0)
						throw new Exception("No blocks");
					showToast(getResources().getString(R.string.s_alert_new_media) + " " + getVolumeName(path) + " @ " + DialogHandler.formatSize((long)sf.getBlockSize() * (long)sf.getAvailableBlocks()));
					refreshBookmarks();
					if(mLastPath.getPath().equals(path))
						goHome();
				} catch(Throwable e)
				{
					Logger.LogWarning("Couldn't read " + path);
					handleRefreshMedia(path, true, retries - 1); // retry again in 1/2 second
				}
			}
		}, 1000);
	}
	
	public void handleMediaReceiver()
	{
		storageReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				String data = intent.getDataString();
				final String path = data.replace("file://", "");
				if(action.equals(Intent.ACTION_MEDIA_MOUNTED))
					handleRefreshMedia(path, true, 10);
				else
					refreshBookmarks();
			}
		};
		
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
		filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
		filter.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED);
		filter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
		filter.addAction(Intent.ACTION_MEDIA_EJECT);
		filter.addDataScheme("file");
		registerReceiver(storageReceiver, filter);
		
		ContentObserver mDbObserver = new ContentObserver(mHandler) {
			@Override
			public void onChange(boolean selfChange) {
				//rebake(false, ImageManager.isMediaScannerScanning(
				 //	   getContentResolver()));
			}
		};
		
		getContentResolver().registerContentObserver(
				MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
				true, mDbObserver);
	}
	
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		ensureCursorCache();
		/*
		if(mSettingsListener != null)
		{
			mSettingsListener.onHiddenFilesChanged(mPreferences.getBoolean(SettingsActivity.PREF_HIDDEN_KEY, false));
			mSettingsListener.onThumbnailChanged(mPreferences.getBoolean(SettingsActivity.PREF_THUMB_KEY, true));
			mSettingsListener.onViewChanged(mPreferences.getString(SettingsActivity.PREF_VIEW_KEY, "list"));
		} */
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		if(storageReceiver != null)
			unregisterReceiver(storageReceiver);
	}
	
	public static OpenClipboard getClipboard() {
		return mClipboard;
	}
	public void updateClipboard()
	{
		if(BEFORE_HONEYCOMB || !USE_ACTION_BAR)
		{
			View paste_view = findViewById(R.id.title_paste);
			if(paste_view != null)
			{
				TextView paste_text = (TextView)findViewById(R.id.title_paste_text);
				if(mClipboard.size() > 0)
				{
					paste_view.setVisibility(View.VISIBLE);
					paste_text.setText("("+mClipboard.size()+")");
				} else paste_view.setVisibility(View.GONE);
			}
		}
	}
	public void addHoldingFile(OpenPath path) { 
		mClipboard.add(path);
		if(!BEFORE_HONEYCOMB)
			invalidateOptionsMenu();
		updateClipboard();
	}
	public void clearHoldingFiles() {
		mClipboard.clear();
		updateClipboard();
		if(!BEFORE_HONEYCOMB)
			invalidateOptionsMenu();
	}
	
	public static final OpenCursor getPhotoParent() {
		//if(mPhotoParent == null) refreshCursors();
		return mPhotoParent;
	}
	public static final OpenCursor getVideoParent() {
		//if(mVideoParent == null) refreshCursors();
		return mVideoParent;
	}
	public static final OpenCursor getMusicParent() {
		//if(mMusicParent == null) refreshCursors();
		return mMusicParent;
	}

	public static final OpenSmartFolder getDownloadParent() {
		return mDownloadParent;
	}
	
	private boolean findCursors()
	{
		if(mVideoParent.isLoaded())
		{
			//Logger.LogDebug("Videos should be found");
		}else
		{
			if(bRetrieveExtraVideoDetails)
				bRetrieveExtraVideoDetails = !getSetting(null, "tag_novidinfo", false);
			mVideoParent.setName(getString(R.string.s_videos));
			Logger.LogVerbose("Finding videos");
			//if(!IS_DEBUG_BUILD)
			try {
				getSupportLoaderManager().initLoader(0, null, this);
			} catch(Exception e) { Logger.LogError("Couldn't query videos.", e); }
			Logger.LogDebug("Done looking for videos");
		}
		if(!mPhotoParent.isLoaded())
		{
			if(bRetrieveDimensionsForPhotos)
				bRetrieveDimensionsForPhotos = !getSetting(null, "tag_nodims", false);
			mPhotoParent.setName(getString(R.string.s_photos));
			Logger.LogVerbose("Finding Photos");
			try {
				getSupportLoaderManager().initLoader(1, null, this);
				Logger.LogDebug("Done looking for photos");
				
			} catch(IllegalStateException e) { Logger.LogError("Couldn't query photos.", e); }
		}
		if(!mMusicParent.isLoaded())
		{
			mMusicParent.setName(getString(R.string.s_music));
			Logger.LogVerbose("Finding Music");
			try {
				getSupportLoaderManager().initLoader(2, null, this);
				Logger.LogDebug("Done looking for music");
			} catch(IllegalStateException e) { Logger.LogError("Couldn't query music.", e); }
		}
		if(!mApkParent.isLoaded())
		{
			Logger.LogVerbose("Finding APKs");
			try {
				getSupportLoaderManager().initLoader(3, null, this);
			} catch(IllegalStateException e) { Logger.LogError("Couldn't get Apks.", e); }
		}
		if(!mDownloadParent.isLoaded())
		{
			mDownloadParent.setName(getString(R.string.s_downloads));
			new Thread(new Runnable(){public void run(){
				OpenFile extDrive = OpenFile.getExternalMemoryDrive(false);
				OpenFile intDrive = OpenFile.getInternalMemoryDrive();
				boolean mHasExternal = false;
				boolean mHasInternal = false;
				if(extDrive != null && extDrive.exists())
					mHasExternal = true;
				if(intDrive != null && intDrive.exists())
					mHasInternal = true;
					//OpenSmartFolder dlSmart = new OpenSmartFolder("Downloads");
				
				for(String dl : new String[]{"Download","Downloads"})
				{
					if(mHasExternal)
					{
						if(extDrive.getChild(dl).exists())
							mDownloadParent.addSearch(new SmartSearch(extDrive.getChild(dl)));
						else if(extDrive.getChild(dl.toLowerCase()).exists())
							mDownloadParent.addSearch(new SmartSearch(extDrive.getChild(dl.toLowerCase())));
					}
					if(mHasInternal)
					{
						if(intDrive.getChild(dl).exists())
							mDownloadParent.addSearch(new SmartSearch(intDrive.getChild(dl)));
						else if(intDrive.getChild(dl.toLowerCase()).exists())
							mDownloadParent.addSearch(new SmartSearch(intDrive.getChild(dl.toLowerCase())));
					}
				}
			}}).start();
		}
		Logger.LogVerbose("Done finding cursors");
		return true;
	}
	private void refreshCursors()
	{
		if(findCursors())
			return;
		//new Thread(new Runnable(){public void run() {
			//ensureCursorCache();
		//}}).start();
	}
	public void ensureCursorCache()
	{
		//findCursors();
		if(mRunningCursorEnsure
				//|| mLastCursorEnsure == 0
				//|| new Date().getTime() - mLastCursorEnsure < 10000 // at least 10 seconds
				)
		{
			Logger.LogVerbose("Skipping ensureCursorCache");
			return;
		} else Logger.LogVerbose("Running ensureCursorCache");
		mRunningCursorEnsure = true;
		
		// group into blocks
		int iTotalSize = 0;
		for(OpenCursor cur : new OpenCursor[]{mVideoParent, mPhotoParent, mApkParent})
			if(cur != null)
				iTotalSize += cur.length();
		int enSize = Math.max(20, iTotalSize / 10);
		Logger.LogDebug("EnsureCursorCache size: " + enSize + " / " + iTotalSize);
		ArrayList<OpenPath> buffer = new ArrayList<OpenPath>(enSize);
		for(OpenCursor curs : new OpenCursor[]{mVideoParent, mPhotoParent, mApkParent})
		{
			if(curs == null) continue;
			for(OpenMediaStore ms : curs.list())
			{
				buffer.add(ms);
				if(buffer.size() == enSize)
				{
					OpenMediaStore[] buff = new OpenMediaStore[buffer.size()];
					buffer.toArray(buff);
					buffer.clear();
					try {
						//Logger.LogDebug("Executing Task of " + buff.length + " items");
						/*if(!BEFORE_HONEYCOMB)
							new EnsureCursorCacheTask().executeOnExecutor(new Executor() {
								public void execute(Runnable command) {
									command.run();
								}
							}, buff);
						else*/
							new EnsureCursorCacheTask().execute(buff);
					} catch(RejectedExecutionException e) {
						Logger.LogWarning("Couldn't ensure cache.", e);
						return;
					}
				}
			}
		}
		if(buffer.size() > 0)
		{
			OpenMediaStore[] buff = new OpenMediaStore[buffer.size()];
			buffer.toArray(buff);
			buffer.clear();
			try {
				Logger.LogDebug("Executing Task of " + buff.length + " items");
				/*if(!BEFORE_HONEYCOMB)
					new EnsureCursorCacheTask().executeOnExecutor(new Executor() {
						public void execute(Runnable command) {
							command.run();
						}
					}, buff);
				else*/
					new EnsureCursorCacheTask().execute(buff);
			} catch(RejectedExecutionException e) {
				Logger.LogWarning("Couldn't ensure cache.", e);
				return;
			}
		}

		Logger.LogVerbose("Done with ensureCursorCache");
		
		//mLastCursorEnsure = new Date().getTime();
		mRunningCursorEnsure = false;
	}
	

	public void toggleBookmarks(Boolean visible)
	{
		if(!mSinglePane) return;
		if(mBookmarksPopup != null)
		{
			if(visible)
				mBookmarksPopup.showLikePopDownMenu();
			else
				mBookmarksPopup.dismiss();
		}
	}
	
	public void toggleBookmarks()
	{
		View mBookmarks = findViewById(R.id.list_frag);
		if(isSinglePane())
			toggleBookmarks(mBookmarks == null || mBookmarks.getVisibility() == View.GONE);
		else
			mBookmarks.setVisibility(mBookmarks.getVisibility() == View.GONE ? View.VISIBLE : View.GONE);
	}

	public void refreshOperations() {
		Fragment f = fragmentManager.findFragmentByTag("ops");
		if(f == null)
			f = new OperationsFragment();
		OperationsFragment ops = (OperationsFragment)f;
		if(findViewById(R.id.frag_log) != null)
		{
			fragmentManager
				.beginTransaction()
				.replace(R.id.frag_log, ops)
				.disallowAddToBackStack()
				.commitAllowingStateLoss();
			findViewById(R.id.frag_log).setVisibility(View.VISIBLE);
		} else {
			int pos = mViewPagerAdapter.getItemPosition(ops);
			if(pos < 0)
			{
				mViewPagerAdapter.add(ops);
				mViewPagerAdapter.notifyDataSetChanged();
			}
		}
	}
	
	public void refreshBookmarks()
	{
		Logger.LogVerbose("refreshBookmarks()");
		refreshCursors();
		if(mBookmarks != null)
		{
			mBookmarks.scanBookmarks();
			mBookmarks.refresh();
		}
		if(mBookmarksList != null)
			mBookmarksList.invalidate();
	}
	public ContentFragment getDirContentFragment(Boolean activate) { return getDirContentFragment(activate, mLastPath); }
	public ContentFragment getDirContentFragment(Boolean activate, OpenPath path)
	{
		//Logger.LogDebug("getDirContentFragment");
		OpenFragment ret = null;
		//if(mViewPager != null && mViewPagerAdapter != null && mViewPagerAdapter instanceof OpenPathPagerAdapter && ((OpenPathPagerAdapter)mViewPagerAdapter).getLastItem() instanceof ContentFragment)
		//	ret = ((ContentFragment)((OpenPathPagerAdapter)mViewPagerAdapter).getLastItem());
		if(mViewPagerAdapter != null && mViewPager != null)
		{
			if(mViewPager.getCurrentItem() > -1)
			{
				ret = mViewPagerAdapter.getItem(mViewPager.getCurrentItem());
				//Logger.LogVerbose("Current Page: " + (mViewPager.getCurrentItem() + 1) + " of " + mViewPagerAdapter.getCount() + (ret instanceof ContentFragment ? " : " + ((ContentFragment)ret).getPath().getPath() : ""));
				if(!(ret instanceof ContentFragment))
					ret = mViewPagerAdapter.getItem(mViewPagerAdapter.getLastPositionOfType(ContentFragment.class));
			} else {
				Logger.LogWarning("Couldn't find current Page. Using last.");
				ret = mViewPagerAdapter.getItem(mViewPagerAdapter.getLastPositionOfType(ContentFragment.class));
			}
		}
		if(ret == null)
			ret = (OpenFragment)fragmentManager.findFragmentById(R.id.content_frag);
		if(ret == null || !ret.getClass().equals(ContentFragment.class))
		{
			Logger.LogWarning("Do I need to create a new ContentFragment?");
   			//ret = ContentFragment.newInstance(mLastPath, getSetting(mLastPath, "view", mViewMode));
   			//mViewPagerAdapter.add(ret);
			/*if(mViewPagerAdapter instanceof OpenPathPagerAdapter)
			{
				((OpenPathPagerAdapter)mViewPagerAdapter).setPath(mLastPath);
				ret = ((OpenPathPagerAdapter)mViewPagerAdapter).getLastItem();
			} else if(mViewPagerAdapter instanceof ArrayPagerAdapter)*/
			if(mViewPagerAdapter != null && mViewPager != null)
				ret = mViewPagerAdapter.getItem(mViewPager.getCurrentItem());
		}
		if(ret == null && path != null)
		{
			ret = ContentFragment.getInstance(path, getSetting(path, "view", 0), getSupportFragmentManager());
			if(mViewPager != null && ret != null)
			{
				//if(mViewPagerAdapter instanceof ArrayPagerAdapter)
					mViewPagerAdapter.set(mViewPager.getCurrentItem(), ret);
			}
		}
		if(activate && !ret.isVisible())
		{
			setCurrentItem(mViewPagerAdapter.getItemPosition(ret), false);
		}
		
		if(ret instanceof ContentFragment)
			return (ContentFragment)ret;
		else return null;
	}
	public OpenFragment getSelectedFragment()
	{
		OpenFragment ret = null;
		//if(mViewPager != null && mViewPagerAdapter != null && mViewPagerAdapter instanceof OpenPathPagerAdapter && ((OpenPathPagerAdapter)mViewPagerAdapter).getLastItem() instanceof ContentFragment)
		//	ret = ((ContentFragment)((OpenPathPagerAdapter)mViewPagerAdapter).getLastItem());
		if(mViewPagerAdapter != null && mViewPager != null)
		{
			if(mViewPager.getCurrentItem() > -1)
			{
				//Logger.LogVerbose("Current Page: " + (mViewPager.getCurrentItem() + 1) + " of " + mViewPagerAdapter.getCount());
				ret = mViewPagerAdapter.getItem(mViewPager.getCurrentItem());
			} else {
				Logger.LogWarning("Couldn't find current Page. Using last.");
				ret = mViewPagerAdapter.getItem(mViewPagerAdapter.getLastPositionOfType(ContentFragment.class));
			}
		}
		if(ret == null && fragmentManager != null)
			ret = (OpenFragment)fragmentManager.findFragmentById(R.id.content_frag);
		
   		return ret;
	}
	
	public void updateTitle(CharSequence cs)
	{
		TextView title = (TextView)findViewById(R.id.title_path);
		if((title == null || !title.isShown()) && !BEFORE_HONEYCOMB && getActionBar() != null && getActionBar().getCustomView() != null)
			title = (TextView)getActionBar().getCustomView().findViewById(R.id.title_path);
		//if(BEFORE_HONEYCOMB || !USE_ACTION_BAR || getActionBar() == null)
		if(title != null && title.getVisibility() != View.GONE)
			title.setText(cs, BufferType.SPANNABLE);
		if(!BEFORE_HONEYCOMB && USE_ACTION_BAR && getActionBar() != null && (title == null || !title.isShown()))
			getActionBar().setSubtitle(cs);
		//else
		{
			SpannableStringBuilder sb = new SpannableStringBuilder(getResources().getString(R.string.app_title));
			sb.append(cs.equals("") ? "" : " - ");
			sb.append(cs);
			setTitle(cs);
		}
	}
	
	private void saveOpenedEditors()
	{
		StringBuilder editing = new StringBuilder(",");
		for(int i = 0; i < mViewPagerAdapter.getCount(); i++)
		{
			OpenFragment f = mViewPagerAdapter.getItem(i);
			if(f instanceof TextEditorFragment)
			{
				TextEditorFragment tf = (TextEditorFragment)f;
				OpenPath path = tf.getPath();
				if(editing.indexOf(","+path.getPath()+",") == -1)
					editing.append(path + ",");
			}
		}
		Logger.LogDebug("Saving [" + editing.toString() + "] as TextEditorFragments");
		setSetting(null, "editing", editing.toString());
	}
	private void restoreOpenedEditors()
	{
		String editing = getSetting(null, "editing", (String)null);
		Logger.LogDebug("Restoring [" + editing + "] to TextEditorFragments");
		if(editing == null) return;
		for(String s : editing.split(","))
		{
			if(s == null || s == "") continue;
			OpenPath path = null;
			if(s.startsWith("content://"))
				path = new OpenContent(Uri.parse(s), this);
			else
				path = FileManager.getOpenCache(s);
			if(path == null) continue;
			editFile(path, true);
		}
		setViewPageAdapter(mViewPagerAdapter, true);
	}
	
	public void editFile(OpenPath path) { editFile(path, false); }
	public void editFile(OpenPath path, boolean batch)
	{
		if(path == null) return;
		if(!path.exists()) return;
		if(path.length() > getResources().getInteger(R.integer.max_text_editor_size)) return;
		TextEditorFragment editor = new TextEditorFragment(path);
		if(mViewPagerAdapter != null)
		{
			int pos = mViewPagerAdapter.getItemPosition(editor);
			if(pos == -1)
			{
				mViewPagerAdapter.add(editor);
				setViewPageAdapter(mViewPagerAdapter, !batch);
				if(!batch)
				{
					saveOpenedEditors();
					pos = mViewPagerAdapter.getItemPosition(editor);
					if(pos > -1)
						setCurrentItem(pos, true);
				}
			} else if(!batch) setCurrentItem(pos, true);
		} else
			fragmentManager.beginTransaction()
				.replace(R.id.content_frag, editor)
				//.addToBackStack(null)
				.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
				.commit();
		//addTab(editor, path.getName(), true);
	}
	
	@Override
	public void startActivity(Intent intent) {
		//if(handleIntent(intent)) return;
		super.startActivity(intent);
	}
	
	public void goHome()
	{
		Bundle b = new Bundle();
		b.putString("last", mLastPath.getPath());
		onSaveInstanceState(b);
		Intent intent = new Intent(this, OpenExplorer.class);
		//intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		if(mLastPath != null)
			intent.putExtra("last", mLastPath.getPath());
		else if(getDirContentFragment(false) != null)
			intent.putExtra("last", getDirContentFragment(false).getPath().getPath());
		intent.putExtra("state", b);
		finish();
		startActivity(intent);
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		Logger.LogDebug("<-- OpenExplorer.onCreateContextMenu");
		super.onCreateContextMenu(menu, v, menuInfo);
		
		switch(v.getId())
		{
		case R.menu.menu_view_flat:
		case R.menu.menu_view:
		case R.id.menu_view:
			getMenuInflater().inflate(R.menu.menu_view, menu);
			if(!CAN_DO_CAROUSEL)
				MenuUtils.setMenuVisible(menu, false, R.id.menu_view_carousel);
			break;
		case R.menu.menu_sort_flat:
		case R.menu.menu_sort:
		case R.id.menu_sort:
			getMenuInflater().inflate(R.menu.menu_sort, menu);
			break;
		case R.menu.main_menu:
		case R.id.title_menu:
			//getMenuInflater().inflate(R.menu.main_menu, menu);
			onCreateOptionsMenu(menu);
			break;
		default:
			Logger.LogWarning("Submenu not found for " + v.getId());
			break;
		}
		Logger.LogDebug("--> OpenExplorer.onCreateContextMenu");
		
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		return onClick(item.getItemId(), item, null);
	}
	
	private void setupBaseBarButtons() {
		if(mMainMenu == null)
			mMainMenu = new MenuBuilder(this);
		else
			mMainMenu.clearAll();
		//getMenuInflater().inflate(R.menu.main_menu, mMainMenu);
		try {
			getSelectedFragment().onCreateOptionsMenu(mMainMenu, getMenuInflater());
			Logger.LogVerbose("Setting up base bar (" + mMainMenu.size() + ")...");
			onPrepareOptionsMenu(mMainMenu);
		} catch(Exception e) {
			Logger.LogError("Couldn't set up base bar.", e);
		}
		//handleBaseBarButtons(mMainMenu);
	}
	
	private static int[] mMenuOptionsToHide = new int[]{R.id.menu_favorites};
	public static int getVisibleChildCount(ViewGroup parent)
	{
		int ret = 0;
		for(int i=0; i<parent.getChildCount(); i++)
			if(parent.getChildAt(i).getVisibility() != View.GONE)
				ret++;
		return ret;
	}
	public void setupBaseBarButtons(Menu menu, boolean flush)
	{
		TableLayout tbl = (TableLayout)findViewById(R.id.base_bar);
		mToolbarButtons = (ViewGroup)findViewById(R.id.base_row);
		OpenFragment f = getSelectedFragment();
		boolean topButtons = false;
		if(!(getSetting(null, "pref_basebar", true) || tbl == null || mToolbarButtons == null) && findViewById(R.id.title_buttons) != null)
		{
			mToolbarButtons = (ViewGroup)findViewById(R.id.title_buttons);
			if(tbl != null)
				tbl.setVisibility(View.GONE);
			topButtons = true;
		}
		USE_SPLIT_ACTION_BAR = !topButtons;
		if(mToolbarButtons != null)
		{
			mToolbarButtons.setVisibility(View.VISIBLE);
			if(tbl != null)
				tbl.setStretchAllColumns(false);
			if(flush)
				mToolbarButtons.setTag(null);
			if(mToolbarButtons.getTag() != null && f.getClass().equals(mToolbarButtons.getTag())) return;
			mToolbarButtons.removeAllViews();
			if(!topButtons)
				mToolbarButtons.measure(LayoutParams.MATCH_PARENT, getResources().getDimensionPixelSize(R.dimen.actionbar_compat_height));
			int i = -1;
			int btnWidth = getResources().getDimensionPixelSize(R.dimen.actionbar_compat_button_width) + (int)(16 * getResources().getDimension(R.dimen.one_dp));
			int tblWidth = mToolbarButtons.getWidth();
			if(tblWidth <= 0 && !topButtons)
				tblWidth = getWindowWidth();
			if(topButtons || tblWidth <= 0 || tblWidth > getWindowWidth() || !getResources().getBoolean(R.bool.ignore_max_base_buttons))
				tblWidth = btnWidth * getResources().getInteger(R.integer.max_base_buttons);
			if(mToolbarButtons.findViewById(R.id.title_paste) != null)
				tblWidth += btnWidth;
			while(++i < menu.size())
			{
				if(mToolbarButtons.getChildCount() * btnWidth >= tblWidth)
				{
					Logger.LogDebug("Base bar full after #" + i + " (" + (mToolbarButtons.getChildCount() * btnWidth) + ">" + tblWidth + ")!");
					break;
				} else if(!checkArray(menu.getItem(i).getItemId(), mMenuOptionsToHide) &&
						menu.getItem(i) instanceof MenuItemImpl)
				{
					final MenuItemImpl item = (MenuItemImpl) menu.getItem(i);
					if(item.getItemId() == R.id.title_menu) break;
					if(!item.isCheckable() && mToolbarButtons.findViewById(item.getItemId()) == null)
					{
						ImageButton btn = (ImageButton)getLayoutInflater().inflate(R.layout.toolbar_button, null);
						if(!item.isVisible())
							btn.setVisibility(View.GONE);
						Drawable d = item.getIcon();
						if(d instanceof BitmapDrawable)
							((BitmapDrawable)d).setGravity(Gravity.CENTER);
						btn.setImageDrawable(d);
						btn.setId(item.getItemId());
						btn.setOnClickListener(this);
						btn.setLongClickable(true);
						btn.setOnLongClickListener(new OnLongClickListener() {
							@Override
							public boolean onLongClick(View v) {
								showToast(item.getTitle());
								return true;
							}
						});
						if(!USE_PRETTY_MENUS || topButtons)
							btn.setOnCreateContextMenuListener(this);
						mToolbarButtons.addView(btn);
						menu.getItem(i--).setVisible(false);
						//menu.removeItem(item.getItemId());
						Logger.LogDebug("Added " + item.getTitle() + " to base bar.");
					} //else Logger.LogWarning(item.getTitle() + " should not show. " + item.getShowAsAction() + " :: " + item.getFlags());
				}
			}
			mToolbarButtons.setTag(f.getClass());
			if(Build.VERSION.SDK_INT > 10)
				MenuUtils.setMenuVisible(menu, false, R.id.title_menu);
			else if(menu.size() > 0 && menu.findItem(R.id.title_menu) instanceof MenuItemImpl)
			{
				final MenuItemImpl item = (MenuItemImpl)menu.findItem(R.id.title_menu);
				if(item != null)
				{
					ImageButton btn = (ImageButton)getLayoutInflater()
								.inflate(R.layout.toolbar_button, null);
					if(!item.isVisible())
						btn.setVisibility(View.GONE);
					Drawable d = item.getIcon();
					if(d instanceof BitmapDrawable)
						((BitmapDrawable)d).setGravity(Gravity.CENTER);
					btn.setImageDrawable(d);
					btn.setId(item.getItemId());
					btn.setTag(item);
					btn.setOnClickListener(this);
					btn.setLongClickable(true);
					btn.setOnLongClickListener(new OnLongClickListener() {
						@Override
						public boolean onLongClick(View v) {
							showToast(item.getTitle());
							return true;
						}
					});
					if(!USE_PRETTY_MENUS)
						btn.setOnCreateContextMenuListener(this);
					mToolbarButtons.addView(btn);
					menu.removeItem(item.getItemId());
				}
			}
			Logger.LogDebug("Added " + mToolbarButtons.getChildCount() + " children to Base Bar.");
			if(tbl != null)
			{
				if(mToolbarButtons.getChildCount() < 1)
					tbl.setVisibility(View.GONE);
				else tbl.setStretchAllColumns(true);
			}
		} else if(BEFORE_HONEYCOMB) Logger.LogWarning("No Base Row!?");
	}
	
	private boolean checkArray(int needle, int[] hayStack) {
		for(int id : hayStack)
			if(id == needle)
				return true;
		return false;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if(menu != null)
			menu.clear();
		else return false;
		if(BEFORE_HONEYCOMB)
		{
			getSelectedFragment().onCreateOptionsMenu(menu, getMenuInflater());
			
		}
		MenuUtils.setMenuVisible(menu, false, R.id.title_menu);
		//Logger.LogVerbose("OpenExplorer.onCreateOptionsMenu");
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		super.onPrepareOptionsMenu(menu);
		//Logger.LogVerbose("OpenExplorer.onPrepareOptionsMenu");
		
		if(getClipboard() != null)
		{
			MenuUtils.setMenuChecked(menu, getClipboard().isMultiselect(), R.id.menu_multi);
			MenuUtils.setMenuVisible(menu, getClipboard().size() > 0, R.id.menu_paste);
		} else
			MenuUtils.setMenuVisible(menu,  false, R.id.menu_paste);
		
		MenuUtils.setMenuChecked(menu, getSetting(null, "pref_basebar", true), R.id.menu_view_split);
		MenuUtils.setMenuChecked(menu, mLogFragment != null && mLogFragment.isVisible(), R.id.menu_view_logview);
		MenuUtils.setMenuChecked(menu, getPreferences().getBoolean("global", "pref_fullscreen", false), R.id.menu_view_fullscreen);
		if(Build.VERSION.SDK_INT < 14 && !BEFORE_HONEYCOMB) // pre-ics
			MenuUtils.setMenuVisible(menu, false, R.id.menu_view_fullscreen);
		if(getWindowWidth() < 500 && Build.VERSION.SDK_INT < 14) // ICS can split the actionbar
		{
			MenuUtils.setMenuShowAsAction(menu, MenuItem.SHOW_AS_ACTION_NEVER, R.id.menu_sort, R.id.menu_view, R.id.menu_new_folder);
			MenuUtils.setMenuVisible(menu, true, R.id.title_menu);
		}
		//if(BEFORE_HONEYCOMB)
		{
			OpenFragment f = getSelectedFragment();
			if(f != null && f.hasOptionsMenu() && !f.isDetached())
				f.onPrepareOptionsMenu(menu);
		}
		
		if(menu != null && menu.findItem(R.id.menu_paste) != null && getClipboard() != null && getClipboard().size() > 0)
		{
			SubMenu sub = menu.findItem(R.id.menu_paste).getSubMenu();
			if(sub != null)
			{
				int i = 0;
				for(final OpenPath item : getClipboard().getAll())
				{
					sub.add(Menu.CATEGORY_CONTAINER, i++, i, item.getName())
						.setCheckable(true)
						.setChecked(true)
						.setOnMenuItemClickListener(new OnMenuItemClickListener() {
							@Override
							public boolean onMenuItemClick(MenuItem menuitem) {
								getClipboard().remove(item);
								return true;
							}
						})
						.setIcon(ThumbnailCreator.getDefaultResourceId(item, 32, 32));
				}
			}
		}
		
		if(!CAN_DO_CAROUSEL)
			MenuUtils.setMenuVisible(menu, false, R.id.menu_view_carousel);
		if(BEFORE_HONEYCOMB)
			setupBaseBarButtons(menu, false);
		
		return true;
	}
	
	/*
	private void hideMenuIfVisible(Menu menu, int menuId, int viewId) {
		View v = findViewById(viewId);
		if(v != null && v.isShown())
			MenuUtils.setMenuVisible(menu, false, menuId);
		else
			MenuUtils.setMenuVisible(menu, true, menuId);
	}
	*/

	public boolean onOptionsItemSelected(MenuItem item)
	{
		if(item.isCheckable())
			item.setChecked(item.getGroupId() > 0 ? true : !item.isChecked());
		
		OpenFragment f = getSelectedFragment();
		if(f != null && f.onOptionsItemSelected(item))
			return true;
		
		return onClick(item.getItemId(), item, null);
	}
	
	@Override
	public void onClick(View v) {
		super.onClick(v);
		int id = v.getId();
		if(v.getTag() != null && v.getTag() instanceof MenuItem)
			id = ((MenuItem)v.getTag()).getItemId();
		else if(v instanceof ImageButton)
		{
			Drawable d = ((ImageButton)v).getDrawable();
			if(d.equals(getResources().getDrawable(R.drawable.ic_menu_view)))
				id = R.id.menu_view;
			else if(d.equals(getResources().getDrawable(R.drawable.ic_menu_sort_by_size)))
				id = R.id.menu_sort;
		}
		if(id == R.id.menu_view || id == R.id.menu_sort)
			if(v.showContextMenu()) return;
		if(USE_PRETTY_MENUS || !v.showContextMenu())
			onClick(id, null, v);
	}
	
	public boolean onClick(int id, MenuItem item, View from)
	{
		super.onClick(id);
		if(mOpenMenu != null)
			mOpenMenu.dismiss();
		if(id != R.id.title_icon && id != android.R.id.home);
			toggleBookmarks(false);
		if(item != null && getSelectedFragment().onOptionsItemSelected(item))
			return true;
		switch(id)
		{
			case R.id.menu_debug:
				debugTest();
				break;
			case R.id.title_icon:
			case android.R.id.home:
			
				if(mSinglePane)
					toggleBookmarks();
				else
					goHome();

				return true;
				
			case R.id.menu_new_folder:
				mEvHandler.createNewFolder(getDirContentFragment(false).getPath().getPath(), this);
				return true;
				
			case R.id.menu_multi:
				if(getClipboard().isMultiselect())
				{
					getClipboard().stopMultiselect();
					//getClipboard().clear();
					if(!BEFORE_HONEYCOMB && mActionMode != null)
						((ActionMode)mActionMode).finish();
					return true;
				}
				
				if(BEFORE_HONEYCOMB || !USE_ACTIONMODE)
				{
					getClipboard().startMultiselect();
				} else {
					mActionMode = startActionMode(new ActionMode.Callback() {
						
						public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
							return false;
						}
						public void onDestroyActionMode(ActionMode mode) {
							getClipboard().clear();
							mActionMode = null;
						}
						public boolean onCreateActionMode(ActionMode mode, Menu menu) {
							mode.setTitle(getString(R.string.s_menu_multi) + ": " + getClipboard().size() + " " + getString(R.string.s_files));
							mode.getMenuInflater().inflate(R.menu.multiselect, menu);
							//MenuUtils.setMenuVisible(menu, false, R.id.menu_context_paste, R.id.menu_context_unzip);
							getDirContentFragment(true).changeMultiSelectState(true);
							return true;
						}
						public boolean onActionItemClicked(ActionMode mode, MenuItem item)
						{
							if(getClipboard().size() < 1)
							{
								mode.finish();
								return true;
							}
							OpenPath file = getClipboard().get(0); //getMultiSelectHandler().getSelectedFiles();
							
							getClipboard().clear();
						
							return getDirContentFragment(false)
									.executeMenu(item.getItemId(), mode, file, mClipboard);
						}
					});
				}
				return true;
				
			case R.id.menu_sort:
				//if(!USE_ACTION_BAR)
				return showMenu(from instanceof MenuItem ? R.menu.menu_sort : R.menu.menu_sort_flat, from);
				
			case R.id.menu_view:
				//if(BEFORE_HONEYCOMB)
				//	showMenu(item.getSubMenu(), from);
				//if(!USE_ACTION_BAR)
				OpenFragment f = getSelectedFragment();
				if(f instanceof ContentFragment)
					return showMenu(from instanceof MenuItem ? R.menu.menu_view : R.menu.menu_view_flat, from);
				else if(f instanceof TextEditorFragment)
					return showMenu(from instanceof MenuItem ? R.menu.text_view : R.menu.text_view_flat, from);
				else return false;
				
			case R.id.menu_view_grid:
				changeViewMode(OpenExplorer.VIEW_GRID, true);
				return true;
				
			case R.id.menu_view_list:
				changeViewMode(OpenExplorer.VIEW_LIST, true);
				return true;
				
			case R.id.menu_view_carousel:
				changeViewMode(OpenExplorer.VIEW_CAROUSEL, true);
				return true;

			case R.id.menu_view_fullscreen:
				getPreferences().setSetting("global", "pref_fullscreen", 
						!getPreferences().getSetting("global", "pref_fullscreen", false));
				goHome();
				return true;

			case R.id.menu_view_split:
				setSetting(null, "pref_basebar", !getSetting(null, "pref_basebar", true));
				goHome();
				return true;
				
			case R.id.menu_view_logview:
				boolean lvenabled = setSetting(null, "pref_logview", !getSetting(null, "pref_logview", true));
				if(mLogFragment == null)
					mLogFragment = new LogViewerFragment();
				if (!lvenabled || !getSetting(null,  "pref_logview", true))
				{
					if(mLogFragment.isVisible())
					{
						if(findViewById(R.id.frag_log) != null)
							findViewById(R.id.frag_log).setVisibility(View.GONE);
						else
							mViewPagerAdapter.remove(mLogFragment);
					}
				} else {
					if(findViewById(R.id.frag_log) != null)
					{
						findViewById(R.id.frag_log).setVisibility(View.VISIBLE);
						OpenFragment lf = (OpenFragment)fragmentManager.findFragmentById(R.id.frag_log);
						if(lf == null || !(lf instanceof LogViewerFragment))
							fragmentManager.beginTransaction()
								.replace(R.id.frag_log, lf).commitAllowingStateLoss();
					} else {
						mViewPagerAdapter.add(mLogFragment);
						mViewPager.setAdapter(mViewPagerAdapter);
					}
				}
				return true;
					
			case R.id.menu_root:
				if(RootManager.Default.isRoot())
				{
					getPreferences().setSetting("global", "pref_root", false);
					showToast(getString(R.string.s_menu_root_disabled));
					RootManager.Default.exitRoot();
					item.setChecked(false);
				} else
				{
					if(RootManager.Default.isRoot() || RootManager.Default.requestRoot())
					{
						getPreferences().setSetting("global", "pref_root", true);
						showToast(getString(R.string.s_menu_root) + "!");
						item.setTitle(getString(R.string.s_menu_root) + "!");
					} else {
						item.setChecked(false);
						showToast("Unable to achieve root.");
					}
				}
				return true;
			case R.id.menu_flush:
				ThumbnailCreator.flushCache(getApplicationContext(), true);
				OpenPath.flushDbCache();
				goHome();
				return true;
				
			case R.id.menu_refresh:
				ContentFragment content = getDirContentFragment(true);
				if(content != null)
				{
					Logger.LogDebug("Refreshing " + content.getPath().getPath());
					FileManager.removeOpenCache(content.getPath().getPath());
					content.getPath().deleteFolderFromDb();
					content.runUpdateTask(true);
					changePath(content.getPath(), false, true);
				}
				mBookmarks.refresh();
				return true;
				
			case R.id.menu_settings:
				showPreferences(null);
				return true;
			
			case R.id.menu_search:
				onSearchRequested();
				return true;

			case R.id.menu_favorites:
				toggleBookmarks();
				return true;
				
			case R.id.title_menu:
				showMenu(mMainMenu, from);
				//Logger.LogInfo("Show menu!");
				//showMenu();
				return true;
			
			case R.id.menu_multi_all_delete:
				DialogHandler.showConfirmationDialog(this,
						getResources().getString(R.string.s_confirm_delete, getClipboard().getCount() + " " + getResources().getString(R.string.s_files)),
						getResources().getString(R.string.s_menu_delete_all),
						new DialogInterface.OnClickListener() { // yes
							public void onClick(DialogInterface dialog, int which) {
								getEventHandler().deleteFile(getClipboard(), OpenExplorer.this, false);
							}
						});
				break;
				
			case R.id.menu_multi_all_clear:
				getClipboard().clear();
				return true;

			case R.id.menu_multi_all_copy:
				mClipboard.DeleteSource = false;
				getDirContentFragment(false).executeMenu(R.id.menu_paste, null, getDirContentFragment(false).getPath(), mClipboard);
				break;
				
			case R.id.menu_multi_all_move:
				mClipboard.DeleteSource = true;
				getDirContentFragment(false).executeMenu(R.id.menu_paste, null, getDirContentFragment(false).getPath(), mClipboard);
				break;
				
			case R.id.title_paste:
			case R.id.title_paste_icon:
			case R.id.title_paste_text:
			case R.id.menu_paste:
				//if(BEFORE_HONEYCOMB)
					showClipboardDropdown(id);
				return true;
				
				//getDirContentFragment(false).executeMenu(R.id.menu_paste, null, mLastPath, mClipboard);
				//return true;
				
			case R.id.menu_about:
				DialogHandler.showAboutDialog(this);
				return true;
				
			case R.id.menu_exit:
				finish();
				return true;
				
			default:
				OpenFragment sf = getSelectedFragment();
				if(sf instanceof ContentFragment)
				{
					if(!((ContentFragment)sf).onContextItemSelected(item) &&
						!((ContentFragment)sf).onOptionsItemSelected(item))
					return ((ContentFragment)sf).executeMenu(id, getDirContentFragment(false).getPath());
				}
				else if(sf instanceof TextEditorFragment)
					((TextEditorFragment)sf).onClick(id);
				else if(sf.onOptionsItemSelected(item)) return true;
		}
		
		//showToast("oops");
		return false;
		//return super.onOptionsItemSelected(item);
	}
	
	private void debugTest() {
		/*
		int bad = 2 / 0;
		Logger.LogInfo("HEY! We know how to divide by 0! It is " + bad);
		*/
		//*
		mEvHandler.copyFile(new OpenFile("/mnt/sdcard/cm9-droid3-20120316-0330.zip"), new OpenFile("/mnt/sdcard/Download"), this);
		refreshOperations();
		//*/
	}
	
	public boolean isSinglePane() { return mSinglePane; }

	private void showClipboardDropdown(int menuId)
	{
		View anchor = findViewById(menuId);
		if(anchor == null)
			anchor = findViewById(R.id.title_icon);
		if(anchor == null)
			anchor = findViewById(R.id.frag_holder);
		
		final BetterPopupWindow clipdrop = new BetterPopupWindow(this, anchor);
		View root = ((LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE))
				.inflate(R.layout.multiselect, null);
		GridView cmds = (GridView)root.findViewById(R.id.multiselect_command_grid);
		final ListView items = (ListView)root.findViewById(R.id.multiselect_item_list);
		items.setAdapter(mClipboard);
		items.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> list, View view, final int pos, long id) {
				//OpenPath file = mClipboard.get(pos);
				//if(file.getParent().equals(mLastPath))
				Animation anim = AnimationUtils.loadAnimation(OpenExplorer.this,
						R.anim.slide_out_left);
				//anim.setDuration(getResources().getInteger(android.R.integer.config_mediumAnimTime));
				list.getChildAt(pos).startAnimation(anim);
				new Handler().postDelayed(new Runnable(){public void run() {
					mClipboard.remove(pos);
					items.invalidate();
					if(mClipboard.getCount() == 0)
						clipdrop.dismiss();
				}}, anim.getDuration());
				//else
					//getEventHandler().copyFile(file, mLastPath, OpenExplorer.this);
			}
		});
		final Menu menu = IconContextMenu.newMenu(this, R.menu.multiselect);
		MenuUtils.setMenuChecked(menu, getClipboard().isMultiselect(), R.id.menu_multi);
		final IconContextMenuAdapter cmdAdapter = new IconContextMenuAdapter(this, menu);
		cmdAdapter.setTextLayout(R.layout.context_item);
		cmds.setAdapter(cmdAdapter);
		cmds.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> list, View view, int pos, long id) {
				MenuItem item = menu.getItem(pos);
				onClick(item.getItemId(), item, view);
				clipdrop.dismiss();
			}
		});
		
		float w = getResources().getDimension(R.dimen.popup_width) * (3 / 2);
		if(w > getWindowWidth())
			w = getWindowWidth() - 20;
		clipdrop.setPopupWidth((int)w);
		clipdrop.setContentView(root);
		
		clipdrop.showLikePopDownMenu();
		//dropdown.setAdapter(this, new IconContextMenuAdapter(context, menu))
		//BetterPopupWindow win = new BetterPopupWindow(this, anchor);
		//ListView list = ((LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(R.layout.multiselect, null);
		//win.setContentView(root)
	}
	
	public OpenPath getCurrentPath() {
		OpenFragment f = getSelectedFragment();
		if(f instanceof OpenPathFragmentInterface)
			return ((OpenPathFragmentInterface)f).getPath();
		return null;
	}
	
	public void changeViewMode(int newView, boolean doSet) {
		int mViewMode = getSetting(getCurrentPath(), "view", 0);
		if(mViewMode == newView) {
			Logger.LogWarning("changeViewMode called unnecessarily! " + newView + " = " + mViewMode);
			//return;
		}
		//Logger.LogVerbose("Changing view mode to " + newView);
		int oldView = mViewMode;
		if(newView == VIEW_CAROUSEL && !CAN_DO_CAROUSEL)
			newView = oldView;
		//setViewMode(newView);
		if(doSet)
			setSetting(getCurrentPath(), "view", newView);
		if(!mSinglePane)
		{
			if(oldView == VIEW_CAROUSEL && mViewPagerEnabled)
			{
				setViewVisibility(false, false, R.id.content_frag);
				setViewVisibility(true, false, R.id.content_pager_frame);
				changePath(getCurrentPath(), false);
			} else if(newView == VIEW_CAROUSEL && mViewPagerEnabled)
			{
				setViewVisibility(false, false, R.id.content_pager_frame);
				setViewVisibility(true, false, R.id.content_frag);
				changePath(getCurrentPath(), false);
			}
			ContentFragment cf = getDirContentFragment(true);
			if(cf != null)
				cf.onViewChanged(newView);
			if(!BEFORE_HONEYCOMB)
				invalidateOptionsMenu();
		} else if(newView == VIEW_CAROUSEL && oldView != VIEW_CAROUSEL && CAN_DO_CAROUSEL)
		{
 			Logger.LogDebug("Switching to carousel!");
			if(mViewPagerEnabled)
			{
				setViewVisibility(false, false, R.id.content_pager_indicator,
						R.id.content_pager_frame_stub, R.id.content_pager);
				setViewVisibility(true, false, R.id.content_frag);
			}
			OpenPath path = getDirContentFragment(false).getPath();
			fragmentManager.beginTransaction()
				.replace(R.id.content_frag, new CarouselFragment(path))
				.setBreadCrumbTitle(path.getPath())
				.commit();
			updateTitle(path.getPath());
		} else if (oldView == VIEW_CAROUSEL && newView != VIEW_CAROUSEL && CAN_DO_CAROUSEL) { // if we need to transition from carousel
			Logger.LogDebug("Switching from carousel!");
			if(mViewPagerEnabled)
			{
				setViewVisibility(true, false, R.id.content_frag);
				setViewVisibility(false, false, R.id.content_pager_frame_stub,
						R.id.content_pager, R.id.content_pager_indicator);
				changePath(getCurrentPath(), false, true);
			} else {
				fragmentManager.beginTransaction()
					.replace(R.id.content_frag, ContentFragment.getInstance(getCurrentPath(), mViewMode, getSupportFragmentManager()))
					.setBreadCrumbTitle(getCurrentPath().getAbsolutePath())
					//.addToBackStack(null)
					.commit();
				updateTitle(getCurrentPath().getPath());
			}
			
			invalidateOptionsMenu();
		} else {
			getDirContentFragment(true).onViewChanged(newView);
			if(!BEFORE_HONEYCOMB)
				invalidateOptionsMenu();
		}
	}
	
	public void showPreferences(OpenPath path)
	{
		if(Build.VERSION.SDK_INT > 100)
		{
			FragmentTransaction ft = fragmentManager.beginTransaction();
			ft.hide(fragmentManager.findFragmentById(R.id.content_frag));
			//ft.replace(R.id.content_frag, new PreferenceFragment(this, path));
			ft.setBreadCrumbTitle("prefs://" + (path != null ? path.getPath() : ""));
			ft.addToBackStack("prefs");
			ft.commit();
			final PreferenceFragmentV11 pf2 = new PreferenceFragmentV11(path);
			getFragmentManager().addOnBackStackChangedListener(new android.app.FragmentManager.OnBackStackChangedListener() {
				
				public void onBackStackChanged() {
					//android.app.FragmentTransaction ft3 = getFragmentManager().beginTransaction();
					Logger.LogDebug("hide me!");
					//getFragmentManager().removeOnBackStackChangedListener(this);
					if(pf2 != null && pf2.getView() != null && getFragmentManager().getBackStackEntryCount() == 0)
						pf2.getView().setVisibility(View.GONE);
				}
			});
			android.app.FragmentTransaction ft2 = getFragmentManager().beginTransaction();
			ft2.replace(R.id.content_frag, pf2);
			ft2.addToBackStack("prefs");
			ft2.commit();
		} else {
			Intent intent = new Intent(this, SettingsActivity.class);
			if(path != null)
				intent.putExtra("path", path.getPath());
			startActivityForResult(intent, PREF_CODE);
		}
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
		
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == PREF_CODE) {
			goHome(); // just restart
		} else if (requestCode == REQ_SPLASH) {
			if(resultCode == RESULT_OK && data != null && data.hasExtra("start"))
			{
				String start = data.getStringExtra("start");
				getPreferences().setSetting("global", "pref_splash", true);
				getPreferences().setSetting("global", "pref_start", start);
				if(!start.equals(getCurrentPath().getPath()))
				{
					if("Videos".equals(start))
						changePath(mVideoParent, true);
					else if("Photos".equals(start))
						changePath(mPhotoParent, true);
					else if("External".equals(start))
						changePath(OpenFile.getExternalMemoryDrive(true).setRoot(), true);
					else if("Internal".equals(start))
						changePath(OpenFile.getInternalMemoryDrive().setRoot(), true);
					else
						changePath(new OpenFile("/").setRoot(), true);
				}
			}
		} else if (requestCode == REQ_INTENT) {
			
		}
	}

	public void goBack()
	{
		//new FileIOTask().execute(new FileIOCommand(FileIOCommandType.PREV, mFileManager.getLastPath()));
		if(fragmentManager.getBackStackEntryCount() == 0) return;
		BackStackEntry entry = fragmentManager.getBackStackEntryAt(fragmentManager.getBackStackEntryCount() - 1);
		OpenPath last = null;
		if(entry != null && entry.getBreadCrumbTitle() != null)
			try {
				last = FileManager.getOpenCache(entry.getBreadCrumbTitle().toString(), false, (SortType)null);
			} catch (IOException e) { }
		if(last == null) return;
		Logger.LogDebug("Going back to " + last.getPath());
		changePath(last, false, true);
		//updateData(last.list());
	}
	
	@Override
	public void onBackPressed() {
		if(mViewPagerAdapter.getCount() > 0 && getSelectedFragment().onBackPressed())
			return;
		super.onBackPressed();
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
				{
					try {
						mLastPath = FileManager.getOpenCache(entry.getBreadCrumbTitle().toString(),
								false, OpenPath.Sorting);
					} catch (Exception e) {
						Logger.LogError("Couldn't get back cache.", e);
					}
					if(mLastPath != null)
					{
						Logger.LogDebug("last path set to " + mLastPath.getPath());
						changePath(mLastPath, false);
						//updateTitle(mLastPath.getPath());
					} else finish();
					
				} 
				else {
					fragmentManager.popBackStack();
				}
			}
			else {
				//updateTitle("");
				showToast(R.string.s_alert_back_to_exit);
			}
		}
		mLastBackIndex = i;
	}

	private void changePath(OpenPath path, Boolean addToStack) { changePath(path, addToStack, false); }
	private void changePath(OpenPath path, Boolean addToStack, Boolean force)
	{
		try {
			//if(mLastPath != null && !mLastPath.equals(path) && mLastPath instanceof OpenNetworkPath)
			//	((OpenNetworkPath)mLastPath).disconnect();
		} catch(Exception e) {
			Logger.LogError("Couldn't disconnect while changing paths.", e);
		}
		toggleBookmarks(false);
		if(path == null) path = mLastPath;
		if(path == null) return;
		if(mLastPath == null && getDirContentFragment(false) != null)
			mLastPath = getDirContentFragment(false).getPath();
		if(!(mLastPath instanceof OpenFile) || !(path instanceof OpenFile))
			force = true;
		
		//if(!BEFORE_HONEYCOMB) force = true;
		//if(!force)
			//if(!addToStack && path.getPath().equals("/")) return;
			//if(mLastPath.getPath().equalsIgnoreCase(path.getPath())) return;
		int newView = getSetting(path, "view", 0);
		if(!CAN_DO_CAROUSEL && newView == VIEW_CAROUSEL)
		{
			setSetting(path, "view", VIEW_LIST);
			newView = VIEW_LIST;
		}
		//boolean isNew = !mLastPath.equals(path);
		int oldView = getSetting(mLastPath, "view", 0);
		
		if(path instanceof OpenNetworkPath)
		{
			if(mLogViewEnabled)
				setViewVisibility(true, false, R.id.frag_log);
		} else
			setViewVisibility(false, false, R.id.frag_log);
		
		final ImageView icon = (ImageView)findViewById(R.id.title_icon);
		if(icon != null)
			ThumbnailCreator.setThumbnail(icon, path, 32, 32,
				new OnUpdateImageListener() {
					public void updateImage(Bitmap b) {
						BitmapDrawable d = new BitmapDrawable(getResources(), b);
						d.setGravity(Gravity.CENTER);
						icon.setImageDrawable(d);
					}
				});
		
		//mFileManager.setShowHiddenFiles(getSetting(path, "hide", false));
		//setViewMode(newView);
		//if(!BEFORE_HONEYCOMB && Build.VERSION.SDK_INT < 14 && newView == VIEW_CAROUSEL) {
			
			//setViewVisibility(true, false, R.id.content_pager_frame);
			//setViewVisibility(false, false, R.id.content_frag);
		
		List<OpenPath> familyTree = path.getAncestors(false);
		
		if(addToStack)
		{
			int bsCount = fragmentManager.getBackStackEntryCount();
			String last = null;
			if(bsCount > 0)
			{
				BackStackEntry entry = fragmentManager.getBackStackEntryAt(bsCount - 1);
				last = entry.getBreadCrumbTitle() != null ? entry.getBreadCrumbTitle().toString() : "";
				Logger.LogVerbose("Changing " + last + " to " + path.getPath() + "? " + (last.equalsIgnoreCase(path.getPath()) ? "No" : "Yes"));
			} else
				Logger.LogVerbose("First changePath to " + path.getPath());
			if(mStateReady && (last == null || !last.equalsIgnoreCase(path.getPath())))
			{
				fragmentManager
					.beginTransaction()
					.setBreadCrumbTitle(path.getPath())
					.addToBackStack("path")
					.commit();
			}
		}
		OpenFragment cf = (CAN_DO_CAROUSEL && newView == VIEW_CAROUSEL) ?
			new CarouselFragment(path) :
			ContentFragment.getInstance(path, newView, getSupportFragmentManager());
				
			if(force || addToStack || path.requiresThread())
			{
				int common = 0;

				for(int i = mViewPagerAdapter.getCount() - 1; i >= 0; i--)
				{
					OpenFragment f = mViewPagerAdapter.getItem(i);
					if(f == null || !(f instanceof ContentFragment)) continue;
					if(!familyTree.contains(((ContentFragment)f).getPath()))
						mViewPagerAdapter.remove(i);
					else common++;
				}
				
				if(force)
				{
					mViewPagerAdapter.remove(cf);
					mViewPagerAdapter.add(cf);
					//notifyPager();
				}
				
				//mViewPagerAdapter.notifyDataSetChanged();
				//mViewPagerAdapter.removeOfType(ContentFragment.class);
				//mViewPagerAdapter = new ArrayPagerAdapter(fragmentManager);
				int iNonContentPages = mViewPagerAdapter.getCount() - common;
				if(common < 0)
					mViewPagerAdapter.add(cf);
				else
					mViewPagerAdapter.add(common, cf);
				OpenPath tmp = path.getParent();
				while(tmp != null) 
				{
					Logger.LogDebug("Adding Parent: " + tmp.getPath());
					try {
						if(common > 0)
							if(tmp.getPath().equals(((ContentFragment)mViewPagerAdapter.getItem(common - 1)).getPath()))
								break;
					} catch(Exception e) { Logger.LogError("I don't trust this!", e); }
					try {
						mViewPagerAdapter.add(common, ContentFragment.getInstance(tmp, getSetting(tmp, "view", newView), getSupportFragmentManager()));
					} catch(Exception e) { Logger.LogError("Downloads?", e); }
					tmp = tmp.getParent();
				}
				//Logger.LogVerbose("All Titles: [" + getPagerTitles() + "] Paths: [" + getFragmentPaths(mViewPagerAdapter.getFragments()) + "]");
				//mViewPagerAdapter = newAdapter;
				//mViewPagerAdapter.getCount() - iNonContentPages - 1;
				setViewPageAdapter(mViewPagerAdapter, true);
				//mViewPagerAdapter.notifyDataSetChanged();
				//index -= iNonContentPages;
				//int index = mViewPagerAdapter.getLastPositionOfType(ContentFragment.class);
				int index = mViewPagerAdapter.getItemPosition(cf);
				setCurrentItem(index, addToStack);
				if(cf instanceof ContentFragment)
					((ContentFragment)cf).refreshData(null, false);
				//updatePagerTitle(index);
			} else {
				OpenPath commonBase = null;
				for(int i = mViewPagerAdapter.getCount() - 1; i >= 0; i--)
				{
					if(!(mViewPagerAdapter.getItem(i) instanceof ContentFragment))
						continue;
					ContentFragment c = (ContentFragment)mViewPagerAdapter.getItem(i);
					if(path.getPath().startsWith(c.getPath().getPath()))
						continue;
					commonBase = ((ContentFragment)mViewPagerAdapter.remove(i)).getPath();
				}
				int depth = 0;
				if(commonBase != null)
					depth = commonBase.getDepth() - 1;
				OpenPath tmp = path;
				while(tmp != null && (commonBase == null || !tmp.equals(commonBase)))
				{
					mViewPagerAdapter.add(depth,
						ContentFragment.getInstance(path, getSetting(path, "view", newView), getSupportFragmentManager()));
					tmp = tmp.getParent();
					if(tmp == null) break;
				}
				setViewPageAdapter(mViewPagerAdapter, false);
				//mViewPager.setAdapter(mViewPagerAdapter);
				setCurrentItem(path.getDepth() - 1, false);
				getDirContentFragment(false).refreshData(null, false);
			}
		//}
		//refreshContent();
		if(!BEFORE_HONEYCOMB)
			invalidateOptionsMenu();
		/*if(content instanceof ContentFragment)
		((ContentFragment)content).setSettings(
			SortType.DATE_DESC,
			getSetting(path, "thumbs", true),
			getSetting(path, "hide", true)
			);*/
		if(path instanceof OpenFile && !path.requiresThread())
			new PeekAtGrandKidsTask().execute((OpenFile)path);
		//ft.replace(R.id.content_frag, content);
		//ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
		Logger.LogDebug("Setting path to " + path.getPath());
		mLastPath = path;
	}
	private void refreshContent()
	{
		ContentFragment frag = getDirContentFragment(false);
		if(frag != null && frag.getPath() instanceof OpenNetworkPath)
		{
			frag.refreshData(null, false);
			frag.runUpdateTask();
		}
	}
	private String getFragmentPaths(List<OpenFragment> frags)
	{
		String ret = "";
		for(int i = 0; i < frags.size(); i++)
		{
			OpenFragment f = frags.get(i);
			if(f instanceof OpenPathFragmentInterface)
				ret += ((OpenPathFragmentInterface)f).getPath().getPath();
			else
				ret += f.getClass().toString();
			if(i < frags.size() - 1)
				ret += ",";
		}
		return ret;
	}
	private String getPagerTitles()
	{
		String ret = "";
		for(int i = 0; i < mViewPagerAdapter.getCount(); i++)
		{
			ret += mViewPagerAdapter.getPageTitle(i);
			if(i < mViewPagerAdapter.getCount() - 1)
				ret += ",";
		}
		return ret;
	}
	@SuppressWarnings("deprecation")
	public void setLights(Boolean on)
	{
		try {
			View root = getCurrentFocus().getRootView();
			int vis = on ? View.STATUS_BAR_VISIBLE : View.STATUS_BAR_HIDDEN;
			if(root.getSystemUiVisibility() != vis)
				root.setSystemUiVisibility(vis);
		} catch(Exception e) { }
	}
	public void onChangeLocation(OpenPath path) {
		changePath(path, true, false);
	}

	public static final FileManager getFileManager() {
		return mFileManager;
	}

	public static final EventHandler getEventHandler() {
		return mEvHandler;
	}
	
	
	

	public class EnsureCursorCacheTask extends AsyncTask<OpenPath, Void, Void>
	{
		@Override
		protected Void doInBackground(OpenPath... params) {
			//int done = 0;
			final Context c = getApplicationContext();
			for(OpenPath path : params)
			{
				if(path.isDirectory())
				{
					try {
						for(OpenPath kid : path.list())
						{
							ThumbnailCreator.generateThumb(kid, 36, 36, c);
							ThumbnailCreator.generateThumb(kid, 128, 128, c);
							//done++;
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				} else {
					ThumbnailCreator.generateThumb(path, 36, 36, c);
					ThumbnailCreator.generateThumb(path, 128, 128, c);
					//done++;
				}
			}
			//Logger.LogDebug("cursor cache of " + done + " generated.");
			return null;
		}
		
	}


	public void setOnClicks(int... ids)
	{
		for(int id : ids)
			if(findViewById(id) != null)
			{
				View v = findViewById(id);
				if(v.isLongClickable())
					v.setOnLongClickListener(this);
				v.setOnClickListener(this);
			}
	}
	
	public static String getVolumeName(String sPath2) {
		Process mLog = null;
		BufferedReader reader = null;
		try {
			mLog = Runtime.getRuntime().exec(new String[] {"logcat", "-d", "MediaVolume:D *:S"});
			reader = new BufferedReader(new InputStreamReader(mLog.getInputStream()));
			String check = sPath2.substring(sPath2.lastIndexOf("/") + 1);
			if(check.indexOf(".") > -1)
				check = check.substring(check.indexOf(".") + 1);
			String s = null;
			String last = null;
			do {
				s = reader.readLine();
				if(s == null) break;
				if(s.indexOf("New volume - Label:[") > -1 && s.indexOf(check) > -1)
				{
					last = s.substring(s.indexOf("[") + 1);
					if(last.indexOf("]") > -1)
						last = last.substring(0, last.indexOf("]"));
				}
			} while(s != null);
			if(last == null) throw new IOException("Couldn't find volume label.");
			sPath2 = last;
		} catch (IOException e) {
			Logger.LogError("Couldn't read LogCat :(", e);
			sPath2 = sPath2.substring(sPath2.lastIndexOf("/") + 1);
			if(sPath2.indexOf("_") > -1 && sPath2.indexOf("usb") < sPath2.indexOf("_"))
				sPath2 = sPath2.substring(0, sPath2.indexOf("_"));
			else if (sPath2.indexOf("_") > -1 && sPath2.indexOf("USB") > sPath2.indexOf("_"))
				sPath2 = sPath2.substring(sPath2.indexOf("_") + 1);
			sPath2 = sPath2.toUpperCase();
		} finally {
			if(reader != null)
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
		return sPath2;
	}
	public boolean onLongClick(View v) {
		switch(v.getId())
		{
			case R.id.title_icon:
				goHome();
				return true;
		}
		return false;
	}
	public void addBookmark(OpenPath file) {
		String sBookmarks = getPreferences().getSetting("bookmarks", "bookmarks", "");
		sBookmarks += sBookmarks != "" ? ";" : file.getPath();
		getPreferences().setSetting("bookmarks", "bookmarks", sBookmarks);
		if(mBookmarkListener != null)
			mBookmarkListener.onBookMarkAdd(file);
	}
	
	public static void setOnBookMarkAddListener(OpenInterfaces.OnBookMarkChangeListener bookmarkListener) {
		mBookmarkListener = bookmarkListener;
	}
	
	public class PeekAtGrandKidsTask extends AsyncTask<OpenFile, Void, Void>
	{
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}
		@Override
		protected Void doInBackground(OpenFile... params) {
			for(OpenFile file : params)
				file.listFiles(true);
			return null;
		}
		
	}

	public OpenPath getLastPath() {
		return mLastPath;
	}
	
	public void onClipboardUpdate() {
		View pb = findViewById(R.id.title_paste);
		if(pb == null && findViewById(R.id.menu_paste) != null)
			pb = findViewById(R.id.menu_paste);
		if(pb == null && BEFORE_HONEYCOMB && mToolbarButtons != null)
		{
			pb = getLayoutInflater().inflate(R.layout.toolbar_button, null);
			pb.setId(R.id.title_paste);
			((ImageButton)pb).setImageResource(R.drawable.ic_menu_paste);
			mToolbarButtons.addView(pb, 0);
			pb.setOnClickListener(this);
		}
		if(pb != null && BEFORE_HONEYCOMB)
		{
			pb.setVisibility(View.VISIBLE);
			if(pb.findViewById(R.id.title_paste_text) != null)
				((TextView)pb.findViewById(R.id.title_paste_text))
					.setText(""+getClipboard().size());
			if(pb.findViewById(R.id.title_paste_icon) != null)
			{
				((ImageView)pb.findViewById(R.id.title_paste_icon))
					.setImageResource(getClipboard().isMultiselect() ?
							R.drawable.ic_menu_paste_multi : R.drawable.ic_menu_paste
							);
				((LayerDrawable)((ImageView)pb.findViewById(R.id.title_paste_icon)).getDrawable())
					.getDrawable(1).setAlpha(getClipboard().isMultiselect()?127:0);
			}
		}
		if(!BEFORE_HONEYCOMB)
			invalidateOptionsMenu();
		if(!BEFORE_HONEYCOMB && USE_ACTIONMODE && mActionMode != null)
		{
			((ActionMode)mActionMode).setTitle(getString(R.string.s_menu_multi) + ": " + getClipboard().size() + " " + getString(R.string.s_files));
		}
		getDirContentFragment(false).notifyDataSetChanged();
	}
	
	public void onClipboardClear()
	{
		View pb = findViewById(R.id.title_paste);
		if(pb != null)
			pb.setVisibility(View.GONE);
		if(!BEFORE_HONEYCOMB)
			invalidateOptionsMenu();
		getDirContentFragment(false).notifyDataSetChanged();
	}

	@Override
	public boolean onPageTitleLongClick(int position, View titleView) {
		try {
			OpenFragment f = mViewPagerAdapter.getItem(position);
			if(f instanceof OnFragmentTitleLongClickListener)
				return ((OnFragmentTitleLongClickListener)f).onTitleLongClick(titleView);
			if(f instanceof TextEditorFragment) return false;
			if(!(f instanceof ContentFragment)) return false;
			OpenPath path = ((ContentFragment)mViewPagerAdapter.getItem(position)).getPath();
			if(path.requiresThread()) return false;
			OpenPath parent = path.getParent();
			if(path instanceof OpenCursor)
				parent = new OpenPathArray(new OpenPath[]{mVideoParent,mPhotoParent,mMusicParent,mDownloadParent});
			if(parent == null) parent = new OpenPathArray(new OpenPath[]{path});
			ArrayList<OpenPath> arr = new ArrayList<OpenPath>();
			for(OpenPath kid : parent.list())
				if((path.equals(kid) || kid.isDirectory()) && !kid.isHidden())
					arr.add(kid);
			Collections.sort(arr, new Comparator<OpenPath>() {
				public int compare(OpenPath a, OpenPath b) {
					return a.getName().compareTo(b.getName());
				}
			});
			OpenPath[] siblings = arr.toArray(new OpenPath[arr.size()]);
			ArrayList<OpenPath> siblingArray = new ArrayList<OpenPath>();
			siblingArray.addAll(arr);
			OpenPath foster = new OpenPathArray(siblings);
			//Logger.LogVerbose("Siblings of " + path.getPath() + ": " + siblings.length);
			
			Context mContext = this;
			View anchor = titleView; //findViewById(R.id.title_bar);
			int[] offset = new int[2];
			titleView.getLocationInWindow(offset);
			int offsetX = 0;
			int arrowLeft = 0;
			if(anchor == null && findViewById(R.id.content_pager_indicator) != null)
			{
				offsetX = titleView.getLeft();
				arrowLeft += titleView.getWidth() / 2;
				Logger.LogDebug("Using Pager Indicator as Sibling anchor (" + offsetX + ")");
				anchor = findViewById(R.id.content_pager_indicator);
				//if(anchor != null)
				//	offsetX -= anchor.getLeft();
			}
			final BetterPopupWindow mSiblingPopup = new BetterPopupWindow(mContext, anchor);
			//mSiblingPopup.USE_INDICATOR = false;
			OpenPathList mSiblingList = new OpenPathList(foster, mContext);
			mSiblingList.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> arg0, View view, int pos, long id) {
					final OpenPath path = (OpenPath)((BaseAdapter)arg0.getAdapter()).getItem(pos);
					mSiblingPopup.setOnDismissListener(new OnDismissListener() {
						@Override
						public void onDismiss() {
							changePath(path, true);
						}
					});
					mSiblingPopup.dismiss();
				}
			});
			mSiblingPopup.setContentView(mSiblingList);
			mSiblingPopup.setAnchorOffset(arrowLeft);
			mSiblingPopup.showLikePopDownMenu(offsetX,0);
			return true;
		} catch (Exception e) {
			Logger.LogError("Couldn't show sibling dropdown", e);
		}
		return false;
	}

	@Override
	public void onWorkerThreadComplete(int type, ArrayList<String> results) {
		try {
			Thread.sleep(50);
			Logger.LogVerbose("Time to wake up!");
		} catch (InterruptedException e) {
			Logger.LogWarning("Woken up too early!");
		}
		ContentFragment frag = getDirContentFragment(false);
		if(frag != null)
		{
			frag.onWorkerThreadComplete(type, results);
			changePath(frag.getPath(), false, true);
		}
		if(getClipboard().ClearAfter)
			getClipboard().clear();
	}
	
	@Override
	public void onWorkerProgressUpdate(int pos, int total)
	{
		ContentFragment frag = getDirContentFragment(false);
		if(frag != null)
			frag.onWorkerProgressUpdate(pos, total);
	}
	
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle arg1) {
		CursorLoader loader = null;
		int flag = (int) Math.pow(2, id + 1);
		if((OpenCursor.LoadedCursors & flag) == flag) return null;
		switch(id)
		{
			case 0: // videos
				loader = new CursorLoader(
					getApplicationContext(),
					MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
					//Uri.parse("content://media/external/video/media"),
					bRetrieveExtraVideoDetails ?
						new String[]{"_id", "_display_name", "_data", "_size", "date_modified",
								MediaStore.Video.VideoColumns.RESOLUTION,
								MediaStore.Video.VideoColumns.DURATION} :
						new String[]{"_id", "_display_name", "_data", "_size", "date_modified"},
					MediaStore.Video.Media.SIZE + " > 10000", null,
					MediaStore.Video.Media.BUCKET_DISPLAY_NAME + " ASC, " +
					MediaStore.Video.Media.DATE_MODIFIED + " DESC"
						);
				if(bRetrieveExtraVideoDetails)
					try {
						loader.loadInBackground();
					} catch(SQLiteException e) {
						bRetrieveExtraVideoDetails = false;
						setSetting(null, "tag_novidinfo", true);
						return onCreateLoader(id, arg1);
					}
				break;
			case 1: // images
				loader = new CursorLoader(
					getApplicationContext(),
					Uri.parse("content://media/external/images/media"),
					bRetrieveDimensionsForPhotos ? // It seems that < 2.3.3 don't have width & height
						new String[]{"_id", "_display_name", "_data", "_size", "date_modified",
								"width", "height"
							} :
						new String[]{"_id", "_display_name", "_data", "_size", "date_modified"},
					MediaStore.Images.Media.SIZE + " > 10000", null,
					MediaStore.Images.Media.DATE_ADDED + " DESC"
					);
				if(bRetrieveDimensionsForPhotos)
				{
					try {
						loader.loadInBackground();
					} catch(SQLiteException e) {
						bRetrieveDimensionsForPhotos = false;
						setSetting(null, "tag_nodims", true);
						return onCreateLoader(id, arg1);
					}
				}
				break;
			case 2: // music
				loader = new CursorLoader(
					getApplicationContext(),
					Uri.parse("content://media/external/audio/media"),
					new String[]{"_id", "_display_name", "_data", "_size", "date_modified",
							MediaStore.Audio.AudioColumns.DURATION
						},
					MediaStore.Audio.Media.SIZE + " > 10000", null,
					MediaStore.Audio.Media.DATE_ADDED + " DESC"
					);
				break;
			case 3: // apks
				if(bRetrieveCursorFiles)
				loader = new CursorLoader(
					getApplicationContext(),
					MediaStore.Files.getContentUri("/mnt"),
					new String[]{"_id", "_display_name", "_data", "_size", "date_modified"},
					"_size > 10000 AND _data LIKE '%apk'", null,
					"date modified DESC"
					);
				break;
			case 4: // downloads
				if(bRetrieveCursorFiles)
				loader = new CursorLoader(
					getApplicationContext(),
					MediaStore.Files.getContentUri("/"),
					new String[]{"_id", "_display_name", "_data", "_size", "date_modified"},
					"_data LIKE '%download%", null,
					"date modified DESC"
					);
		}

		return loader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> l, Cursor c) {
		if(c == null) return;
		int flag = (int)Math.pow(2, l.getId() + 1);
		if((OpenCursor.LoadedCursors & flag) == flag) {
			c.close();
			return;
		}
		OpenCursor.LoadedCursors |= flag;
		Logger.LogVerbose("LoadedCursors: " + OpenCursor.LoadedCursors);
		OpenCursor mParent = mVideoParent;
		if(l.getId() == 1)
			mParent = mPhotoParent;
		else if(l.getId() == 2)
			mParent = mMusicParent;
		else if(l.getId() == 3)
			mParent = mApkParent;
		mParent.setCursor(c);
		mBookmarks.refresh();
		OpenFragment f = getSelectedFragment();
		if(f instanceof ContentFragment)
			((ContentFragment)f).refreshData(null, false);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> l) {
		onLoadFinished(l, null);
	}

	public void setProgressVisibility(boolean visible) {
		setProgressBarIndeterminateVisibility(visible);
		MenuUtils.setViewsVisible(this, visible, android.R.id.progress, R.id.title_progress);
	}

	public void removeFragment(OpenFragment frag) {
		setCurrentItem(mViewPagerAdapter.getCount() - 1, false);
		if(!mViewPagerAdapter.remove(frag))
			Logger.LogWarning("Unable to remove fragment");
		setViewPageAdapter(mViewPagerAdapter);
		refreshContent();
	}

	@Override
	public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }

	@Override
	public void onPageScrollStateChanged(int state) { }

	@Override
	public void onPageSelected(int position) {
		Logger.LogDebug("onPageSelected(" + position + ")");
		if(BEFORE_HONEYCOMB)
			setupBaseBarButtons();
		final OpenFragment f = getSelectedFragment();
		if(f == null) return;
		if(!f.isDetached())
			ImageUtils.fadeToDrawable((ImageView)findViewById(R.id.title_icon), f.getIcon());
		if((f instanceof ContentFragment) && (((ContentFragment)f).getPath() instanceof OpenNetworkPath))
			((ContentFragment)f).refreshData(null, false);
	}

	public void notifyPager() {
		mViewPager.notifyDataSetChanged();
	}
	
	public OpenApplication getOpenApplication()
	{
		return (OpenApplication)getApplication();
	}

	@Override
	public DataManager getDataManager() {
		return getOpenApplication().getDataManager();
	}

	@Override
	public ImageCacheService getImageCacheService() {
		return getOpenApplication().getImageCacheService();
	}

	@Override
	public DownloadCache getDownloadCache() {
		return getOpenApplication().getDownloadCache();
	}

	@Override
	public ThreadPool getThreadPool() {
		return getOpenApplication().getThreadPool();
	}

	@Override
	public LruCache<String, Bitmap> getMemoryCache() {
		return getOpenApplication().getMemoryCache();
	}

	@Override
	public DiskLruCache getDiskCache() {
		return getOpenApplication().getDiskCache();
	}

	@Override
	public Context getAndroidContext() {
		return getOpenApplication().getAndroidContext();
	}

}

