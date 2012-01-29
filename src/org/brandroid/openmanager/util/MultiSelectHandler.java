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

package org.brandroid.openmanager.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.MenuItem;
import android.view.View;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.View.OnLongClickListener;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.SlidingDrawer.OnDrawerCloseListener;

import java.util.ArrayList;
import java.io.File;

import org.brandroid.openmanager.R;
import org.brandroid.openmanager.R.drawable;
import org.brandroid.openmanager.R.id;
import org.brandroid.openmanager.R.layout;
import org.brandroid.openmanager.adapters.IconContextMenu;
import org.brandroid.openmanager.adapters.IconContextMenu.IconContextItemSelectedListener;
import org.brandroid.openmanager.data.OpenFile;
import org.brandroid.openmanager.data.OpenPath;
import org.brandroid.utils.Logger;

public class MultiSelectHandler extends BaseAdapter
{
	private static MultiSelectHandler mInstance = null;
	private static Context mContext;
	private static LayoutInflater mInflater;
	private static ArrayList<OpenPath> mFileList = null;
	
	public static MultiSelectHandler getInstance(Context context) {
		//make this cleaner
		if(mInstance == null)
			mInstance = new MultiSelectHandler();
		if(mFileList == null)
			mFileList = new ArrayList<OpenPath>();
		
		mContext = context;
		mInflater = (LayoutInflater)mContext
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		return mInstance;
	}
	
	public View addFile(OpenPath file) {
		//Logger.LogVerbose("Adding " + file.getName() + " to Multiselect.");
		if(!mFileList.contains(file))
			mFileList.add(0, file);
		notifyDataSetChanged();
		return getView(mFileList.indexOf(file), null, null);
	}
	
	public ArrayList<OpenPath> getSelectedFiles() {
		return mFileList;
	}
	
	public int clearFileEntry(OpenPath path) {
		int index = mFileList.indexOf(path);
		
		if(index > -1)
			mFileList.remove(index);
		
		notifyDataSetChanged();
		
		return index;
	}
	
	public void cancelMultiSelect() {
		mFileList.clear();
		mFileList = null;
		mInstance = null;		
	}
	
	public int getCount() {
		return mFileList.size();
	}

	public Object getItem(int position) {
		return mFileList.get(position);
	}

	public long getItemId(int position) {
		return 0;
	}

	public View getView(int position, View convertView, ViewGroup parent)
	{
		View ret = convertView;
		if(ret == null)
		{
			ret = mInflater.inflate(R.layout.multiselect_layout, null); 
		}
		int w = mContext.getResources().getDimensionPixelSize(R.dimen.multiselect_width);
		//ret.setLayoutParams(new Gallery.LayoutParams(w, w));
		//double sz = (double)w * 0.7;
		
		final OpenPath file = (OpenPath)getItem(position);
		
		ImageView image = (ImageView)ret.findViewById(R.id.multi_icon);
		TextView text = (TextView)ret.findViewById(R.id.multi_text);
		
		text.setText(file.getName());
		ThumbnailCreator.setThumbnail(image, file, w, w); //(int)(w * (3f/4f)), (int)(w * (3f/4f)));

		return ret;
	}

	public void clear() {
		mFileList.clear();
		notifyDataSetChanged();
	}

	public OpenPath[] getSelectedFilesArray() {
		OpenPath[] ret = new OpenPath[getCount()];
		getSelectedFiles().toArray(ret);
		return ret;
	}
}
