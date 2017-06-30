
package org.brandroid.openmanager.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.brandroid.openmanager.R;
import org.brandroid.openmanager.data.OpenPath;
import org.brandroid.openmanager.util.ThumbnailCreator;

import java.io.IOException;
import java.util.Arrays;

public class ContentTreeAdapter extends BaseAdapter {
    private OpenPath mPath;
    private OpenPath[] mContent = null;

    public ContentTreeAdapter(Context context, OpenPath path) {
        mPath = path;
        try {
            mContent = path.listDirectories();
            Arrays.sort(mContent);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public int getCount() {
        return mPath.getDepth() + mContent.length;
    }

    public int getDepth() {
        return mPath.getDepth();
    }

    @Override
    public OpenPath getItem(int position) {
        if (position < getDepth())
            return mPath.getAncestors(true).get((getDepth() - 1) - position);
        else
            return mContent[position - getDepth()];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        Context c = parent.getContext();
        if (view == null) {
            view = ((LayoutInflater)c.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(
                    R.layout.list_content_layout, parent, false);
        }

        final OpenPath file = getItem(position);

        if (file == null)
            return view;

        Object o = file.getTag();
        if (o != null && o instanceof OpenPath && ((OpenPath)o).equals(file))
            return view;

        view.setPadding(
                (file.getDepth() - 1) * c.getResources().getDimensionPixelSize(R.dimen.one_dp) * 10,
                0, 0, 0);

        TextView mInfo = (TextView)view.findViewById(R.id.content_info);
        TextView mNameView = (TextView)view.findViewById(R.id.content_text);
        final ImageView mIcon = (ImageView)view.findViewById(R.id.content_icon);

        mInfo.setVisibility(View.GONE);

        String name = file.getName();
        if (!name.endsWith("/"))
            name += "/";
        mNameView.setText(name);
        mIcon.setImageResource(ThumbnailCreator.getDefaultResourceId(file, 32, 32));

        return view;
    }

}
