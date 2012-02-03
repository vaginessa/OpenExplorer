package org.brandroid.openmanager.util;

import java.lang.ref.SoftReference;

import org.brandroid.openmanager.data.BookmarkHolder;
import org.brandroid.openmanager.data.OpenPath;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.view.Gravity;
import android.widget.ImageView;



public class ThumbnailStruct
{
	public OpenPath File;
	public int Width = 0, Height = 0;
	//public BookmarkHolder Holder;
	private SoftReference<Bitmap> mBitmap; 
	public final ImageView ImageView; 
	//public Handler Handler;
	public ThumbnailStruct(OpenPath path, BookmarkHolder holder, int width, int height)
	{
		File = path;
		//Holder = holder;
		ImageView = holder.getIconView();
		//Handler = handler;
		Width = width;
		Height = height;
	}
	public ThumbnailStruct(OpenPath path, ImageView view, int width, int height)
	{
		File = path;
		ImageView = view;
		Width = width;
		Height = height;
	}
	public void setBitmap(SoftReference<Bitmap> thumb)
	{
		mBitmap = thumb;
	}
	public void updateHolder()
	{
		if(mBitmap != null && mBitmap.get() != null)
		{
			BitmapDrawable bd = new BitmapDrawable(mBitmap.get());
			bd.setGravity(Gravity.CENTER);
			//if(Holder != null) Holder.setIconDrawable(bd, this);
			ImageView.setImageDrawable(bd);
		}
	}
}
