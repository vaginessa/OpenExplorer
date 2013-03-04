
package org.brandroid.openmanager.data;

import java.io.IOException;
import java.util.List;
import java.util.Vector;

import org.brandroid.openmanager.activities.OpenExplorer;
import org.brandroid.openmanager.util.PrivatePreferences;
import org.brandroid.utils.Logger;
import org.brandroid.utils.Preferences;

import com.box.androidlib.Box;
import com.box.androidlib.BoxFile;
import com.box.androidlib.BoxFolder;
import com.box.androidlib.DAO;
import com.box.androidlib.GetAccountTreeListener;
import com.box.androidlib.GetFileInfoListener;

import android.net.Uri;

public class OpenBox extends OpenPath implements OpenPath.OpenPathUpdateHandler {

    private static final long serialVersionUID = 5742031992345655964L;
    private final Box mBox;
    private final String mToken;
    private final DAO mFile;
    private final OpenBox mParent;

    private List<OpenPath> mChildren = null;

    private final boolean DEBUG = OpenExplorer.IS_DEBUG_BUILD && true;

    public OpenBox(String token)
    {
        mToken = token;
        mBox = Box.getInstance(PrivatePreferences.getBoxAPIKey());
        mFile = new BoxFolder();
        ((BoxFolder)mFile).setId(0);
        mParent = null;
    }
    
    public OpenBox(OpenBox parent, DAO child)
    {
        mParent = parent;
        mBox = parent.mBox;
        mToken = parent.mToken;
        mFile = child;
    }
    
    public BoxFile getFile()
    {
        if(mFile instanceof BoxFile)
            return ((BoxFile)mFile);
        return null;
    }
    
    public BoxFolder getFolder()
    {
        if(mFile instanceof BoxFolder)
            return (BoxFolder)mFile;
        return null;
    }
    
    public long getId()
    {
        if(isDirectory())
            return getFolder().getId();
        return getFile().getId();
    }
    
    @Override
    public void list(final OpenContentUpdateListener callback) throws IOException {
        if(mChildren != null)
        {
            for(OpenPath kid : mChildren)
                callback.addContentPath(kid);
            callback.doneUpdating();
            return;
        }
        mChildren = new Vector<OpenPath>();
        if(DEBUG)
            Logger.LogDebug("Box listing for " + getId() + "!");
        mBox.getAccountTree(mToken, getId(), new String[0], new GetAccountTreeListener() {
            
            @Override
            public void onIOException(IOException e) {
                callback.onUpdateException(e);
            }
            
            @Override
            public void onComplete(BoxFolder targetFolder, String status) {
                if(status.equals("not_logged_in"))
                    Preferences.getPreferences("box").edit().clear().commit();
                if(DEBUG)
                    Logger.LogDebug("Box.onComplete!: " + status + " " + targetFolder.toString());
                if(targetFolder != null)
                {
                    for (BoxFolder f : targetFolder.getFoldersInFolder())
                    {
                        OpenBox kid = new OpenBox(OpenBox.this, f);
                        mChildren.add(kid);
                        callback.addContentPath(kid);
                    }
                    for (BoxFile f : targetFolder.getFilesInFolder())
                    {
                        OpenBox kid = new OpenBox(OpenBox.this, f);
                        mChildren.add(kid);
                        callback.addContentPath(kid);
                    }
                }
                callback.doneUpdating();                
            }
        });
    }
    
    @Override
    public Boolean requiresThread() {
        return true;
    }

    @Override
    public String getName() {
        if(isDirectory() && getFolder().getId() == 0)
            return "Box";
        if(isDirectory() && getFolder().getFolderName() != null)
            return getFolder().getFolderName();
        if(isFile() && getFile().getFileName() != null)
            return getFile().getFileName();
        return "???";
    }

    @Override
    public String getPath() {
        if(getId() == 0)
            return "/";
        if(isDirectory())
            return getFolder().getFolderPath();
        if(getFile().getFolder() != null)
            return getFile().getFolder().getFolderPath() + "/" + getName();
       return getName();
    }

    @Override
    public String getAbsolutePath() {
        return "box://" + mToken + "/" + getPath();
    }

    @Override
    public long length() {
        if(isFile())
            return getFile().getSize();
        return getFolder().getFileCount();
    }

    @Override
    public OpenPath getParent() {
        if(mParent != null)
            return mParent;
        if(getId() == 0)
            return null;
        if(isDirectory() && getFolder().getParentFolder() != null)
            return new OpenBox(this, getFolder().getParentFolder());
        if(isFile() && getFile().getFolder() != null)
            return new OpenBox(this, getFile().getFolder());
        return null;
    }

    @Override
    public OpenPath getChild(String name) {
        try {
            for(OpenPath p : listFiles())
                if(p.getName().equals(name))
                    return p;
        } catch(Exception e) { }
        return null;
    }

    @Override
    public OpenPath[] list() throws IOException {
        return null;
    }

    @Override
    public OpenPath[] listFiles() throws IOException {
        if(mChildren != null)
            return mChildren.toArray(new OpenPath[mChildren.size()]);
        return list();
    }

    @Override
    public Boolean isDirectory() {
        return (mFile instanceof BoxFolder);
    }

    @Override
    public Boolean isFile() {
        return !isDirectory();
    }

    @Override
    public Boolean isHidden() {
        return false;
    }

    @Override
    public Uri getUri() {
        return Uri.parse(getAbsolutePath());
    }

    @Override
    public Long lastModified() {
        if(isDirectory())
            return getFolder().getUpdated();
        return getFile().getUpdated();
    }

    @Override
    public Boolean canRead() {
        return true;
    }

    @Override
    public Boolean canWrite() {
        return false;
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
