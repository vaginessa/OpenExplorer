package org.brandroid.openmanager.interfaces;

import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.DownloadCache;
import com.android.gallery3d.data.ImageCacheService;
import com.android.gallery3d.util.ThreadPool;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.os.Looper;

public interface OpenApp {
    public DataManager getDataManager();
    public ImageCacheService getImageCacheService();
    public DownloadCache getDownloadCache();
    public ThreadPool getThreadPool();

    public Context getAndroidContext();
    public Looper getMainLooper();
    public ContentResolver getContentResolver();
    public Resources getResources();
}
