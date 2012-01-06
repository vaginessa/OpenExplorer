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
import org.brandroid.openmanager.R;
import org.brandroid.openmanager.activities.OpenExplorer;
import org.brandroid.openmanager.activities.SettingsActivity;
import org.brandroid.openmanager.data.BookmarkHolder;
import org.brandroid.openmanager.data.OpenCursor;
import org.brandroid.openmanager.data.OpenFTP;
import org.brandroid.openmanager.data.OpenFile;
import org.brandroid.openmanager.data.OpenPath;
import org.brandroid.openmanager.data.OpenServer;
import org.brandroid.openmanager.data.OpenServers;
import org.brandroid.openmanager.fragments.ContentFragment.OnBookMarkAddListener;
import org.brandroid.openmanager.ftp.FTPManager;
import org.brandroid.openmanager.util.DFInfo;
import org.brandroid.openmanager.util.RootManager;
import org.brandroid.openmanager.util.ThumbnailCreator;
import org.brandroid.utils.Logger;
import org.brandroid.utils.Preferences;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Bundle;
import android.os.Environment;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v4.app.ListFragment;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.text.method.SingleLineTransformationMethod;
import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.app.AlertDialog;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.ArrayAdapter;
import java.util.ArrayList;
import java.util.Hashtable;
import java.io.File;

public class BookmarkFragment extends OpenListFragment implements OnBookMarkAddListener,
															 OnItemLongClickListener{
	private ArrayList<OpenPath> mBookmarks;
	private Context mContext;
	//private ImageView mLastIndicater = null;
	private BookmarkAdapter mBookmarkAdapter;
	private String mBookmarkString;
	private Boolean mHasExternal = false;
	private Boolean mShowTitles = true;
	private Long mAllDataSize = 0l;
	private ListView mListViewOverride;
	
	public class AnimatorEndListen implements AnimatorListener
	{
		public void onAnimationCancel(Animator animation) { }
		public void onAnimationEnd(Animator animation) { }
		public void onAnimationRepeat(Animator animation) { }
		public void onAnimationStart(Animator animation) { }	
	}
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = getActivity();
		mBookmarks = new ArrayList<OpenPath>();
		mBookmarkString = getSetting(null, "bookmarks", "");
		scanBookmarks();
	}
	
	public BookmarkFragment() {
		super();
	}
	public BookmarkFragment(ListView newList)
	{
		super();
		mListViewOverride = newList;
		mContext = newList.getContext();
		mBookmarks = new ArrayList<OpenPath>();
		if(mBookmarkString == null)
		mBookmarkString = "";
		if(newList != null)
			setupListView(newList);
	}
	
	@Override
	public void onPause() {
		super.onPause();
		setSetting(null, "bookmarks", mBookmarkString);
	}
	
	public void scanBookmarks()
	{
		Logger.LogDebug("Scanning bookmarks...");
		OpenFile storage = new OpenFile(Environment.getExternalStorageDirectory());
		mBookmarks.clear();
		
		checkAndAdd(new OpenFile("/"));
		checkAndAdd(storage);
		
		checkAndAdd(getExplorer().getVideoParent());
		checkAndAdd(getExplorer().getPhotoParent());
		checkAndAdd(getExplorer().getMusicParent());
		
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
				checkAndAdd(getOpenBookmark(s));
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
	
	private OpenPath getOpenBookmark(String s) {
		if(new File(s).exists())
			return new OpenFile(s);
		JSONObject json = new Preferences(getActivity()).getSettings(s);
		final String type = json.optString("type");
		final String path = json.optString("path");
		if(type.equalsIgnoreCase("ftp"))
		{
			try {
				return new OpenFTP(path, null,
						new FTPManager(
								json.getString("host"),
								json.getString("user"),
								json.getString("password"),
								path));
			} catch (JSONException e) {
				Logger.LogError("Couldn't add FTP bookmark - " + path, e);
			}
		}
		return null;
	}

	private boolean hasBookmark(OpenPath path)
	{
		for(OpenPath p : mBookmarks)
			if(p.getPath().replaceAll("/", "").equals(path.getPath().replaceAll("/", "")))
				return true;
		return false;
	}
	
	public String getSetting(OpenPath file, String key, String defValue)
	{
		return getExplorer().getPreferences().getSetting("bookmarks", key + (file != null ? "_" + file.getPath() : ""), defValue);
	}
	public Boolean getSetting(OpenPath file, String key, Boolean defValue)
	{
		return getExplorer().getPreferences().getSetting("bookmarks", key + (file != null ? "_" + file.getPath() : ""), defValue);
	}
	public void setSetting(OpenPath file, String key, String value)
	{
		getExplorer().getPreferences().setSetting("bookmarks", key + (file != null ? "_" + file.getPath() : ""), value);
	}
	public void setSetting(OpenPath file, String key, Boolean value)
	{
		getExplorer().getPreferences().setSetting("bookmarks", key + (file != null ? "_" + file.getPath() : ""), value);
	}

	public String getPathTitle(OpenPath path)
	{
		return getSetting(path, "title", getPathTitleDefault(path));
	}
	
	public void setPathTitle(OpenPath path, String title)
	{
		setSetting(path, "title", title);
	}
	public String getPathTitleDefault(OpenPath file)
	{
		String path = file.getPath().toLowerCase();
		if(path.equals("/"))
			return "/";
		else if(path.indexOf("ext") > -1)
			return getString(R.string.s_external);
		else if(path.indexOf("download") > -1)
			return getString(R.string.s_downloads);
		else if(path.indexOf("sdcard") > -1)
			return getString(mHasExternal ? R.string.s_internal : R.string.s_external);
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
			if(getSetting(path, "hide", false))
				return false;
		} catch(NullPointerException e) { }
		if(hasBookmark(path)) return false;
		if(OpenCursor.class.equals(path.getClass()) || OpenFTP.class.equals(path.getClass()) || checkDir(path.getPath()))
		{
			mBookmarks.add(path);
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
	
	private static boolean checkDir(String sPath)
	{
		File fTest = new File(sPath);
		if(fTest.exists())
			return true;
		else return false;
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		//Logger.LogDebug("Bookmark View Created");
	}
	
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		//Logger.LogDebug("Bookmark Fragment Created");
		
		ListView lv = getListView();
		setupListView(lv);
	}
	private void setupListView(ListView lv)
	{
		lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		lv.setCacheColorHint(0x00000000);
		lv.setDrawSelectorOnTop(true);
		lv.setOnItemLongClickListener(this);
		//lv.setBackgroundResource(R.drawable.listgradback);
		
		//Logger.LogDebug(mBookmarks.size() + " bookmarks");
		
		registerForContextMenu(lv);
		
		if(mBookmarkAdapter == null)
			mBookmarkAdapter = new BookmarkAdapter(mContext, R.layout.bookmark_layout, mBookmarks);
		lv.setAdapter(mBookmarkAdapter);
		
		ContentFragment.setOnBookMarkAddListener(this);
		
	}
	
	public ListAdapter getListAdapter() { return mBookmarkAdapter; }
	
	
	public void onListItemClick(ListView list, View view, int pos, long id) {
		super.onItemClick(list, view, pos, id);
		getExplorer().onChangeLocation(mBookmarks.get(pos));
	}
	
	public AnimatorEndListen getDefaultAnimatorListener()
	{
		return new AnimatorEndListen(){
			public void onAnimationEnd(Animator animation) {
				getExplorer().refreshBookmarks();
			}};
	}
	
	public boolean onItemLongClick(AdapterView<?> list, View view, final int pos, long id)
	{
		super.onItemLongClick(list, view, pos, id);
		final OpenPath mPath = mBookmarks.get(pos);
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
			.setNegativeButton(getResources().getString(R.string.s_cancel), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
				}
			})
			.setNeutralButton(getResources().getString(R.string.s_remove), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					if(iServersIndex > -1)
						servers.remove(iServersIndex);
					getExplorer().refreshBookmarks();
				}
			})
			.setPositiveButton(getResources().getString(R.string.s_update), new DialogInterface.OnClickListener() {
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
						.setText(getResources().getString(R.string.s_alert_bookmark_rename));
		mText.setText(title);
		
		if(mHolder.isEjectable())
		{	
			builder.setNeutralButton(getResources().getString(R.string.s_eject), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					tryEject(mPath.getPath(), mHolder);
				}
			});
		} else
			builder.setNeutralButton(getResources().getString(R.string.s_remove), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					setSetting(mPath, "hide", true);
					if(mBookmarkString != null && (";"+mBookmarkString+";").indexOf(mPath.getPath()) > -1)
						mBookmarkString = (";" + mBookmarkString + ";").replace(";" + mPath.getPath() + ";", ";").replaceAll("^;|;$", "");
					v.animate().alpha(0).setDuration(200).setListener(getDefaultAnimatorListener());
				}
			});
		
		builder
			.setView(v)
			.setIcon(mHolder.getIconView().getDrawable())
			.setNegativeButton(getResources().getString(R.string.s_cancel), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}})
			.setPositiveButton(getResources().getString(R.string.s_update), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					setPathTitle(mPath, mText.getText().toString());					
					mBookmarkAdapter.notifyDataSetChanged();
				}
			})
			.setTitle(getResources().getString(R.string.s_title_bookmark_prefix) + " " + title)
			.create()
			.show();
		return true;
	}

	
	protected void tryEject(String sPath, BookmarkHolder mHolder) {
		final View viewf = mHolder.getView();
		if(RootManager.tryExecute("umount " + sPath))
		{
			getExplorer().showToast(getString(R.string.s_alert_remove_safe));
			viewf.animate().setDuration(500).y(viewf.getY() - viewf.getHeight()).alpha(0)
				.setListener(getDefaultAnimatorListener());
		} else
			getExplorer().showToast(getString(R.string.s_alert_remove_error));
	}

	public void onBookMarkAdd(OpenPath path) {
		mBookmarks.add(path);
		mBookmarkString = (mBookmarkString != null && mBookmarkString != "" ? mBookmarkString + ";" : "") + path.getPath();
		mBookmarkAdapter.notifyDataSetChanged();
	}
	
	public String getBookMarkNameString() {
		return mBookmarkString;
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
			final OpenPath path = mBookmarks.get(position);
			final String sPath = path.getPath();
			if(view == null || view.getTag() == null || !view.getTag().getClass().equals(BookmarkHolder.class) || !((BookmarkHolder)view.getTag()).getPath().equalsIgnoreCase(sPath)) {
				LayoutInflater in = (LayoutInflater)
					mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = in.inflate(R.layout.bookmark_layout, parent, false);
				mHolder = new BookmarkHolder(path, getPathTitle(path), view);
				
				mHolder.setEjectClickListener(new OnClickListener() {
					public void onClick(View v) {
						getActivity().runOnUiThread(new Runnable() {
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


}
