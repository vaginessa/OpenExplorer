package org.brandroid.openmanager.data;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.brandroid.openmanager.activities.OpenExplorer;
import org.brandroid.openmanager.adapters.OpenPathDbAdapter;
import org.brandroid.openmanager.util.SortType;
import org.brandroid.utils.Logger;

import android.database.Cursor;
import android.net.Uri;

public class OpenFTP2 extends OpenNetworkPath implements OpenPath.ListHandler, OpenPath.OpsHandler,
        OpenPath.DownloadHandler, OpenPath.UploadHandler, OpenPath.OpenStream {
    private static final long serialVersionUID = -8062167915444507685L;
    private final FTPFile mFile;
    private final FTPManager mManager;
    private OpenFTP2[] mChildren = null;
    private final OpenFTP2 mParent;
    private Thread mThread;

    private final String mPath;

    public OpenFTP2(FTPManager manager)
    {
        mPath = manager.getBasePath();
        mManager = manager;
        mFile = null;
        mParent = null;
    }

    public OpenFTP2(OpenFTP2 parent, FTPFile child)
    {
        mManager = parent.mManager;
        mPath = parent.mPath + (parent.mPath.endsWith("/") ? "" : "/") + child.getName();
        mFile = child;
        mParent = parent;
    }

    public void upload(final UploadListener callback)
    {
        mThread = new Thread(new Runnable() {
            public void run() {
                try {
                    connect();
                    InputStream in = callback.getInputStream();
                    OutputStream out = mManager.getOutputStream(getBasePath());
                    copyStreams(in, out, true, true, callback);
                } catch (final Exception e) {
                    OpenExplorer.getHandler().post(new Runnable() {
                        public void run() {
                            callback.onException(e);
                        }
                    });
                }
            }
        });
    }

    @Override
    public void download(final DownloadListener callback) {
        mThread = new Thread(new Runnable() {
            public void run() {
                try {
                    connect();
                    InputStream in = mManager.getInputStream(getBasePath());
                    OutputStream out = callback.getOutputStream();
                    copyStreams(in, out, true, true, callback);
                } catch (final Exception e) {
                    OpenExplorer.getHandler().post(new Runnable() {
                        public void run() {
                            callback.onException(e);
                        }
                    });
                }
            }
        });
        mThread.start();
    }
    
    @Override
    public void connect() throws IOException {
        mManager.connect();
    }
    
    @Override
    public void list(final OpenPath.ListListener callback) {
        mThread = new Thread(new Runnable() {
            public void run() {
                try {
                    connect();
                } catch(Exception e) { }
                try {
                    listFiles();
                    OpenExplorer.getHandler().post(new Runnable() {
                        public void run() {
                            callback.onListReceived(mChildren);
                        }
                    });
                } catch (final Exception e) {
                    OpenExplorer.getHandler().post(new Runnable() {
                        public void run() {
                            callback.onException(e);
                        }
                    });
                }
            }
        });
        mThread.start();
    }
    
    public FTPManager getManager() { return mManager; }

    @Override
    public boolean listFromDb(SortType sort) {
        if (!OpenPath.AllowDBCache)
            return false;
        String folder = getPath(); // .replace("/" + getName(), "");
        if (!isDirectory())
            folder = folder.replace("/" + getName(), "");
        if (!folder.endsWith("/"))
            folder += "/";
        if (folder.endsWith("//"))
            folder = folder.substring(0, folder.length() - 1);
        Cursor c = mDb.fetchItemsFromFolder(folder, sort);
        if (c == null)
            return false;
        if (c.getCount() == 0) {
            c.close();
            c = mDb.fetchItemsFromFolder(folder.substring(0, folder.length() - 1), sort);
        }
        List<OpenFTP2> kids = new ArrayList<OpenFTP2>();
        c.moveToFirst();
        while (!c.isAfterLast()) {
            // String folder =
            // c.getString(OpenPathDbAdapter.getKeyIndex(OpenPathDbAdapter.KEY_FOLDER));
            String name = c.getString(OpenPathDbAdapter.getKeyIndex(OpenPathDbAdapter.KEY_NAME));
            int size = c.getInt(OpenPathDbAdapter.getKeyIndex(OpenPathDbAdapter.KEY_SIZE));
            int modified = c.getInt(OpenPathDbAdapter.getKeyIndex(OpenPathDbAdapter.KEY_MTIME));
            String path = folder + name;
            int atts = c.getInt(OpenPathDbAdapter.getKeyIndex(OpenPathDbAdapter.KEY_ATTRIBUTES));
            if ((atts & 2) == 2 && !path.endsWith("/"))
                path += "/";
            if (path.endsWith("//"))
                path = path.substring(0, path.length() - 1);
            FTPFile file = new FTPFile();
            file.setSize(size);
            file.setTimestamp(Calendar.getInstance());
            file.setName(path);
            OpenFTP2 child = new OpenFTP2(this, file);
            kids.add(child);
            c.moveToNext();
        }
        mChildren = kids.toArray(new OpenFTP2[kids.size()]);
        c.close();
        return true;
    }

    @Override
    public String getName() {
        if (mFile != null)
            return mFile.getName() + (isDirectory() && !mFile.getName().endsWith("/") ? "/" : "");
        return mManager.getHost();
    }

    public String getPath() {
        return "ftp://" + mManager.getHost() + getBasePath();
    }

    @Override
    public String getAbsolutePath() {
        return "ftp://" + mManager.getUser() + ":" + mManager.getPassword() + "@"
                + mManager.getHost() + getBasePath();
    }

    @Override
    public long length() {
        if (mFile != null)
            return mFile.getSize();
        return 0;
    }

    @Override
    public OpenPath getParent() {
        return mParent;
    }

    @Override
    public OpenPath getChild(String name) {
        try {
            for (OpenPath kid : listFiles())
                if (kid.getName().equals(name))
                    return kid;
            if(name.endsWith("/"))
                mManager.mkdir(name);
            else
                mManager.touch(name);
            for (OpenPath kid : list())
                if (kid.getName().equals(name))
                    return kid;
            return null;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
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
        if(OpenExplorer.UiThread.equals(Thread.currentThread())) return mChildren;
        List<OpenFTP2> kids = new Vector<OpenFTP2>();
        String path = getBasePath();
        mManager.cd(path);
        mManager.setBasePath(path);
        FTPFile[] arr = mManager.listFiles(path);
        for (FTPFile f : arr)
            kids.add(new OpenFTP2(OpenFTP2.this, f));
        Collections.sort(kids);
        mChildren = kids.toArray(new OpenFTP2[kids.size()]);
        return mChildren;
    }
    
    private String getBasePath()
    {
        String path = mPath;
        if(!path.startsWith("/"))
            path = "/" + path;
        return path;
    }

    @Override
    public Boolean isDirectory() {
        if (mParent == null)
            return true;
        if (mFile == null)
            return true;
        return mFile.isDirectory();
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
            return mFile.getTimestamp().getTimeInMillis();
        return null;
    }

    @Override
    public Boolean canRead() {
        return true;
    }

    @Override
    public Boolean canWrite() {
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
    public InputStream getInputStream() throws IOException {
        return new BufferedInputStream(mManager.getInputStream(getBasePath()));
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return new BufferedOutputStream(mManager.getOutputStream(getBasePath()));
    }

    @Override
    public void makeChild(final String name, final OpsListener listener) {
        mThread = new Thread(new Runnable() {
            public void run() {
                try {
                    final boolean ret = (name.endsWith("/") ?
                            mManager.mkdir(name) != 0 :
                            mManager.touch(name));
                    OpenExplorer.getHandler().post(new Runnable() {
                        public void run() {
                            listener.onMakeFinished(name, ret);
                        }
                    });
                } catch (Exception e) {
                    postException(e, listener);
                }
            }
        });
        mThread.start();
    }

    @Override
    public void delete(final OpsListener listener) {
        mThread = new Thread(new Runnable() {
            public void run() {
                try {
                    final boolean ret = mManager.delete(getBasePath());
                    OpenExplorer.getHandler().post(new Runnable() {
                        public void run() {
                            listener.onDeleteFinished(ret);
                        }
                    });
                } catch (Exception e) {
                    postException(e, listener);
                }
            }
        });
        mThread.start();
    }
    
    @Override
    public boolean isConnected() throws IOException {
        return mManager.isConnected();
    }

    @Override
    public OpenNetworkPath[] getChildren() {
        return mChildren;
    }

    @Override
    public boolean syncUpload(OpenFile f, NetworkListener l) {
        Logger.LogDebug("OpenFTP.copyFrom(" + f + ")");
        InputStream is = null;
        try {
            connect();
            is = f.getInputStream();
            FTPClient client = mManager.getClient();
            client.cwd(getParent().getUri().getPath());
            boolean ret = client.storeFile(getName(), is);
            if (!ret)
                throw new IOException("Unable to upload to FTP.");
            if (l != null)
                l.OnNetworkCopyFinished(this, f);
            return ret;
        } catch (Exception e) {
            if (l != null)
                l.OnNetworkFailure(this, f, e);
            return false;
        } finally {
            if (is != null)
                try {
                    is.close();
                } catch (IOException e) {
                }
        }
    }

    @Override
    public boolean syncDownload(OpenFile f, NetworkListener l) {
        OutputStream os = null;
        Logger.LogDebug("OpenFTP.copyTo(" + f + ")");
        try {
            connect();
            os = f.getOutputStream();
            FTPClient client = mManager.getClient();
            client.cwd(getParent().getUri().getPath());
            boolean ret = client.retrieveFile(getName(), os);
            if (!ret)
                throw new IOException("Unable to download from FTP.");
            if (l != null)
                l.OnNetworkCopyFinished(this, f);
            return ret;
        } catch (Exception e) {
            if (l != null)
                l.OnNetworkFailure(this, f, e);
            return false;
        } finally {
            if (os != null)
                try {
                    os.close();
                } catch (IOException e) {
                }
        }
    }

}
