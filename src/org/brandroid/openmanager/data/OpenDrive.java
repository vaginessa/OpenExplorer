
package org.brandroid.openmanager.data;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.brandroid.openmanager.R;
import org.brandroid.openmanager.activities.OpenExplorer;
import org.brandroid.openmanager.util.PrivatePreferences;
import org.brandroid.utils.Utils;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.CredentialRefreshListener;
import com.google.api.client.auth.oauth2.TokenErrorResponse;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.ChildReference;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.ParentReference;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;

public class OpenDrive extends OpenNetworkPath implements OpenNetworkPath.CloudOpsHandler,
        OpenPath.ThumbnailOverlayInterface, OpenPath.ThumbnailHandler,
        OpenPath.OpenPathUpdateHandler {

    public final static String DRIVE_SCOPE_AUTH_TYPE = "oauth2:" + DriveScopes.DRIVE;

    public static Drive mGlobalDrive;
    private GoogleCredential mCredential;
    private String mName = "Drive";
    private String mFolderId = "";
    private final OpenDrive mParent;
    private final File mFile;
    private final ParentReference mFolder;
    private OpenDrive[] mChildren;
    public static final HttpTransport mTransport = AndroidHttp.newCompatibleTransport();
    public static final JsonFactory mJsonFactory = new GsonFactory();

    static {
        if (OpenExplorer.IS_DEBUG_BUILD)
            Logger.getLogger(HttpTransport.class.getName()).setLevel(Level.ALL);
    }

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
                    .Builder(mTransport, mJsonFactory, mCredential)
                            .setApplicationName("OpenExplorer/1.0")
                            .setHttpRequestInitializer(mCredential)
                            .build();
        mParent = null;
        mFile = null;
        mFolder = null;
    }

    public GoogleCredential getCredential() {
        return mCredential;
    }

    public void setCredential(GoogleCredential cred) {
        mCredential = cred;
        if (mGlobalDrive == null)
            mGlobalDrive = new Drive
                    .Builder(mTransport, mJsonFactory, mCredential)
                            .setApplicationName("OpenExplorer/1.0")
                            .setHttpRequestInitializer(mCredential)
                            .build();
    }

    public OpenDrive(OpenDrive parent, File file)
    {
        mParent = parent;
        mCredential = mParent.mCredential;
        mFile = file;
        mFolderId = file.getId();
        mFolder = null;
    }

    public OpenDrive(OpenDrive parent, ParentReference pr)
    {
        mParent = parent;
        mCredential = mParent.mCredential;
        mFile = null;
        mFolder = pr;
        mFolderId = pr.getId();
    }

    @Override
    public void setName(String name) {
        mName = name;
    }

    @Override
    public String getName() {
        if (mFile != null)
        {
            if (mFile.getTitle() != null)
                return mFile.getTitle();
            if (mFile.getDescription() != null)
                return mFile.getDescription();
            if (mFile.getOriginalFilename() != null)
                return mFile.getOriginalFilename();
        }
        return mName;
    }

    @Override
    public boolean syncUpload(OpenFile f, NetworkListener l) {
        return false;
    }

    @Override
    public boolean syncDownload(OpenFile f, NetworkListener l) {
        try {
            OutputStream os = new BufferedOutputStream(f.getOutputStream());
            mGlobalDrive.files().get(getFile().getId()).executeAndDownloadTo(os);
            return true;
        } catch (Exception e) {
            l.OnNetworkFailure(this, f, e);
        }
        return false;
    }

    @Override
    public OpenNetworkPath[] getChildren() {
        return mChildren;
    }

    @Override
    public String getPath() {
        if (mFile != null)
            return getPathPrefix(false) + mFile.getSelfLink();
        return getPathPrefix(false) + mFolderId;
    }

    public String getPathPrefix(boolean includeToken)
    {
        String ret = "drive://";
        if (mCredential != null && mCredential.getServiceAccountUser() != null)
            ret += Utils.urlencode(mCredential.getServiceAccountUser()) + ":";
        if (includeToken && mCredential != null)
            ret += mCredential.getAccessToken() + "@";
        ret += "drive.brandroid.org/";
        return ret;
    }

    @Override
    public String getAbsolutePath() {
        if (mFile != null)
            return getPathPrefix(true) + mFile.getSelfLink();
        return getPathPrefix(true) + mFolderId;
    }

    @Override
    public long length() {
        if (mFile != null && mFile.getFileSize() != null)
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
        {
            if (mFile.getMimeType().equals("application/vnd.google-apps.folder"))
                return true;
            return false;
        }
        return true;
    }

    @Override
    public Boolean isFile() {
        return !isDirectory();
    }

    @Override
    public Boolean isHidden() {
        if (mFile != null)
            if (mFile.getLabels() != null)
                return mFile.getLabels().getHidden();
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

    @Override
    public Drawable getOverlayDrawable(Context c, boolean large) {
        return c.getResources().getDrawable(
                large ? R.drawable.lg_drive_overlay : R.drawable.sm_drive_overlay);
    }

    public File getFile() {
        return mFile;
    }

    @Override
    public boolean copyTo(OpenNetworkPath path, final CloudCopyListener callback) {
        if (path instanceof OpenDrive)
        {
            final OpenDrive folder = (OpenDrive)path;
            thread(new Runnable() {
                public void run() {
                    try {
                        mGlobalDrive.files().copy(getFile().getId(), folder.getFile()).execute();
                        post(new Runnable() {
                            public void run() {
                                callback.onCopyComplete("Copy completed successfully");
                            }
                        });
                    } catch (Exception e) {
                        postException(e, callback);
                    }
                }
            });
        }
        return false;
    }

    @Override
    public boolean copyTo(OpenFile f, AsyncTask task) {
        OutputStream out = null;
        try {
            out = new BufferedOutputStream(f.getOutputStream());
            mGlobalDrive.files().get(mFile.getId()).executeAndDownloadTo(out);
            return true;
        } catch (Exception e) {
            org.brandroid.utils.Logger.LogError("Unable to download Drive file!", e);
            return false;
        } finally {
            if (out != null)
                try {
                    out.close();
                } catch (IOException e) {
                }
        }
    }

    @Override
    public boolean delete(final CloudDeleteListener callback) {
        thread(new Runnable() {
            public void run() {
                try {
                    mGlobalDrive.files().delete(getFile().getId()).execute();
                    post(new Runnable() {
                        public void run() {
                            callback.onDeleteComplete("Delete complete.");
                        }
                    });
                } catch (Exception e) {
                    postException(e, callback);
                }
            }
        });
        return false;
    }
    
    private static OpenDrive[] getOpenDrives(OpenDrive parent, List<File> files)
    {
        List<OpenDrive> ret = new Vector<OpenDrive>();
        for(File f : files)
            ret.add(new OpenDrive(parent, f));
        return ret.toArray(new OpenDrive[ret.size()]);
    }

    @Override
    public void list(final OpenContentUpdateListener callback) throws IOException {
        thread(new Runnable() {
            public void run() {
                try {
                    String pg = "";
                    for (;;)
                    {
                        com.google.api.services.drive.Drive.Files.List lst =
                                mGlobalDrive.files().list().setMaxResults(100);
                        if(!pg.equals(""))
                            lst.setPageToken(pg);
                        final FileList fl = lst.execute();
                        if(fl.size() == 0) break;
                        post(new Runnable() {
                            public void run() {
                                callback.addContentPath(getOpenDrives(OpenDrive.this, fl.getItems()));
                            }
                        });
                        if(pg.equals(fl.getNextPageToken())) break;
                        pg = fl.getNextPageToken();
                        if(pg == null || pg.equals("")) break;
                    }
                } catch (Exception e) {
                    postException(e, callback);
                }
            }
        });
    }

    public Thread list(final ListListener listener) {
        return thread(new Runnable() {
            public void run() {
                try {
                    List<OpenDrive> kids = new Vector<OpenDrive>();
                    int added = 0;
                    String pg = "";
                    do {
                        com.google.api.services.drive.Drive.Files.List lst =
                                mGlobalDrive.files().list();
                        if(!pg.equals(""))
                            lst.setPageToken(pg);
                        final FileList fl = lst.execute();
                        added = fl.size();
                        for (File f : fl.getItems())
                            kids.add(new OpenDrive(OpenDrive.this, f));
                        pg = fl.getNextPageToken();
                    } while (added > 0);
                    mChildren = kids.toArray(new OpenDrive[kids.size()]);
                    postListReceived(mChildren, listener);
                } catch (Exception e) {
                    postException(e, listener);
                }
            }
        });
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new BufferedInputStream(mGlobalDrive.files().get(getFile().getId())
                .executeAsInputStream());
    }

    @Override
    public boolean copyFrom(final OpenFile f, AsyncTask task) {
        try {
            final InputStream in = new BufferedInputStream(f.getInputStream());
            mGlobalDrive.files().insert(getFile(), new AbstractInputStreamContent(getExtension()) {
                public boolean retrySupported() {
                    return true;
                }

                public long getLength() throws IOException {
                    return f.length();
                }

                public InputStream getInputStream() throws IOException {
                    return in;
                }
            }).execute();
            return true;
        } catch (Exception e) {

        }
        return super.copyFrom(f, task);
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return null;
    }

    @Override
    public boolean getThumbnail(int w, final ThumbnailReturnCallback callback) {
        thread(new Runnable() {
            public void run() {
                try {
                    InputStream in = new BufferedInputStream(mGlobalDrive.files()
                            .get(mFile.getThumbnail().getImage()).executeMediaAsInputStream());
                    Bitmap bmp = BitmapFactory.decodeStream(in);
                    callback.onThumbReturned(bmp);
                } catch (Exception e) {
                    postException(e, callback);
                }
            }
        });
        return false;
    }

}
