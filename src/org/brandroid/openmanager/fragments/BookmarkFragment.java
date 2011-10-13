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
import org.brandroid.openmanager.R.drawable;
import org.brandroid.openmanager.R.id;
import org.brandroid.openmanager.R.layout;
import org.brandroid.openmanager.data.BookmarkHolder;
import org.brandroid.openmanager.data.OpenFTP;
import org.brandroid.openmanager.data.OpenFile;
import org.brandroid.openmanager.data.OpenPath;
import org.brandroid.openmanager.fragments.DirContentActivity.OnBookMarkAddListener;
import org.brandroid.openmanager.util.ExecuteAsRootBase;
import org.brandroid.utils.Logger;

import android.os.Bundle;
import android.os.Environment;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.preference.PreferenceManager;
import android.support.v4.app.ListFragment;
import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.app.AlertDialog;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.LinearLayout.LayoutParams;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.io.File;

public class BookmarkFragment extends ListFragment implements OnBookMarkAddListener,
															 OnItemLongClickListener{
	private static int BOOKMARK_POS = 6;
	
	private static OnChangeLocationListener mChangeLocList;
	private ArrayList<OpenPath> mBookmarks;
	private Context mContext;
	//private ImageView mLastIndicater = null;
	private BookmarkAdapter mBookmarkAdapter;
	private String mDirListString;
	private String mBookmarkString;
	private Boolean mHasExternal = false;
	private Boolean mShowTitles = true;

	public interface OnChangeLocationListener {
		void onChangeLocation(String name);
	}
	
	
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = getActivity();
		mBookmarks = new ArrayList<OpenPath>();
		mDirListString = (PreferenceManager.getDefaultSharedPreferences(mContext))
										    .getString(SettingsActivity.PREF_LIST_KEY, "");
		
		mBookmarkString = (PreferenceManager.getDefaultSharedPreferences(mContext))
											.getString(SettingsActivity.PREF_BOOKNAME_KEY, "");
		
		if (mDirListString.length() > 0) {
			String[] l = mDirListString.replace(":", ";").replace(";//","://").split(";");
			
			for(String s : l)
			{
				if(s.indexOf("external") > -1 || s.indexOf("-ext") > -1)
					mHasExternal = true;
				checkAndAdd(new OpenFile(s));
			}
		
		} else {
			OpenFile storage = new OpenFile(Environment.getExternalStorageDirectory());
			mBookmarks.add(new OpenFile(Environment.getRootDirectory()));
			mBookmarks.add(storage);
			checkAndAdd(storage.getChild("Download"));
			checkAndAdd(storage.getChild("Music"));
			checkAndAdd(storage.getChild("Movies"));
			checkAndAdd(storage.getChild("DCIM"));
			if(checkAndAdd(new OpenFile("/mnt/external_sd")))
				mHasExternal = true;
			if(checkAndAdd(new OpenFile("/mnt/sdcard-ext")))
				mHasExternal = true;
			checkAndAdd(new OpenFile("/mnt/usbdrive"));
			checkAndAdd(new OpenFile("/mnt/usbstorage"));
		}
		
		if (mBookmarkString.length() > 0) {
			String[] l = mBookmarkString.split(";");
			
			for(String s : l)
				checkAndAdd(new OpenFile(s));
		}
		
		mBookmarks.add(new OpenFile("ftp://Brandon:Brandon@psusadev2.celebros.com"));
	}
	
	private boolean checkAndAdd(OpenPath path)
	{
		if(checkDir(path.getPath()))
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
		if(fTest.exists() && fTest.isDirectory() && fTest.list() != null && fTest.list().length > 0)
			return true;
		else return false;
	}
	
	
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		ListView lv = getListView();		
		lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		lv.setCacheColorHint(0x00000000);
		lv.setDrawSelectorOnTop(true);
		lv.setOnItemLongClickListener(this);
		//lv.setBackgroundResource(R.drawable.listgradback);
		
		mBookmarkAdapter = new BookmarkAdapter(mContext, R.layout.bookmark_layout, mBookmarks);
		registerForContextMenu(lv);
		setListAdapter(mBookmarkAdapter);
		
		DirContentActivity.setOnBookMarkAddListener(this);
		
	}
	
	
	public void onListItemClick(ListView list, View view, int pos, long id) {
		ImageView v;
		
		//if(mLastIndicater != null)
		//	mLastIndicater.setVisibility(View.GONE);
			
		//v.setVisibility(View.VISIBLE);
		//mLastIndicater = v;
		
		
		
		//getFragmentManager().popBackStackImmediate("Settings", 0);
		
		if(mChangeLocList != null)
			mChangeLocList.onChangeLocation(mBookmarks.get(pos).getPath());
	}
	
	
	public boolean onItemLongClick(AdapterView<?> list, View view, final int pos, long id) {
		AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
		LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		View v = inflater.inflate(R.layout.input_dialog_layout, null);
		final EditText mText = (EditText)v.findViewById(R.id.dialog_input);
		final EditText mTextTop = (EditText)v.findViewById(R.id.dialog_input_top);
		final BookmarkHolder mHolder = (BookmarkHolder)view.getTag();
		
		/* the first two items in our dir list is / and sdcard.
		 * the user should not be able to change the location
		 * of these two entries. Everything else is fair game */
		if (pos > 1 && pos < BOOKMARK_POS) {
						
			((TextView)v.findViewById(R.id.dialog_message))
							.setText("Change the location of this directory.");
			
			((TextView)v.findViewById(R.id.dialog_message_top))
							.setText("Change the title of your bookmark.");
			v.findViewById(R.id.dialog_layout_top).setVisibility(View.VISIBLE);
			
			mTextTop.setText(mHolder.getText());
						
			mText.setText(mBookmarks.get(pos).getName());
			builder.setTitle("Bookmark Location");
			builder.setView(v);
			
			builder.setIcon(mHolder.getIconView().getDrawable());
			
			if(mHolder.isEjectable())
			{
				v.findViewById(R.id.dialog_message).setVisibility(View.GONE);
				((View)mText.getParent()).setVisibility(View.GONE);
				
				builder.setNeutralButton("Eject", new DialogInterface.OnClickListener() {
					
					public void onClick(DialogInterface dialog, int which) {
						tryEject(mBookmarks.get(pos).getPath(), mHolder);
					}
				});
			}
			
			builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				
				
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
			
			builder.setPositiveButton("Change", new DialogInterface.OnClickListener() {
				
				
				public void onClick(DialogInterface dialog, int which) {
					String location = mText.getText().toString();
					File file = new File(location);
					
					if (!file.isDirectory()) {
						Toast.makeText(mContext, 
									   location + " is an invalid directory", 
									   Toast.LENGTH_LONG).show();
						dialog.dismiss();
					
					} else {
						mBookmarks.set(pos, new OpenFile(file));
						buildDirString();
						mBookmarkAdapter.notifyDataSetChanged();
					}
				}
			});
			
			builder.create().show();
			return true;
		
		/*manage the users bookmarks, delete or rename*/
		} else if (pos > BOOKMARK_POS) {
			
			String bookmark = mHolder.getPath(); // mBookmarkNames.get(p - (BOOKMARK_POS + 1));
			
			
			builder.setTitle("Manage bookmark: " + bookmark);
			builder.setIcon(R.drawable.folder);
			builder.setView(v);
			
			((TextView)v.findViewById(R.id.dialog_message))
						.setText("Would you like to delete or rename this bookmark?");
			
			mText.setText(bookmark);
			builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
				
				
				public void onClick(DialogInterface dialog, int which) {
					mBookmarks.remove(pos);
					buildDirString();
					mBookmarkAdapter.notifyDataSetChanged();
				}
			});
			builder.setNegativeButton("Rename", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					/*
					mBookmarkNames.remove(p - (BOOKMARK_POS + 1));
					mBookmarkNames.add(p - (BOOKMARK_POS + 1), mText.getText().toString());
					*/
					/// TODO: Add ability to rename OpenPath
					
					buildDirString();
					mBookmarkAdapter.notifyDataSetChanged();
				}
			});
			
			builder.create().show();
			return true;
			
		}
		return false;
	}

	
	protected void tryEject(String sPath, BookmarkHolder mHolder) {
		final View viewf = mHolder.getView();
		if(ExecuteAsRootBase.execute("umount " + sPath))
		{
			((OpenExplorer)getActivity()).showToast("You may now safely remove the drive.");
			viewf.animate().setListener(new AnimatorListener() {
				public void onAnimationStart(Animator animation) {}
				public void onAnimationRepeat(Animator animation) {}
				public void onAnimationCancel(Animator animation) {}
				public void onAnimationEnd(Animator animation) {
					((OpenExplorer)getActivity()).refreshBookmarks();
				}
			}).setDuration(500).y(viewf.getY() - viewf.getHeight()).alpha(0);
		} else
			((OpenExplorer)getActivity()).showToast("Unable to eject.");
	}

	public void onBookMarkAdd(String path) {
		mBookmarks.add(new OpenFile(path));
		
		buildDirString();
		mBookmarkAdapter.notifyDataSetChanged();
	}
	
	public static void setOnChangeLocationListener(OnChangeLocationListener l) {
		mChangeLocList = l;
	}
	
	public String getDirListString() {
		return mDirListString;
	}
	
	
	public String getBookMarkNameString() {
		return mBookmarkString;
	}
	
	/*
	 * Builds a string from mDirList to easily save and recall
	 * from preferences. 
	 */
	private void buildDirString() {
		int len = mBookmarks.size();
		String mDirListString = "";
		
		if(mDirListString != null && mDirListString.length() > 0) {
			mDirListString = "";
			mBookmarkString = "";
		}
		
		for (int i = 0; i <len; i++)
			mDirListString += mBookmarks.get(i).getPath() + ";";
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
			final OpenPath path = super.getItem(position);
			final String sPath = path.getPath();
			if(view == null || view.getTag() == null || !view.getTag().getClass().equals(BookmarkHolder.class) || !!((BookmarkHolder)view.getTag()).getPath().equalsIgnoreCase(sPath)) {
				LayoutInflater in = (LayoutInflater)
					mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = in.inflate(R.layout.bookmark_layout, parent, false);
				mHolder = new BookmarkHolder(sPath, sPath, view);
				
				mHolder.setEjectClickListener(new OnClickListener() {
					public void onClick(View v) {
						getActivity().runOnUiThread(new Runnable() {
							public void run() {
								tryEject(sPath, mHolder);
							}
						});
					}
				});
				
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

			String sPath2 = sPath.toLowerCase();
			if(position == 0)
			{
				mHolder.setText("/");
				mHolder.setIconResource(R.drawable.drive);
			} else if(sPath2.endsWith("sdcard")) {
				if(mHasExternal)
				{
					mHolder.setText("Internal Storage");
					mHolder.setIconResource(R.drawable.drive);
				} else {
					mHolder.setText("sdcard");
					mHolder.setIconResource(R.drawable.sdcard);
				}
			} else if(sPath2.indexOf("download") > -1) {
				mHolder.setText("Downloads");
				if(mHasExternal)
					mHolder.setText("Downloads (Internal)");
				mHolder.setIconResource(R.drawable.download);
			} else if(sPath2.indexOf("music") > -1) {
				mHolder.setText("Music");
				if(mHasExternal)
					mHolder.setText("Music (Internal)");
				mHolder.setIconResource(R.drawable.music);
			} else if(sPath2.indexOf("movie") > -1) {
				mHolder.setText("Movies");
				if(mHasExternal)
					mHolder.setText("Movies (Internal)");
				mHolder.setIconResource(R.drawable.movie);
			} else if(sPath2.indexOf("photo") > -1 || sPath2.indexOf("dcim") > -1 || sPath2.indexOf("camera") > -1) {
				mHolder.setText("Photos");
				if(mHasExternal)
					mHolder.setText("Photos (Internal)");
				mHolder.setIconResource(R.drawable.photo);
			} else if(sPath2.indexOf("usb") > -1) {
				mHolder.setText("USB");
				mHolder.setEjectable(true);
				mHolder.setIconResource(R.drawable.usb);
			} else if(sPath2.indexOf("sdcard-ext") > -1 || sPath2.indexOf("external") > -1) {
				mHolder.setText("External SD");
				mHolder.setEjectable(true);
				mHolder.setIconResource(R.drawable.sdcard);
			} else if(sPath2.equals("bookmarks")) {
				view.setBackgroundColor(Color.BLACK);
				view.setFocusable(false);
				view.setEnabled(false);
				view.setClickable(false);
				view.setPadding(0, 0, 0, 0);
				mHolder.setText("Bookmarks");
				//mHolder.setTextSize(18);
				if(mHolder.getIconView() != null)
				{
					mHolder.getIconView().setVisibility(View.GONE);
					mHolder.setIconResource(R.drawable.favorites);
					mHolder.getIconView().setMaxHeight(24);
				}
				BOOKMARK_POS = position;
				//view.setBackgroundColor(R.color.black);
			} else if(sPath2.startsWith("ftp:/")) {
				String item = sPath.replace("ftp:/","");
				if(item.indexOf("@") > -1)
					item = item.substring(item.indexOf("@") + 1);
				mHolder.setText(item);
				mHolder.setIconResource(R.drawable.ftp);
			} else {
				mHolder.setText(super.getItem(position).getName());
				mHolder.setIconResource(R.drawable.folder);
			}
			
			return view;
		}
	}	
}
