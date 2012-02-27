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

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.StatFs;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.LayerDrawable;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentManager.BackStackEntry;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.Loader.OnLoadCompleteListener;
import android.support.v4.view.PagerAdapter;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.ActionMode;
import android.view.InflateException;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewStub;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ExpandableListView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.TextView.BufferType;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.RejectedExecutionException;
import java.util.zip.GZIPOutputStream;

import org.brandroid.openmanager.R;
import org.brandroid.openmanager.adapters.ArrayPagerAdapter;
import org.brandroid.openmanager.adapters.ArrayPagerAdapter.OnPageTitleClickListener;
import org.brandroid.openmanager.adapters.IconContextMenu;
import org.brandroid.openmanager.adapters.IconContextMenu.IconContextItemSelectedListener;
import org.brandroid.openmanager.adapters.IconContextMenuAdapter;
import org.brandroid.openmanager.adapters.OpenArrayAdapter;
import org.brandroid.openmanager.adapters.OpenPathAdapter;
import org.brandroid.openmanager.data.OpenBookmarks;
import org.brandroid.openmanager.data.OpenClipboard;
import org.brandroid.openmanager.data.OpenClipboard.OnClipboardUpdateListener;
import org.brandroid.openmanager.data.OpenCursor;
import org.brandroid.openmanager.data.OpenFTP;
import org.brandroid.openmanager.data.OpenFile;
import org.brandroid.openmanager.data.OpenMediaStore;
import org.brandroid.openmanager.data.OpenPath;
import org.brandroid.openmanager.data.OpenPathArray;
import org.brandroid.openmanager.fragments.BookmarkFragment;
import org.brandroid.openmanager.fragments.CarouselFragment;
import org.brandroid.openmanager.fragments.DialogHandler;
import org.brandroid.openmanager.fragments.ContentFragment;
import org.brandroid.openmanager.fragments.OpenFragmentActivity;
import org.brandroid.openmanager.fragments.PreferenceFragmentV11;
import org.brandroid.openmanager.fragments.TextEditorFragment;
import org.brandroid.openmanager.ftp.FTPManager;
import org.brandroid.openmanager.util.BetterPopupWindow;
import org.brandroid.openmanager.util.EventHandler;
import org.brandroid.openmanager.util.EventHandler.OnWorkerThreadFinishedListener;
import org.brandroid.openmanager.util.FileManager.SortType;
import org.brandroid.openmanager.util.MimeTypes;
import org.brandroid.openmanager.util.OpenChromeClient;
import org.brandroid.openmanager.util.OpenInterfaces.OnBookMarkChangeListener;
import org.brandroid.openmanager.util.MimeTypeParser;
import org.brandroid.openmanager.util.OpenInterfaces;
import org.brandroid.openmanager.util.RootManager;
import org.brandroid.openmanager.util.FileManager;
import org.brandroid.openmanager.util.ThumbnailCreator;
import org.brandroid.openmanager.views.OpenPathList;
import org.brandroid.openmanager.views.OpenViewPager;
import org.brandroid.openmanager.views.OpenViewPager.OnPageIndicatorChangeListener;
import org.brandroid.utils.Logger;
import org.brandroid.utils.LoggerDbAdapter;
import org.brandroid.utils.MenuBuilderNew;
import org.brandroid.utils.Preferences;
import com.viewpagerindicator.PageIndicator;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParserException;

public class OpenExplorer
		extends OpenFragmentActivity
		implements OnBackStackChangedListener, OnClipboardUpdateListener,
			OnPageIndicatorChangeListener, OnWorkerThreadFinishedListener,
			OnPageTitleClickListener
	{	

	private static final int PREF_CODE =		0x6;
	private static final int REQ_SPLASH = 7;
	public static final int VIEW_LIST = 0;
	public static final int VIEW_GRID = 1;
	public static final int VIEW_CAROUSEL = Build.VERSION.SDK_INT > 11 ? 2 : 1;
	
	public static final boolean BEFORE_HONEYCOMB = Build.VERSION.SDK_INT < 11;
	public static boolean USE_ACTION_BAR = false;
	public static boolean IS_DEBUG_BUILD = false;
	public static final int REQUEST_CANCEL = 101;
	public static boolean LOW_MEMORY = false;
	public static final boolean SHOW_FILE_DETAILS = false;
	private static final boolean USE_PRETTY_MENUS = !BEFORE_HONEYCOMB;
	
	private static MimeTypes mMimeTypes;
	
	private Preferences mPreferences = null;
	private SearchView mSearchView;
	private ActionMode mActionMode;
	private static OpenClipboard mClipboard;
	private int mLastBackIndex = -1;
	private OpenPath mLastPath = null;
	private BroadcastReceiver storageReceiver = null;
	private Handler mHandler = new Handler();  // handler for the main thread
	//private int mViewMode = VIEW_LIST;
	private static long mLastCursorEnsure = 0;
	private static boolean mRunningCursorEnsure = false;
	private static int mLoadingCount = 0;
	private Boolean mSinglePane = false;
	private Boolean mSinglePanePager = true;
	private Boolean mCreated = false;
	
	private static Fragment mFavoritesFragment = null,
			mContentFragment = null;
	private OpenViewPager mViewPager;
	private static ArrayPagerAdapter mViewPagerAdapter;
	private static boolean mViewPagerEnabled = true; 
	private ExpandableListView mBookmarksList;
	private OpenBookmarks mBookmarks;
	private OpenPathList mSiblingList = null;
	private BetterPopupWindow mBookmarksPopup, mSiblingPopup;
	private static OnBookMarkChangeListener mBookmarkListener;
	
	private static final FileManager mFileManager = new FileManager();
	private static final EventHandler mEvHandler = new EventHandler(mFileManager);
	
	private FragmentManager fragmentManager;
	
	private final static OpenCursor
			mPhotoParent = new OpenCursor("Photos"),
			mVideoParent = new OpenCursor("Videos"),
			mMusicParent = new OpenCursor("Music"),
			mApkParent = new OpenCursor("Apps");
	
	public boolean isViewPagerEnabled() { return mViewPagerEnabled; }
	
	public void onCreate(Bundle savedInstanceState) {

		if(getPreferences().getBoolean("global", "pref_fullscreen", false))
		{
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
								 WindowManager.LayoutParams.FLAG_FULLSCREEN);
		} //else getWindow().addFlags(WindowManager.LayoutParams.FLAG
		mViewPagerEnabled = getPreferences().getBoolean("global", "pref_pagers", false);
		
		Preferences.Pref_Intents_Internal = getPreferences().getBoolean("global", "pref_intent_internal", true);
		Preferences.Pref_Text_Internal = getPreferences().getBoolean("global", "pref_text_internal", true);

		if(BEFORE_HONEYCOMB)
		{
			requestWindowFeature(Window.FEATURE_NO_TITLE);
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
					ab.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_CUSTOM);
					ab.getCustomView().findViewById(R.id.title_menu).setVisibility(View.GONE);
					ab.getCustomView().findViewById(R.id.title_icon).setVisibility(View.GONE);
				} catch(InflateException e) {
					Logger.LogWarning("Couldn't set up ActionBar custom view", e);
				}
			} else USE_ACTION_BAR = false;
		}
		
		getMimeTypes();
		setupLoggingDb();
		
		showWarnings();
		
		mEvHandler.setOnWorkerThreadFinishedListener(this);
		
		mClipboard = new OpenClipboard(this);
		mClipboard.setClipboardUpdateListener(this);
		
		super.onCreate(savedInstanceState);
		
		try {
			Signature[] sigs = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNATURES).signatures;
			for(Signature sig : sigs)
				if(sig.toCharsString().indexOf("4465627567") > -1) // check for "Debug" in signature
					IS_DEBUG_BUILD = true;
		} catch (NameNotFoundException e1) { }
		
		setContentView(R.layout.main_fragments);

		ThumbnailCreator.setContext(getApplicationContext());
		
		Logger.LogDebug("Refreshing cursors");
		
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
		} catch(Exception e) { Logger.LogWarning("Couldn't get root.", e); }
		
		if(!BEFORE_HONEYCOMB)
		{
			setViewVisibility(false, false, R.id.title_underline);
			//if(USE_ACTION_BAR)
			//	setViewVisibility(false, false, R.id.title_bar, R.id.title_underline, R.id.title_underline_2);
		} else {
			setTheme(android.R.style.Theme_Black_NoTitleBar);
		}
		if(BEFORE_HONEYCOMB || !USE_ACTION_BAR)
		{
			ViewStub mTitleStub = (ViewStub)findViewById(R.id.title_stub);
			if(mTitleStub != null)
				mTitleStub.inflate();
			setOnClicks(R.id.title_icon, R.id.title_search, R.id.title_menu, R.id.title_paste, R.id.title_paste_icon, R.id.title_paste_text);
		}
		
		if(findViewById(R.id.list_frag) == null)
			mSinglePane = true;
		else if(findViewById(R.id.list_frag).getVisibility() == View.GONE)
			mSinglePane = true;
		
		mFavoritesFragment = new BookmarkFragment();

		if(mSinglePane || !USE_ACTION_BAR)
		{
			mBookmarksList = new ExpandableListView(getApplicationContext());
			View anchor = findViewById(R.id.title_icon);
			if(anchor == null)
				anchor = findViewById(android.R.id.home);
			mBookmarksPopup = new BetterPopupWindow(this, anchor);
			mBookmarksPopup.setContentView(mBookmarksList);
			mBookmarks = new OpenBookmarks(this, mBookmarksList);
			for(int i = 0; i < mBookmarksList.getCount(); i++)
				mBookmarksList.expandGroup(i);
		}
		
		OpenPath path = mLastPath;
		if(savedInstanceState == null || path == null)
		{	
			String start = getPreferences().getString("global", "pref_start", "Videos");
	
			if(savedInstanceState != null && savedInstanceState.containsKey("last") && !savedInstanceState.getString("last").equals(""))
				start = savedInstanceState.getString("last");

			if(start.startsWith("/"))
				path = new OpenFile(start);
			else if(start.indexOf(":/") > -1)
				path = new OpenFTP(start, null, new FTPManager());
			else if(start.equals("Videos"))
				path = mVideoParent;
			else if(start.equals("Photos"))
				path = mPhotoParent;
			else if(start.equals("Music"))
				path = mMusicParent;
			else if(start.equals("External"))
				path = OpenFile.getExternalMemoryDrive(false);
			else if(start.equals("Internal"))
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
		
		mViewPager = ((OpenViewPager)findViewById(R.id.content_pager));
		if(mViewPagerEnabled && mViewPager != null)
		{
			setViewVisibility(false, false, R.id.content_frag, R.id.title_text, R.id.title_path, R.id.title_bar_inner, R.id.title_underline_2);
			setViewVisibility(true, false, R.id.content_pager, R.id.content_pager_indicator);
			mViewPager.setOnPageIndicatorChangeListener(this);
			ViewGroup indicator_frame = (ViewGroup)findViewById(R.id.content_pager_indicator);
			try {
				//LayoutAnimationController lac = new LayoutAnimationController(AnimationUtils.makeInAnimation(getApplicationContext(), false));
				if(indicator_frame != null)
					indicator_frame.setAnimation(AnimationUtils.makeInAnimation(getApplicationContext(), false));
			} catch(Resources.NotFoundException e)
			{
				Logger.LogError("Couldn't load pager animation.", e);
			}
			PageIndicator indicator = (PageIndicator)findViewById(R.id.content_pager_indicator);
			if(indicator != null)
				mViewPager.setIndicator(indicator);
			//mViewPager = new ViewPager(getApplicationContext());
			//((ViewGroup)findViewById(R.id.content_frag)).addView(mViewPager);
			//findViewById(R.id.content_frag).setId(R.id.fake_content_id);
		} else {
			mViewPagerEnabled = false;
			mViewPager = null; //(ViewPager)findViewById(R.id.content_pager);
			setViewVisibility(false, false, R.id.content_pager, R.id.content_pager_indicator);
			setViewVisibility(true, false, R.id.content_frag, R.id.title_text, R.id.title_path, R.id.title_bar_inner, R.id.title_underline_2);
		}

		mContentFragment = ContentFragment.getInstance(mLastPath, mViewMode);
		
		if(fragmentManager == null)
		{
			fragmentManager = getSupportFragmentManager();
			fragmentManager.addOnBackStackChangedListener(this);
		}

		if(mViewPager != null && mViewPagerEnabled)
		{
			Logger.LogVerbose("Setting up ViewPager");
			mViewPagerAdapter = new ArrayPagerAdapter(fragmentManager);
			mViewPagerAdapter.setOnPageTitleClickListener(this);
			mViewPagerAdapter.add(mContentFragment);
			setViewPageAdapter(mViewPagerAdapter);
		}

		FragmentTransaction ft = fragmentManager.beginTransaction();

		Logger.LogDebug("Creating with " + path.getPath());
		if(OpenFile.class.equals(path.getClass()))
			new PeekAtGrandKidsTask().execute((OpenFile)path);

		if(!mSinglePane && mFavoritesFragment != null)
		{
			fragmentManager.beginTransaction()
				.replace(R.id.list_frag, mFavoritesFragment)
				.commit();
		}
				
		Intent intent = getIntent();
		if(intent != null && intent.getAction() != null &&
				(intent.getAction().equals(Intent.ACTION_EDIT) ||
						(intent.getAction().equals(Intent.ACTION_VIEW) && intent.getData() != null)))
		{
			Uri file = intent.getData();
			Logger.LogInfo("Editing " + file.toString());
			mContentFragment = new TextEditorFragment(new OpenFile(file.getPath()));
			bAddToStack = false;
		}
		
		if(findViewById(R.id.content_frag) != null && !mViewPagerEnabled)
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
		} else if(mViewPager != null && mViewPagerAdapter != null)
		{
			//mViewPagerAdapter.add(mContentFragment);
			mViewPager.setCurrentItem(mViewPagerAdapter.getCount() - 1);
			if(bAddToStack)
			{
				ft.addToBackStack("path");
				ft.setBreadCrumbTitle(path.getPath());
			}
		}

		ft.commit();
		
		updateTitle(mLastPath.getPath());
		if(!BEFORE_HONEYCOMB)
			invalidateOptionsMenu();
		
		handleMediaReceiver();

		if(!getPreferences().getBoolean("global", "pref_splash", false))
			showSplashIntent(this, getPreferences().getString("global", "pref_start", "External"));
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
							// TODO Auto-generated method stub
							
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

	private void setViewPageAdapter(PagerAdapter adapter)
	{
		if(mViewPager != null)
		{
			mViewPager.setAdapter(adapter);
		}
	}
	
	private void showWarnings()
	{
		if(!getPreferences().getBoolean("warn", "pager", false))
		{
			new AlertDialog.Builder(this)
				.setTitle(R.string.s_warn_pager_title)
				.setMessage(getString(R.string.s_warn_pager) + "\n" + getString(R.string.s_warn_confirm))
				.setPositiveButton(android.R.string.yes, new Dialog.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							getPreferences().setSetting("global", "pref_pagers", true);
							getPreferences().setSetting("warn", "pager", true);
							goHome();
							finish();
						}
					})
				.setNegativeButton(android.R.string.no, null)
				.setNeutralButton(R.string.s_warn_disable, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							getPreferences().setSetting("warn", "pager", true);
							getPreferences().setSetting("global", "pref_pagers", false);
							goHome();
							finish();
						}
					})
				.create()
				.show();
		}
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
			Fragment f = mViewPagerAdapter.getItem(i);
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
		Fragment curr = mViewPagerAdapter.getItem(page);
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
			Fragment f = mViewPagerAdapter.getItem(i);
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
	}
	
	public void setBookmarksPopupListAdapter(ListAdapter adapter)
	{
		mBookmarksList.setAdapter(adapter);
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		setupLoggingDb();
		submitStats();
		if(mFavoritesFragment != null)
			((BookmarkFragment)mFavoritesFragment).scanBookmarks();
	}
	
	private void setupLoggingDb()
	{
		if(Logger.isLoggingEnabled())
		{
			if(getPreferences().getBoolean("global", "pref_stats", false))
			{
				if(!Logger.hasDb())
					Logger.setDb(new LoggerDbAdapter(getApplicationContext()));
			} else
				Logger.setLoggingEnabled(false);
		}
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		if(Logger.isLoggingEnabled() && Logger.hasDb())
			Logger.closeDb();
	}
	
	private JSONObject getDeviceInfo()
	{
		JSONObject ret = new JSONObject();
		try {
			ret.put("SDK", Build.VERSION.SDK_INT);
			ret.put("Language", Locale.getDefault().getDisplayLanguage());
			ret.put("Country", Locale.getDefault().getDisplayCountry());
			ret.put("Brand", Build.BRAND);
			ret.put("Manufacturer", Build.MANUFACTURER);
			ret.put("Model", Build.MODEL);
			ret.put("Product", Build.PRODUCT);
			ret.put("Board", Build.BOARD);
			ret.put("Tags", Build.TAGS);
			ret.put("Type", Build.TYPE);
			ret.put("Bootloader", Build.BOOTLOADER);
			ret.put("Hardware", Build.HARDWARE);
			ret.put("User", Build.USER);
			if(Build.UNKNOWN != null)
				ret.put("Unknown", Build.UNKNOWN);
			ret.put("Display", Build.DISPLAY);
			ret.put("Fingerprint", Build.FINGERPRINT);
			ret.put("ID", Build.ID);
		} catch (JSONException e) {
			
		}
		return ret;
	}
	
	private void submitStats()
	{
		if(!Logger.isLoggingEnabled()) return;
		setupLoggingDb();
		if(IS_DEBUG_BUILD) return;
		String logs = Logger.getDbLogs(false);
		if(logs == null) logs = "[]";
		//if(logs != null && logs != "") {
			Logger.LogDebug("Found " + logs.length() + " bytes of logs.");
			new SubmitStatsTask().execute(logs);
		//} else Logger.LogWarning("Logs not found.");
	}
	
	public Preferences getPreferences() {
		if(mPreferences == null)
			mPreferences = new Preferences(getApplicationContext());
		return mPreferences;
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
	
	private class LoadMediaStore extends CursorLoader
	{
		public int which = 0;

		public LoadMediaStore(Context context, Uri uri, String[] projection,
				String selection, String[] selectionArgs, String sortOrder) {
			super(context, uri, projection, selection, selectionArgs, sortOrder);
		}
		
		@Override
		protected void onStopLoading() {
			super.onStopLoading();
			Logger.LogDebug("LoadMediaStore onStopLoading");
		}
		
		@Override
		protected void onAbandon() {
			super.onAbandon();
			Logger.LogDebug("LoadMediaStore onAbandon");
		}
		
		@Override
		public void onCanceled(Cursor cursor) {
			super.onCanceled(cursor);
			Logger.LogDebug("LoadMediaStore onCancelled");
		}
		
		@Override
		protected void onStartLoading() {
			super.onStartLoading();
			Logger.LogDebug("LoadMediaStore onStartLoading");
		}

		@Override
		public Cursor loadInBackground() {
			// TODO Auto-generated method stub
			Cursor c = super.loadInBackground();
			if(c == null) {
				Logger.LogWarning("Cursor was null :(");
				return c;
			}
			if(which == 0 && mVideoParent == null)
			{
				//Logger.LogInfo("We got videos");
				mVideoParent.setCursor(c);
				ensureCache(mVideoParent);
			} else if(which == 1 && mPhotoParent == null) {
				//Logger.LogInfo("We got photos");
				mPhotoParent.setCursor(c);
				ensureCache(mPhotoParent);
			} else if(which == 2 && mMusicParent == null) {
				//Logger.LogInfo("We got music");
				mMusicParent.setCursor(c);
			} else if(which == 3)
			{
				//Logger.LogInfo("We got apps");
				mApkParent.setCursor(c);
				ensureCache(mApkParent);
			}
			c.close();
			return c;
		}

		public LoadMediaStore setCursorType(int type) { which = type; return this; }
	}
	

	
	private void ensureCache(OpenCursor parent)
	{
		int done = 0;
		for(OpenPath kid : parent.list())
		{
			ThumbnailCreator.generateThumb(kid, 36, 36);
			ThumbnailCreator.generateThumb(kid, 128, 128);
			done++;
		}
		Logger.LogInfo("ensureCache on " + parent.getName() + " = " + done + "/" + parent.list().length);
	}
	
	private boolean findCursors()
	{
		if(mVideoParent.isLoaded())
		{
			//Logger.LogDebug("Videos should be found");
		}else
		{
			//Logger.LogDebug("Finding videos");
			//if(!IS_DEBUG_BUILD)
			try {
				CursorLoader loader = new CursorLoader(
				//new LoadMediaStore(
						getApplicationContext(),
						Uri.parse("content://media/external/video/media"),
						new String[]{"_id", "_display_name", "_data", "_size", "date_modified"},
						MediaStore.Video.Media.SIZE + " > 10000", null,
						MediaStore.Video.Media.BUCKET_DISPLAY_NAME + " ASC, " +
						MediaStore.Video.Media.DATE_MODIFIED + " DESC"
						//).setCursorType(0).forceLoad();
						);
				Cursor c = loader.loadInBackground();
				if(c != null)
				{
					mVideoParent.setCursor(c);
					c.close();
				}
				//*/
			} catch(Exception e) { Logger.LogError("Couldn't query videos.", e); }
			Logger.LogDebug("Done looking for videos");
		}
		if(!mPhotoParent.isLoaded())
		{
			//Logger.LogDebug("Finding Photos");
			try {
				CursorLoader loader = new CursorLoader(
				//new LoadMediaStore(
						getApplicationContext(),
						Uri.parse("content://media/external/images/media"),
						new String[]{"_id", "_display_name", "_data", "_size", "date_modified"},
						MediaStore.Images.Media.SIZE + " > 10000", null,
						MediaStore.Images.Media.DATE_ADDED + " DESC"
						//).setCursorType(1).forceLoad();
						);
				/*
				loader.registerListener(1, new OnLoadCompleteListener<Cursor>() {
					@Override
					public void onLoadComplete(Loader<Cursor> arg0, Cursor cursor) {
						Logger.LogVerbose("PhotoLoader onLoadComplete");
						if(cursor != null)
						{
							mPhotoParent.setCursor(cursor);
							cursor.close();
						} else Logger.LogWarning("Bad Photo Cursor");
					}
				});
				loader.startLoading();
				*/
				Cursor c = loader.loadInBackground();
				if(c != null)
				{
					mPhotoParent.setCursor(c);
					c.close();
				}
				
			} catch(IllegalStateException e) { Logger.LogError("Couldn't query photos.", e); }
		}
		if(!mMusicParent.isLoaded())
		{
			//Logger.LogDebug("Finding Music");
			try {
				CursorLoader loader = new CursorLoader(
				//new LoadMediaStore(
						getApplicationContext(),
						Uri.parse("content://media/external/audio/media"),
						new String[]{"_id", "_display_name", "_data", "_size", "date_modified"},
						MediaStore.Audio.Media.SIZE + " > 10000", null,
						MediaStore.Audio.Media.DATE_ADDED + " DESC"
						//).setCursorType(2).forceLoad();
						);
				Cursor c = loader.loadInBackground();
				if(c != null)
				{
					mMusicParent.setCursor(c);
					c.close();
				}
			} catch(IllegalStateException e) { Logger.LogError("Couldn't query music.", e); }
		}
		if(!mApkParent.isLoaded() && Build.VERSION.SDK_INT > 10)
		{
			//Logger.LogDebug("Finding APKs");
			try {
				CursorLoader loader = new CursorLoader(
				//new LoadMediaStore(
						getApplicationContext(),
						MediaStore.Files.getContentUri("/mnt"),
						new String[]{"_id", "_display_name", "_data", "_size", "date_modified"},
						"_size > 10000 AND _data LIKE '%apk'", null,
						"date modified DESC"
						//).setCursorType(3).forceLoad();
						);
				Cursor c = loader.loadInBackground();
				if(c != null)
				{
					mApkParent.setCursor(c);
					c.close();
				}
			} catch(IllegalStateException e) { Logger.LogError("Couldn't get Apks.", e); }
		}
		return true;
	}
	private void refreshCursors()
	{
		if(findCursors())
			return;
		/*
		new Thread(new Runnable() {public void run() {
			ensureCursorCache();
		}}).run();
		*/
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
		
		mLastCursorEnsure = new Date().getTime();
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
		} else {
			View v = findViewById(R.id.list_frag);
			if(v == null && fragmentManager.findFragmentById(R.id.list_frag) != null)
				v = fragmentManager.findFragmentById(R.id.list_frag).getView();
			if(v != null)
			{
				v.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), visible ? android.R.anim.fade_in : android.R.anim.fade_out));
				v.setVisibility(visible ? View.VISIBLE : View.GONE);
			} else
			{
				if(mFavoritesFragment == null)
					mFavoritesFragment = new BookmarkFragment();
				
				if(mViewPager != null && findViewById(R.id.content_frag) != null)
				{
					FragmentTransaction ft = fragmentManager.beginTransaction();
					if(mFavoritesFragment.isVisible())
						ft.replace(R.id.content_frag, ContentFragment.getInstance(mLastPath, getSetting(mLastPath, "view", 0)));
					else
						ft.replace(R.id.content_frag, mFavoritesFragment);
		
					ft.addToBackStack("favs");
					ft.commit();
				} else if(mViewPager != null)
					mViewPager.setCurrentItem(0, true);
			}
		}
	}
	
	public void toggleBookmarks()
	{
		View mBookmarks = findViewById(R.id.list_frag);
		toggleBookmarks(mBookmarks == null || mBookmarks.getVisibility() == View.GONE);
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
	public ContentFragment getDirContentFragment(Boolean activate)
	{
		//Logger.LogDebug("getDirContentFragment");
		Fragment ret = null;
		//if(mViewPager != null && mViewPagerAdapter != null && mViewPagerAdapter instanceof OpenPathPagerAdapter && ((OpenPathPagerAdapter)mViewPagerAdapter).getLastItem() instanceof ContentFragment)
		//	ret = ((ContentFragment)((OpenPathPagerAdapter)mViewPagerAdapter).getLastItem());
		if(mViewPagerAdapter != null)
			ret = mViewPagerAdapter.getItem(mViewPagerAdapter.getLastPositionOfType(ContentFragment.class));
		if(ret == null)
			ret = fragmentManager.findFragmentById(R.id.content_frag);
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
			if(mViewPagerAdapter != null)
				ret = mViewPagerAdapter.getItem(mViewPager.getCurrentItem());
		}
		if(ret == null)
		{
			ret = ContentFragment.getInstance(mLastPath, getSetting(mLastPath, "view", 0));
			if(mViewPager != null)
			{
				//if(mViewPagerAdapter instanceof ArrayPagerAdapter)
					mViewPagerAdapter.set(mViewPager.getCurrentItem(), ret);
			}
		}
		if(activate && !ret.isVisible())
		{
			if(mViewPager != null)
				mViewPager.setCurrentItem(mViewPagerAdapter.getItemPosition(ret));
			else {
				Logger.LogDebug("Activating content fragment");
				fragmentManager.beginTransaction()
					.replace(R.id.content_frag, ret)
					.commitAllowingStateLoss();
			}
		}
		
   		return (ContentFragment)ret;
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
	
	public void editFile(OpenPath path)
	{
		TextEditorFragment editor = new TextEditorFragment(path);
		if(mViewPagerAdapter != null && mViewPagerAdapter instanceof ArrayPagerAdapter)
		{
			mViewPagerAdapter.add(editor);
			mViewPager.setCurrentItem(mViewPagerAdapter.getCount() - 1, true);
		} else
			fragmentManager.beginTransaction()
				.replace(R.id.content_frag, editor)
				//.addToBackStack(null)
				.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
				.commit();
		//addTab(editor, path.getName(), true);
	}
	
	public void goHome()
	{
		Intent intent = new Intent(this, OpenExplorer.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.putExtra("last", mLastPath.getPath());
		startActivity(intent);
	}
	
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main_menu, menu);
		if(!BEFORE_HONEYCOMB && USE_ACTION_BAR)
		{
			mSearchView = (SearchView)menu.findItem(R.id.menu_search).getActionView();
			mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
				public boolean onQueryTextSubmit(String query) {
					mSearchView.clearFocus();
					mEvHandler.searchFile(mLastPath, query, getApplicationContext());
					return true;
				}
				public boolean onQueryTextChange(String newText) {
					return false;
				}
			});
		}
		if(!USE_PRETTY_MENUS||!BEFORE_HONEYCOMB)
		{
			MenuItem sort = menu.findItem(R.id.menu_sort);
			try {
				getMenuInflater().inflate(R.menu.menu_sort, sort.getSubMenu());
			} catch(NullPointerException npe) { }
			MenuItem view = menu.findItem(R.id.menu_view);
			try {
				getMenuInflater().inflate(R.menu.menu_view, view.getSubMenu());
			} catch(NullPointerException npe) { }
			MenuItem paste = menu.findItem(R.id.menu_paste);
			try {
				getMenuInflater().inflate(R.menu.multiselect, paste.getSubMenu());
			} catch(NullPointerException npe) { }
		}
		onPrepareOptionsMenu(menu);
		super.onCreateOptionsMenu(menu);
		return true;
	}
	
	public static void setMenuChecked(Menu menu, boolean checked, int toCheck, int... toOppose)
	{
		for(int id : toOppose)
			if(menu.findItem(id) != null)
				menu.findItem(id).setChecked(!checked);
		if(menu.findItem(toCheck) != null)
			menu.findItem(toCheck).setChecked(checked);
	}

	public static void setMenuVisible(Menu menu, boolean visible, int... ids)
	{
		for(int id : ids)
			if(menu.findItem(id) != null)
				menu.findItem(id).setVisible(visible);
	}
	public static void setMenuShowAsAction(Menu menu, int show, int... ids)
	{
		for(int id : ids)
			if(menu.findItem(id) != null)
				menu.findItem(id).setShowAsAction(show);
	}
	
	public static void setMenuEnabled(Menu menu, boolean enabled, int... ids)
	{
		for(int id : ids)
			if(menu.findItem(id) != null)
				menu.findItem(id).setEnabled(enabled);
	}
	
	public void showAboutDialog()
	{
		LayoutInflater li = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		View view = li.inflate(R.layout.about, null);

		String sVersionInfo = "";
		try {
			PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
			sVersionInfo += pi.versionName;
			if(!pi.versionName.contains(""+pi.versionCode))
				sVersionInfo += " (" + pi.versionCode + ")";
			if(OpenExplorer.IS_DEBUG_BUILD)
				sVersionInfo += " *debug*";
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		final String sSubject = "Feedback for OpenExplorer " + sVersionInfo; 

		OnClickListener email = new OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_SEND);
				intent.setType("text/plain");
				//intent.addCategory(Intent.CATEGORY_APP_EMAIL);
				intent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{"brandroid64@gmail.com"});
				intent.putExtra(android.content.Intent.EXTRA_SUBJECT, sSubject);
				startActivity(Intent.createChooser(intent, getString(R.string.s_chooser_email)));
			}
		};
		OnClickListener viewsite = new OnClickListener() {
			public void onClick(View v) {
				startActivity(
					new Intent(android.content.Intent.ACTION_VIEW, Uri.parse("http://brandroid.org/open/"))
					);
			}
		};
		view.findViewById(R.id.about_email).setOnClickListener(email);
		view.findViewById(R.id.about_email_btn).setOnClickListener(email);
		view.findViewById(R.id.about_site).setOnClickListener(viewsite);
		view.findViewById(R.id.about_site_btn).setOnClickListener(viewsite);
		WebView mRecent = (WebView)view.findViewById(R.id.about_recent);
		OpenChromeClient occ = new OpenChromeClient();
		occ.mStatus = (TextView)view.findViewById(R.id.about_recent_status);
		mRecent.setWebChromeClient(occ);
		mRecent.setBackgroundColor(Color.TRANSPARENT);
		mRecent.loadUrl("http://brandroid.org/open/?show=recent");
		
		((TextView)view.findViewById(R.id.about_version)).setText(sVersionInfo);
		AlertDialog mDlgAbout = new AlertDialog.Builder(this)
			.setTitle(R.string.app_name)
			.setView(view)
			.create();
		
		mDlgAbout.getWindow().getAttributes().windowAnimations = R.style.SlideDialogAnimation;
		mDlgAbout.getWindow().getAttributes().alpha = 0.9f;
		
		mDlgAbout.show();
	}
	
	public AlertDialog showConfirmationDialog(String msg, String title, DialogInterface.OnClickListener onYes)
	{
		return new AlertDialog.Builder(this)
			.setTitle(title)
			.setMessage(msg)
			.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.cancel();
				}
			})
			.setPositiveButton(android.R.string.yes, onYes)
			.show();
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if(BEFORE_HONEYCOMB)
			setMenuVisible(menu, false, R.id.menu_view_carousel);
		else if(getWindowManager().getDefaultDisplay().getWidth() < 500) {
			if(Build.VERSION.SDK_INT < 14) // ICS can split the actionbar
			{
				setMenuShowAsAction(menu, MenuItem.SHOW_AS_ACTION_NEVER, R.id.menu_sort, R.id.menu_view, R.id.menu_new_folder);
				setMenuVisible(menu, true, R.id.title_menu);
			}
		}
		
		ContentFragment frag = getDirContentFragment(false);
		
		switch(frag.getSorting())
		{
		case ALPHA:
			setMenuChecked(menu, true, R.id.menu_sort_name_asc);
			break;
		case ALPHA_DESC:
			setMenuChecked(menu, true, R.id.menu_sort_name_desc);
			break;
		case DATE:
			setMenuChecked(menu, true, R.id.menu_sort_date_asc);
			break;
		case DATE_DESC:
			setMenuChecked(menu, true, R.id.menu_sort_date_desc);
			break;
		case SIZE:
			setMenuChecked(menu, true, R.id.menu_sort_size_asc);
			break;
		case SIZE_DESC:
			setMenuChecked(menu, true, R.id.menu_sort_size_desc);
			break;
		case TYPE:
			setMenuChecked(menu, true, R.id.menu_sort_type);
			break;
		}
		
		setMenuChecked(menu, getClipboard().isMultiselect(), R.id.menu_multi);
		
		setMenuChecked(menu, getPreferences().getBoolean("global", "pref_fullscreen", true), R.id.menu_view_fullscreen);
		if(Build.VERSION.SDK_INT < 14 && !BEFORE_HONEYCOMB) // honeycomb
			setMenuVisible(menu, false, R.id.menu_view_fullscreen);
		
		if(menu.findItem(R.id.menu_context_unzip) != null && getClipboard().getCount() == 0)
			menu.findItem(R.id.menu_context_unzip).setVisible(false);
		
		if(!mSinglePane)
			setMenuVisible(menu, false, R.id.menu_favorites);
		
		if(mClipboard == null || mClipboard.size() == 0)
		{
			setMenuVisible(menu, false, R.id.menu_paste);
		} else {
			MenuItem mPaste = menu.findItem(R.id.menu_paste);
			mPaste.setTitle(getString(R.string.s_menu_paste) + " (" + getClipboard().size() + ")");
			if(getClipboard().isMultiselect())
			{
				LayerDrawable d = (LayerDrawable) getResources().getDrawable(R.drawable.ic_menu_paste_multi);
				d.getDrawable(1).setAlpha(127);
				menu.findItem(R.id.menu_paste).setIcon(d);
			}
			/*
			if(!BEFORE_HONEYCOMB)
			{
				SubMenu pasties = mPaste.getSubMenu();
				pasties.clear();
				getMenuInflater().inflate(R.menu.multiselect, pasties);
				if(getClipboard().isMultiselect())
					pasties.findItem(R.id.menu_multi).setChecked(true);
				for(OpenPath p : getClipboard().getAll())
					pasties.add(p.getName());
			}
			*/
			//if()
			//mPaste.setIcon();
			//mPaste.setIcon(R.drawable.bluetooth);
			mPaste.setVisible(true);
		}
		
		int mViewMode = frag.getViewMode();
		setMenuChecked(menu, true, 0, R.id.menu_view_grid, R.id.menu_view_list, R.id.menu_view_carousel);
		if(mViewMode == VIEW_GRID)
			setMenuChecked(menu, true, R.id.menu_view_grid, R.id.menu_view_list, R.id.menu_view_carousel);
		else if(mViewMode == VIEW_LIST)
			setMenuChecked(menu, true, R.id.menu_view_list, R.id.menu_view_grid, R.id.menu_view_carousel);
		else if(mViewMode == VIEW_CAROUSEL)
			setMenuChecked(menu, true, R.id.menu_view_carousel, R.id.menu_view_grid, R.id.menu_view_list);
		
		setMenuChecked(menu, frag.getShowHiddenFiles(), R.id.menu_view_hidden);
		setMenuChecked(menu, frag.getShowThumbnails(), R.id.menu_view_thumbs);
		
		if(RootManager.Default.isRoot())
			setMenuChecked(menu, true, R.id.menu_root);
		
		return super.onPrepareOptionsMenu(menu);
	}
	
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if(item.isCheckable())
			item.setChecked(item.getGroupId() > 0 ? true : !item.isChecked());
		
		return onClick(item.getItemId(), item, null);
	}
	public boolean onClick(int id, MenuItem item, View from)
	{
		super.onClick(id);
		if(id != R.id.title_icon && id != android.R.id.home);
			toggleBookmarks(false);
		switch(id)
		{
			case R.id.title_icon:
			case android.R.id.home:
			
				if(mSinglePane)
					toggleBookmarks();
				else
					goHome();

				return true;
				
			
			case R.id.menu_new_folder:
				mEvHandler.createNewFolder(mLastPath.getPath(), this);
				return true;
				
			case R.id.menu_multi:
				if(getClipboard().isMultiselect())
				{
					getClipboard().stopMultiselect();
					if(!BEFORE_HONEYCOMB && mActionMode != null)
						((ActionMode)mActionMode).finish();
					return true;
				}
				
				if(BEFORE_HONEYCOMB)
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
							//setMenuVisible(menu, false, R.id.menu_context_paste, R.id.menu_context_unzip);
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
				if(!USE_ACTION_BAR)
					showMenu(R.menu.menu_sort, from);
				return true;
				
			case R.id.menu_sort_name_asc:	setSorting(FileManager.SortType.ALPHA); return true; 
			case R.id.menu_sort_name_desc:	setSorting(FileManager.SortType.ALPHA_DESC); return true; 
			case R.id.menu_sort_date_asc: 	setSorting(FileManager.SortType.DATE); return true;
			case R.id.menu_sort_date_desc: 	setSorting(FileManager.SortType.DATE_DESC); return true; 
			case R.id.menu_sort_size_asc: 	setSorting(FileManager.SortType.SIZE); return true; 
			case R.id.menu_sort_size_desc: 	setSorting(FileManager.SortType.SIZE_DESC); return true; 
			case R.id.menu_sort_type: 		setSorting(FileManager.SortType.TYPE); return true;

			
			case R.id.menu_view:
				//if(BEFORE_HONEYCOMB)
				//	showMenu(item.getSubMenu(), from);
				if(!USE_ACTION_BAR)
					showMenu(R.menu.menu_view, from);
				return true;
				
			case R.id.menu_view_fullscreen:
				getPreferences().setSetting("global", "pref_fullscreen", item.isChecked());
				goHome();
				return true;
				
			case R.id.menu_view_grid:
				changeViewMode(VIEW_GRID, true);
				return true;
			case R.id.menu_view_list:
				changeViewMode(VIEW_LIST, true);
				return true;
			case R.id.menu_view_carousel:
				changeViewMode(VIEW_CAROUSEL, true);
				return true;
			case R.id.menu_view_hidden: setShowHiddenFiles(item.isChecked()); return true;
			case R.id.menu_view_thumbs: setShowThumbnails(item.isChecked()); return true;
					
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
				ThumbnailCreator.flushCache();
				goHome();
				return true;
				
			case R.id.menu_settings:
				showPreferences(null);
				return true;
			
			case R.id.menu_settings_folder:
				showPreferences(mLastPath);
				return true;
				
			case R.id.title_search:
			case R.id.menu_search:
				return onSearchRequested();

			case R.id.menu_favorites:
				toggleBookmarks();
				return true;
				
			case R.id.title_menu:
				Logger.LogInfo("Show menu!");
				showMenu();
				return true;
			
			case R.id.menu_multi_all_delete:
				showConfirmationDialog(
						getResources().getString(R.string.s_confirm_delete).replace("xxx", getClipboard().getCount() + " " + getResources().getString(R.string.s_files)),
						getResources().getString(R.string.s_menu_delete_all),
						new DialogInterface.OnClickListener() { // yes
							public void onClick(DialogInterface dialog, int which) {
								for(OpenPath file : getClipboard().getAll())
									file.delete();
							}
						});
				break;
				
			case R.id.menu_multi_all_clear:
				getClipboard().clear();
				return true;

			case R.id.menu_multi_all_copy:
				mClipboard.DeleteSource = false;
				getDirContentFragment(false).executeMenu(R.id.menu_paste, null, mLastPath, mClipboard);
				break;
				
			case R.id.menu_multi_all_move:
				mClipboard.DeleteSource = true;
				getDirContentFragment(false).executeMenu(R.id.menu_paste, null, mLastPath, mClipboard);
				break;
				
			case R.id.title_paste:
			case R.id.title_paste_icon:
			case R.id.title_paste_text:
			case R.id.menu_paste:
				showClipboardDropdown(id);
				return true;
				
				//getDirContentFragment(false).executeMenu(R.id.menu_paste, null, mLastPath, mClipboard);
				//return true;
				
			case R.id.menu_about:
				showAboutDialog();
				return true;
				
			default:
				getDirContentFragment(false).executeMenu(id, mLastPath);
				return true;
		}
		
		return true;
		//return super.onOptionsItemSelected(item);
	}
	
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
		ListView items = (ListView)root.findViewById(R.id.multiselect_item_list);
		items.setAdapter(mClipboard);
		items.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> list, View view, int pos, long id) {
				OpenPath file = mClipboard.get(pos);
				//if(file.getParent().equals(mLastPath))
					mClipboard.remove(pos);
				//else
					//getEventHandler().copyFile(file, mLastPath, OpenExplorer.this);
			}
		});
		final Menu menu = IconContextMenu.newMenu(this, R.menu.multiselect);
		setMenuChecked(menu, getClipboard().isMultiselect(), R.id.menu_multi);
		final IconContextMenuAdapter cmdAdapter = new IconContextMenuAdapter(this, menu);
		cmds.setAdapter(cmdAdapter);
		cmds.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> list, View view, int pos, long id) {
				MenuItem item = menu.getItem(pos);
				onClick(item.getItemId(), item, view);
				clipdrop.dismiss();
			}
		});
		
		float w = getResources().getDimension(R.dimen.popup_width) * (3 / 2);
		if(w > getWindow().getWindowManager().getDefaultDisplay().getWidth())
			w = getWindow().getWindowManager().getDefaultDisplay().getWidth() - 20;
		clipdrop.setPopupWidth((int)w);
		clipdrop.setContentView(root);
		
		clipdrop.showLikePopDownMenu();
		//dropdown.setAdapter(this, new IconContextMenuAdapter(context, menu))
		//BetterPopupWindow win = new BetterPopupWindow(this, anchor);
		//ListView list = ((LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(R.layout.multiselect, null);
		//win.setContentView(root)
	}
	
	private void setShowThumbnails(boolean checked) {
		//setSetting(mLastPath, "thumbs", checked);
		getDirContentFragment(true).onThumbnailChanged(checked);
	}
	private void setShowHiddenFiles(boolean checked) {
		//setSetting(mLastPath, "hide", checked);
		getDirContentFragment(true).onHiddenFilesChanged(checked);
	}
	private void setSorting(SortType sort) {
		//setSetting(mLastPath, "sort", sort.toString());
		//getFileManager().setSorting(sort);
		getDirContentFragment(true).onSortingChanged(sort);
		if(!BEFORE_HONEYCOMB)
			invalidateOptionsMenu();
	}
	
	public void showMenu() {
		if(USE_PRETTY_MENUS)
			showMenu(R.menu.main_menu, findViewById(R.id.title_menu));
		else
			openOptionsMenu();
		//showMenu(R.menu.main_menu_top);
	}
	public void showMenu(int menuId, final View from)
	{
		//if(mMenuPopup == null)
		if(showContextMenu(menuId, from) == null)
			if(menuId == R.menu.main_menu || menuId == R.id.title_menu)
				openOptionsMenu();
			else if (menuId == R.id.menu_sort)
				showMenu(R.menu.menu_sort, from);
			else if(menuId == R.id.menu_view)
				showMenu(R.menu.menu_view, from);
			//else
			//	showToast("Invalid option (" + menuId + ")" + (from != null ? " under " + from.toString() + " (" + from.getLeft() + "," + from.getTop() + ")" : ""));
	}
	public void showMenu(Menu menu, final View from)
	{
		if(from != null){
			if(showContextMenu(menu, from) == null)
				openOptionsMenu();
		} else openOptionsMenu();
	}
	public IconContextMenu showContextMenu(int menuId, final View from)
	{
		try {
			Logger.LogDebug("Trying to show context menu" + (from != null ? " under " + from.toString() + " (" + from.getLeft() + "," + from.getTop() + ")" : "") + ".");
			if(menuId == R.id.menu_sort || menuId == R.menu.menu_sort)
				menuId = R.menu.menu_sort;
			else if(menuId == R.id.menu_view || menuId == R.menu.menu_view)
				menuId = R.menu.menu_view;
			else if(menuId == R.id.title_menu || menuId == R.menu.main_menu)
				menuId = R.menu.main_menu;
			else
				return null;
			if(menuId == R.menu.context_file || menuId == R.menu.main_menu || menuId == R.menu.menu_sort || menuId == R.menu.menu_view)
			{
				Menu menu = new MenuBuilderNew(this);
				new MenuInflater(this).inflate(menuId, menu);
				onPrepareOptionsMenu(menu);
				final IconContextMenu icm = new IconContextMenu(OpenExplorer.this, menu, from, null, null);
				if(icm == null)
					throw new NullPointerException("context menu returned null");
				icm.setOnIconContextItemSelectedListener(new IconContextItemSelectedListener() {
					public void onIconContextItemSelected(MenuItem item, Object info, View view) {
						//showToast(item.getTitle().toString());
						if(item.getItemId() == R.id.menu_sort)
							showMenu(R.menu.menu_sort, view);
						else if(item.getItemId() == R.id.menu_view)
							showMenu(R.menu.menu_view, view);
						else
							onClick(item.getItemId(), item, view);
						//icm.dismiss();
						//mMenuPopup.dismiss();
					}
				});
				if(menuId == R.menu.menu_sort)
					icm.setTitle(R.string.s_menu_sort);
				else if(menuId == R.menu.menu_view)
					icm.setTitle(R.string.s_view);
				icm.show();
				return icm;
			} else return showContextMenu(menuId, null);
		} catch(Exception e) {
			Logger.LogWarning("Couldn't show icon context menu" + (from != null ? " under " + from.toString() + " (" + from.getLeft() + "," + from.getTop() + ")" : "") + ".", e);
			if(from != null)
				return showContextMenu(menuId, null);
		}
		Logger.LogInfo("Not sure what happend with " + menuId + (from != null ? " under " + from.toString() + " (" + from.getLeft() + "," + from.getTop() + ")" : "") + ".");
		return null;
	}
	public IconContextMenu showContextMenu(Menu menu, final View from)
	{
		Logger.LogDebug("Trying to show context menu " + menu.toString() + (from != null ? " under " + from.toString() + " (" + from.getLeft() + "," + from.getTop() + ")" : "") + ".");
		IconContextMenu icm = null;
		try {
			icm = new IconContextMenu(this, menu, from, null, null);
			if(menu.size() > 10)
				icm.setNumColumns(2);
			icm.setOnIconContextItemSelectedListener(new IconContextItemSelectedListener() {
				public void onIconContextItemSelected(MenuItem item, Object info, View view) {
					//showToast(item.getTitle().toString());
					onClick(item.getItemId(), item, view);
					//mMenuPopup.dismiss();
				}
			});
			icm.show();
		} catch(Exception e) {
			Logger.LogWarning("Couldn't show icon context menu.", e);
		}
		return icm;
	}
	
	@Override
	public boolean onSearchRequested() {
		if(USE_ACTION_BAR) return false;
		mEvHandler.startSearch(mLastPath, this);
		//showToast("Sorry, not working yet.");
		return super.onSearchRequested();
	}
	
	public void changeViewMode(int newView, boolean doSet) {
		int mViewMode = getSetting(mLastPath, "view", 0);
		if(mViewMode == newView) {
			Logger.LogWarning("changeViewMode called unnecessarily! " + newView + " = " + mViewMode);
			return;
		}
		//Logger.LogVerbose("Changing view mode to " + newView);
		int oldView = mViewMode;
		if(newView == VIEW_CAROUSEL && BEFORE_HONEYCOMB)
			newView = mViewMode == VIEW_LIST ? VIEW_GRID : VIEW_LIST;
		//setViewMode(newView);
		if(doSet)
			setSetting(mLastPath, "view", newView);
		if(!mSinglePane)
		{
			if(oldView == VIEW_CAROUSEL && mViewPagerEnabled)
			{
				setViewVisibility(false, false, R.id.content_frag);
				setViewVisibility(true, false, R.id.content_pager_frame);
				changePath(mLastPath, false);
			} else if(newView == VIEW_CAROUSEL && mViewPagerEnabled)
			{
				setViewVisibility(false, false, R.id.content_pager_frame);
				setViewVisibility(true, false, R.id.content_frag);
				changePath(mLastPath, false);
			}
			getDirContentFragment(true).onViewChanged(newView);
			if(!BEFORE_HONEYCOMB)
				invalidateOptionsMenu();
		} else if(newView == VIEW_CAROUSEL && !BEFORE_HONEYCOMB)
		{
			Logger.LogDebug("Switching to carousel!");
			if(mViewPagerEnabled)
			{
				setViewVisibility(false, false, R.id.content_pager_frame_stub, R.id.content_pager);
				setViewVisibility(true, false, R.id.content_frag);
			}
			fragmentManager.beginTransaction()
				.replace(R.id.content_frag, new CarouselFragment(mLastPath))
				.setBreadCrumbTitle(mLastPath.getAbsolutePath())
				.commit();
			updateTitle(mLastPath.getPath());
			invalidateOptionsMenu();
		} else if (oldView == VIEW_CAROUSEL && !BEFORE_HONEYCOMB) { // if we need to transition from carousel
			if(mViewPagerEnabled)
			{
				setViewVisibility(true, false, R.id.content_frag);
				setViewVisibility(false, false, R.id.content_pager_frame_stub, R.id.content_pager);
				changePath(mLastPath, false);
			} else {
				fragmentManager.beginTransaction()
					.replace(R.id.content_frag, ContentFragment.getInstance(mLastPath, mViewMode))
					.setBreadCrumbTitle(mLastPath.getAbsolutePath())
					//.addToBackStack(null)
					.commit();
				updateTitle(mLastPath.getPath());
			}
			
			invalidateOptionsMenu();
		} else {
			getDirContentFragment(true).onViewChanged(newView);
			if(!BEFORE_HONEYCOMB)
				invalidateOptionsMenu();
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
	
	public String getSetting(OpenPath file, String key, String defValue)
	{
		return getPreferences().getSetting("global", key + (file != null ? "_" + file.getPath() : ""), defValue);
	}
	public Boolean getSetting(OpenPath file, String key, Boolean defValue)
	{
		return getPreferences().getSetting("global", key + (file != null ? "_" + file.getPath() : ""), defValue);
	}
	public Integer getSetting(OpenPath file, String key, Integer defValue)
	{
		return getPreferences().getSetting("global", key + (file != null ? "_" + file.getPath() : ""), defValue);
	}
	public void setSetting(OpenPath file, String key, String value)
	{
		getPreferences().setSetting("global", key + (file != null ? "_" + file.getPath() : ""), value);
	}
	public void setSetting(OpenPath file, String key, Boolean value)
	{
		getPreferences().setSetting("global", key + (file != null ? "_" + file.getPath() : ""), value);
	}
	public void setSetting(OpenPath file, String key, Integer value)
	{
		getPreferences().setSetting("global", key + (file != null ? "_" + file.getPath() : ""), value);
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
				if(!start.equals(mLastPath.getPath()))
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
		}
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
				{
					if(mFileManager != null && mFileManager.getStack() != null && mFileManager.getStack().size() > 0)
						mFileManager.popStack();
					if(new OpenFile(entry.getBreadCrumbTitle().toString()).exists())
					{
						mLastPath = new OpenFile(entry.getBreadCrumbTitle().toString());
						Logger.LogDebug("last path set to " + mLastPath.getPath());
						changePath(mLastPath, false);
						updateTitle(mLastPath.getPath());
					}
				} else if(mFileManager != null && mFileManager.getStack() != null && mFileManager.getStack().size() > 0)
					goBack();
				else {
					Logger.LogWarning("No Breadcrumb Title");
					updateTitle("");
				}
			} else if (mSinglePane) {
				finish();
			}
			else {
				updateTitle("");
				//showToast("Press back again to exit.");
			}
		}
		if(isBack && mFileManager != null && mFileManager.getStack() != null)
		{
			while(mFileManager.getStack().size() > i)
				mFileManager.popStack();
			//goBack();
		}
		mLastBackIndex = i;
	}
	
	public boolean isGTV() { return getPackageManager().hasSystemFeature("com.google.android.tv"); }
	public void showToast(final String message)  {
		showToast(message, Toast.LENGTH_SHORT);
	}
	public void showToast(final int iStringResource) { showToast(getResources().getString(iStringResource)); }
	public void showToast(final String message, final int toastLength)  {
		Logger.LogInfo("Made toast: " + message);
		runOnUiThread(new Runnable() {
			public void run() {
				Toast.makeText(getBaseContext(), message, toastLength).show();
			}
		});
	}
	public void showToast(final int resId, final int length)  {
		showToast(getString(resId), length);
	}

	public void changePath(OpenPath path) { changePath(path, true, false); }
	public void changePath(OpenPath path, Boolean addToStack) { changePath(path, addToStack, false); }
	public void changePath(OpenPath path, Boolean addToStack, Boolean force)
	{
		toggleBookmarks(false);
		if(path == null) path = mLastPath;
		if(!force)
		if(!addToStack && path.getPath().equals("/")) return;
		//if(mLastPath.equalsIgnoreCase(path.getPath())) return;
		int newView = getSetting(path, "view", 0);
		boolean isNew = !mLastPath.equals(path);
		int oldView = getSetting(mLastPath, "view", 0);
		mLastPath = path;
		//mFileManager.setShowHiddenFiles(getSetting(path, "hide", false));
		//setViewMode(newView);
		Fragment content = ContentFragment.getInstance(path, newView);
		if(mViewPager != null && mViewPagerEnabled &&
				(BEFORE_HONEYCOMB || newView != VIEW_CAROUSEL))
		{
			//mViewPagerAdapter.add(content);
			//mViewPagerAdapter.setPath(path);
			//mViewPager.setAdapter(null);
			/*
			mViewPagerAdapter = new OpenPathPagerAdapter(getSupportFragmentManager())
										.setPath(path);
			if(getResources().getBoolean(R.bool.add_bookmarks_to_pager) && (findViewById(R.id.list_frag) == null || !findViewById(R.id.list_frag).isShown()))
				mViewPagerAdapter.setFirstFragment(mFavoritesFragment);
			else
				mViewPagerAdapter.setFirstFragment(null);
				*/
			//ArrayPagerAdapter newAdapter = new ArrayPagerAdapter(fragmentManager);
			// switch from /mnt/sdcard-ext/Pictures to /mnt/sdcard-ext/download
			setViewVisibility(true, false, R.id.content_pager_frame);
			setViewVisibility(false, false, R.id.content_frag);
			mViewPagerAdapter.removeOfType(ContentFragment.class);
			try {
				mViewPagerAdapter.add(0, content);
			} catch(IllegalStateException e)
			{
				Logger.LogWarning("Fragment already active?", e); // crash fix
			}
			if(!path.requiresThread())
			{
				OpenPath tmp = path.getParent();
				while(tmp != null)
				{
					mViewPagerAdapter.add(0, ContentFragment.getInstance(tmp, getSetting(tmp, "view", newView)));
					tmp = tmp.getParent();
				}
			}
			//mViewPagerAdapter = newAdapter;
			setViewPageAdapter(mViewPagerAdapter);
			int index = mViewPagerAdapter.getLastPositionOfType(ContentFragment.class);
			if(mViewPager.getCurrentItem() != index) // crash fix
				mViewPager.setCurrentItem(index, true);
			updatePagerTitle(index);
			if(addToStack)
			{
				try {
					fragmentManager
						.beginTransaction()
						.setBreadCrumbTitle(path.getPath())
						.addToBackStack("path")
						.commit();
				} catch(IllegalStateException e) {
					Logger.LogError("Couldn't add frag to stack", e);
				}
			}
		} else {
			setViewVisibility(false, false, R.id.content_pager_frame);
			setViewVisibility(true, false, R.id.content_frag);
			FragmentTransaction ft = fragmentManager.beginTransaction();
			//ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
			ft.setBreadCrumbTitle(path.getPath());
			updateTitle(path.getPath());
			if(addToStack)
			{
				int bsCount = fragmentManager.getBackStackEntryCount();
				if(bsCount > 0)
				{
					BackStackEntry entry = fragmentManager.getBackStackEntryAt(bsCount - 1);
					String last = entry.getBreadCrumbTitle() != null ? entry.getBreadCrumbTitle().toString() : "";
					Logger.LogVerbose("Changing " + last + " to " + path.getPath() + "? " + (last.equalsIgnoreCase(path.getPath()) ? "No" : "Yes"));
					if(last.equalsIgnoreCase(path.getPath()))
						return;
				}
				mFileManager.pushStack(path);
				ft.addToBackStack("path");
			} else Logger.LogVerbose("Covertly changing to " + path.getPath());
			if(newView == VIEW_CAROUSEL && !BEFORE_HONEYCOMB)
			{
				content = new CarouselFragment(path);
				ft.replace(R.id.content_frag, content);
				updateTitle(path.getPath());
			} else {
				if(newView == VIEW_CAROUSEL && BEFORE_HONEYCOMB)
					newView = VIEW_LIST;
				if(!BEFORE_HONEYCOMB && (content == null || content instanceof CarouselFragment))
				{
					content = ContentFragment.getInstance(path, getSetting(path, "view", 0));
					ft.replace(R.id.content_frag, content);
				} else if(content instanceof ContentFragment) {
					//((ContentFragment)content).changePath(path); // the main selection
					//ft.replace(R.id.content_frag, content);
					if(mContentFragment.isVisible())
						((ContentFragment)mContentFragment).changePath(path);
					else
						ft.replace(R.id.content_frag, content);
				}
			}
			if(mViewPagerAdapter != null)
			{
				mViewPagerAdapter.clear();
				mViewPagerAdapter.add(getDirContentFragment(false));
				setViewPageAdapter(mViewPagerAdapter);
			}
			try {
				ft.commit();
			} catch(IllegalStateException e) {
				Logger.LogWarning("Trying to commit after onSave", e);
			}
			updateTitle(path.getPath());
			changeViewMode(getSetting(path, "view", 0), false); // bug fix
		}
		if(!BEFORE_HONEYCOMB)
			invalidateOptionsMenu();
		/*if(content instanceof ContentFragment)
		((ContentFragment)content).setSettings(
			SortType.DATE_DESC,
			getSetting(path, "thumbs", true),
			getSetting(path, "hide", true)
			);*/
		if(path instanceof OpenFile)
			new PeekAtGrandKidsTask().execute((OpenFile)path);
		//ft.replace(R.id.content_frag, content);
		//ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
		Logger.LogDebug("Setting path to " + path.getPath());
		
	}
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
		changePath(path, true);
	}

	public static final FileManager getFileManager() {
		return mFileManager;
	}

	public static final EventHandler getEventHandler() {
		return mEvHandler;
	}
	
	public class SubmitStatsTask extends AsyncTask<String, Void, Void>
	{
		@Override
		protected Void doInBackground(String... params) {
			HttpURLConnection uc = null;
			try {
				uc = (HttpURLConnection)new URL("http://brandroid.org/stats.php").openConnection();
				uc.setReadTimeout(2000);
				PackageManager pm = OpenExplorer.this.getPackageManager();
				PackageInfo pi = pm.getPackageInfo(getPackageName(), 0);
				JSONObject device = getDeviceInfo();
				if(device == null) device = new JSONObject();
				String data = "{\"Version\":" + pi.versionCode + ",\"DeviceInfo\":" + device.toString() + ",\"Logs\":" + params[0] + ",\"App\":\"" + getPackageName() + "\"}"; 
				//uc.addRequestProperty("Accept-Encoding", "gzip, deflate");
				uc.addRequestProperty("App", getPackageName());
				uc.addRequestProperty("Version", ""+pi.versionCode);
				uc.setDoOutput(true);
				Logger.LogDebug("Sending logs...");
				GZIPOutputStream out = new GZIPOutputStream(uc.getOutputStream());
				out.write(data.getBytes());
				out.flush();
				out.close();
				uc.connect();
				if(uc.getResponseCode() == HttpURLConnection.HTTP_OK)
				{
					BufferedReader br = new BufferedReader(new InputStreamReader(uc.getInputStream()));
					String line = br.readLine();
					if(line == null)
					{
						Logger.LogWarning("No response on stat submit.");
					} else {
						Logger.LogDebug("Response: " + line);
						if(line.indexOf("Thanks") > -1)
						{
							while((line = br.readLine()) != null)
								Logger.LogDebug("Response: " + line);
							Logger.LogDebug("Sent logs successfully.");
							Logger.clearDb();
						} else {
							Logger.LogWarning("Logs not thanked");
						}
					}
				} else {
					Logger.LogWarning("Couldn't send logs (" + uc.getResponseCode() + ")");
				}
			} catch(Exception e)
			{
				Logger.LogError("Error sending logs.", e);
			}
			return null;
		}
		
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
					try {
						for(OpenPath kid : path.list())
						{
							ThumbnailCreator.generateThumb(kid, 36, 36);
							ThumbnailCreator.generateThumb(kid, 128, 128);
							done++;
						}
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else {
					if(!ThumbnailCreator.hasContext())
						ThumbnailCreator.setContext(getApplicationContext());
					ThumbnailCreator.generateThumb(path, 36, 36);
					ThumbnailCreator.generateThumb(path, 128, 128);
					done++;
				}
			}
			//Logger.LogDebug("cursor cache of " + done + " generated.");
			return null;
		}
		
	}


	public void showFileInfo(OpenPath path) {
		new AlertDialog.Builder(this)
			.setView(DialogHandler.createFileInfoDialog(getLayoutInflater(), path))
			.setTitle(path.getName())
			.setIcon(new BitmapDrawable(path.getThumbnail(ContentFragment.mListImageSize, ContentFragment.mListImageSize).get()))
			.create()
			.show();
		//DialogHandler dialogInfo = DialogHandler.newDialog(DialogHandler.DialogType.FILEINFO_DIALOG, this);
		//dialogInfo.setFilePath(path.getPath());
		//dialogInfo.show(fragmentManager, "info");
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
	
	public void onClick(View v) {
		super.onClick(v);
		onClick(v.getId(), null, v);
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
					// TODO Auto-generated catch block
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
				if(getClipboard().isMultiselect())
					((LayerDrawable)((ImageView)pb.findViewById(R.id.title_paste_icon)).getDrawable())
						.getDrawable(1).setAlpha(127);
			}
		}
		if(!BEFORE_HONEYCOMB)
			invalidateOptionsMenu();
		if(!BEFORE_HONEYCOMB && USE_ACTION_BAR && mActionMode != null)
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
		else if(!BEFORE_HONEYCOMB)
			invalidateOptionsMenu();
		getDirContentFragment(false).notifyDataSetChanged();
	}

	@Override
	public boolean onPageTitleLongClick(int position, View titleView) {
		try {
			OpenFile path = (OpenFile)((ContentFragment)mViewPagerAdapter.getItem(position)).getPath();
			OpenPath parent = path.getParent();
			if(parent == null) parent = new OpenPathArray(new OpenPath[]{path});
			OpenPath.Sorting = SortType.ALPHA;
			SortedSet<OpenPath> arr = new TreeSet<OpenPath>();
			for(OpenPath kid : parent.list())
				if(path.equals(kid) || kid.isDirectory())
					arr.add(kid);
			OpenPath[] siblings = new OpenPath[arr.size()];
			siblings = arr.toArray(new OpenPath[0]);
			ArrayList<OpenPath> siblingArray = new ArrayList<OpenPath>();
			siblingArray.addAll(arr);
			OpenPath foster = new OpenPathArray(siblings);
			Logger.LogVerbose("Siblings of " + path.getPath() + ": " + siblings.length);
			Context mContext = this;
			View anchor = findViewById(R.id.title_bar);
			int[] offset = new int[2];
			titleView.getLocationInWindow(offset);
			int offsetX = offset[0];
			if(!BEFORE_HONEYCOMB)
			{
				anchor = titleView;
				Logger.LogVerbose("Is this the width? " + offsetX);
			} else if(findViewById(R.id.content_pager_indicator) != null)
				anchor = findViewById(R.id.content_pager_indicator);
			if(anchor != null)
				offsetX -= anchor.getLeft();
			if(mSiblingList == null)
			{
				mSiblingPopup = new BetterPopupWindow(mContext, anchor);
				mSiblingList = new OpenPathList(foster, mContext);
				mSiblingList.setOnItemClickListener(new OnItemClickListener() {
					@Override
					public void onItemClick(AdapterView<?> arg0, View view, int pos, long id) {
						final OpenPath path = (OpenPath)((BaseAdapter)arg0.getAdapter()).getItem(pos);
						changePath(path, false);
						mSiblingPopup.dismiss();
					}
				});
				mSiblingPopup.setContentView(mSiblingList);
			}
			else {
				mSiblingPopup.setAnchor(anchor);
				//mSiblingPopup.setAnchor(titleView);
				mSiblingList.setPath(foster);
			}
			mSiblingPopup.setAnchorOffset(offsetX);
			mSiblingPopup.showLikePopDownMenu();
			return true;
		} catch (Exception e) {
			Logger.LogError("Couldn't show sibling dropdown", e);
		}
		return false;
	}

	@Override
	public void onPageIndicatorChange() {
		Logger.LogVerbose("onPageIndicatorChange");
		/*
		if(mViewPagerAdapter == null || mViewPagerAdapter.getCount() < 2)
		{
			setViewVisibility(false, true, R.id.content_pager_indicator);
			setViewVisibility(true, false, R.id.title_text, R.id.title_path);
			//setViewVisibility(true, false, R.id.title_underline);
		} else {
			setViewVisibility(true, true, R.id.content_pager_indicator);
			setViewVisibility(false, false, R.id.title_underline);
		}
		*/
	}

	@Override
	public void onWorkerThreadComplete(int type, ArrayList<String> results) {
		try {
			Thread.sleep(50);
			Logger.LogVerbose("Time to wake up!");
		} catch (InterruptedException e) {
			Logger.LogWarning("Woken up too early!");
		}
		ContentFragment frag = getDirContentFragment(true);
		if(frag != null)
			frag.onWorkerThreadComplete(type, results);
	}
}

