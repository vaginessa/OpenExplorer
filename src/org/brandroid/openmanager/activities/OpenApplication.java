package org.brandroid.openmanager.activities;

import java.io.File;
import java.io.IOException;

import org.brandroid.openmanager.interfaces.OpenApp;
import org.brandroid.utils.DiskLruCache;

import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.DownloadCache;
import com.android.gallery3d.data.DownloadUtils;
import com.android.gallery3d.data.ImageCacheService;
import com.android.gallery3d.util.GalleryUtils;
import com.android.gallery3d.util.ThreadPool;

import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

public class OpenApplication extends Application implements OpenApp
{
    private static final String DOWNLOAD_FOLDER = "download";
    private static final long DOWNLOAD_CAPACITY = 64 * 1024 * 1024; // 64M

    private DataManager mDataManager;
    private ImageCacheService mImageCacheService;
    private ThreadPool mThreadPool;
    private DownloadCache mDownloadCache;
    private LruCache<String, Bitmap> mBitmapCache;
    private DiskLruCache mBitmapDiskCache;
    
    @Override
    public void onCreate() {
    	super.onCreate();
        GalleryUtils.initialize(this);
    }
    
    @Override
    public void onTerminate() {
    	super.onTerminate();
    	if(mBitmapCache != null)
    		mBitmapCache.evictAll();
    	if(mBitmapDiskCache != null)
			try {
				mBitmapDiskCache.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    }

    @Override
    public void onLowMemory() {
    	super.onLowMemory();
    	
    }

    public Context getAndroidContext() {
        return this;
    }

    public synchronized DataManager getDataManager() {
        if (mDataManager == null) {
            mDataManager = new DataManager(this);
            mDataManager.initializeSourceMap();
        }
        return mDataManager;
    }
    
    public synchronized ImageCacheService getImageCacheService() {
        if (mImageCacheService == null) {
            mImageCacheService = new ImageCacheService(getAndroidContext());
        }
        return mImageCacheService;
    }

    public synchronized ThreadPool getThreadPool() {
        if (mThreadPool == null) {
            mThreadPool = new ThreadPool();
        }
        return mThreadPool;
    }
    public synchronized DownloadCache getDownloadCache() {
        if (mDownloadCache == null) {
            File cacheDir = new File(getExternalCacheDir(), DOWNLOAD_FOLDER);

            if (!cacheDir.isDirectory()) cacheDir.mkdirs();

            if (!cacheDir.isDirectory()) {
                throw new RuntimeException(
                        "fail to create: " + cacheDir.getAbsolutePath());
            }
            mDownloadCache = new DownloadCache(this, cacheDir, DOWNLOAD_CAPACITY);
        }
        return mDownloadCache;
    }
}
