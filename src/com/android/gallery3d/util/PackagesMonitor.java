package com.android.gallery3d.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class PackagesMonitor extends BroadcastReceiver {
    public static final String KEY_PACKAGES_VERSION  = "packages-version";

    public synchronized static int getPackagesVersion(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getInt(KEY_PACKAGES_VERSION, 1);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        int version = prefs.getInt(KEY_PACKAGES_VERSION, 1);
        prefs.edit().putInt(KEY_PACKAGES_VERSION, version + 1).commit();

        /*
        String action = intent.getAction();
        String packageName = intent.getData().getSchemeSpecificPart();
        if (Intent.ACTION_PACKAGE_ADDED.equals(action)) {
            PicasaSource.onPackageAdded(context, packageName);
        } else if (Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
            PicasaSource.onPackageRemoved(context, packageName);
        }
        */
    }
}
