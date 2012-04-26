package org.brandroid.openmanager.adapters;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.brandroid.openmanager.R;
import org.brandroid.openmanager.activities.OpenExplorer;
import org.brandroid.openmanager.data.OpenMediaStore;
import org.brandroid.openmanager.data.OpenPath;
import org.brandroid.openmanager.fragments.DialogHandler;
import org.brandroid.openmanager.fragments.OpenFragment;
import org.brandroid.openmanager.util.ThumbnailCreator;
import org.brandroid.openmanager.util.ThumbnailStruct.OnUpdateImageListener;
import org.brandroid.utils.ImageUtils;
import org.brandroid.utils.Logger;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;



/**
 * 
 */
public class FileSystemAdapter extends ArrayAdapter<OpenPath> {
	private final int KB = 1024;
	private final int MG = KB * KB;
	private final int GB = MG * KB;
	
	private final OpenExplorer mExplorer; 
	public int mViewMode = OpenExplorer.VIEW_LIST;
	public boolean mShowThumbnails = true;
	
	public FileSystemAdapter(OpenExplorer explorer, int layout, List<OpenPath> data) {
		super(explorer, layout, data);
		mExplorer = explorer;
	}
	
	public Resources getResources() { return super.getContext().getResources(); }
	public int getViewMode() { return mViewMode; }
	public void setViewMode(int mode) { mViewMode = mode; } 
	public OpenExplorer getExplorer() { return mExplorer; }
	
	@Override
	public void notifyDataSetChanged() {
		//Logger.LogDebug("Data set changed for FileSystemAdapter - Size = " + getCount() + ". (" + mPath.getPath() + ")");
		try {
			//if(getManager() != null)
			//	getExplorer().updateTitle(getManager().peekStack().getPath());
			super.notifyDataSetChanged();
		} catch(NullPointerException npe) {
			Logger.LogError("Null found while notifying data change.", npe);
		}
	}
	
	private final Handler handler = new Handler(new Handler.Callback() {
		public boolean handleMessage(Message msg) {
			notifyDataSetChanged();
			return true;
		}
	});
	
	////@Override
	public View getView(int position, View view, ViewGroup parent)
	{
		final OpenPath file = super.getItem(position);
		final String mName = file.getName();
		
		int mWidth = getResources().getInteger(R.integer.content_list_image_size);
		int mHeight = mWidth;
		if(getViewMode() == OpenExplorer.VIEW_GRID)
			mWidth = mHeight = getResources().getInteger(R.integer.content_grid_image_size);
		
		int mode = getViewMode() == OpenExplorer.VIEW_GRID ?
				R.layout.grid_content_layout : R.layout.list_content_layout;
		
		boolean showLongDate = false;
		if(getResources().getBoolean(R.bool.show_long_date))
			showLongDate = true;
		
		if(view == null
					//|| view.getTag() == null
					//|| !BookmarkHolder.class.equals(view.getTag())
					//|| ((BookmarkHolder)view.getTag()).getMode() != mode
					)
		{
			LayoutInflater in = (LayoutInflater)getContext()
									.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			
			view = in.inflate(mode, parent, false);
			//mHolder = new BookmarkHolder(file, mName, view, mode);
			//view.setTag(mHolder);
			//file.setTag(mHolder);
		} //else mHolder = (BookmarkHolder)view.getTag();

		//mHolder.getIconView().measure(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		//Logger.LogVerbose("Content Icon Size: " + mHolder.getIconView().getMeasuredWidth() + "x" + mHolder.getIconView().getMeasuredHeight());

		//view.measure(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		//mHolder.setInfo(getFileDetails(file, false));
		TextView mInfo = (TextView)view.findViewById(R.id.content_info);
		if(mInfo != null)
			mInfo.setText(getFileDetails(file, showLongDate));
		
		TextView mPathView = (TextView)view.findViewById(R.id.content_fullpath); 
		if(mPathView != null)
		{
			if(file.getClass().equals(OpenMediaStore.class))
			{
				mPathView.setText(file.getPath());
				mPathView.setVisibility(View.VISIBLE);
				//mHolder.setPath(file.getPath());
				//mHolder.showPath(true);
			}
			else
				mPathView.setVisibility(View.GONE);
				//mHolder.showPath(false);
		}
		
		TextView mNameView = (TextView)view.findViewById(R.id.content_text);
		if(mNameView != null)
			mNameView.setText(mName);

		if(getExplorer().getClipboard().contains(file))
			mNameView.setTextAppearance(getContext(), R.style.Highlight);
		else
			mNameView.setTextAppearance(getContext(),  R.style.Large);
		
		if(file.isHidden())
			OpenFragment.setAlpha(0.5f, mNameView, mPathView, mInfo);
		else
			OpenFragment.setAlpha(1.0f, mNameView, mPathView, mInfo);
						
		//if(!mHolder.getTitle().equals(mName))
		//	mHolder.setTitle(mName);
		final ImageView mIcon = (ImageView)view.findViewById(R.id.content_icon);
		//RemoteImageView mIcon = (RemoteImageView)view.findViewById(R.id.content_icon);
		
		if(mIcon != null)
		{
			mIcon.invalidate();
			if(file.isHidden())
				mIcon.setAlpha(100);
			else
				mIcon.setAlpha(255);
			if(file.isTextFile())
				mIcon.setImageBitmap(ThumbnailCreator.getFileExtIcon(file.getExtension(), getContext(), mWidth > 72));
			else if(!mShowThumbnails||!file.hasThumbnail())
			{
				if(file.isDirectory())
				{
					boolean bCountHidden = false;// !getExplorer().getSetting(file, "hide", true);
					try {
						if(!file.requiresThread() && file.getChildCount(bCountHidden) > 0)
							mIcon.setImageDrawable(getResources().getDrawable(mWidth > 72 ? R.drawable.lg_folder_full : R.drawable.sm_folder_full));
						else if(!file.requiresThread())
							mIcon.setImageDrawable(getResources().getDrawable(mWidth > 72 ? R.drawable.lg_folder : R.drawable.sm_folder));
						else
							mIcon.setImageResource(ThumbnailCreator.getDefaultResourceId(file, mWidth, mHeight));
					} catch (Exception e) {
						mIcon.setImageResource(ThumbnailCreator.getDefaultResourceId(file, mWidth, mHeight));
					}
				} else {
					mIcon.setImageResource(ThumbnailCreator.getDefaultResourceId(file, mWidth, mHeight));
				}
			} else if(!ThumbnailCreator.getImagePath(mIcon).equals(file.getPath())) {
				ThumbnailCreator.setThumbnail(mIcon, new OnUpdateImageListener() {
					public void updateImage(Bitmap b) {
						if(!ThumbnailCreator.getImagePath(mIcon).equals(file.getPath()))
						{
							BitmapDrawable d = new BitmapDrawable(getResources(), b);
							d.setGravity(Gravity.CENTER);
							ImageUtils.fadeToDrawable(mIcon, d);
							mIcon.setTag(file.getPath());
						}
					}
					public Context getContext() { return mIcon.getContext(); }
					}, file, mWidth, mHeight);
			}
		}
		
		return view;
	}

	private String getFileDetails(OpenPath file, Boolean longDate) {
		//OpenPath file = getManager().peekStack().getChild(name); 
		String deets = ""; //file.getPath() + "\t\t";
		
		if(file instanceof OpenMediaStore)
		{
			OpenMediaStore ms = (OpenMediaStore)file;
			if(ms.getWidth() > 0 || ms.getHeight() > 0)
				deets += ms.getWidth() + "x" + ms.getHeight() + " | ";
			if(ms.getDuration() > 0)
				deets += DialogHandler.formatDuration(ms.getDuration()) + " | ";
		}
		
		if(file.isDirectory() && !file.requiresThread()) {
			try {
				boolean bCountHidden = getExplorer().getPreferences().getSetting("views", "show_" + file.getPath(), false);
				deets += file.getChildCount(bCountHidden) + " " + getContext().getString(R.string.s_files) + " | ";
				//deets = file.list().length + " items";
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else if(file.isFile()) {
			deets += DialogHandler.formatSize(file.length()) + " | ";
		} 
		
		Long last = file.lastModified();
		if(last != null)
		{
			DateFormat df = new SimpleDateFormat(longDate ? "MM-dd-yyyy HH:mm" : "MM-dd-yy");
			deets += df.format(file.lastModified());
		}
		
		/*
		
		deets += " | ";
		
		deets += (file.isDirectory()?"d":"-");
		deets += (file.canRead()?"r":"-");
		deets += (file.canWrite()?"w":"-");
		deets += (file.canExecute()?"x":"-");
		
		*/
		
		return deets;
	}
	
}