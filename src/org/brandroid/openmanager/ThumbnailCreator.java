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

package org.brandroid.openmanager;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.view.Gravity;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.brandroid.openmanager.data.OpenFile;
import org.brandroid.openmanager.data.OpenMediaStore;
import org.brandroid.openmanager.data.OpenPath;
import org.brandroid.openmanager.fragments.ContentFragment.FileSystemAdapter.ThumbnailStruct;
import org.brandroid.utils.Decoder;
import org.brandroid.utils.Logger;

public class ThumbnailCreator extends Thread {
	private SoftReference<Bitmap> mThumb;
	private static HashMap<String, Bitmap> mCacheMap = new HashMap<String, Bitmap>();
	private ArrayList<ThumbnailStruct> mPending;
	private Handler mHandler;
	
	private static Context mContext;
	private static Cursor mVideoCursor;
	private boolean mStop = false;

	public ThumbnailCreator(Context context, Handler handler) {
		mContext = context;
		mPending = new ArrayList<ThumbnailStruct>();
		mHandler = handler;
	}
	
	public static void setContext(Context c) { mContext = c; }
	
	public Bitmap isBitmapCached(String name) {
		return mCacheMap.get(name);
	}
	
	/*
	public void createNewThumbnail(ArrayList<OpenPath> files, String dir, int w, int h) {
		mPending = new ArrayList<ThumbnailStruct>(files.size());
		for(OpenPath path : files)
			mPending.add(new ThumbnailStruct(path, w, h));
	}
	
	public void createNewThumbnail(OpenPath file, int w, int h)
	{
		mPending.add(new ThumbnailStruct(file, w, h));
	}
	*/
	
	public void setCancelThumbnails(boolean stop) {
		mStop = stop;
	}
	
	@Override
	public void destroy() {
		if(mVideoCursor != null && !mVideoCursor.isClosed())
			mVideoCursor.close();
		super.destroy();
	}
	
	public void run() {
		while(mPending.size() > 0)
		{
			if (mStop) {
				mStop = false;
				//mPending = null;
				return;
			}
			
			ThumbnailStruct next = mPending.get(0);
			mThumb = generateThumb(next.File, next.Width, next.Height);
			sendThumbBack(mThumb, next.File.getPath());
		}
	}
	
	public static SoftReference<Bitmap> generateThumb(final OpenPath file, int mWidth, int mHeight)
	{
		SoftReference<Bitmap> mThumb = null;
		//final Handler mHandler = next.Handler;
		
		String mCacheFilename = file.getPath().replace("/", "_") + "_" + mWidth + "x" + mHeight + ".jpg";
		
		//we already loaded this thumbnail, just return it.
		if (mCacheMap.containsKey(mCacheFilename)) 
		{
			Bitmap bd = mCacheMap.get(mCacheFilename);
			mThumb = new SoftReference<Bitmap>(bd);
			return mThumb;
		}
		Bitmap bmp = loadThumbnail(mCacheFilename);
		if(bmp != null)
		{
			mCacheMap.put(mCacheFilename, bmp);
			mThumb = new SoftReference<Bitmap>(bmp);
			return mThumb;
		}
		if(mThumb == null)
		{
			Boolean valid = false;
			if (file.getClass().equals(OpenMediaStore.class))
			{
				OpenMediaStore om = (OpenMediaStore)file;
				BitmapFactory.Options opts = new BitmapFactory.Options();
				opts.outWidth = mWidth;
				//opts.outHeight = mHeight;
				opts.inSampleSize = 1;
				int w = om.getWidth();
				int h = om.getHeight();
				if(w > 0 && h > 0)
					opts.outHeight = mHeight = (int)(mWidth * ((double)h / (double)w));
				if(om.getParent().getName().equals("Photos"))
					bmp = MediaStore.Images.Thumbnails.getThumbnail(
								mContext.getContentResolver(),
								om.getMediaID(), MediaStore.Images.Thumbnails.MICRO_KIND, opts
							);
				else if(om.getParent().getName().equals("Videos"))
					bmp = MediaStore.Video.Thumbnails.getThumbnail(
								mContext.getContentResolver(),
								om.getMediaID(), MediaStore.Video.Thumbnails.MINI_KIND, opts
							);
				if(bmp != null) {
					w = bmp.getWidth();
					h = bmp.getHeight();
					if(w > mWidth)
					{
						//mHeight = (int)(mWidth * ((double)h / (double)w));
						//Logger.LogDebug("Bitmap is " + w + "x" + h + " to " + mWidth + "x" + mHeight);	
						bmp = Bitmap.createScaledBitmap(bmp, mWidth, mHeight, false);
					}
					mThumb = new SoftReference<Bitmap>(bmp);
					valid = true;
				}
			}
			if (!valid && isAPKFile(file.getName()))
			{
				//Logger.LogInfo("Getting apk icon for " + file.getName());
				JarFile apk = null;
				InputStream in = null;
				try {
					apk = new JarFile(((OpenFile)file).getFile());
					JarEntry icon = apk.getJarEntry("res/drawable-hdpi/icon.apk");
					if(icon != null && icon.getSize() > 0) {
						in = apk.getInputStream(icon);
						Bitmap pic = BitmapFactory.decodeStream(in);
						in.close();
						in = null;
						if(pic != null)
						{
							mThumb = new SoftReference<Bitmap>(Bitmap.createScaledBitmap(pic, mWidth, mHeight, false));
							valid = true;
						}
					}
					if(!valid) {
						PackageManager man = mContext.getPackageManager();
						PackageInfo pinfo = man.getPackageArchiveInfo(file.getAbsolutePath(), PackageManager.GET_ACTIVITIES);
						ApplicationInfo ainfo = pinfo.applicationInfo;
						if(Build.VERSION.SDK_INT >= 8)
							ainfo.publicSourceDir = ainfo.sourceDir = file.getPath();
						Drawable mIcon = ainfo.loadIcon(man);
						if(mIcon != null)
						{
							Bitmap pic = ((BitmapDrawable)mIcon).getBitmap();
							if(pic != null)
							{
								mThumb = new SoftReference<Bitmap>(Bitmap.createScaledBitmap(pic, mWidth, mHeight, false));
								valid = true;
							}
						}
					}
					if(!valid) {
						Logger.LogWarning("Couldn't get icon for " + file.getAbsolutePath());
						String iconName = "icon"; //getIconName(apk, file);
						if(iconName.indexOf(" ") > -1)
							iconName = "icon";
						for(String s : new String[]{"drawable-mdpi","drawable","drawable-hdpi","drawable-ldpi"})
						{
							icon = apk.getJarEntry("res/" + s + "/" + iconName + ".png");
							if(icon != null && icon.getSize() > 0)
							{
								in = apk.getInputStream(icon);
								Bitmap pic = BitmapFactory.decodeStream(in);
								in.close();
								in = null;
								if(pic == null)
									continue;
								mThumb = new SoftReference<Bitmap>(Bitmap.createScaledBitmap(pic, mWidth, mHeight, false));
								valid = true;
								break;
							}
						}
					}
					if(!valid)
					{
						mThumb = new SoftReference<Bitmap>(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.apk));
					}
				} catch(IOException ix) {
					Logger.LogError("Invalid APK: " + file.getPath(), ix);
				}
				finally {
					try {
						if(apk != null)
							apk.close();
					} catch(IOException nix) {
						Logger.LogError("Error closing APK while handling invalid APK exception.", nix);
					}
					try {
						if(in != null)
							in.close();
					} catch(IOException nix) {
						Logger.LogError("Error closing input stream while handling invalid APK exception.", nix);
					}
				}
			} else if (isImageFile(file.getName())) {
				long len_kb = file.length() / 1024;
				
				BitmapFactory.Options options = new BitmapFactory.Options();
				options.outWidth = mWidth;
				options.outHeight = mHeight;
										
				if (len_kb > 500 && len_kb < 2000) {
					options.inSampleSize = 16;
					options.inPurgeable = true;						
					mThumb = new SoftReference<Bitmap>(BitmapFactory.decodeFile(file.getPath(), options));
										
				} else if (len_kb >= 2000) {
					options.inSampleSize = 32;
					options.inPurgeable = true;
					mThumb = new SoftReference<Bitmap>(BitmapFactory.decodeFile(file.getPath(), options));
									
				} else if (len_kb <= 500) {
					options.inPurgeable = true;
					Bitmap b = BitmapFactory.decodeFile(file.getPath());
					
					if (b == null) 
						b = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.photo);
					
					mThumb = new SoftReference<Bitmap>(Bitmap.createScaledBitmap(b, mWidth, mHeight, false));
				}
			} else if (isVideoFile(file.getName()))
			{
				ContentResolver cr = mContext.getContentResolver();
				BitmapFactory.Options opts = new BitmapFactory.Options();
				opts.inSampleSize = 1;
				String[] cols = {MediaStore.Video.Media._ID, MediaStore.Video.Media.DISPLAY_NAME};
				int id = -1;
				try {
					if(mVideoCursor == null)
					{
						mVideoCursor = MediaStore.Video.query(cr, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, cols);
						Logger.LogDebug("Video cursor returned " + mVideoCursor.getCount());
						mVideoCursor.moveToFirst();
					}
					for(int vi = 0; vi < mVideoCursor.getCount(); vi++)
					{
						mVideoCursor.moveToPosition(vi);
						id = mVideoCursor.getInt(0);
						String name = mVideoCursor.getString(mVideoCursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME));
						if(name.equalsIgnoreCase(file.getName()))
							break;
					}
				} catch(Exception e) {
					Logger.LogError("Exception querying video thumbnail for " + file.getPath(), e);
				}
				if(id > -1)
					mThumb = new SoftReference<Bitmap>(Bitmap.createScaledBitmap(MediaStore.Video.Thumbnails.getThumbnail(cr, id, MediaStore.Video.Thumbnails.MICRO_KIND, opts), mWidth, mHeight, false));
				else
					mThumb = new SoftReference<Bitmap>(Bitmap.createScaledBitmap(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.movie), mWidth, mHeight, false));
			}
		}
		
		if(mThumb != null)
		{
			saveThumbnail(mCacheFilename, mThumb.get());
			mCacheMap.put(mCacheFilename, mThumb.get());
		}
		return mThumb;
	}
	
	private static Bitmap loadThumbnail(String file)
	{
		Bitmap ret = null;
		FileInputStream is = null;
		try {
			is = mContext.openFileInput(file);
			ret = BitmapFactory.decodeStream(is);
		} catch(FileNotFoundException e) {
		} catch(IOException e)
		{
			Logger.LogError("Unable to load bitmap.", e);
		} finally {
			if(is != null)
				try {
					is.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
		return ret;
	}
	
	private static void saveThumbnail(String file, Bitmap bmp)
	{
		FileOutputStream os = null;
		try {
			os = mContext.openFileOutput(file, 0);
			bmp.compress(CompressFormat.JPEG, 85, os);
		} catch(IOException e) {
			Logger.LogError("Unable to save thumbnail for " + file, e);
		} finally {
			if(os != null)
				try {
					os.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
	}
	
	
	private void sendThumbBack(SoftReference<Bitmap> mThumb, String path)
	{
		final Bitmap d = mThumb.get();
		new BitmapDrawable(d).setGravity(Gravity.CENTER);
		mCacheMap.put(path, d);
		
		mHandler.post(new Runnable() {
			
			public void run() {
				Message msg = mHandler.obtainMessage();
				msg.obj = d;
				msg.sendToTarget();
			}
		});
	}
	
	private static boolean isImageFile(String file) {
		String ext = file.substring(file.lastIndexOf(".") + 1);
		
		if (ext.equalsIgnoreCase("png") || ext.equalsIgnoreCase("jpg") ||
			ext.equalsIgnoreCase("jpeg")|| ext.equalsIgnoreCase("gif") ||
			ext.equalsIgnoreCase("tiff")|| ext.equalsIgnoreCase("tif"))
			return true;
		
		return false;
	}
	
	private static boolean isAPKFile(String file) {
		String ext = file.substring(file.lastIndexOf(".") + 1);
		
		if (ext.equalsIgnoreCase("apk"))
			return true;
		
		return false;
	}
	
	private static boolean isVideoFile(String path)
	{
		String ext = path.substring(path.lastIndexOf(".") + 1);
		if(ext.equalsIgnoreCase("mp4") || 
			  ext.equalsIgnoreCase("3gp") || 
			  ext.equalsIgnoreCase("avi") ||
			  ext.equalsIgnoreCase("webm") || 
			  ext.equalsIgnoreCase("m4v"))
			return true;
		return false;
	}
}
