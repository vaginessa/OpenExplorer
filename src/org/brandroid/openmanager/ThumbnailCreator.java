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
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.brandroid.openmanager.data.OpenFace;
import org.brandroid.utils.Decoder;
import org.brandroid.utils.Logger;

public class ThumbnailCreator extends Thread {
	private int mWidth;
	private int mHeight;
	private SoftReference<Bitmap> mThumb;
	private static HashMap<String, BitmapDrawable> mCacheMap = null;	
	private ArrayList<OpenFace> mFiles;
	
	private Context mContext;
	private String mDir;
	private Handler mHandler;
	private boolean mStop = false;

	public ThumbnailCreator(Context context, int width, int height) {
		mHeight = height;
		mWidth = width;
		mContext = context;
		
		if(mCacheMap == null)
			mCacheMap = new HashMap<String, BitmapDrawable>();
	}
	
	public BitmapDrawable isBitmapCached(String name) {
		return mCacheMap.get(name);
	}
	
	public void createNewThumbnail(ArrayList<OpenFace> files,  String dir,  Handler handler) {
		this.mFiles = files;
		this.mDir = dir;
		this.mHandler = handler;		
	}
	
	public void setCancelThumbnails(boolean stop) {
		mStop = stop;
	}

	
	public void run() {
		int len = mFiles.size();
		
		for (int i = 0; i < len; i++) {	
			if (mStop) {
				mStop = false;
				mFiles = null;
				return;
			}
			
			final File file = new File(mDir + "/" + mFiles.get(i));
			
			//we already loaded this thumbnail, just return it.
			if (mCacheMap.containsKey(file.getPath())) {
				mHandler.post(new Runnable() {
					
					public void run() {
						Message msg = mHandler.obtainMessage();
						msg.obj = mCacheMap.get(file.getPath());
						msg.sendToTarget();
					}
				});
			
			//we havn't loaded it yet, lets make it. 
			} else {
				if (isAPKFile(file.getName()))
				{
					JarFile apk = null;
					InputStream in = null;
					try {
						apk = new JarFile(file);
						JarEntry icon = apk.getJarEntry("res/drawable-hdpi/icon.apk");
						Boolean valid = false;
						if(icon != null && icon.getSize() > 0) {
							in = apk.getInputStream(icon);
							Bitmap pic = BitmapFactory.decodeStream(in);
							in.close();
							in = null;
							if(pic == null)
								continue;
							mThumb = new SoftReference<Bitmap>(Bitmap.createScaledBitmap(pic, mWidth, mHeight, false));
							sendThumbBack(mThumb, file.getPath());
							valid = true;
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
									sendThumbBack(mThumb, file.getAbsolutePath());
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
									sendThumbBack(mThumb, file.getPath());
									valid = true;
									break;
								}
							}
						}
						if(!valid)
						{
							mThumb = new SoftReference<Bitmap>(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.apk));
							sendThumbBack(mThumb, file.getPath());
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
				}
				if (isImageFile(file.getName())) {
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

					sendThumbBack(mThumb, file.getPath());
				}
			}
		}
	}
	
	
	private void sendThumbBack(SoftReference<Bitmap> mThumb, String path)
	{
		final BitmapDrawable d = new BitmapDrawable(mThumb.get());					
		d.setGravity(Gravity.CENTER);
		mCacheMap.put(path, d);
		
		mHandler.post(new Runnable() {
			
			public void run() {
				Message msg = mHandler.obtainMessage();
				msg.obj = (BitmapDrawable)d;
				msg.sendToTarget();
			}
		});
	}
	
	private boolean isImageFile(String file) {
		String ext = file.substring(file.lastIndexOf(".") + 1);
		
		if (ext.equalsIgnoreCase("png") || ext.equalsIgnoreCase("jpg") ||
			ext.equalsIgnoreCase("jpeg")|| ext.equalsIgnoreCase("gif") ||
			ext.equalsIgnoreCase("tiff")|| ext.equalsIgnoreCase("tif"))
			return true;
		
		return false;
	}
	
	private boolean isAPKFile(String file) {
		String ext = file.substring(file.lastIndexOf(".") + 1);
		
		if (ext.equalsIgnoreCase("apk"))
			return true;
		
		return false;
	}
}
