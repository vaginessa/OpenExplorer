package org.brandroid.openmanager.adapters;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;
import java.util.zip.Inflater;

import org.brandroid.openmanager.R;
import org.brandroid.utils.Logger;

import android.content.Context;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class RecursiveExpandableDirectoryAdapter extends BaseExpandableListAdapter
{
	private File[] mDirectories;
	private Context mContext;
	private static Hashtable<File, File[]> DirectoryCache = new Hashtable<File, File[]>();
	public static FileComparer Comparer;
	
	public RecursiveExpandableDirectoryAdapter(File parent, Context context)
	{
		mDirectories = getSubDirectories(parent);
		Logger.LogDebug("Recursing " + parent.getAbsolutePath() + " (" + (mDirectories != null ? mDirectories.length : "-1") + " directories)...");
		mContext = context;
	}
	
	public static File[] getSubDirectories(File parent)
	{
		Logger.LogDebug("Getting subdirs for " + parent.getAbsolutePath());
		if(DirectoryCache.containsKey(parent))
			return DirectoryCache.get(parent);
		ArrayList<File> dirs = new ArrayList<File>();
		if(parent.list() == null) return new File[0];
		for(String s : parent.list())
		{
			File f = new File(parent, s);
			if(!f.exists()) continue;
			if(!f.isDirectory()) continue;
			//Logger.LogDebug(s);
			dirs.add(f);
		}
		//Logger.LogDebug("Found " + dirs.size() + " Subdirs");
		//DirectoryCache.put(parent, dirs);
		File[] ret = getArray(dirs);
		Arrays.sort(ret, Comparer);
		return ret;
	}
	
	public static File[] getArray(ArrayList<File> files) {
		if(files == null) return new File[0];
		File[] ret = new File[files.size()];
		for(int i = 0; i < ret.length; i++)
			ret[i] = files.get(i);
		return ret;
	}

	public Object getChild(int groupPosition, int childPosition) {
		return getSubDirectories(mDirectories[groupPosition])[childPosition];
	}

	public long getChildId(int groupPosition, int childPosition) {
		// TODO Auto-generated method stub
		return 0;
	}

	public View getChildView(int groupPosition, int childPosition,
			boolean isLastChild, View convertView, ViewGroup parent) {
		View view = convertView;
		if(convertView == null)
		{
			File selFile = (File)getChild(groupPosition, childPosition);
			if(selFile.list().length > 0)
			{
				view = LayoutInflater.from(mContext).inflate(R.layout.directory_tree_list, null);
				view.setPadding(50, 4, 4, 4);
				ExpandableListView mList = (ExpandableListView)view.findViewById(android.R.id.list);
				RecursiveExpandableDirectoryAdapter adapter = new RecursiveExpandableDirectoryAdapter(selFile, mContext);
				mList.setAdapter(adapter);
			} else {
				TextView tv = new TextView(mContext);
				tv.setText(selFile.getName());
				tv.setTextSize(20);
				tv.setPadding(40, 4, 4, 4);
				view = tv;
			}
		}
		return view;
	}

	public int getChildrenCount(int groupPosition) {
		return getSubDirectories(mDirectories[groupPosition]).length;
	}

	public Object getGroup(int groupPosition) {
		return mDirectories[groupPosition];
	}

	public int getGroupCount() {
		if(mDirectories == null)
			return 0;
		return mDirectories.length;
	}

	public long getGroupId(int groupPosition) {
		return 0;
	}

	public View getGroupView(int groupPosition, boolean isExpanded,
			View convertView, ViewGroup parent) {
		//TextView tv = LayoutInflater.from(mContext).inflate(android.R.layout.simple_list_item_1, null)
		View ret = convertView;
		if(convertView == null)
		{
			TextView tv = new TextView(mContext);
			tv.setText(mDirectories[groupPosition].getName());
			tv.setTextSize(20);
			tv.setPadding(40, 4, 4, 4);
			ret = tv;
		}
		return ret;
	}

	public boolean hasStableIds() {
		return false;
	}

	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return true;
	}

	public class FileComparer implements Comparator<File>
	{
		public int compare(File a, File b) 
		{
			return a.getName().compareTo(b.getName());
		}
		
	}

}
