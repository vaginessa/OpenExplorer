
package org.brandroid.openmanager.data;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;

import org.brandroid.openmanager.R;
import org.brandroid.openmanager.interfaces.OpenApp;
import org.brandroid.openmanager.util.FileManager;
import org.brandroid.openmanager.util.MimeTypes;
import org.brandroid.openmanager.util.SortType.Type;
import org.brandroid.openmanager.util.ThumbnailCreator;
import org.brandroid.openmanager.views.RemoteImageView;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.text.Html;
import android.widget.ImageView;

public class OpenData implements Comparable<OpenData> {
    private final String mName;
    private final String mFullPath;
    private final CharSequence mInfo;
    private final long mDate;
    private final long mSize;
    private final CharSequence mDateString;
    private final Boolean isDirectory;
    private WeakReference<OpenPath> mPath;
    private SoftReference<Bitmap> mBitmap;
    private boolean showLongDate = true;
    private final boolean hasThumbnail;
    private final int mOverlay, mDrawable;
    private final boolean isTextFile;
    
    public OpenData(OpenPath path, Resources r, boolean large)
    {
        mName = path.getName();
        mFullPath = path.getAbsolutePath();
        isDirectory = path.isDirectory();
        showLongDate = r.getBoolean(R.bool.show_long_date);
        mInfo = parseDetails(path, r);
        mDate = path.lastModified();
        
        if(isDirectory && !path.requiresThread())
            mSize = path.getListLength();
        else if(!isDirectory)
            mSize = path.length();
        else mSize = -1;
        
        mDateString = path.getFormattedDate(showLongDate);
        mPath = new WeakReference<OpenPath>(path);
        if(path instanceof OpenPath.ThumbnailOverlayInterface)
            mOverlay = ((OpenPath.ThumbnailOverlayInterface)path).getOverlayDrawableId(large);
        else mOverlay = 0;
        mDrawable = ThumbnailCreator.getDefaultResourceId(path, large ? 96 : 32, large ? 96 : 32);
        hasThumbnail = path.hasThumbnail();
        isTextFile = path.isTextFile();
    }
    
    public boolean hasThumbnail() { return hasThumbnail; }
    public int getOverlayDrawableId() { return mOverlay; }
    public int getDrawableId() { return mDrawable; }
    
    private CharSequence parseDetails(OpenPath path, Resources r)
    {
        String info = path.getDetails(false);
        info = String.format(info, r.getString(R.string.s_files));
        if (OpenPath.Sorting.getType() == Type.SIZE
                || OpenPath.Sorting.getType() == Type.SIZE_DESC)
            info = "<b>" + info + "</b>";
        if (path.getParent() != null && path.getParent().showChildPath()) {
            info += " :: " + path.getPath().replace(path.getName(), "");
            showLongDate = false;
        }
        return Html.fromHtml(info);
    }

    public String getName()
    {
        return mName;
    }

    public long lastModified()
    {
        return mDate;
    }

    public long length()
    {
        return mSize;
    }
    
    public boolean isDirectory()
    {
        if (isDirectory != null)
            return isDirectory;
        return false;
    }

    public OpenPath getPath() {
        if (mPath != null && mPath.get() != null)
            return mPath.get();
        return FileManager.getOpenCache(mFullPath);
    }
    
    public CharSequence getInfo() {
        return mInfo;
    }
    
    public CharSequence getDateString() {
        return mDateString;
    }
    
    public void setImageBitmap(RemoteImageView iv, OpenApp app, int w)
    {
        if(mBitmap != null)
            iv.setImageBitmap(mBitmap.get());
        else
            ThumbnailCreator.setThumbnail(app, iv, getPath(), w, w);
    }

    @Override
    public int compareTo(OpenData another) {
        return compare(this, another);
    }

    public boolean isHidden() {
        return getName().startsWith(".");
    }

    public static int compare(OpenData fa, OpenData fb)
    {
        if (fa == null && fb != null)
            return 1;
        if (fb == null && fa != null)
            return 0;
        if (fb == null || fa == null)
            return 0;
        if (OpenPath.Sorting.foldersFirst()) {
            if (fb.isDirectory() && !fa.isDirectory())
                return 1;
            if (fa.isDirectory() && !fb.isDirectory())
                return -1;
        }
        String a = fa.getName();
        String b = fb.getName();
        Long sa = fa.length();
        Long sb = fb.length();
        Long ma = fa.lastModified();
        Long mb = fb.lastModified();
        if (a == null && b != null)
            return 1;
        if (a == null || b == null)
            return 0;
        switch (OpenPath.Sorting.getType()) {
            case ALPHA_DESC:
                return b.toLowerCase().compareTo(a.toLowerCase());
            case ALPHA:
                return a.toLowerCase().compareTo(b.toLowerCase());
            case SIZE_DESC:
                if (sa == null && sb != null)
                    return 1;
                if (sa == null || sb == null)
                    return 0;
                return sa.compareTo(sb);
            case SIZE:
                if (sb == null && sa != null)
                    return 1;
                if (sa == null || sb == null)
                    return 0;
                return sb.compareTo(sa);
            case DATE_DESC:
                if (ma == null && mb != null)
                    return 1;
                if (ma == null || mb == null)
                    return 0;
                return ma.compareTo(mb);
            case DATE:
                if (mb == null && ma != null)
                    return 1;
                if (ma == null || mb == null)
                    return 0;
                return mb.compareTo(ma);
            case TYPE:
                String ea = a.substring(a.lastIndexOf(".") + 1, a.length()).toLowerCase();
                String eb = b.substring(b.lastIndexOf(".") + 1, b.length()).toLowerCase();
                return ea.compareTo(eb);
            case NONE:
                return 0;
            default:
                return a.toLowerCase().compareTo(b.toLowerCase());
        }
    }

    public String getMimeType() {
        if (MimeTypes.Default != null)
            return MimeTypes.Default.getMimeType(mFullPath);
        return "*/*";
    }
    

    /**
     * Get file extension (if any). This is any characters after the last period
     * in the filename. If there is no period, then a blank string will be
     * returned.
     * 
     * @return String representing file extension
     */
    public String getExtension() {
        String ret = getName();
        if (ret == null)
            return "";
        if (ret.indexOf(".") > -1)
            return ret.substring(ret.lastIndexOf(".") + 1);
        else
            return "";
    }
    
    public boolean isTextFile()
    {
        return isTextFile;
    }
}
