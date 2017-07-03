
package org.brandroid.openmanager.data;

import android.database.Cursor;
import android.net.Uri;
import android.os.Build;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;

import org.brandroid.openmanager.activities.OpenExplorer;
import org.brandroid.openmanager.adapters.OpenPathDbAdapter;
import org.brandroid.openmanager.util.FileManager;
import org.brandroid.openmanager.util.SimpleUserInfo;
import org.brandroid.openmanager.util.SortType;
import org.brandroid.utils.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;

import static com.jcraft.jsch.ChannelSftp.SSH_FX_NO_SUCH_FILE;

/**
 * Main class for SFTP connections. Please note that as a descendent of
 * OpenNetworkPath, this class does not create its own threads for networking,
 * so make sure not to call listFiles via the UI thread.
 * 
 * @author Brandon Bowles
 * @see OpenNetworkPath
 */
public class OpenSFTP extends OpenNetworkPath implements OpenNetworkPath.PipeNeeded {
    private static final long serialVersionUID = 3263112609308933024L;
    //private long filesize = 0l;
    private Session mSession = null;
    private ChannelSftp mChannel = null;
    private InputStream in = null;
    private OutputStream out = null;
    private final String mHost, mUser;
    private String mRemotePath;
    private SftpATTRS mAttrs = null;
    private String mName = null;
    protected OpenSFTP mParent = null;
    private OpenSFTP[] mChildren = null;
    private Long mSize = null;
    private Long mModified = null;

    static {
        Logger.LogDebug("Configure spongy castle security");
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
    }

    public OpenSFTP(String fullPath) {
        Uri uri = Uri.parse(fullPath);
        Logger.LogDebug("OpenSFTP: " + uri.getScheme() + "://" + (uri.getUserInfo() != null ?
                uri.getUserInfo().replaceFirst(":.*", ":...") + "@" : "") + uri.getHost() + ":" + uri.getPort() +
                uri.getPath());
        mHost = uri.getHost();
        String user = uri.getUserInfo();
        mUserInfo = new SimpleUserInfo();
        if (user != null && user.indexOf(":") > -1) {
            String pw = Uri.decode(user.substring(user.indexOf(":") + 1));
            mUser = Uri.decode(user.substring(0, user.indexOf(":")));
            ((SimpleUserInfo)mUserInfo).setPassword(pw);
        } else if (user != null)
            mUser = Uri.decode(user);
        else
            mUser = "";
        mRemotePath = uri.getPath();
        if (uri.getPort() > 0)
            setPort(uri.getPort());
        //getAttrs();
    }

    public OpenSFTP(Uri uri) {
        Logger.LogDebug("OpenSFTP: " + uri.getScheme() + "://" + (uri.getUserInfo() != null ?
                uri.getUserInfo().replaceFirst(":.*", ":...") + "@" : "") + uri.getHost() + ":" + uri.getPort() +
                uri.getPath());
        mHost = uri.getHost();
        mUser = uri.getUserInfo();
        mRemotePath = uri.getPath();
        if (uri.getPort() > 0)
            setPort(uri.getPort());
        //getAttrs();
    }

    public OpenSFTP(String host, String user, String path) {
        Logger.LogDebug("OpenSFTP: " + host + ", " + user + ", " + path);
        mHost = host;
        mUser = user;
        mRemotePath = path;
    }

    /*public OpenSFTP(String host, String user, String path, UserInfo info) {
        Logger.LogDebug("OpenSFTP: " + host + ", " + user + ", " + path + ", " + (info.getPassword() != null && info
                .getPassword().length() > 0 ? "<nonempty password>" : "<no password>"));
        mHost = host;
        mUser = user;
        mRemotePath = path;
        mUserInfo = info;
    }*/

    public OpenSFTP(OpenSFTP parent, String child) {
        mUserInfo = parent.getUserInfo();
        mHost = parent.getHost();
        mUser = parent.getUser();
        mParent = parent;
        Uri pUri = mParent.getUri();
        Uri myUri = Uri.withAppendedPath(pUri, child);
        mRemotePath = myUri.getPath();
        mSession = parent.mSession;
        mChannel = parent.mChannel;
    }

    public OpenSFTP(OpenSFTP parent, LsEntry child) {
        mUserInfo = parent.getUserInfo();
        mHost = parent.getHost();
        mUser = parent.getUser();
        mParent = parent;
        mAttrs = child.getAttrs();
        mSize = mAttrs.getSize();
        mModified = (long)mAttrs.getMTime();
        Uri pUri = mParent.getUri();
        String name = child.getFilename();
        name = name.substring(name.lastIndexOf("/") + 1);
        Uri myUri = Uri.withAppendedPath(pUri, name);
        mRemotePath = myUri.getPath();
        mSession = parent.mSession;
        mChannel = parent.mChannel;
    }

    public OpenSFTP(String path, int size, int modified) {
        this(Uri.parse(path));
        mSize = (long)size;
        mModified = (long)modified;
    }

    public String getHost() {
        return mHost;
    }

    public String getUser() {
        return mUser;
    }

    @Override
    public String getName() {
        if (mName != null)
            return mName;
        if (mRemotePath.equals("") || mRemotePath.equals("/"))
            return mHost;
        String ret = mRemotePath
                .substring(mRemotePath.lastIndexOf("/", mRemotePath.length() - 1) + 1);
        if (ret.equals(""))
            ret = mRemotePath;
        return ret;
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public int getPort() {
        if (mSession != null)
            return mSession.getPort();
        int port = super.getPort();
        if (port > 0)
            return port;
        return 22;
    }

    @Override
    public String getPath() {
        //return getAbsolutePath();
        return "sftp://" + mHost + ":" + getPort() + (mRemotePath.startsWith("/") ? "" : "/") + mRemotePath;
    }

    public String getRootPath() {
        String ret = "";
        if(getUser() != null || mPrivPwd != null)
        {
            if(getUser() != null)
                ret += Uri.encode(getUser());
            if(mUserInfo.getPassword() != null)
                ret += ":" + Uri.encode(mUserInfo.getPassword());
            ret += "@";
        }
        if(getHost() != null)
            ret += getHost();
        if(getPort() > 0)
            ret += ":" + getPort();
        return "sftp://" + ret;
    }

    @Override
    public String getAbsolutePath() {
        String ret = getRootPath();
        if (ret.endsWith("/") && mRemotePath.startsWith("/"))
            ret = ret.substring(0, ret.length() - 1);
        else if(!ret.endsWith("/") && !mRemotePath.startsWith("/"))
            ret += "/";
        ret += mRemotePath;
        Logger.LogDebug("getAbsolutePath: mRemotePath = " + mRemotePath + " => " + ret);
        /*for (StackTraceElement s : Thread.currentThread().getStackTrace()) {
            Logger.LogDebug(s.toString());
        }*/
        return ret;
    }

    @Override
    public void setName(String name) {
        mName = name;
    }

    @Override
    public long length() {
        if (mSize != null)
            return mSize;
        if (mAttrs != null)
            return mAttrs.getMTime();
        return 0;
    }

    public SftpATTRS getAttrs() {
        try {
            connect();
            mAttrs = mChannel.lstat(mRemotePath);
        } catch (SftpException e) {
            if (e.id == SSH_FX_NO_SUCH_FILE) {
                return null;
            }
            Logger.LogError("SftpException during getAttrs", e);
            return null;
        } catch (JSchException e) {
            Logger.LogError("JSchException during getAttrs", e);
            return null;
        } catch (IOException e) {
            Logger.LogError("IOException during getAttrs", e);
            return null;
        }
        return mAttrs;
    }

    @Override
    public OpenPath getParent() {
        if (mParent != null)
            return mParent;
        try {
            if (getUri().getPath().length() > 2) {
                String path = getAbsolutePath().replace("/" + getName(), "");
                if (path.length() > 8) {
                    OpenSFTP par = (OpenSFTP)FileManager.getOpenCache(path);
                    if (par != null && par.getPath().length() < getPath().length()) {
                        par.setServer(getServer());
                        par.mSession = this.mSession;
                        par.mChannel = this.mChannel;
                        return par;
                    }
                }
            }
        } catch (Exception e) {
            Logger.LogError("Unable to get OpenSFTP.getParent(" + getPath() + ")", e);
        }
        return null;
    }

    @Override
    public OpenPath getChild(String name) {
        return new OpenSFTP(this, name);
    }

    @Override
    public int getChildCount(boolean countHidden) throws IOException {
        if (mChildren == null)
            return 0;
        return super.getChildCount(countHidden);
    }
    
    @Override
    public OpenPath[] list() throws IOException {
        if (mChildren != null)
            return mChildren;
        return listFiles();
    }

    @Override
    public OpenPath[] listFiles() throws IOException {
        if (Thread.currentThread().equals(OpenExplorer.UiThread)) return getChildren();
        getAttrs();
        try {
            connect();
            /*String lsPath = mRemotePath.replace(mChannel.pwd() + "/", "");
            if (lsPath.equals(""))
                lsPath = ".";
            else
                lsPath += "/";*/
            String lsPath = mRemotePath + "/";
            Logger.LogVerbose("OpenSFTP (" + Integer.toHexString(this.hashCode()) + "): ls " + lsPath);
            Vector<LsEntry> vv = mChannel.ls(lsPath);
            mChildren = new OpenSFTP[vv.size()];
            int i = 0;
            for (LsEntry item : vv) {
                String name = item.getFilename();
                name = name.substring(name.lastIndexOf("/") + 1);
                if (name.equals("."))
                    continue;
                if (name.equals(".."))
                    continue;
                mChildren[i++] = new OpenSFTP(this, item);
            }
        } catch (SftpException e) {
            Logger.LogError("SftpException during listFiles", e);
            mChildren = null;
            throw new IOException("SftpException during listFiles");
        } catch (JSchException e) {
            Logger.LogError("JSchException during listFiles", e);
            mChildren = null;
            if(Build.VERSION.SDK_INT > 8)
                throw new IOException("JSchException during listFiles", e);
            else
                throw new IOException("JSchException during listFiles - " + e.getMessage());
        }
        FileManager.setOpenCache(getAbsolutePath(), this);
        return mChildren;
    }

    @Override
    public OpenNetworkPath[] getChildren() {
        return mChildren;
    }

    @Override
    public Boolean isDirectory() {
        return mAttrs != null ? mAttrs.isDir() : true;
    }

    @Override
    public Boolean isFile() {
        return true;
    }

    @Override
    public Boolean isHidden() {
        return getName().startsWith(".");
    }

    @Override
    public Uri getUri() {
        return Uri.parse("sftp://" + mHost + ":" + getPort()
                + (!mRemotePath.startsWith("/") ? "/" : "") + mRemotePath);
    }

    @Override
    public Long lastModified() {
        if (mModified != null)
            return mModified;
        return mAttrs != null ? (long)mAttrs.getMTime() : null;
    }

    @Override
    public Boolean canRead() {
        return mAttrs != null && (mAttrs.getPermissions() & SftpATTRS.S_IRUSR) != 0;
    }

    @Override
    public Boolean canWrite() {
        /*if (!super.canWrite())
            return false;*/
        Logger.LogDebug("OpenSFTP.canWrite (" + Integer.toHexString(this.hashCode()) + " = " + mRemotePath + "): " +
                mAttrs + ", " + (mAttrs != null ? mAttrs.getPermissionsString() : ""));
        return mAttrs != null && (mAttrs.getPermissions() & SftpATTRS.S_IWUSR) != 0;
    }

    @Override
    public Boolean canExecute() {
        return mAttrs != null && (mAttrs.getPermissions() & SftpATTRS.S_IXUSR) != 0;
    }

    @Override
    public Boolean exists() {
        return mAttrs != null;
    }

    @Override
    public Boolean delete() {
        try {
            Logger.LogDebug("delete: " + mRemotePath);
            connect();
            mChannel.rm(mRemotePath);
            return true;
        } catch (SftpException e) {
            Logger.LogError("SftpException during delete", e);
            return false;
        } catch (JSchException e) {
            Logger.LogError("JSchException during delete", e);
            return false;
        } catch (IOException e) {
            Logger.LogError("IOException during delete", e);
            return false;
        }
    }

    @Override
    public Boolean mkdir() {
        try {
            Logger.LogDebug("mkdir: " + mRemotePath);
            connect();
            mChannel.mkdir(mRemotePath);
            return true;
        } catch (SftpException e) {
            Logger.LogError("SftpException during mkdir", e);
            return false;
        } catch (JSchException e) {
            Logger.LogError("JSchException during mkdir", e);
            return false;
        } catch (IOException e) {
            Logger.LogError("IOException during mkdir", e);
            return false;
        }
    }

    @Override
    public boolean listFromDb(SortType sort) {
        if (!AllowDBCache)
            return false;
        String parent = getPath();
        if (!parent.endsWith("/"))
            parent += "/";
        Logger.LogDebug("Fetching from folder: " + parent);
        Cursor c = mDb.fetchItemsFromFolder(parent, sort);
        if (c == null) {
            Logger.LogWarning("DB Fetch returned null?");
            return false;
        }
        ArrayList<OpenSFTP> arr = new ArrayList<OpenSFTP>();
        c.moveToFirst();
        while (!c.isAfterLast()) {
            String folder = c
                    .getString(OpenPathDbAdapter.getKeyIndex(OpenPathDbAdapter.KEY_FOLDER));
            String name = c.getString(OpenPathDbAdapter.getKeyIndex(OpenPathDbAdapter.KEY_NAME));
            int size = c.getInt(OpenPathDbAdapter.getKeyIndex(OpenPathDbAdapter.KEY_SIZE));
            int modified = c.getInt(OpenPathDbAdapter.getKeyIndex(OpenPathDbAdapter.KEY_MTIME));
            arr.add(new OpenSFTP(folder + name, size, modified));
            Logger.LogDebug("listFromDb: " + folder + name);
            c.moveToNext();
        }
        Logger.LogDebug("listFromDb returning " + arr.size() + " children");
        c.close();
        mChildren = arr.toArray(new OpenSFTP[0]);
        return true;
    }

    @Override
    public void disconnect() {
        if (mChannel == null && mSession == null)
            return;
        Logger.LogDebug("Disconnecting OpenSFTP " + getName());
        if (mChannel != null)
            mChannel.disconnect();
        if (mSession != null)
            mSession.disconnect();
        try {
            if (in != null)
                in.close();
        } catch (IOException e) {
        }
        try {
            if (out != null)
                out.close();
        } catch (IOException e) {
        }
    }

    @Override
    public void connect() throws IOException, JSchException {
        if (mSession != null && mSession.isConnected() && mChannel != null
                && mChannel.isConnected()) {
            Logger.LogInfo("No need for new OpenSFTP connection @ " + getName());
            return;
        }
        Logger.LogDebug("Attempting to connect to OpenSFTP " + getName());
        for (Object id : DefaultJSch.getIdentityNames()) {
            Logger.LogDebug("Identity " + id);
        }
        if (mSession == null || !mSession.isConnected()) {
            mSession = DefaultJSch.getSession(mUser, mHost, getPort());
            if (mUserInfo != null)
                mSession.setUserInfo(mUserInfo);
            else
                Logger.LogWarning("No User Info!");
            mSession.setTimeout(Timeout);
            mSession.setConfig("UserAuth",  "userauth.publickey");
            Logger.LogDebug("Connecting session...");
            mSession.connect();
        }
        Logger.LogDebug("Session achieved. Opening Channel...");
        mChannel = (ChannelSftp)mSession.openChannel("sftp");
        mChannel.connect();

        Logger.LogDebug("Channel open! Ready for action!");

        try {
            String pwd = mChannel.pwd();
            /*if (!pwd.equals(mRemotePath)) {
                Logger.LogWarning("Working Directory (" + pwd + ") != remote directory ("
                        + mRemotePath + ")");

                mRemotePath = pwd;
                if (mParent == null && mRemotePath.indexOf("/", 1) > -1) {
                    mName = mRemotePath.substring(mRemotePath.lastIndexOf("/") + 1);
                    String par = getAbsolutePath();
                    par = par.substring(0, par.lastIndexOf("/"));
                    mParent = new OpenSFTP(par);
                    mParent.mSession = this.mSession;
                    mParent.mChannel = this.mChannel;
                }
            }*/
        } catch (SftpException e) {
            Logger.LogError("Unable to retrieve remote working directory.", e);
        }
    }

    @Override
    public boolean isConnected() throws IOException {
        if (mSession == null)
            return false;
        return mSession.isConnected();
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        Logger.LogDebug("getOutputStream: " + getUri().getPath());
        final PipedOutputStream pipeOut = new PipedOutputStream();
        final PipedInputStream pipeIn = new PipedInputStream(pipeOut);
        thread(new Runnable() {
            public void run() {
                try {
                    mChannel.put(pipeIn, getUri().getPath());
                } catch (SftpException e) {
                    Logger.LogError("getOutputStream.run", e);
                }
            }
        });
        return pipeOut;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        Logger.LogDebug("getInputStream: " + getUri().getPath());
        final PipedOutputStream pipeOut = new PipedOutputStream();
        final PipedInputStream pipeIn = new PipedInputStream(pipeOut);
        thread(new Runnable() {
            public void run() {
                try {
                    mChannel.get(getUri().getPath(), pipeOut);
                } catch (Exception e) {
                    Logger.LogError("getOutputStream.run", e);
                }
            }
        });
        return pipeIn;
    }

    @Override
    public void clearChildren() {
        mChildren = null;
    }

    @Override
    public boolean syncUpload(OpenFile f, NetworkListener l) {
        try {
            Logger.LogDebug("syncUpload: " + f.getPath() + " -> " + getUri().getPath());
            mChannel.put(f.getPath(), getUri().getPath());
            l.OnNetworkCopyFinished(this, f);
            return true;
        } catch (Exception e) {
            l.OnNetworkFailure(this, f, e);
        }
        return false;
    }

    @Override
    public boolean syncDownload(OpenFile f, NetworkListener l) {
        try {
            Logger.LogDebug("syncDownload: " + getUri().getPath() + " -> " + f.getPath());
            mChannel.get(getUri().getPath(), f.getPath());
            l.OnNetworkCopyFinished(this, f);
            return true;
        } catch (SftpException e) {
            l.OnNetworkFailure(this, f, e);
        }
        return false;
    }
}
