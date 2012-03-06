package org.brandroid.openmanager.data;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;

import org.apache.commons.net.ftp.FTPFile;
import org.brandroid.openmanager.R;
import org.brandroid.openmanager.activities.OpenExplorer;
import org.brandroid.openmanager.activities.SettingsActivity;
import org.brandroid.openmanager.fragments.DialogHandler;
import org.brandroid.openmanager.ftp.FTPManager;
import org.brandroid.openmanager.util.DFInfo;
import org.brandroid.openmanager.util.FileManager;
import org.brandroid.openmanager.util.RootManager;
import org.brandroid.openmanager.util.SimpleUserInfo;
import org.brandroid.openmanager.util.ThumbnailCreator;
import org.brandroid.openmanager.util.OpenInterfaces.OnBookMarkChangeListener;
import org.brandroid.utils.Logger;
import org.brandroid.utils.Preferences;
import android.animation.Animator;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager.BadTokenException;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;

public class OpenBookmarks implements OnBookMarkChangeListener,
								OnGroupClickListener,
								OnChildClickListener,
								OnItemLongClickListener
{
	private Map<Integer, ArrayList<OpenPath>> mBookmarksArray;
	//private ImageView mLastIndicater = null;
	private BookmarkAdapter mBookmarkAdapter;
	private String mBookmarkString;
	private Boolean mHasExternal = false, mHasInternal = false;
	private Boolean mShowTitles = true;
	private Long mAllDataSize = 0l;
	private SharedPreferences mPrefs;
	private final OpenExplorer mExplorer;
	public static final int BOOKMARK_DRIVE = 0;
	public static final int BOOKMARK_SMART_FOLDER = 1;
	public static final int BOOKMARK_FAVORITE = 2;
	public static final int BOOKMARK_SERVER = 3;
	public static final int BOOKMARK_OFFLINE = 4;
	
	public OpenBookmarks(OpenExplorer explorer, ExpandableListView newList)
	{
		mExplorer = explorer;
		mBookmarksArray = new Hashtable<Integer, ArrayList<OpenPath>>();
		//for(BookmarkType type : BookmarkType.values())
		//	mBookmarksArray.put(getTypeInteger(type), new ArrayList<OpenPath>());
		mPrefs = new Preferences(getExplorer()).getPreferences("bookmarks"); 
		if(mBookmarkString == null)
			mBookmarkString = mPrefs.getString("bookmarks", "");
		if(newList != null)
			setupListView(newList);
		if(mExplorer != null)
			scanBookmarks();
	}
	
	public enum BookmarkType
	{
		BOOKMARK_DRIVE,
		BOOKMARK_SMART_FOLDER,
		BOOKMARK_FAVORITE,
		BOOKMARK_SERVER,
		BOOKMARK_OFFLINE
	}
	
	private int getTypeInteger(BookmarkType type)
	{
		int ret = -1;
		for(BookmarkType item : BookmarkType.values())
		{
			ret++;
			if(type.equals(item))
				break;
		}
		return ret;
	}
	
	public int size()
	{
		return mBookmarksArray.size();
	}
	private OpenExplorer getExplorer() { return mExplorer; }
	
	public void scanBookmarks()
	{
		new Thread(new Runnable() {
			@Override
			public void run() {
				scanBookmarksInner();
			}
		}).run();
	}
	private void scanBookmarksInner()
	{
		Logger.LogDebug("Scanning bookmarks...");
		OpenFile storage = new OpenFile(Environment.getExternalStorageDirectory());
		//mBookmarksArray.clear();
		clearBookmarks();
		
		checkAndAdd(BookmarkType.BOOKMARK_SMART_FOLDER, OpenExplorer.getVideoParent());
		checkAndAdd(BookmarkType.BOOKMARK_SMART_FOLDER, OpenExplorer.getPhotoParent());
		checkAndAdd(BookmarkType.BOOKMARK_SMART_FOLDER, OpenExplorer.getMusicParent());
		
		checkAndAdd(BookmarkType.BOOKMARK_DRIVE, new OpenFile("/").setRoot());
		
		checkAndAdd(BookmarkType.BOOKMARK_DRIVE, storage.setRoot());
		
		checkAndAdd(BookmarkType.BOOKMARK_SMART_FOLDER, storage.getChild("Download"));
		if(checkAndAdd(BookmarkType.BOOKMARK_DRIVE, OpenFile.getInternalMemoryDrive()))
			mHasInternal = true;
		if(checkAndAdd(BookmarkType.BOOKMARK_DRIVE, OpenFile.getExternalMemoryDrive(false)))
			mHasExternal = true;
		Hashtable<String, DFInfo> df = DFInfo.LoadDF();
		for(String sItem : df.keySet())
		{
			if(sItem.toLowerCase().startsWith("/dev")) continue;
			if(sItem.toLowerCase().indexOf("/system") > -1) continue;
			if(sItem.toLowerCase().indexOf("vendor") > -1) continue;
			OpenFile file = new OpenFile(sItem);
			if(file.isHidden()) continue;
			if(file.getTotalSpace() > 0)
				mAllDataSize += file.getTotalSpace();
			//if(!file.getFile().canWrite()) continue;
			//if(sItem.toLowerCase().indexOf("asec") > -1) continue;
			checkAndAdd(BookmarkType.BOOKMARK_DRIVE, file.setRoot());
		}
		if (mBookmarkString.length() > 0) {
			String[] l = mBookmarkString.split(";");
			
			for(String s : l)
				checkAndAdd(BookmarkType.BOOKMARK_FAVORITE, new OpenFile(s).setRoot());
		}
		
		OpenServers servers = SettingsActivity.LoadDefaultServers(getExplorer());
		for(int i = 0; i < servers.size(); i++)
		{
			OpenServer server = servers.get(i);
			if(server.getType().equalsIgnoreCase("ftp"))
			{
				FTPManager man = new FTPManager(server.getHost(), server.getUser(), server.getPassword(), server.getPath());
				FTPFile file = new FTPFile();
				file.setName(server.getName());
				OpenFTP ftp = new OpenFTP(file, man);
				ftp.setServersIndex(i);
				checkAndAdd(BookmarkType.BOOKMARK_SERVER, ftp);
			} else if(server.getType().equalsIgnoreCase("sftp"))
			{
				SimpleUserInfo info = new SimpleUserInfo(getExplorer());
				info.setPassword(server.getPassword());
				OpenSFTP sftp = new OpenSFTP(
						server.getHost(), server.getUser(), server.getPath(),
						info);
				sftp.setName(server.getName());
				sftp.setServersIndex(i);
				if(server.getPort() > 0)
					sftp.setPort(server.getPort());
				checkAndAdd(BookmarkType.BOOKMARK_SERVER, sftp);
			}
		}
		addBookmark(BookmarkType.BOOKMARK_SERVER, new OpenCommand(getExplorer().getString(R.string.s_pref_server_add), OpenCommand.COMMAND_ADD_SERVER, android.R.drawable.ic_menu_add));
		if(mBookmarkAdapter != null)
			mBookmarkAdapter.notifyDataSetChanged();
	}
	
	public void saveBookmarks()
	{
		setSetting("bookmarks", mBookmarkString);
	}
	

	public String getSetting(String key, String defValue)
	{
		return mPrefs.getString(key, defValue);
	}
	public Boolean getSetting(String key, Boolean defValue)
	{
		return mPrefs.getBoolean(key, defValue);
	}
	public void setSetting(String key, String value)
	{
		mPrefs.edit().putString(key, value).commit();
	}
	public void setSetting(String key, Boolean value)
	{
		mPrefs.edit().putBoolean(key, value).commit();
	}
	

	private boolean hasBookmark(OpenPath path)
	{
		for(ArrayList<OpenPath> arr : mBookmarksArray.values())
			for(OpenPath p : arr)
				if(p.getPath().replaceAll("/", "").equals(path.getPath().replaceAll("/", "")))
					return true;
		return false;
	}
	
	private void addBookmark(BookmarkType type, OpenPath path)
	{
		int iType = getTypeInteger(type);
		ArrayList<OpenPath> paths = new ArrayList<OpenPath>();
		if(mBookmarksArray.containsKey(iType))
			paths = mBookmarksArray.get(iType);
		paths.add(path);
		mBookmarksArray.put(iType, paths);
		if(mBookmarkAdapter != null)
			mBookmarkAdapter.notifyDataSetChanged();
	}
	
	public void refresh()
	{
		if(mBookmarkAdapter != null)
			mBookmarkAdapter.notifyDataSetChanged();
	}
	
	private void clearBookmarks()
	{
		for(int i=0; i < BookmarkType.values().length; i++)
			mBookmarksArray.put(i, new ArrayList<OpenPath>());
	}

	public String getPathTitle(OpenPath path)
	{
		return getSetting("title_" + path.getAbsolutePath(), getPathTitleDefault(path));
	}
	
	public void setPathTitle(OpenPath path, String title)
	{
		setSetting("title_" + path.getAbsolutePath(), title);
	}
	public String getPathTitleDefault(OpenPath file)
	{
		String path = file.getPath().toLowerCase();
		if(path.equals("/"))
			return "/";
		else if(path.indexOf("ext") > -1)
			return getExplorer().getString(R.string.s_external);
		else if(path.indexOf("download") > -1)
			return getExplorer().getString(R.string.s_downloads);
		else if(path.indexOf("sdcard") > -1)
			return getExplorer().getString(mHasExternal ? R.string.s_internal : R.string.s_external);
		else if(path.indexOf("usb") > -1 || path.indexOf("removeable") > -1)
		{
			try {
				return OpenExplorer.getVolumeName(file.getPath());
			} catch(Exception e) {
				Logger.LogWarning("Unable to get actual volume name.", e);
			}
		}
		
		return file.getName();
	}
	
	
	private boolean checkAndAdd(BookmarkType type, OpenPath path)
	{
		if(path == null) return false;
		if(OpenCursor.class.equals(path.getClass()))
			if(((OpenCursor)path).length() == 0)
				return false;
		try {
			if(path.getPath().equals("/"))
			{
				if(!getExplorer().getPreferences().getSetting("global", "pref_show_root", !getSetting("hide_" + path.getAbsolutePath(), false)))
					return false;
			} else if(OpenFile.getInternalMemoryDrive().equals(path))
			{
				if(!getExplorer().getPreferences().getSetting("global", "pref_show_internal", !getSetting("hide_" + path.getAbsolutePath(), false)))
					return false;
			} else if(OpenFile.getExternalMemoryDrive(true).equals(path)) {
				if(!getExplorer().getPreferences().getSetting("global", "pref_show_internal", !getSetting("hide_" + path.getAbsolutePath(), false)))
					return false;
			} else if(getSetting("hide_" + path.getAbsolutePath(), false))
				return false;
		} catch(NullPointerException e) { }
		if(hasBookmark(path)) return false;
		if(OpenCursor.class.equals(path.getClass()) ||
				OpenFTP.class.equals(path.getClass()) ||
				path instanceof OpenNetworkPath ||
				path.exists())
		{
			addBookmark(type, path);
			return true;
		}
		return false;
	}
	
	public void hideTitles()
	{
		mShowTitles = false;
		refresh();
	}
	
	public void showTitles()
	{
		mShowTitles = true;
		refresh();
	}
	

	public void setupListView(ExpandableListView lv)
	{
		Logger.LogDebug("Setting up ListView in OpenBookmarks");
		lv.setDrawSelectorOnTop(true);
		lv.setSelector(R.drawable.selector_blue);
		lv.setOnChildClickListener(this);
		lv.setOnGroupClickListener(this);
		lv.setGroupIndicator(null);
		lv.setOnItemLongClickListener(this);
		lv.setLongClickable(true);
		//lv.setOnItemClickListener(this);
		//lv.setBackgroundResource(R.drawable.listgradback);
		
		//Logger.LogDebug(mBookmarks.size() + " bookmarks");
		
		//registerForContextMenu(lv);
		
		if(mBookmarkAdapter == null)
			mBookmarkAdapter = new BookmarkAdapter();
			//mBookmarkAdapter = new BookmarkAdapter(getExplorer(), R.layout.bookmark_layout, mBookmarksArray);
		lv.setAdapter(mBookmarkAdapter);
		
		OpenExplorer.setOnBookMarkAddListener(this);
		
	}
	
	private void handleCommand(int command)
	{
		switch(command)
		{
			case OpenCommand.COMMAND_ADD_SERVER:
				ShowServerDialog(new OpenFTP(null, null), null, true);
				break;
		}
	}

	public boolean onItemLongClick(AdapterView<?> list, View v, int pos, long id) {
		Logger.LogDebug("Long Click pos: " + pos + " (" + id + "," + v.getTag() + "!)");
		return onLongClick(v);
	}
	
	public boolean onChildClick(ExpandableListView parent, View v,
			int groupPosition, int childPosition, long id) {
		OpenPath path = mBookmarkAdapter.getChild(groupPosition, childPosition);
		if(path != null)
		{
			if(path instanceof OpenCommand)
				handleCommand(((OpenCommand)path).getCommand());
			else
				getExplorer().onChangeLocation(path);
			return true;
		}
		return false;
	}

	public boolean onGroupClick(ExpandableListView parent, View v,
			int groupPosition, long id) {
		//if(mBookmarksArray.get(groupPosition).size() > 0)
		//	return false;
		//else return true; // don't allow expand of empty groups
		return false;
	}
	
	
	/*
	public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long arg3)
	{
		getExplorer().onChangeLocation(mBookmarksArray.get(pos));
	}
	public void onListItemClick(ListView list, View view, int pos, long id) {
		//super.onItemClick(list, view, pos, id);
		getExplorer().onChangeLocation(mBookmarksArray.get(pos));
	}
	*/

	public void onBookMarkAdd(OpenPath path) {
		int type = getTypeInteger(BookmarkType.BOOKMARK_FAVORITE);
		if(mBookmarksArray == null)
			mBookmarksArray = new Hashtable<Integer, ArrayList<OpenPath>>();
		if(mBookmarksArray.get(type) == null)
			mBookmarksArray.put(type, new ArrayList<OpenPath>());
		mBookmarksArray.get(type).add(path);
		mBookmarkString = (mBookmarkString != null && mBookmarkString != "" ? mBookmarkString + ";" : "") + path.getPath();
		mBookmarkAdapter.notifyDataSetChanged();
	}

	public boolean ShowServerDialog(final OpenFTP mPath, final BookmarkHolder mHolder, final boolean allowShowPass)
	{
		return ShowServerDialog(mPath.getServersIndex(), mHolder, allowShowPass);
	}
	public boolean ShowServerDialog(final OpenNetworkPath mPath, final BookmarkHolder mHolder, final boolean allowShowPass)
	{
		return ShowServerDialog(mPath.getServersIndex(), mHolder, allowShowPass);
	}
	public boolean ShowServerDialog(final int iServersIndex, final BookmarkHolder mHolder, final boolean allowShowPass)
	{
		final OpenServers servers = SettingsActivity.LoadDefaultServers(getExplorer());
		final OpenServer server = iServersIndex > -1 ? servers.get(iServersIndex) : new OpenServer().setName("New Server");
		LayoutInflater inflater = (LayoutInflater)getExplorer().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		final View v = inflater.inflate(R.layout.server, null);
		final EditText mHost = (EditText)v.findViewById(R.id.text_server);
		final EditText mUser = (EditText)v.findViewById(R.id.text_user);
		final EditText mPassword = (EditText)v.findViewById(R.id.text_password);
		final EditText mTextPath = (EditText)v.findViewById(R.id.text_path);
		final EditText mTextName = (EditText)v.findViewById(R.id.text_name);
		final CheckBox mCheckPassword = (CheckBox)v.findViewById(R.id.check_password);
		final Spinner mTypeSpinner = (Spinner)v.findViewById(R.id.server_type);
		final EditText mTextPort = (EditText)v.findViewById(R.id.text_port);
		if(!allowShowPass)
			mCheckPassword.setVisibility(View.GONE);
		OpenServer.setupServerDialog(server, mHost, mUser, mPassword, mTextPath, mTextName, mCheckPassword, mTypeSpinner, mTextPort);
		int addStrId = R.string.s_update;
		if(iServersIndex > -1)
		{
			mHost.setText(server.getHost());
			mUser.setText(server.getUser());
			mPassword.setText(server.getPassword());
			mTextPath.setText(server.getPath());
			mTextName.setText(server.getName());
			String[] types = mTypeSpinner.getResources().getStringArray(R.array.server_types_values);
			int pos = 0;
			for(int i=0; i<types.length; i++)
				if(types[i].equals(server.getType()))
				{
					pos = i;
					break;
				}
			mTypeSpinner.setSelection(pos);
		} else addStrId = R.string.s_add;
		final AlertDialog dialog = new AlertDialog.Builder(getExplorer())
			.setView(v)
			.setIcon(mHolder != null && mHolder.getIcon() != null ? mHolder.getIcon() : getExplorer().getResources().getDrawable(R.drawable.sm_ftp))
			.setNegativeButton(getExplorer().getString(R.string.s_cancel), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			})
			.setNeutralButton(getExplorer().getString(R.string.s_remove), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					if(iServersIndex > -1)
						servers.remove(iServersIndex);
					dialog.dismiss();
					getExplorer().refreshBookmarks();
				}
			})
			.setPositiveButton(getExplorer().getString(addStrId), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					if(iServersIndex > -1)
						servers.set(iServersIndex, server);
					else
						servers.add(server);
					SettingsActivity.SaveToDefaultServers(servers, getExplorer());
					getExplorer().refreshBookmarks();
					dialog.dismiss();
				}
			})
			.setTitle(server.getName())
			.create();
		try {
			dialog.show();
		} catch(BadTokenException e) {
			Logger.LogError("Couldn't show dialog.", e);
			return false;
		}
		return true;
	}
	
	public boolean ShowStandardDialog(final OpenPath mPath, final BookmarkHolder mHolder)
	{
		LayoutInflater inflater = (LayoutInflater)getExplorer().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		AlertDialog.Builder builder = new AlertDialog.Builder(getExplorer());
		final View v = inflater.inflate(R.layout.input_dialog_layout, null);
		final EditText mText = (EditText)v.findViewById(R.id.dialog_input);
		//final EditText mTextTop = (EditText)v.findViewById(R.id.dialog_input_top);
		final String title = getPathTitle(mPath);

		((TextView)v.findViewById(R.id.dialog_message))
						.setText(getExplorer().getString(R.string.s_alert_bookmark_rename));
		mText.setText(title);
		
		if(mHolder != null && mHolder.isEjectable())
		{	
			builder.setNeutralButton(getExplorer().getString(R.string.s_eject), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					tryEject(mPath.getPath(), mHolder);
				}
			});
		} else
			builder.setNeutralButton(getExplorer().getString(R.string.s_remove), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					if(mPath.getPath().equals("/"))
						getExplorer().getPreferences().setSetting("global", "pref_show_root", false);
					else if(mPath.equals(OpenFile.getInternalMemoryDrive()))
						getExplorer().getPreferences().setSetting("global", "pref_show_internal", false);
					else if(mPath.equals(OpenFile.getExternalMemoryDrive(true)))
						getExplorer().getPreferences().setSetting("global", "pref_show_external", false);
					else
						setSetting("hide_" + mPath.getAbsolutePath(), true);
					if(mBookmarkString != null && (";"+mBookmarkString+";").indexOf(mPath.getPath()) > -1)
						mBookmarkString = (";" + mBookmarkString + ";").replace(";" + mPath.getPath() + ";", ";").replaceAll("^;|;$", "");
					if(Build.VERSION.SDK_INT >= 12)
						v.animate().alpha(0).setDuration(200).setListener(new org.brandroid.openmanager.adapters.AnimatorEndListener(){
							public void onAnimationEnd(Animator animation) {
								scanBookmarks();
							}});
					else
						v.setVisibility(View.GONE);
				}
			});
		
		builder
			.setView(v)
			.setIcon(mHolder != null ? mHolder.getIcon() : null)
			.setNegativeButton(getExplorer().getString(R.string.s_cancel), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}})
			.setPositiveButton(getExplorer().getString(R.string.s_update), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					setPathTitle(mPath, mText.getText().toString());					
					mBookmarkAdapter.notifyDataSetChanged();
				}
			})
			.setTitle(getExplorer().getString(R.string.s_title_bookmark_prefix) + " " + title)
			.create();
		try {
			builder.show();
		} catch(BadTokenException e) {
			Logger.LogWarning("Couldn't show AlertDialog. Bad token?", e);
		}
		return true;
	}

	
	protected void tryEject(String sPath, BookmarkHolder mHolder) {
		final View viewf = mHolder.getView();
		if(RootManager.tryExecute("umount " + sPath))
		{
			getExplorer().showToast(getExplorer().getString(R.string.s_alert_remove_safe));
			viewf.animate().setDuration(500).y(viewf.getY() - viewf.getHeight()).alpha(0)
				.setListener(new org.brandroid.openmanager.adapters.AnimatorEndListener(){
					public void onAnimationEnd(Animator animation) {
						scanBookmarks();
					}});
		} else
			getExplorer().showToast(getExplorer().getString(R.string.s_alert_remove_error));
	}

	public String getBookMarkNameString() {
		return mBookmarkString;
	}

	public void updateSizeIndicator(OpenPath mFile, View mParentView)
	{
		View mSizeView = (View)mParentView.findViewById(R.id.size_layout);
		View size_bar = mParentView.findViewById(R.id.size_bar);
		TextView mSizeText = (TextView)mParentView.findViewById(R.id.size_text);
		if(size_bar == null) return;
		if(mFile != null && mFile.getClass().equals(OpenFile.class) && mFile.getPath().indexOf("usic") == -1 && mFile.getPath().indexOf("ownload") ==-1)
		{
			OpenFile f = (OpenFile)mFile;
			long size = f.getTotalSpace();
			long free = f.getFreeSpace();
			
			if(size > 0 && free < size)
			{
				String sFree = DialogHandler.formatSize(free);
				String sTotal = DialogHandler.formatSize(size);
				//if(sFree.endsWith(sTotal.substring(sTotal.lastIndexOf(" ") + 1)))
				//	sFree = DFInfo.getFriendlySize(free, false);
				if(sFree.indexOf(" ") > -1 && sFree.endsWith(sTotal.substring(sFree.lastIndexOf(" "))))
					sFree = sFree.substring(0, sFree.lastIndexOf(" "));
				mSizeText.setText(sFree + "/" + sTotal);
				mSizeText.setVisibility(View.VISIBLE);
				
				while(size > 100000)
				{
					size /= 10;
					free /= 10;
				}
				if(size_bar instanceof ProgressBar)
				{
					ProgressBar bar = (ProgressBar)size_bar;
					bar.setMax((int)size);
					bar.setProgress((int)(size - free));
					if(bar.getProgress() == 0)
						bar.setVisibility(View.GONE);
				} else {
					long taken = Math.min(0, size - free);
					float percent = (float)taken / (float)size;
					//mParentView.measure(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
					int size_width = 250; //mParentView.getMeasuredWidth();
					Logger.LogVerbose("Parent Width: " + size_width);
					size_width = Math.min(0, (int) (percent * size_width));
					size_bar.getBackground().setBounds(0,0,size_width,0);
				}
				size_bar.setVisibility(View.VISIBLE);
				/*
				
				
				*/
				//Logger.LogDebug(bar.getProgress() + "?");
				//else Logger.LogInfo(f.getPath() + " has " + bar.getProgress() + " / " + bar.getMax());
			} else if(size_bar.getTag() == null) size_bar.setVisibility(View.GONE);
		} else if(mFile != null && OpenCursor.class.equals(mFile.getClass())) {
			//bar.setVisibility(View.INVISIBLE);
			if(size_bar.getTag() == null) size_bar.setVisibility(View.GONE);
			mSizeText.setText(DialogHandler.formatSize(((OpenCursor)mFile).getTotalSize()));
		} else mSizeView.setVisibility(View.GONE);
	}
	
	
	private class BookmarkAdapter extends BaseExpandableListAdapter
	{
		public OpenPath getChild(int group, int pos) {
			return mBookmarksArray.get(group).get(pos);
		}

		public long getChildId(int group, int pos) {
			return pos;
		}

		public View getChildView(int group, int pos,
				boolean isLastChild, View convertView, ViewGroup parent) {
			View ret = getExplorer().getLayoutInflater().inflate(R.layout.bookmark_layout, null); //convertView;;
			OpenPath path = getChild(group, pos);
			BookmarkHolder mHolder = null;
			mHolder = new BookmarkHolder(path, getPathTitle(path), ret, 0);
			ret.setTag(mHolder);
		
			TextView mCountText = (TextView)ret.findViewById(R.id.content_count);
			if(mCountText != null)
			{
				if(path instanceof OpenCursor)
				{
					((OpenCursor)path).setContentCountTextView(mCountText);
				} else mCountText.setVisibility(View.GONE);
			}
				
			
			if(group == 0)
				updateSizeIndicator(path, ret);
			else 
				ret.findViewById(R.id.size_layout).setVisibility(View.GONE);
			
			ImageView mIcon = (ImageView)ret.findViewById(R.id.bookmark_icon);
			
			((TextView)ret.findViewById(R.id.content_text)).setText(getPathTitle(getChild(group, pos)));
			ThumbnailCreator.setThumbnail(mIcon, getChild(group, pos), 36, 36);
			
            return ret;
		}

		public int getChildrenCount(int group) {
			if(mBookmarksArray.containsKey(group))
				return mBookmarksArray.get(group).size();
			else return 0;
		}

		public ArrayList<OpenPath> getGroup(int group) {
			return mBookmarksArray.get(group);
		}

		public int getGroupCount() {
			return mBookmarksArray.size();
		}

		public long getGroupId(int group) {
			return group;
		}

		public View getGroupView(int group, boolean isExpanded,
				final View convertView, ViewGroup parent) {
			View ret = convertView;
			if(ret == null)
			{
				ret = getExplorer().getLayoutInflater().inflate(android.R.layout.preference_category, null);
						//R.layout.bookmark_group, null);
			}
			Button button1 = (Button)ret.findViewById(android.R.id.button1);
			if((group == BOOKMARK_FAVORITE || group == BOOKMARK_SERVER || group == BOOKMARK_SMART_FOLDER)
					&& button1 != null)
			{
				button1.setVisibility(View.VISIBLE);
			} else if(button1 != null) {
				button1.setVisibility(View.GONE);
			}
			TextView mText = (TextView)ret.findViewById(android.R.id.title);
			if(isExpanded)
				mText.setTypeface(Typeface.DEFAULT_BOLD);
			else
				mText.setTypeface(Typeface.DEFAULT);
			mText.setBackgroundDrawable(null);
			
			//ret.setBackgroundColor(android.R.color.background_dark);
			//mText.setTextColor(android.R.color.secondary_text_light);
			
			String[] groups = getExplorer().getResources().getStringArray(R.array.bookmark_groups);
			if(mText != null)
				mText.setText(groups[group] + (getChildrenCount(group) > 0 ? " (" + (getChildrenCount(group)  - (group == BOOKMARK_SERVER ? 1 : 0)) + ")" : ""));
			return ret;
		}

		public boolean hasStableIds() {
			return false;
		}

		public boolean isChildSelectable(int group, int pos) {
			return true;
		}

		
	}

	public BookmarkAdapter getListAdapter() {
		return mBookmarkAdapter;
	}

	public boolean onLongClick(View v) {
		if(v.getTag() == null) {
			//((ExpandableListAdapter)list.getAdapter()).get
			Logger.LogWarning("No tag set on long click in OpenBookmarks.");
			return false;
		}
		Logger.LogInfo("Long click detected in OpenBookmarks");
		return onLongClick((BookmarkHolder)v.getTag());
	}
	public boolean onLongClick(BookmarkHolder h)
	{
		OpenPath path = h.getOpenPath();
		if(path instanceof OpenCommand)
			handleCommand(((OpenCommand)path).getCommand());
		else if(path instanceof OpenFTP)
			ShowServerDialog((OpenFTP)path, h, false);
		else if(path instanceof OpenNetworkPath)
			ShowServerDialog((OpenNetworkPath)path, h, false);
		else
			ShowStandardDialog(path, h);
		return true;
	}

}
