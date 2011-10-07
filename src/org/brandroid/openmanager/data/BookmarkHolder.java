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

package org.brandroid.openmanager.data;

import org.brandroid.openmanager.R;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class BookmarkHolder {
	public ImageView mIcon;
	public ImageView mEject;
	public ImageView mIndicate;
	public TextView mMainText;
	public TextView mInfo;
	public TextView mPath;
	private String sTitle;
	private String sPath;
	private OpenFace mFile;
	
	public BookmarkHolder(String path, View view) {
		this(path, getTitleFromPath(path), view);
	}
	public BookmarkHolder(String path, String title, View view)
	{
		mIcon = (ImageView)view.findViewById(R.id.content_icon);
		mMainText = (TextView)view.findViewById(R.id.content_text);
		sPath = path;
		setTitle(title);
	}
	
	public String getPath() { return sPath; }
	public String getTitle() { return sTitle; }
	public void setTitle(String title) {
		if(mMainText != null)
			mMainText.setText(title);
		sTitle = title;
	}
	
	private static String getTitleFromPath(String path)
	{
		if(path != "/")
		{
			if(path.endsWith("/"))
				path = path.substring(0, path.length() - 1);
			path = path.substring(path.lastIndexOf("/") + 1);
		}
		return path;
	}
}
