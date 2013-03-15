
package org.brandroid.openmanager.data;

import java.io.IOException;
import java.util.List;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.brandroid.openmanager.activities.OpenExplorer;
import org.brandroid.openmanager.util.PrivatePreferences;
import org.brandroid.utils.Logger;
import org.brandroid.utils.Utils;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.extensions.android.accounts.GoogleAccountManager;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.services.GoogleKeyInitializer;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

public class OpenDrive extends OpenNetworkPath {

    public final static String DRIVE_SCOPE_AUTH_TYPE = "oauth2:" + DriveScopes.DRIVE;

    private static Drive mGlobalDrive;
    private final GoogleCredential mCredential;
    private String mName = "Drive";
    private String mFolderId = "*";
    private final OpenDrive mParent;
    private final File mFile;
    private OpenDrive[] mChildren;

    public OpenDrive(String token)
    {
        mCredential = new GoogleCredential.Builder()
                .setClientSecrets(
                        PrivatePreferences.getKey("oauth_drive_client_id", ""),
                        PrivatePreferences.getKey("oauth_drive_secret", ""))
                .build();
        mCredential.setAccessToken(token);
        if (mGlobalDrive == null)
            mGlobalDrive = new Drive
                    .Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), mCredential)
                            .setApplicationName("OpenExplorer")
                            .build();
        mParent = null;
        mFile = null;
    }

    public OpenDrive(OpenDrive parent, File file)
    {
        mParent = parent;
        mCredential = mParent.mCredential;
        mFile = file;
        mFolderId = file.getId();
    }

    public interface OnAuthTokenListener extends OpenPath.ExceptionListener
    {
        public void onDriveAuthTokenReceived(String account, String token);
    }

    private static void postAuthCallback(final String account, final String token,
            final OnAuthTokenListener callback)
    {
        OpenExplorer.getHandler().post(new Runnable() {
            public void run() {
                callback.onDriveAuthTokenReceived(account, token);
            }
        });
    }

    public static void getAuthToken(final Activity activity, final OnAuthTokenListener callback,
            String accountName)
    {
        GoogleAccountManager accountManager = new GoogleAccountManager(activity);
        Account account = accountManager.getAccountByName(accountName);
        accountManager.getAccountManager()
                .getAuthToken(account, DRIVE_SCOPE_AUTH_TYPE, null, activity,
                        new AccountManagerCallback<Bundle>() {
                            public void run(AccountManagerFuture<Bundle> future) {
                                Bundle bundle = new Bundle();
                                try {
                                    bundle = future.getResult();
                                    getAuthToken(activity, callback, bundle);
                                } catch (Exception e) {
                                    callback.onException(e);
                                }
                            }

                        }, OpenExplorer.getHandler());
    }

    public static void getAuthToken(final Activity activity, final OnAuthTokenListener callback,
            Bundle bundle)
    {
        Toast.makeText(activity, "Auth Token? " + bundle, Toast.LENGTH_LONG).show();
        Logger.LogVerbose("Account Callback: " + bundle);
        if (bundle.containsKey(AccountManager.KEY_INTENT)) {
            Intent intent = (Intent)bundle.getParcelable(AccountManager.KEY_INTENT);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivityForResult(intent, OpenExplorer.REQ_AUTHENTICATE_DRIVE);
        } else if (bundle.containsKey(AccountManager.KEY_AUTHTOKEN)) {
            String account = "";
            if (bundle.containsKey(AccountManager.KEY_ACCOUNT_NAME))
                account = bundle.getString(AccountManager.KEY_ACCOUNT_NAME);
            String token = bundle.getString(AccountManager.KEY_AUTHTOKEN);
            postAuthCallback(account, token, callback);
        } else if (bundle.containsKey(AccountManager.KEY_ACCOUNT_NAME))
        {
            String accountName = bundle.getString(AccountManager.KEY_ACCOUNT_NAME);
            getAuthToken(activity, callback, accountName);
        }
    }

    public static void selectAccount(final Activity activity, final OnAuthTokenListener callback)
    {
        if (mGlobalDrive == null)
        {
            GoogleCredential credential = new GoogleCredential.Builder()
                    .setClientSecrets(
                            PrivatePreferences.getKey("oauth_drive_client_id", ""),
                            PrivatePreferences.getKey("oauth_drive_secret", "")
                    )
                    .build();
            String appName = activity.getApplicationInfo().name;
            try {
                appName += "/"
                        + activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionName;
            } catch (NameNotFoundException e) {
            }
            mGlobalDrive = new Drive.Builder(AndroidHttp.newCompatibleTransport(),
                    new GsonFactory(), credential)
                    .setApplicationName(appName)
                    .setHttpRequestInitializer(credential)
                    .build();
        }
        // new Thread(new Runnable() {
        // public void run() {
        GoogleAccountCredential acct = GoogleAccountCredential.usingOAuth2(activity,
                DriveScopes.DRIVE);
        Account[] accounts = acct.getAllAccounts();
        if (accounts.length > 0)
        {
            Account a = accounts[0];
            acct.getGoogleAccountManager()
                    .getAccountManager()
                    .getAuthToken(a, DRIVE_SCOPE_AUTH_TYPE, null, activity,
                            new AccountManagerCallback<Bundle>() {
                                public void run(AccountManagerFuture<Bundle> future) {
                                    Bundle bundle = new Bundle();
                                    try {
                                        bundle = future.getResult();
                                        getAuthToken(activity, callback, bundle);
                                    } catch (Exception e) {
                                        callback.onException(e);
                                    }
                                }

                            }, OpenExplorer.getHandler());
        }
        // activity.startActivityForResult(acct.newChooseAccountIntent(),
        // OpenExplorer.REQ_AUTHENTICATE_DRIVE);
        // }
        // }).start();
    }

    @Override
    public void setName(String name) {
        mName = name;
    }

    @Override
    public String getName() {
        if (mFile != null)
        {
            if (mFile.getDescription() != null)
                return mFile.getDescription();
            else if (mFile.getOriginalFilename() != null)
                return mFile.getOriginalFilename();
        }
        return mName;
    }

    @Override
    public boolean syncUpload(OpenFile f, NetworkListener l) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean syncDownload(OpenFile f, NetworkListener l) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public OpenNetworkPath[] getChildren() {
        return mChildren;
    }

    @Override
    public String getPath() {
        if (mFile != null)
            return "drive://drive.brandroid.org/" + mFile.getSelfLink();
        return "drive://drive.brandroid.org/" + mFolderId;
    }

    @Override
    public String getAbsolutePath() {
        return "drive://" + Utils.urlencode(mCredential.getServiceAccountUser()) + ":"
                + mCredential.getAccessToken() + "@drive.brandroid.org/" + mFolderId;
    }

    @Override
    public long length() {
        if (mFile != null)
            return mFile.getFileSize();
        return 0;
    }

    @Override
    public OpenPath getParent() {
        return mParent;
    }

    @Override
    public OpenPath getChild(String name) {
        return null;
    }

    @Override
    public OpenPath[] list() throws IOException {
        if (mChildren != null)
            return mChildren;
        return listFiles();
    }

    @Override
    public OpenPath[] listFiles() throws IOException {
        if (Thread.currentThread().equals(OpenExplorer.UiThread))
            return getChildren();
        List<OpenDrive> kids = new Vector<OpenDrive>();
        for (File f : mGlobalDrive.files().list()
                .setMaxResults(100)
                .execute().getItems())
        {
            OpenDrive kid = new OpenDrive(this, f);
            kids.add(kid);
        }
        mChildren = kids.toArray(new OpenDrive[kids.size()]);
        return mChildren;
    }

    @Override
    public Boolean isDirectory() {
        if (mFile != null)
            return false;
        return true;
    }

    @Override
    public Boolean isFile() {
        return !isDirectory();
    }

    @Override
    public Boolean isHidden() {
        return getName().startsWith(".");
    }

    @Override
    public Uri getUri() {
        return Uri.parse(getAbsolutePath());
    }

    @Override
    public Long lastModified() {
        if (mFile != null)
            return mFile.getModifiedDate().getValue();
        return null;
    }

    @Override
    public Boolean canRead() {
        return true;
    }

    @Override
    public Boolean canExecute() {
        return false;
    }

    @Override
    public Boolean exists() {
        return true;
    }

    @Override
    public Boolean delete() {
        return false;
    }

    @Override
    public Boolean mkdir() {
        return false;
    }

}
