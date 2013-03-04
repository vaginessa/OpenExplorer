
package org.brandroid.openmanager.data;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import org.apache.commons.net.ftp.FTPFile;
import org.brandroid.openmanager.activities.OpenExplorer;
import android.net.Uri;

public class OpenFTP2 extends OpenPath implements OpenPath.ListHandler, OpenPath.OpsHandler,
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
                    OutputStream out = mManager.getOutputStream(mPath);
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
                    InputStream in = mManager.getInputStream(mPath);
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

    private void connect() throws IOException
    {
        mManager.connect();
    }

    @Override
    public void list(final OpenPath.ListListener callback) {
        if (mChildren != null)
            callback.onListReceived(mChildren);
        mThread = new Thread(new Runnable() {
            public void run() {
                try {
                    connect();
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

    @Override
    public String getName() {
        if (mFile != null)
            return mFile.getName() + (isDirectory() && !mFile.getName().endsWith("/") ? "/" : "");
        return mManager.getHost();
    }

    public String getPath() {
        return "ftp://" + mManager.getHost() + mPath;
    }

    @Override
    public String getAbsolutePath() {
        return "ftp://" + mManager.getUser() + ":" + mManager.getPassword() + "@"
                + mManager.getHost() + mPath;
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
        List<OpenFTP2> kids = new Vector<OpenFTP2>();
        mManager.cd(mPath);
        mManager.setBasePath(mPath);
        FTPFile[] arr = mManager.listFiles(mPath);
        for (FTPFile f : arr)
            kids.add(new OpenFTP2(OpenFTP2.this, f));
        Collections.sort(kids);
        mChildren = kids.toArray(new OpenFTP2[kids.size()]);
        return mChildren;
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
    public Boolean requiresThread() {
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
        return new BufferedInputStream(mManager.getInputStream(mPath));
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return new BufferedOutputStream(mManager.getOutputStream(mPath));
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
                    final boolean ret = mManager.delete(mPath);
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

}
