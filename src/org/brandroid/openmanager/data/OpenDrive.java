
package org.brandroid.openmanager.data;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Vector;
import java.util.WeakHashMap;
import org.brandroid.openmanager.R;
import org.brandroid.openmanager.activities.OpenExplorer;
import org.brandroid.openmanager.activities.ServerSetupActivity;
import org.brandroid.openmanager.data.OpenPath.SpaceHandler;
import org.brandroid.openmanager.interfaces.OpenApp;
import org.brandroid.openmanager.util.IntentManager;
import org.brandroid.openmanager.util.PrivatePreferences;
import org.brandroid.openmanager.util.ThumbnailCreator;
import org.brandroid.utils.Logger;
import org.brandroid.utils.Utils;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Files;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.About;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.ParentReference;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;

public class OpenDrive extends OpenNetworkPath implements OpenNetworkPath.CloudOpsHandler,
        OpenPath.ThumbnailOverlayInterface, OpenPath.ThumbnailHandler, SpaceHandler,
        OpenPath.OpenPathUpdateHandler {

    public final static String DRIVE_SCOPE_AUTH_TYPE = "oauth2:" + DriveScopes.DRIVE;

    public Drive mDrive;
    private GoogleCredential mCredential;
    private String mName = null;
    private String mFolderId = "root";
    private OpenDrive mParent;
    private final File mFile;
    private static final WeakHashMap<String, OpenDrive> mGlobalFiles = new WeakHashMap<String, OpenDrive>();
    private static final WeakHashMap<String, List<OpenDrive>> mGlobalChildren = new WeakHashMap<String, List<OpenDrive>>();
    public static final HttpTransport mTransport = AndroidHttp.newCompatibleTransport();
    public static final JsonFactory mJsonFactory = new GsonFactory();
    public static final boolean DEBUG = OpenExplorer.IS_DEBUG_BUILD && true;

    public OpenDrive(String token)
    {
        mCredential = new GoogleCredential.Builder()
                .setClientSecrets(
                        PrivatePreferences.getKey("oauth_drive_client_id", ""),
                        PrivatePreferences.getKey("oauth_drive_secret", ""))
                .build();
        mCredential.setAccessToken(token);
        setCredential(mCredential);
        mParent = null;
        mFile = null;
        Logger.setHandler("com.google.api.client.http");
    }

    public OpenDrive(OpenDrive parent, File file)
    {
        mParent = parent;
        mCredential = mParent.mCredential;
        mFile = file;
        mFolderId = file.getId();
    }

    public GoogleCredential getCredential() {
        return mCredential;
    }

    public void setCredential(GoogleCredential cred) {
        mCredential = cred;
        mDrive = new Drive
                .Builder(mTransport, mJsonFactory, mCredential)
                        .setApplicationName("OpenExplorer/1.0")
                        .build();
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
            InputStream in = new BufferedInputStream(getInputStream());
            copyStreams(in, os);
            return true;
        } catch (Exception e) {
            l.OnNetworkFailure(this, f, e);
        }
        return false;
    }

    @Override
    public OpenNetworkPath[] getChildren() {
        if (mGlobalChildren != null && mGlobalChildren.containsKey(mFolderId))
            return mGlobalChildren.get(mFolderId).toArray(new OpenDrive[0]);
        return new OpenNetworkPath[0];
    }

    @Override
    public String getPath() {
        String ret = "";
        if(getParent() != null)
            ret = getParent().getPath();
        else ret = getPathPrefix(false);
        if(!ret.endsWith("/"))
            ret += "/";
        if(mFile != null)
            ret += mFile.getId();
        if(isDirectory() && !ret.endsWith("/"))
            ret += "/";
        return ret;
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
        return getPathPrefix(true) + getId();
    }

    @Override
    public long length() {
        if (mFile != null && mFile.getFileSize() != null)
            return mFile.getFileSize();
        return -1;
    }
    
    @Override
    public String getMimeType() {
        if(mFile != null && mFile.getMimeType() != null)
            return mFile.getMimeType();
        return super.getMimeType();
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
        if (mGlobalChildren != null)
            return getChildren();
        return listFiles();
    }

    @Override
    public OpenPath[] listFiles() throws IOException {
        if (Thread.currentThread().equals(OpenExplorer.UiThread))
            return getChildren();
        getOpenDrives(this, mDrive.files().list()
                .execute().getItems());
        return getChildren();
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
        if(mFile != null && Utils.isNullOrEmpty(mFile.getDownloadUrl()))
            return false;
        return true;
    }

    @Override
    public Boolean canWrite() {
        if (mFile != null)
            return mFile.getEditable();
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
    public boolean copyTo(OpenNetworkPath path, final CloudCompletionListener callback) {
        if (path instanceof OpenDrive)
        {
            final OpenDrive folder = (OpenDrive)path;
            thread(new Runnable() {
                public void run() {
                    try {
                        mDrive.files().copy(getFile().getId(), folder.getFile()).execute();
                        post(new Runnable() {
                            public void run() {
                                callback.onCloudComplete("Copy completed successfully");
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
            InputStream in = getInputStream();
            copyStreams(in, out);
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
                    mDrive.files().delete(getFile().getId()).execute();
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
        for (File f : files)
        {
            OpenDrive d = new OpenDrive(parent, f);
            d.cacheParents();
            if (d.getParent().equals(parent))
                ret.add(d);
        }
        return ret.toArray(new OpenDrive[ret.size()]);
    }

    public boolean hasParent(String folderId)
    {
        for (ParentReference p : getFile().getParents())
        {
            if (folderId.equals("root") && p.getIsRoot())
                return true;
            if (p.getId().equals(folderId))
                return true;
        }
        return false;
    }

    public void cacheParents()
    {
        boolean changedParent = false;
        for (ParentReference p : getFile().getParents())
        {
            String id = p.getId();
            // if(p.getIsRoot())
            // id = "root";
            if (!id.equals(mFolderId) && !(p.getIsRoot() && mFolderId.equals("root"))
                    && mGlobalFiles != null && mGlobalFiles.containsKey(id) && !changedParent)
            {
                if(DEBUG)
                    Logger.LogVerbose("Drive.setParent(" + mGlobalFiles.get(id) + ") from " + mParent);
                mParent = mGlobalFiles.get(id);
                changedParent = true;
            }
            List<OpenDrive> kids;
            if (!mGlobalChildren.containsKey(id) || mGlobalChildren.get(id) == null)
                kids = new Vector<OpenDrive>();
            else
                kids = mGlobalChildren.get(id);
            kids.add(this);
            mGlobalChildren.put(id, kids);
        }
    }

    @Override
    public Cancellable list(final OpenContentUpdateListener callback) {
        return cancelify(thread(new Runnable() {
            public void run() {
                try {
                    if (mFolderId.equals("root"))
                    {
                        mFolderId = mDrive.files().get(mFolderId).setFields("id").execute()
                                .getId();
                        Logger.LogDebug("Drive root = " + mFolderId);
                    }
                    String pg = "";
                    List<OpenDrive> kids = mGlobalChildren.get(mFolderId);
                    if (kids == null)
                        kids = new Vector<OpenDrive>();
                    else if (kids.size() > 0)
                        callback.addContentPath(kids.toArray(new OpenDrive[kids.size()]));
                    for (;;)
                    {
                        Files.List lst = mDrive
                                .files()
                                .list()
                                .setQ("'" + mFolderId + "' in parents")
                                .setFields(
                                        "items(downloadUrl,editable,fileSize,iconLink,thumbnailLink,id,kind,labels/hidden,mimeType,modifiedDate,parents(id,isRoot),thumbnail/image,title),nextPageToken")
                                .setMaxResults(100);
                        if (!pg.equals(""))
                            lst.setPageToken(pg);
                        final FileList fl = lst.execute();
                        if (fl.size() == 0)
                            break;
                        Vector<OpenDrive> theseKids = new Vector<OpenDrive>();
                        for (OpenDrive kid : getOpenDrives(OpenDrive.this, fl.getItems()))
                        {
                            if (kids.contains(kid))
                                continue;
                            else if (kid.hasParent(mFolderId))
                                theseKids.add(kid);
                        }
                        if (theseKids.size() > 0)
                        {
                            final OpenDrive[] ka = theseKids
                                    .toArray(new OpenDrive[theseKids.size()]);
                            post(new Runnable() {
                                public void run() {
                                    callback.addContentPath(ka);
                                }
                            });
                        }
                        if (pg.equals(fl.getNextPageToken()))
                            break;
                        pg = fl.getNextPageToken();
                        if (pg == null || pg.equals(""))
                            break;
                    }
                } catch (Exception e) {
                    postException(e, callback);
                }
            }
        }));
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
                                mDrive.files().list();
                        if (!pg.equals(""))
                            lst.setPageToken(pg);
                        final FileList fl = lst.execute();
                        added = fl.size();
                        getOpenDrives(OpenDrive.this, fl.getItems());
                        pg = fl.getNextPageToken();
                    } while (added > 0);
                    postListReceived(getChildren(), listener);
                } catch (Exception e) {
                    postException(e, listener);
                }
            }
        });
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if(DEBUG)
            Logger.LogVerbose("OpenDrive.getInputStream");
        InputStream in = null;
        String url = mFile.getDownloadUrl();
        if(url == null || url.length() == 0) return in;
        return new BufferedInputStream(mDrive.getRequestFactory()
                    .buildGetRequest(new GenericUrl(url))
                .execute().getContent());
    }

    @Override
    public boolean copyFrom(final OpenFile f, AsyncTask task) {
        if(DEBUG)
            Logger.LogVerbose("OpenDrive.copyFrom");
        try {
            final InputStream in = new BufferedInputStream(f.getInputStream());
            mDrive.files().insert(getFile(), new AbstractInputStreamContent(getExtension()) {
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
        if(DEBUG)
            Logger.LogVerbose("OpenDrive.getOutputStream");
        return null;
    }
    
    public String getId()
    {
        if(mFile != null)
            return mFile.getId();
        return mFolderId;
    }
    
    public OpenDrive setId(String id)
    {
        if(id.indexOf("@") > -1) return this;
        if(id.equals(mName)) return this;
        if(DEBUG)
            Logger.LogVerbose("Drive.setId(" + id + ") on " + mFolderId);
        mFolderId = id;
        return this;
    }
    
    @Override
    public boolean hasThumbnail() {
        if(mFile != null && !Utils.isNullOrEmpty(mFile.getThumbnailLink()))
            return true;
        else return false;
    }
    
    @Override
    public String getThumbnailCacheFilename(int w) {
        if(hasThumbnail())
            return ThumbnailCreator.getCacheFilename(getThumbnailUrl(), w, w);
        else return super.getThumbnailCacheFilename(w);
    }
    
    public String getThumbnailUrl() {
        if(!hasThumbnail()) return null;
        String url = mFile.getThumbnailLink();
        if(Utils.isNullOrEmpty(url))
            url = mFile.getIconLink();
        return url;
    }
    
    @Override
    public boolean getThumbnail(final OpenApp app, final int w, final ThumbnailReturnCallback callback) {
        if(DEBUG)
            Logger.LogVerbose("OpenDrive.getThumbnail");
        if(hasThumbnail())
        thread(new Runnable() {
            public void run() {
                try {
                    InputStream in = new BufferedInputStream(mDrive.getRequestFactory()
                            .buildGetRequest(new GenericUrl(getThumbnailUrl()))
                            .execute().getContent());
                    callback.onThumbReturned(new BitmapDrawable(app.getResources(), in));
                } catch (Exception e) {
                    postException(e, callback);
                }
            }
        });
        else
            callback.onThumbReturned(IntentManager.getDefaultIcon(this, app));
        return true;
    }

    @Override
    public Cancellable uploadToCloud(final OpenFile file, final CloudProgressListener callback) {
        if(DEBUG)
            Logger.LogVerbose("OpenDrive.uploadToCloud");
        return runCloud(new Runnable() {
            public void run() {
                try {
                    final AbstractInputStreamContent input = new AbstractInputStreamContent(
                            "*/*") {

                        @Override
                        public boolean retrySupported() {
                            return false;
                        }

                        @Override
                        public long getLength() throws IOException {
                            return file.length();
                        }

                        @Override
                        public InputStream getInputStream() throws IOException {
                            return file.getInputStream();
                        }
                    };
                    mDrive.files().update(getId(), getFile(), input).execute();
                    postCompletion("Upload complete", callback);
                } catch (Exception e) {
                    postException(e, callback);
                }
            }
        }, callback);
    }

    @Override
    public Cancellable downloadFromCloud(final OpenFile file, final CloudProgressListener callback) {
        if(DEBUG)
            Logger.LogVerbose("OpenDrive.downloadFromCloud");
        return runCloud(new Runnable() {
            public void run() {
                try {
                    String url = mFile.getDownloadUrl();
                    if(url == null || url.length() == 0) {
                        callback.onException(new Exception("Bad Download URL"));
                        return;
                    }
                    copyStreams(getInputStream(), file.getOutputStream(), true, true, new ProgressUpdateListener() {
                        public boolean isCancelled() {
                            return false;
                        }
                        public void onProgressUpdate(Integer... progress) {
                            if(progress.length > 0)
                                callback.onProgress((long)progress[0]);
                        }
                    });
                } catch (Exception e) {
                    postException(e, callback);
                }
            }
        }, callback);
    }

    @Override
    public boolean touch(final CloudCompletionListener callback) {
        thread(new Runnable() {
            public void run() {
                try {
                    mDrive.files().touch(getId());
                    postCompletion("File touched", callback);
                } catch (Exception e) {
                    postException(e, callback);
                }
            }
        });
        return true;
    }
    
    @Override
    public void getSpace(final SpaceListener callback) {
        final OpenServer server = getServer();
        if(server != null && server.has("total"))
        {
            callback.onSpaceReturned(server.get("total", 0),
                    server.get("agg", 0), 
                    server.get("used", 0));
            return;
        }
        thread(new Runnable() {
            public void run() {
                try {
                    About me = mDrive.about().get().execute();
                    if(server != null)
                    {
                        server
                            .setSetting("total", me.getQuotaBytesTotal())
                            .setSetting("agg", me.getQuotaBytesUsedAggregate())
                            .setSetting("used", me.getQuotaBytesUsed());
                    }
                    callback.onSpaceReturned(
                            me.getQuotaBytesTotal(),
                            me.getQuotaBytesUsedAggregate(),
                            me.getQuotaBytesUsed());
                } catch(Exception e) {
                    postException(e, callback);
                }
            }
        });
    }

}
