package org.brandroid.openmanager.util;

import java.lang.ref.SoftReference;

import org.brandroid.openmanager.data.OpenPath;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.Gravity;



public class ThumbnailStruct
{
	public OpenPath File;
	public int Width = 0, Height = 0;
	//public BookmarkHolder Holder;
	private SoftReference<Bitmap> mBitmap; 
	//public Handler Handler;
	private final OnUpdateImageListener mListener;
	
	public interface OnUpdateImageListener
	{
		void updateImage(Drawable d);
		Context getContext();
	}
	public ThumbnailStruct(OpenPath path, OnUpdateImageListener listener, int width, int height)
	{
		File = path;
		//Holder = holder;
		mListener = listener;
		//Handler = handler;
		Width = width;
		Height = height;
	}
	public Context getContext() { return mListener.getContext(); }
	
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
			//ImageView.setImageDrawable(bd);
			//ImageUtils.fadeToDrawable(ImageView, bd);
			mListener.updateImage(bd);
		}
	}
}
