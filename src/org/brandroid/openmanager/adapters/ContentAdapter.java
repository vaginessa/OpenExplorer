
package org.brandroid.openmanager.adapters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.CopyOnWriteArrayList;

import org.brandroid.openmanager.R;
import org.brandroid.openmanager.activities.OpenApplication;
import org.brandroid.openmanager.activities.OpenExplorer;
import org.brandroid.openmanager.data.BookmarkHolder;
import org.brandroid.openmanager.data.OpenData;
import org.brandroid.openmanager.data.OpenFile;
import org.brandroid.openmanager.data.OpenFileRoot;
import org.brandroid.openmanager.data.OpenPath.OpenPathUpdateHandler;
import org.brandroid.openmanager.data.OpenPath;
import org.brandroid.openmanager.interfaces.OpenApp;
import org.brandroid.openmanager.util.SortType;
import org.brandroid.openmanager.util.SortType.Type;
import org.brandroid.openmanager.util.ThumbnailCreator;
import org.brandroid.openmanager.util.ThumbnailCreator.OnUpdateImageListener;
import org.brandroid.openmanager.views.OpenPathView;
import org.brandroid.openmanager.views.RemoteImageView;
import org.brandroid.utils.ImageUtils;
import org.brandroid.utils.Logger;
import org.brandroid.utils.Preferences;
import org.brandroid.utils.ViewUtils;

import com.stericson.RootTools.RootTools;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.text.Html;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Main Adapter used in OpenExplorer. Adapts Grid & List View.
 */
public class ContentAdapter extends BaseAdapter {
    private final int KB = 1024;
    private final int MG = KB * KB;
    private final int GB = MG * KB;

    private final OpenPath mParent;
    private SortedSet<OpenPath> mData2 = new TreeSet<OpenPath>();
    private OpenData[] mFinalItems;
    private boolean isFinal = false;
    // private final List<OpenData> mData2 = new ArrayList<OpenData>();
    public int mViewMode = OpenExplorer.VIEW_LIST;
    public boolean mShowThumbnails = true;
    private boolean mShowHiddenFiles = false;
    private SortType mSorting = SortType.ALPHA;
    private OpenApp mApp;
    private boolean mPlusParent = false;
    private boolean mShowDetails = true;
    private boolean mShowFiles = true;
    private boolean mShowChecks = false;

    private int checkboxOnId = -1;
    private int checkboxOffId = -1;
    private int clipboardId = -1;

    /**
     * Set of seleced message IDs.
     */
    private final CopyOnWriteArrayList<OpenData> mSelectedSet = new CopyOnWriteArrayList<OpenData>();

    /**
     * Callback from MessageListAdapter. All methods are called on the UI
     * thread.
     */
    public interface Callback {
        /** Called when the user selects/unselects a message */
        void onAdapterSelectedChanged(OpenData path, boolean newSelected, int mSelectedCount);
    }

    private final Callback mCallback;

    public ContentAdapter(OpenApp app, Callback callback, int mode, OpenPath parent) {
        // super(context, layout, data);
        mApp = app;
        mCallback = callback;
        mParent = parent;

        if (Preferences.Pref_ShowUp && mParent.getParent() != null)
            mPlusParent = true;
        mViewMode = mode;
        fetchThemedAttributes();
    }

    public void fetchThemedAttributes() {
        checkboxOnId = mApp.getThemedResourceId(R.styleable.AppTheme_checkboxButtonOn,
                R.drawable.btn_check_on_holo_light);
        checkboxOffId = mApp.getThemedResourceId(R.styleable.AppTheme_checkboxButtonOff,
                R.drawable.btn_check_off_holo_light);
        clipboardId = mApp.getThemedResourceId(R.styleable.AppTheme_actionIconClipboard,
                R.drawable.ic_menu_clipboard_light);
    }

    public interface CheckClipboardListener {
        public boolean checkClipboard(OpenData file);

        public void removeFromClipboard(OpenData file);
    }

    public void setShowPlusParent(boolean showUp) {
        mPlusParent = showUp;
    }

    public void setShowDetails(boolean showDeets) {
        mShowDetails = showDeets;
    }

    public void setShowFiles(boolean showFiles) {
        mShowFiles = showFiles;
    }

    public Context getContext() {
        return mApp.getContext();
    }

    public Resources getResources() {
        return mApp.getResources();
    }

    public int getViewMode() {
        return mViewMode;
    }

    public void setViewMode(int mode) {
        mViewMode = mode;
        notifyDataSetChanged();
    }

    public void setSorting(SortType sort) {
        mSorting = sort;
        notifyDataSetChanged();
    }

    public SortType getSorting() {
        return mSorting;
    }

    public void updateData() {
        OpenPath[] list = getList();
        if (list == null)
            list = new OpenPath[0];
        updateData(list);
    }

    public void updateData(final OpenPath[] items) {
        updateData(items, true);
    }

    private void updateData(final OpenPath[] items, final boolean doSort) {
        long time = new Date().getTime();
        if (items == null) {
            // Logger.LogWarning("ContentAdapter.updateData warning: Items are null!");
            super.notifyDataSetChanged();
            return;
        }

        // new Thread(new Runnable(){public void run() {
        boolean showHidden = getShowHiddenFiles();
        boolean foldersFirst = getSorting().foldersFirst();
        Logger.LogVerbose("updateData on " + items.length + " items (for " + mParent + ") : "
                + (showHidden ? "show" : "hide") + " + " + (foldersFirst ? "folders" : "files")
                + " + " + (doSort ? mSorting.toString() : "no sort"));

        OpenPath.Sorting = mSorting;
        mData2.clear();
        isFinal = false;
        mFinalItems = null;
        
        if (items != null)
            for (OpenPath f : items) {
                if (f == null)
                    continue;
                if (!showHidden && f.isHidden())
                    continue;
                if (!mShowFiles && !f.isDirectory())
                    continue;
                mData2.add(f);
            }

        finalize();
    }
    
    private OpenData[] convertToData(Collection<OpenPath> paths)
    {
        OpenData[] ret = new OpenData[paths.size()];
        int i = 0;
        final Resources r = getResources();
        for(OpenPath p : paths)
            ret[i++] = new OpenData(p, r, mViewMode == OpenExplorer.VIEW_GRID);
        return ret;
    }
    
    private void prefinalize() {
        if(isFinal) return;
        mFinalItems = convertToData(mData2);
        mData2.clear();
        isFinal = true;
    }

    public void finalize() {
        prefinalize();
        super.notifyDataSetChanged();
    }
    
    private void unfinalize() {
        if(!isFinal) return;
        isFinal = false;
        mData2.clear();
        //mData2.addAll(Arrays.asList(mFinalItems));
    }

    public boolean isFinalized() {
        return isFinal;
    }

    @Override
    public void notifyDataSetChanged() {
        finalize();
        // super.notifyDataSetChanged();
        /*
         * Please note, this is on purpose. We want to hook into
         * notifyDataSetChanged to ensure filters & sorting are enabled.
         */
        super.notifyDataSetChanged();
    }

    private OpenPath[] getList() {
        try {
            if (mParent == null
                    || (!mParent.isLoaded() && mParent instanceof OpenPathUpdateHandler))
                return new OpenPath[0];
            if (mParent.requiresThread() && Thread.currentThread().equals(OpenExplorer.UiThread))
                return mParent.list();
            else if (!mParent.canRead())
            {
                if ((mParent instanceof OpenFile)
                        && OpenApplication.hasRootAccess(true))
                    return new OpenFileRoot(mParent).listFiles();
                else
                    return new OpenPath[0];
            } else
                return mParent.listFiles();
        } catch (Exception e) {
            Logger.LogError("Couldn't getList in ContentAdapter", e);
            return new OpenPath[0];
        }
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        int mode = getViewMode();
        final int layout = getViewMode() == OpenExplorer.VIEW_GRID ? (!OpenExplorer.DEBUG_TOGGLE ? R.layout.file_grid_item
                : R.layout.grid_content_layout)
                : (!OpenExplorer.DEBUG_TOGGLE ? R.layout.file_list_item
                        : R.layout.list_content_layout);
        final boolean useLarge = getViewMode() == OpenExplorer.VIEW_GRID;
        final OpenData data = getItem(position); // super.getItem(position);
        //final OpenData file = data.getPath();

        View row;

        if (view == null || view.getTag() == null || !(view.getTag() instanceof BookmarkHolder)
                || ((BookmarkHolder)view.getTag()).getMode() != mode) {
            LayoutInflater in = (LayoutInflater)getContext().getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);

            row = in.inflate(layout, parent, false);
            //BookmarkHolder mHolder = new BookmarkHolder(file, file.getName(), row, mode);
            row.setTag(data);
            // file.setTag(mHolder);
        } else {
            row = view;
        }

        if (data == null) {
            return row;
        } else if (row instanceof OpenPathView) {
            ((OpenPathView)row).associateFile(data, this);
        }

        TextView mInfo = (TextView)row.findViewById(R.id.content_info);
        TextView mDate = (TextView)row.findViewById(R.id.content_date);
        // TextView mPathView =
        // (TextView)row.findViewById(R.id.content_fullpath);
        TextView mNameView = (TextView)row.findViewById(R.id.content_text);
        final ImageView mIcon = (ImageView)row.findViewById(R.id.content_icon);
        ImageView mCheck = (ImageView)row.findViewById(R.id.content_check);

        if (mPlusParent && position == 0) {
            mNameView.setText(R.string.s_menu_up);
            mIcon.setImageResource(useLarge ? R.drawable.lg_folder_up : R.drawable.sm_folder_up);
            if (mInfo != null)
                mInfo.setText("");
            if (mDate != null)
                mDate.setText("");
            return row;
        }
        final String mName = data.getName();

        int mWidth = getViewMode() == OpenExplorer.VIEW_GRID ? OpenExplorer.IMAGE_SIZE_GRID
                : OpenExplorer.IMAGE_SIZE_LIST;
        int mHeight = mWidth;

        if (mInfo != null && mShowDetails)
                mInfo.setText(data.getInfo());
        if (mDate != null)
            mDate.setText(data.getDateString());

        if (mNameView != null)
            mNameView.setText(mName);

        if (mIcon != null) {
            // mIcon.invalidate();
            if (data.isHidden())
                mIcon.setAlpha(100);
            else
                mIcon.setAlpha(255);
            if (!mShowThumbnails || !data.hasThumbnail()) {
                mIcon.setImageDrawable(ThumbnailCreator.getDefaultDrawable(data, mWidth, mHeight,
                        getContext()));
            } else {
                data.setImageBitmap((RemoteImageView)mIcon, mApp, mWidth);
            }
        }

        // row.setTag(file);
        boolean mChecked = (mSelectedSet != null && mSelectedSet.contains(data));
        boolean mShowCheck = true; // mChecked || (mSelectedSet != null &&
        // mSelectedSet.size() > 0);
        boolean mShowClip = mApp.getClipboard().contains(data);

        if (mShowClip) {
            ViewUtils.setViewsVisible(row, true, R.id.content_clipboard);
            ViewUtils.setOnClicks(row, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mApp.getClipboard().remove(data);
                    v.setVisibility(View.GONE);
                }
            }, R.id.content_clipboard);
        } else {
            ViewUtils.setViewsVisible(row, false, R.id.content_clipboard);
            ViewUtils.setOnClicks(row, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleSelected(data);
                }
            }, R.id.checkmark_area);
        }

        if (mCheck != null)
            mCheck.setImageResource(mChecked ? checkboxOnId : checkboxOffId);
        ViewUtils.setViewsVisible(row, mShowCheck, R.id.content_check);

        switch (OpenPath.Sorting.getType()) {
            case DATE:
            case DATE_DESC:
                mDate.setTextAppearance(getContext(), R.style.Text_Small_Highlight);
                // mInfo.setTextAppearance(getContext(), R.style.Text_Small);
                mNameView.setTextAppearance(getContext(), R.style.Text_Large_Dim);
                break;
            case SIZE:
            case SIZE_DESC:
                // mInfo.setTextAppearance(getContext(),
                // R.style.Text_Small_Highlight);
                mDate.setTextAppearance(getContext(), R.style.Text_Small);
                mNameView.setTextAppearance(getContext(), R.style.Text_Large_Dim);
                break;
            case ALPHA:
            case ALPHA_DESC:
                mNameView.setTextAppearance(getContext(), R.style.Text_Large);
                // mInfo.setTextAppearance(getContext(), R.style.Text_Small);
                mDate.setTextAppearance(getContext(), R.style.Text_Small);
                break;
            default:
                mNameView.setTextAppearance(getContext(), R.style.Text_Large_Dim);
                // mInfo.setTextAppearance(getContext(), R.style.Text_Small);
                mDate.setTextAppearance(getContext(), R.style.Text_Small);
                break;
        }

        return row;
    }

    @Override
    public int getCount() {
        if(!isFinal)
            prefinalize();
        return mFinalItems.length + (mPlusParent ? 1 : 0);
    }

    @Override
    public OpenData getItem(int position) {
        if(!isFinal)
            prefinalize();
        if (mPlusParent)
        {
            if (position == 0)
                return new OpenData(mParent.getParent(), getResources(), mViewMode == OpenExplorer.VIEW_GRID);
            else
                position--;
        }
        if (position < 0 || position >= mFinalItems.length)
            return null;
        return mFinalItems[position];
    }

    @Override
    public long getItemId(int position) {
        if(!isFinal)
            prefinalize();
        return position;
    }

    public void clearData() {
        mData2.clear();
        isFinal = false;
        mFinalItems = null;
    }

    public void addAll(Collection<? extends OpenPath> collection) {
        if (isFinal)
            unfinalize();
        mData2.addAll(collection);
        notifyDataSetChanged();
    }

    public void selectAll()
    {
        mSelectedSet.addAll(Arrays.asList(mFinalItems));
    }

    public List<OpenData> getSelectedSet() {
        return mSelectedSet;
    }
    
    public List<OpenPath> getSelectedPaths() {
        List<OpenPath> ret = new Vector<OpenPath>();
        for(OpenData data : mSelectedSet)
            ret.add(data.getPath());
        return ret;
    }

    public void setSelectedSet(ArrayList<OpenData> set) {
        for (OpenData rememberedPath : set) {
            mSelectedSet.add(rememberedPath);
        }
    }

    public void addSelection(OpenData path) {
        if (!mSelectedSet.contains(path))
            mSelectedSet.add(path);
    }

    /**
     * Clear the selection. It's preferable to calling {@link Set#clear()} on
     * {@link #getSelectedSet()}, because it also notifies observers.
     */
    public void clearSelection() {
        if (mSelectedSet.size() > 0) {
            mSelectedSet.clear();
            notifyDataSetChanged();
        }
    }

    public boolean isSelected(OpenData path) {
        return getSelectedSet().contains(path);
    }

    public void toggleSelected(OpenData path) {
        updateSelected(path, !isSelected(path));
    }

    public void toggleSelected(OpenData path, Callback callback) {
        updateSelected(path, !isSelected(path), callback);
    }

    /**
     * This is used as a callback from the list items, to set the selected state
     * <p>
     * Must be called on the UI thread.
     * 
     * @param itemView the item being changed
     * @param newSelected the new value of the selected flag (checkbox state)
     */
    private void updateSelected(OpenData path, boolean newSelected, Callback mCallback) {
        if (newSelected) {
            mSelectedSet.add(path);
        } else {
            mSelectedSet.remove(path);
        }
        if (mCallback != null) {
            mCallback.onAdapterSelectedChanged(path, newSelected, mSelectedSet.size());
        }
    }

    private void updateSelected(OpenData path, boolean newSelected) {
        updateSelected(path, newSelected, mCallback);
    }

    public void setShowHiddenFiles(boolean show) {
        mShowHiddenFiles = show;
    }

    public boolean getShowHiddenFiles() {
        return mShowHiddenFiles;
    }
}
