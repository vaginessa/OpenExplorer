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
import android.graphics.Matrix;
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
import org.brandroid.openmanager.fragments.ContentFragment.ThumbnailStruct;
import org.brandroid.utils.Decoder;
import org.brandroid.utils.General;
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
	
	public Bitmap isBitmapCached(String name, int w, int h) {
		return mCacheMap.get(getCacheFilename(name, w, h));
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
	
	private static String getCacheFilename(String path, int w, int h)
	{
		return w + "x" + h + "_" + path.replaceAll("[^A-Za-z0-9]", "-") + ".png";
	}
	
	public static SoftReference<Bitmap> generateThumb(final OpenPath file, int mWidth, int mHeight) { return generateThumb(file, mWidth, mHeight, true, true); }
	public static SoftReference<Bitmap> generateThumb(final OpenPath file, int mWidth, int mHeight, final boolean readCache, final boolean writeCache)
	{
		//SoftReference<Bitmap> mThumb = null;
		Bitmap bmp = null;
		//final Handler mHandler = next.Handler;
		
		String path = file.getPath();
		
		String mCacheFilename = getCacheFilename(path, mWidth, mHeight);
		
		//we already loaded this thumbnail, just return it.
		if (mCacheMap.containsKey(mCacheFilename)) 
			return new SoftReference<Bitmap>(mCacheMap.get(mCacheFilename));
		if(readCache && bmp == null)
			bmp = loadThumbnail(mCacheFilename);
		
		if(bmp == null)
		{
			Boolean valid = false;
			if (file.getClass().equals(OpenMediaStore.class))
			{
				OpenMediaStore om = (OpenMediaStore)file;
				BitmapFactory.Options opts = new BitmapFactory.Options();
				opts.inSampleSize = 1;
				//opts.outWidth = mWidth;
				//opts.outHeight = mHeight;
				int kind = mWidth > 96 ? MediaStore.Video.Thumbnails.MINI_KIND : MediaStore.Video.Thumbnails.MICRO_KIND;
				if(om.getParent().getName().equals("Photos"))
					bmp = MediaStore.Images.Thumbnails.getThumbnail(
								mContext.getContentResolver(),
								om.getMediaID(), kind, opts
							);
				else // if(om.getParent().getName().equals("Videos"))
					bmp = MediaStore.Video.Thumbnails.getThumbnail(
								mContext.getContentResolver(),
								om.getMediaID(), kind, opts
							);
				if(bmp != null) {
					//Logger.LogDebug("Bitmap is " + bmp.getWidth() + "x" + bmp.getHeight() + " to " + mWidth + "x" + mHeight);
					valid = true;
				} else Logger.LogError("Unable to create MediaStore thumbnail.");
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
						bmp = BitmapFactory.decodeStream(in);
						in.close();
						in = null;
					}
					if(!valid) {
						PackageManager man = mContext.getPackageManager();
						PackageInfo pinfo = man.getPackageArchiveInfo(file.getAbsolutePath(), PackageManager.GET_ACTIVITIES);
						ApplicationInfo ainfo = pinfo.applicationInfo;
						if(Build.VERSION.SDK_INT >= 8)
							ainfo.publicSourceDir = ainfo.sourceDir = file.getPath();
						Drawable mIcon = ainfo.loadIcon(man);
						if(mIcon != null)
							bmp = ((BitmapDrawable)mIcon).getBitmap();
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
								bmp = BitmapFactory.decodeStream(in);
								in.close();
								in = null;
							}
						}
					}
					if(bmp == null)
						bmp = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.apk);
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
			} else if (!valid && isImageFile(file.getPath())) {
				long len_kb = file.length() / 1024;
				
				BitmapFactory.Options options = new BitmapFactory.Options();
				options.outWidth = mWidth;
				options.outHeight = mHeight;
										
				if (len_kb > 500 && len_kb < 2000) {
					options.inSampleSize = 16;
					options.inPurgeable = true;						
					bmp = BitmapFactory.decodeFile(file.getPath(), options);
										
				} else if (len_kb >= 2000) {
					options.inSampleSize = 32;
					options.inPurgeable = true;
					bmp = BitmapFactory.decodeFile(file.getPath(), options);
									
				} else if (len_kb <= 500) {
					options.inPurgeable = true;
					bmp = BitmapFactory.decodeFile(file.getPath());
					
					if (bmp == null) 
						bmp = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.photo);
					
				}
			} else if (bmp == null && isVideoFile(file.getPath()))
			{
				Logger.LogDebug("Video File? " + file.getClass().getName());
				ContentResolver cr = mContext.getContentResolver();
				BitmapFactory.Options opts = new BitmapFactory.Options();
				opts.inSampleSize = 1;
				int kind = mWidth > 96 || mHeight > 96 ? MediaStore.Video.Thumbnails.MINI_KIND : MediaStore.Video.Thumbnails.MICRO_KIND;
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
						if(!mVideoCursor.moveToNext()) break;
						id = mVideoCursor.getInt(0);
						String name = mVideoCursor.getString(mVideoCursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME));
						if(name.equalsIgnoreCase(file.getName()))
							break;
					}
				} catch(Exception e) {
					Logger.LogError("Exception querying video thumbnail for " + file.getPath(), e);
				}
				if(id > -1)
					bmp = MediaStore.Video.Thumbnails.getThumbnail(cr, id, kind, opts);
				else
					bmp = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.movie);
			} else if (bmp == null && file.getClass().equals(OpenFile.class))
			{
				if(file.isDirectory())
					bmp = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.folder);
				else
					bmp = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.unknown);
			}
		}
		
		if(bmp != null && bmp.getWidth() > mWidth)
		{
			int h = (int) Math.floor(mWidth * ((double)bmp.getHeight() / (double)bmp.getWidth())); 
			bmp = Bitmap.createScaledBitmap(bmp, mWidth, h, false);
		}
		
		if(bmp != null)
		{
			if(writeCache)
				saveThumbnail(mCacheFilename, bmp);
			mCacheMap.put(mCacheFilename, bmp);
		}
		//Logger.LogDebug("Created " + bmp.getWidth() + "x" + bmp.getHeight() + " thumb (" + mWidth + "x" + mHeight + ")");
		return new SoftReference<Bitmap>(bmp);
	}
	
	private static Bitmap loadThumbnail(String file)
	{
		Bitmap ret = null;
		FileInputStream is = null;
		try {
			is = mContext.openFileInput(file);
			ret = BitmapFactory.decodeStream(is);
		} catch(FileNotFoundException e) {
		} catch(Exception e)
		{
			Logger.LogError("Unable to load bitmap.", e);
		} finally {
			if(is != null)
				try {
					is.close();
				} catch (IOException e) { }
		}
		return ret;
	}
	
	private static void saveThumbnail(String file, Bitmap bmp)
	{
		FileOutputStream os = null;
		try {
			os = mContext.openFileOutput(file, 0);
			bmp.compress(CompressFormat.PNG, 100, os);
		} catch(IOException e) {
			Logger.LogError("Unable to save thumbnail for " + file, e);
		} finally {
			if(os != null)
				try {
					os.close();
				} catch (IOException e) { }
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

	public static void flushCache() {
		Logger.LogInfo("Flushing" + mCacheMap.size() + " from memory & " + mContext.fileList().length + " from disk.");
		mCacheMap.clear();
		for(String s : mContext.fileList())
			mContext.deleteFile(s);
	}
}
