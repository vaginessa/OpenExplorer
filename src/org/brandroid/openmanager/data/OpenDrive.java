
package org.brandroid.openmanager.data;

import java.io.IOException;
import java.util.List;
import java.util.Vector;

import org.brandroid.openmanager.activities.OpenExplorer;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.extensions.android.accounts.GoogleAccountManager;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;

import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

public class OpenDrive extends OpenNetworkPath {

    private Drive mDrive;
    private final GoogleCredential mCredential;
    private String mName = "Drive";
    private String mFolderId = "0";
    private final OpenDrive mParent;
    private final File mFile;
    private OpenDrive[] mChildren;

    public OpenDrive(String token)
    {
        mCredential = new GoogleCredential();
        mCredential.setAccessToken(token);
        mDrive = new Drive.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(),
                mCredential)
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
        public void onAuthTokenReceived(String token);
    }

    private static void postAuthCallback(final String token, final OnAuthTokenListener callback)
    {
        OpenExplorer.getHandler().post(new Runnable() {
            public void run() {
                callback.onAuthTokenReceived(token);
            }
        });
    }

    public static void selectAccount(final Activity activity, final OnAuthTokenListener callback)
    {
        new Thread(new Runnable() {
            public void run() {
                new GoogleAccountManager(activity).getAccountManager()
                        .getAuthTokenByFeatures(GoogleAccountManager.ACCOUNT_TYPE,
                                "oauth2:" + DriveScopes.DRIVE, null,
                                activity, null, null, new AccountManagerCallback<Bundle>() {
                                    public void run(AccountManagerFuture<Bundle> future) {
                                        try {
                                            Bundle bundle = future.getResult();
                                            if (bundle.containsKey(AccountManager.KEY_INTENT))
                                            {
                                                Intent intent = (Intent)bundle
                                                        .getParcelable(AccountManager.KEY_INTENT);
                                                activity.startActivityForResult(intent,
                                                        OpenExplorer.REQ_AUTHENTICATE_DRIVE);
                                            } else if (bundle
                                                    .containsKey(AccountManager.KEY_AUTHTOKEN))
                                            {
                                                postAuthCallback(bundle
                                                        .getString(AccountManager.KEY_AUTHTOKEN),
                                                        callback);
                                            }
                                        } catch (OperationCanceledException e) {
                                            // TODO Auto-generated catch block
                                            e.printStackTrace();
                                        } catch (AuthenticatorException e) {
                                            // TODO Auto-generated catch block
                                            e.printStackTrace();
                                        } catch (IOException e) {
                                            // TODO Auto-generated catch block
                                            e.printStackTrace();
                                        }
                                    }
                                }, OpenExplorer.getHandler());
            }
        }).start();
    }

    @Override
    public void setName(String name) {
        mName = name;
    }

    @Override
    public String getName() {
        if (mFile != null)
            return mFile.getDescription();
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
            return mFile.getSelfLink();
        return null;
    }

    @Override
    public String getAbsolutePath() {
        return null;
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
        for (File f : mDrive.files().list().setQ("\"" + mFolderId + "\" in parents").execute()
                .getItems())
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
