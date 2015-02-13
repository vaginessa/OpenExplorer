package org.brandroid.openmanager.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.brandroid.openmanager.R;
import org.brandroid.openmanager.activities.OpenExplorer;
import org.brandroid.openmanager.util.PrivatePreferences;
import org.json.JSONObject;

import com.box.androidlib.box2.AccessToken;
import com.box.androidlib.box2.Box2;
import com.box.androidlib.box2.Folder;
import com.box.androidlib.box2.JSONParent;
import com.box.androidlib.box2.User;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;

public class OpenBox2 extends OpenNetworkPath implements OpenPath.SpaceHandler,
			OpenPath.ThumbnailOverlayInterface, OpenNetworkPath.CloudOpsHandler,
			OpenPath.OpenPathSizable {
	
	private final Box2 mBox;
	private final OpenBox2 mParent;
	private final JSONParent mFile;

    private List<OpenBox2> mChildren = null;
	
    public OpenBox2(JSONParent jo)
    {
    	mBox = new Box2(PrivatePreferences.getBoxAPIKey());
    	if(jo != null)
    	{
	    	if(jo instanceof AccessToken)
	    		mBox.setAccessToken((AccessToken)jo);
	    	else if(jo.has("access_token"))
	    	{
	    		Iterator<String> keys = jo.getRoot().keys();
	    		while(keys.hasNext())
	    		{
	    			String key = keys.next();
	    			mBox.getAccessToken().put(key, jo.getRoot().optString(key));
	    		}
	    	}
	    		
    	}
    	mParent = null;
    	mFile = null;
    }
    public OpenBox2(JSONParent jo, OpenBox2 parent)
    {
    	mBox = null;
    	mParent = parent;
    	mFile = jo;
    }
	public OpenBox2(String access, String refresh)
	{
		mBox = new Box2(PrivatePreferences.getBoxAPIKey());
		mBox.getAccessToken()
			.setAccessToken(access)
			.setRefreshToken(refresh);
		mParent = null;
		mFile = null;
	}

	@Override
	public long getTotalSpace() {
		User u = getUser();
		if(u == null) return 0;
		return u.getSpaceAvailable();
	}

	@Override
	public long getUsedSpace() {
		User u = getUser();
		if(u == null) return 0;
		return u.getSpaceUsed();
	}

	@Override
	public long getThirdSpace() {
		User u = getUser();
		if(u == null) return 0;
		return u.getSpaceAvailable() - u.getSpaceUsed();
	}
	
	@Override
	public String getName() {
		if(mFile != null)
			return mFile.getString("name", "");
		User u = getUser();
		if(u != null)
			return u.getLogin();
		return "Box";
	}
	
	public User getUser()
	{
		if(mBox != null)
			return mBox.getUser();
		return null;
	}

	@Override
	public boolean copyTo(OpenNetworkPath folder,
			CloudCompletionListener callback) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean delete(CloudDeleteListener callback) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Cancellable uploadToCloud(OpenFile file,
			CloudProgressListener callback) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Cancellable downloadFromCloud(OpenFile file,
			CloudProgressListener callback) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean touch(CloudCompletionListener callback) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Drawable getOverlayDrawable(Context c, boolean large) {
        return c.getResources().getDrawable(
                large ? R.drawable.lg_box_overlay : R.drawable.sm_box_overlay);
	}

	@Override
	public void getSpace(final SpaceListener callback) {
		if(getUser() != null)
			callback.onSpaceReturned(getTotalSpace(), getUsedSpace(), getThirdSpace());
		if(mBox == null) return;
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					final User u = mBox.getAccountInfo();
					if(u != null)
					post(new Runnable() {
						@Override
						public void run() {
							callback.onSpaceReturned(u.getSpaceAvailable(), u.getSpaceUsed(), 0);
						}
					});
				} catch (IOException e) {
					postException(e, callback);
				}
			}
		}).start();
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getPath() {
		if(mParent == null)
			return "/";
		return "/";
	}

	@Override
	public String getAbsolutePath() {
		// TODO Auto-generated method stub
		AccessToken access = mBox.getAccessToken();
		return "box://" + access.getAccessToken() + ":" + access.getRefreshToken() + "@www.box.com" + getPath();
	}

	@Override
	public long length() {
		if(mChildren != null)
			return mChildren.size();
		return 0;
	}

	@Override
	public OpenPath getParent() {
		return mParent;
	}

	@Override
	public OpenPath getChild(String name) {
		if(mChildren != null)
		{
			for(OpenPath kid : mChildren)
				if(kid.getName() == name)
					return kid;
		}
		return null;
	}

	@Override
	public OpenPath[] list() throws IOException {
		if(mChildren != null)
			return mChildren.toArray(new OpenPath[mChildren.size()]);
		return null;
	}
	
	public Box2 getBox() {
		if(mParent != null)
			return mParent.getBox();
		return mBox;
	}

	@Override
	public OpenPath[] listFiles() throws IOException {
		mChildren = new ArrayList<OpenBox2>();
		String folderId = "0";
		if(mFile != null && (mFile instanceof Folder || mFile.getString("type", "folder") == "folder"))
			folderId = mFile.getString("id", folderId);	
		for(JSONParent item : getBox().listFiles(folderId))
		{
			mChildren.add(new OpenBox2(item, this));
		}
		return list();
	}

	@Override
	public Boolean isDirectory() {
		if(mParent == null)
			return true;
		if(mFile != null)
			return mFile.getString("type", "folder") == "folder";
		return false;
	}

	@Override
	public Boolean isFile() {
		return !isDirectory();
	}

	@Override
	public Boolean isHidden() {
		return false;
	}
	
	public AccessToken getAccessToken()
	{
		if(mBox != null)
			return mBox.getAccessToken();
		if(mParent != null)
			return mParent.getAccessToken();
		return null;
	}

	@Override
	public Uri getUri() {
		String authority = "";
		AccessToken token = getAccessToken();
		if(token != null)
			authority = token.getAccessToken() + ":" + token.getRefreshToken() + "@";
		authority += "api.box.com";
		Uri.Builder builder = new Uri.Builder()
			.scheme("box")
			.authority(authority);
		if(mParent != null)
		{
			builder.path(mParent.getPath());
		}
		builder.appendPath(getName());
		return builder.build();
	}

	@Override
	public Long lastModified() {
		if(mFile != null)
			return mFile.getLong("modified_at", -1L);
		return -1L;
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
