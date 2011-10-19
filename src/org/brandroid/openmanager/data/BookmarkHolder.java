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
import org.brandroid.utils.Logger;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class BookmarkHolder {
	private ImageView mIcon;
	private ImageView mEject;
	private ImageView mIndicate;
	private TextView mMainText;
	private TextView mInfo;
	private TextView mPath;
	private View mParentView;
	private String sTitle;
	private String sPath;
	private OpenPath mFile;
	
	public BookmarkHolder(String path, View view) {
		this(path, getTitleFromPath(path), view);
	}
	public BookmarkHolder(String path, String title, final View view)
	{
		mParentView = view;
		sPath = path;
		mFile = new OpenFile(path);
		ensureViews();
		//mIndicate = (ImageView)view.findViewById(R.id.)
		setTitle(title);
	}
	
	private void ensureViews()
	{
		if(mIcon == null)
			mIcon = (ImageView)mParentView.findViewById(R.id.content_icon);
		if(mMainText == null)
			mMainText = (TextView)mParentView.findViewById(R.id.content_text);
		if(mIndicate == null)
			mIndicate = (ImageView)mParentView.findViewById(R.id.list_arrow);
		if(mEject == null)
			mEject = (ImageView)mParentView.findViewById(R.id.eject);
		if(mInfo == null)
			mInfo = (TextView)mParentView.findViewById(R.id.content_info);
		if(mPath == null)
			mPath = (TextView)mParentView.findViewById(R.id.content_fullpath);
		updateSizeIndicator();
	}
	
	public void updateSizeIndicator()
	{
		ProgressBar bar = (ProgressBar)mParentView.findViewById(R.id.size_bar);
		if(bar == null) return;
		if(mFile != null && mFile.getClass().equals(OpenFile.class))
		{
			OpenFile f = (OpenFile)mFile;
			if(f.getTotalSpace() > 0 && f.getFreeSpace() < f.getTotalSpace())
			{
				bar.setMax((int)f.getTotalSpace());
				bar.setProgress((int)(f.getTotalSpace() - f.getFreeSpace()));
				if(bar.getProgress() == 0)
					bar.setVisibility(View.GONE);
				else
					Logger.LogInfo(f.getPath() + " has " + bar.getProgress() + " / " + bar.getMax());
			} else bar.setVisibility(View.GONE);
		} else bar.setVisibility(View.GONE);
	}
	
	public ImageView getIconView() { ensureViews(); return mIcon; }
	public void setIconResource(int res) { ensureViews(); if(mIcon != null) mIcon.setImageResource(res); }
	public void setIconDrawable(Drawable d) { ensureViews(); if(mIcon != null) mIcon.setImageDrawable(d); }
	
	public void setEjectClickListener(View.OnClickListener listener)
	{
		if(mEject != null)
			mEject.setOnClickListener(listener);
	}
	public void setEjectable(Boolean eject) {
		if(mEject != null)
			mEject.setVisibility(eject ? View.VISIBLE : View.GONE);
	}
	public Boolean isEjectable() { return mEject != null && mEject.getVisibility() == View.VISIBLE; }
	
	public void setSelected(Boolean sel)
	{
		if(mIndicate != null)
			mIndicate.setVisibility(sel ? View.VISIBLE : View.GONE);
	}
	
	public String getPath() { return sPath; }
	public void setPath(String path)
	{
		sPath = path;
		if(mPath != null)
			mPath.setText(path);
	}
	public String getTitle() { return sTitle; }
	public void setTitle(String title) {
		if(mMainText != null)
			mMainText.setText(title);
		sTitle = title;
	}
	public void hideTitle() { if(mMainText != null) mMainText.setVisibility(View.GONE); }
	public void showTitle() { if(mMainText != null) mMainText.setVisibility(View.VISIBLE); }
	public String getText() { return mMainText != null ? mMainText.getText().toString() : null; }
	public void setText(String text) { if(mMainText != null) mMainText.setText(text); }
	public View getView() { return mParentView; }
	public String getInfo() { return mInfo != null ? mInfo.getText().toString() : null; }
	public void setInfo(String info) { if(mInfo != null) mInfo.setText(info); }
	
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
