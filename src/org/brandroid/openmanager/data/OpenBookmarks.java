package org.brandroid.openmanager.data;

import java.util.ArrayList;
import java.util.Hashtable;

import org.apache.commons.net.ftp.FTPFile;
import org.brandroid.openmanager.R;
import org.brandroid.openmanager.activities.OpenExplorer;
import org.brandroid.openmanager.activities.SettingsActivity;
import org.brandroid.openmanager.fragments.BookmarkFragment;
import org.brandroid.openmanager.fragments.ContentFragment;
import org.brandroid.openmanager.fragments.DialogHandler;
import org.brandroid.openmanager.ftp.FTPManager;
import org.brandroid.openmanager.util.DFInfo;
import org.brandroid.openmanager.util.RootManager;
import org.brandroid.openmanager.util.ThumbnailCreator;
import org.brandroid.openmanager.util.OpenInterfaces.OnBookMarkChangeListener;
import org.brandroid.utils.Logger;
import org.brandroid.utils.Preferences;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Environment;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.text.method.SingleLineTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class OpenBookmarks implements OnBookMarkChangeListener, OnItemLongClickListener, OnItemClickListener
{
	private ArrayList<OpenPath> mBookmarksArray;
	private Context mContext;
	//private ImageView mLastIndicater = null;
	private BookmarkAdapter mBookmarkAdapter;
	private String mBookmarkString;
	private Boolean mHasExternal = false;
	private Boolean mShowTitles = true;
	private Long mAllDataSize = 0l;
	private SharedPreferences mPrefs;
	private OpenExplorer mExplorer;
	
	public OpenBookmarks(OpenExplorer explorer, ListView newList)
	{
		mContext = explorer;
		mBookmarksArray = new ArrayList<OpenPath>();
		mPrefs = new Preferences(mContext).getPreferences("bookmarks"); 
		if(mBookmarkString == null)
			mBookmarkString = mPrefs.getString("bookmarks", "");
		if(newList != null)
			setupListView(newList);
		mExplorer = explorer;
		if(mExplorer != null)
			scanBookmarks();
	}
	
	private OpenExplorer getExplorer() { return mExplorer; }
	
	public void scanBookmarks()
	{
		Logger.LogDebug("Scanning bookmarks...");
		OpenFile storage = new OpenFile(Environment.getExternalStorageDirectory());
		mBookmarksArray.clear();
		
		checkAndAdd(getExplorer().getVideoParent());
		checkAndAdd(getExplorer().getPhotoParent());
		checkAndAdd(getExplorer().getMusicParent());
		
		checkAndAdd(new OpenFile("/"));
		
		checkAndAdd(storage);
		
		checkAndAdd(storage.getChild("Download"));
		if(checkAndAdd(new OpenFile("/mnt/external_sd")))
			mHasExternal = true;
		if(checkAndAdd(new OpenFile("/mnt/sdcard-ext")))
			mHasExternal = true;
		if(checkAndAdd(new OpenFile("/Removable/MicroSD")))
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
			checkAndAdd(file);
		}
		if (mBookmarkString.length() > 0) {
			String[] l = mBookmarkString.split(";");
			
			for(String s : l)
				checkAndAdd(new OpenFile(s));
		}
		
		OpenServers servers = SettingsActivity.LoadDefaultServers(getExplorer());
		for(int i = 0; i < servers.size(); i++)
		{
			OpenServer server = servers.get(i);
			FTPManager man = new FTPManager(server.getHost(), server.getUser(), server.getPassword(), server.getPath());
			FTPFile file = new FTPFile();
			file.setName(server.getName());
			OpenFTP ftp = new OpenFTP(file, man);
			ftp.setServersIndex(i);
			checkAndAdd(ftp);
		}
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
		for(OpenPath p : mBookmarksArray)
			if(p.getPath().replaceAll("/", "").equals(path.getPath().replaceAll("/", "")))
				return true;
		return false;
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
			return mContext.getString(R.string.s_external);
		else if(path.indexOf("download") > -1)
			return mContext.getString(R.string.s_downloads);
		else if(path.indexOf("sdcard") > -1)
			return mContext.getString(mHasExternal ? R.string.s_internal : R.string.s_external);
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
	
	
	private boolean checkAndAdd(OpenPath path)
	{
		if(path == null) return false;
		if(OpenCursor.class.equals(path.getClass()))
			if(((OpenCursor)path).length() == 0)
				return false;
		try {
			if(getSetting("hide_" + path.getAbsolutePath(), false))
				return false;
		} catch(NullPointerException e) { }
		if(hasBookmark(path)) return false;
		if(OpenCursor.class.equals(path.getClass()) || OpenFTP.class.equals(path.getClass()) || path.exists())
		{
			mBookmarksArray.add(path);
			return true;
		} else return false;
	}
	
	public void hideTitles()
	{
		mShowTitles = false;
		mBookmarkAdapter.notifyDataSetChanged();
	}
	
	public void showTitles()
	{
		mShowTitles = true;
		mBookmarkAdapter.notifyDataSetChanged();
	}
	

	public void setupListView(ListView lv)
	{
		Logger.LogDebug("Setting up ListView in OpenBookmarks");
		lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		lv.setCacheColorHint(0x00000000);
		lv.setDrawSelectorOnTop(true);
		lv.setOnItemLongClickListener(this);
		lv.setOnItemClickListener(this);
		//lv.setBackgroundResource(R.drawable.listgradback);
		
		//Logger.LogDebug(mBookmarks.size() + " bookmarks");
		
		//registerForContextMenu(lv);
		
		if(mBookmarkAdapter == null)
			mBookmarkAdapter = new BookmarkAdapter(mContext, R.layout.bookmark_layout, mBookmarksArray);
		lv.setAdapter(mBookmarkAdapter);
		
		OpenExplorer.setOnBookMarkAddListener(this);
		
	}
	

	public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long arg3)
	{
		getExplorer().onChangeLocation(mBookmarksArray.get(pos));
	}
	public void onListItemClick(ListView list, View view, int pos, long id) {
		//super.onItemClick(list, view, pos, id);
		getExplorer().onChangeLocation(mBookmarksArray.get(pos));
	}
	

	public void onBookMarkAdd(OpenPath path) {
		mBookmarksArray.add(path);
		mBookmarkString = (mBookmarkString != null && mBookmarkString != "" ? mBookmarkString + ";" : "") + path.getPath();
		mBookmarkAdapter.notifyDataSetChanged();
	}

	public boolean onItemLongClick(AdapterView<?> list, View view, final int pos, long id)
	{
		//super.onItemLongClick(list, view, pos, id);
		final OpenPath mPath = mBookmarksArray.get(pos);
		final BookmarkHolder mHolder = (BookmarkHolder)view.getTag();
		if(OpenFTP.class.equals(mPath.getClass()))
			return ShowServerDialog((OpenFTP)mPath, mHolder);
		else return ShowStandardDialog(mPath, mHolder);
	}
	

	public boolean ShowServerDialog(final OpenFTP mPath, final BookmarkHolder mHolder)
	{
		final OpenServers servers = SettingsActivity.LoadDefaultServers(mContext);
		final int iServersIndex = mPath.getServersIndex();
		final OpenServer server = iServersIndex > -1 ? servers.get(iServersIndex) : new OpenServer().setName("New Server");
		LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		final View v = inflater.inflate(R.layout.server, null);
		final EditText mHost = (EditText)v.findViewById(R.id.text_server);
		final EditText mUser = (EditText)v.findViewById(R.id.text_user);
		final EditText mPassword = (EditText)v.findViewById(R.id.text_password);
		final EditText mTextPath = (EditText)v.findViewById(R.id.text_path);
		final EditText mTextName = (EditText)v.findViewById(R.id.text_name);
		final CheckBox mCheckPassword = (CheckBox)v.findViewById(R.id.check_password);
		mCheckPassword.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked)
				{
					mPassword.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
					mPassword.setTransformationMethod(new SingleLineTransformationMethod());
				} else {
					mPassword.setRawInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
					mPassword.setTransformationMethod(new PasswordTransformationMethod());
				}
			}
		});
		mHost.addTextChangedListener(new TextWatcher() {
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			public void afterTextChanged(Editable s) {
				if(server.getName().equals(server.getHost()) || server.getName().equals("New Server") || server.getName().equals(""))
					server.setName(s.toString());
				server.setHost(s.toString());
			}
		});
		mUser.addTextChangedListener(new TextWatcher() {
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			public void afterTextChanged(Editable s) {
				server.setUser(s.toString());
			}
		});
		mPassword.addTextChangedListener(new TextWatcher() {
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			public void afterTextChanged(Editable s) {
				server.setPassword(s.toString());
			}
		});
		mTextPath.addTextChangedListener(new TextWatcher() {
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			public void afterTextChanged(Editable s) {
				server.setPath(s.toString());
			}
		});
		mTextName.addTextChangedListener(new TextWatcher() {
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			public void afterTextChanged(Editable s) {
				server.setName(s.toString());
			}
		});
		if(iServersIndex > -1)
		{
			mHost.setText(server.getHost());
			mUser.setText(server.getUser());
			mPassword.setText(server.getPassword());
			mTextPath.setText(server.getPath());
			mTextName.setText(server.getName());
		}
		new AlertDialog.Builder(mContext)
			.setView(v)
			.setIcon(mHolder.getIconView().getDrawable())
			.setNegativeButton(mContext.getString(R.string.s_cancel), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
				}
			})
			.setNeutralButton(mContext.getString(R.string.s_remove), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					if(iServersIndex > -1)
						servers.remove(iServersIndex);
					getExplorer().refreshBookmarks();
				}
			})
			.setPositiveButton(mContext.getString(R.string.s_update), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					if(iServersIndex > -1)
						servers.set(iServersIndex, server);
					else
						servers.add(server);
					SettingsActivity.SaveToDefaultServers(servers, mContext);
					getExplorer().refreshBookmarks();
				}
			})
			.setTitle(server.getName())
			.create().show();
		return true;
	}
	public boolean ShowStandardDialog(final OpenPath mPath, final BookmarkHolder mHolder)
	{
		LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
		final View v = inflater.inflate(R.layout.input_dialog_layout, null);
		final EditText mText = (EditText)v.findViewById(R.id.dialog_input);
		//final EditText mTextTop = (EditText)v.findViewById(R.id.dialog_input_top);
		final String title = getPathTitle(mPath);

		((TextView)v.findViewById(R.id.dialog_message))
						.setText(mContext.getString(R.string.s_alert_bookmark_rename));
		mText.setText(title);
		
		if(mHolder.isEjectable())
		{	
			builder.setNeutralButton(mContext.getString(R.string.s_eject), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					tryEject(mPath.getPath(), mHolder);
				}
			});
		} else
			builder.setNeutralButton(mContext.getString(R.string.s_remove), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					setSetting("hide_" + mPath.getAbsolutePath(), true);
					if(mBookmarkString != null && (";"+mBookmarkString+";").indexOf(mPath.getPath()) > -1)
						mBookmarkString = (";" + mBookmarkString + ";").replace(";" + mPath.getPath() + ";", ";").replaceAll("^;|;$", "");
					if(Build.VERSION.SDK_INT >= 12)
						v.animate().alpha(0).setDuration(200).setListener(getDefaultAnimatorListener());
					else
						v.setVisibility(View.GONE);
				}
			});
		
		builder
			.setView(v)
			.setIcon(mHolder.getIconView().getDrawable())
			.setNegativeButton(mContext.getString(R.string.s_cancel), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}})
			.setPositiveButton(mContext.getString(R.string.s_update), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					setPathTitle(mPath, mText.getText().toString());					
					mBookmarkAdapter.notifyDataSetChanged();
				}
			})
			.setTitle(mContext.getString(R.string.s_title_bookmark_prefix) + " " + title)
			.create()
			.show();
		return true;
	}

	
	protected void tryEject(String sPath, BookmarkHolder mHolder) {
		final View viewf = mHolder.getView();
		if(RootManager.tryExecute("umount " + sPath))
		{
			getExplorer().showToast(mContext.getString(R.string.s_alert_remove_safe));
			viewf.animate().setDuration(500).y(viewf.getY() - viewf.getHeight()).alpha(0)
				.setListener(getDefaultAnimatorListener());
		} else
			getExplorer().showToast(mContext.getString(R.string.s_alert_remove_error));
	}

	public String getBookMarkNameString() {
		return mBookmarkString;
	}
	

	public AnimatorEndListen getDefaultAnimatorListener()
	{
		return new AnimatorEndListen(){
			public void onAnimationEnd(Animator animation) {
				scanBookmarks();
			}};
	}
	
	public void updateSizeIndicator(OpenPath mFile, View mParentView)
	{
		View mSizeView = (View)mParentView.findViewById(R.id.size_layout);
		ProgressBar bar = (ProgressBar)mParentView.findViewById(R.id.size_bar);
		TextView mSizeText = (TextView)mParentView.findViewById(R.id.size_text);
		if(bar == null) return;
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
				
				while(size > 100000)
				{
					size /= 10;
					free /= 10;
				}
				bar.setMax((int)size);
				bar.setProgress((int)(size - free));
				if(bar.getProgress() == 0)
					bar.setVisibility(View.GONE);
				//Logger.LogDebug(bar.getProgress() + "?");
				//else Logger.LogInfo(f.getPath() + " has " + bar.getProgress() + " / " + bar.getMax());
			} else mSizeView.setVisibility(View.GONE);
		} else if(mFile != null && OpenCursor.class.equals(mFile.getClass())) {
			bar.setVisibility(View.INVISIBLE);
			mSizeText.setText(DialogHandler.formatSize(((OpenCursor)mFile).getTotalSize()));
		} else mSizeView.setVisibility(View.GONE);
	}
	
	
	
	/*
	 * 
	 */
	private class BookmarkAdapter extends ArrayAdapter<OpenPath> {
		private BookmarkHolder mHolder;
		
		BookmarkAdapter(Context context, int layout, ArrayList<OpenPath> data) {
			super(context, layout, data);		
		}
		
		public View getView(int position, View view, ViewGroup parent) {			
			final OpenPath path = mBookmarksArray.get(position);
			final String sPath = path.getPath();
			if(view == null || view.getTag() == null || !view.getTag().getClass().equals(BookmarkHolder.class) || !((BookmarkHolder)view.getTag()).getPath().equalsIgnoreCase(sPath)) {
				LayoutInflater in = (LayoutInflater)
					mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = in.inflate(R.layout.bookmark_layout, parent, false);
				mHolder = new BookmarkHolder(path, getPathTitle(path), view);
				
				mHolder.setEjectClickListener(new OnClickListener() {
					public void onClick(View v) {
						mExplorer.runOnUiThread(new Runnable() {
							public void run() {
								tryEject(sPath, mHolder);
							}
						});
					}
				});
				
				updateSizeIndicator(path, view);
				
				view.setTag(mHolder);
				
			} else {
				mHolder = (BookmarkHolder)view.getTag();
				if(mHolder == null)
					Logger.LogWarning("preView Bookmark Holder is null");
				if(mHolder.getView() == null)
					Logger.LogWarning("preView Bookmark Holder View is null");
			}
			
			/*
			if(mLastIndicater == null) {
				if(position == 1) {
					//mHolder.mIndicate.setVisibility(View.VISIBLE);
					mLastIndicater = mHolder.mIndicate;
				}
			}
			*/
			
			if(!mShowTitles)
			{
				mHolder.hideTitle();
				//((RelativeLayout)mHolder.mMainText.getParent()).setGravity(Gravity.CENTER);
			} else {
				//((RelativeLayout)mHolder.mMainText.getParent()).setGravity(Gravity.LEFT);
				mHolder.showTitle();
			}
			
			ThumbnailCreator.setThumbnail(((ImageView)view.findViewById(R.id.content_icon)), path, 36, 36);
			
			mHolder.setTitle(getPathTitle(path));
			if(path.getClass().equals(OpenCursor.class))
				mHolder.setTitle(mHolder.getTitle() + " (" + ((OpenCursor)path).length() + ")", false);
			
			return view;
		}
	}

	public class AnimatorEndListen implements AnimatorListener
	{
		public void onAnimationCancel(Animator animation) { }
		public void onAnimationEnd(Animator animation) { }
		public void onAnimationRepeat(Animator animation) { }
		public void onAnimationStart(Animator animation) { }	
	}

	public ListAdapter getListAdapter() {
		return mBookmarkAdapter;
	}

}
