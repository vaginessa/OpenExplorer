package org.brandroid.openmanager.adapters;

import java.io.File;
import java.util.ArrayList;
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

public class RecursiveDirectoryAdapter extends BaseExpandableListAdapter
{
	private ArrayList<File> mDirectories;
	private Context mContext;
	private static Hashtable<File, ArrayList<File>> DirectoryCache = new Hashtable<File, ArrayList<File>>();
	
	public RecursiveDirectoryAdapter(File parent, Context context)
	{
		mDirectories = getSubDirectories(parent);
		Logger.LogDebug("Recursing " + parent.getAbsolutePath() + " (" + (mDirectories != null ? mDirectories.size() : "-1") + " directories)...");
		mContext = context;
	}
	
	public static ArrayList<File> getSubDirectories(File parent)
	{
		Logger.LogDebug("Getting subdirs for " + parent.getAbsolutePath());
		if(DirectoryCache.containsKey(parent))
			return DirectoryCache.get(parent);
		ArrayList<File> dirs = new ArrayList<File>();
		if(parent.list() == null) return dirs;
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
		return dirs;
	}
	
	public static File[] getArray(ArrayList<File> files) {
		if(files == null) return new File[0];
		File[] ret = new File[files.size()];
		for(int i = 0; i < ret.length; i++)
			ret[i] = files.get(i);
		return ret;
	}

	public Object getChild(int groupPosition, int childPosition) {
		return getSubDirectories(mDirectories.get(groupPosition)).get(childPosition);
	}

	public long getChildId(int groupPosition, int childPosition) {
		// TODO Auto-generated method stub
		return 0;
	}

	public View getChildView(int groupPosition, int childPosition,
			boolean isLastChild, View convertView, ViewGroup parent) {
		View view = LayoutInflater.from(mContext).inflate(R.layout.directory_tree_list, null);
		view.setPadding(50, 4, 4, 4);
		ExpandableListView mList = (ExpandableListView)view.findViewById(R.id.list_tree);
		//ExpandableListAdapter adapter = new
		RecursiveDirectoryAdapter adapter = new RecursiveDirectoryAdapter(getSubDirectories(mDirectories.get(groupPosition)).get(childPosition), mContext);
		mList.setAdapter(adapter);
		return view;
	}

	public int getChildrenCount(int groupPosition) {
		return getSubDirectories(mDirectories.get(groupPosition)).size();
	}

	public Object getGroup(int groupPosition) {
		return mDirectories.get(groupPosition);
	}

	public int getGroupCount() {
		if(mDirectories == null)
			return 0;
		return mDirectories.size();
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
			tv.setText(mDirectories.get(groupPosition).getName());
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

}
