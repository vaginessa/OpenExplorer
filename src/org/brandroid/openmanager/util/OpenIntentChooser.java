package org.brandroid.openmanager.util;

import java.util.List;

import org.brandroid.openmanager.adapters.IconContextMenuAdapter;
import org.brandroid.openmanager.adapters.IconContextMenu.IconContextItemSelectedListener;
import org.brandroid.openmanager.data.OpenPath;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class OpenIntentChooser
{
	private AlertDialog mDialog;
	private Object mTag;
	private List<ResolveInfo> mListResolves;
	private IntentSelectedListener mListener;

	public interface IntentSelectedListener {
		void onIntentSelected(ResolveInfo item);
	}

	public OpenIntentChooser(final Context activity, final OpenPath file)
	{
		this(activity,IntentManager.getIntent(file, activity));
	}
	public OpenIntentChooser(final Context activity, final Intent intent)
	{
		mListResolves = activity.getPackageManager().queryIntentActivities(
				intent,
				PackageManager.MATCH_DEFAULT_ONLY);
		setAdapter(activity, new OpenIntentAdapter(activity, mListResolves));
	}
	public OpenIntentChooser(final Context activity, List<ResolveInfo> mResolves)
	{
		mListResolves = mResolves;
		setAdapter(activity, new OpenIntentAdapter(activity, mResolves));
	}
	public OpenIntentChooser setAdapter(Context context, final OpenIntentAdapter adapter)
	{
		mDialog = new AlertDialog.Builder(context)
			.setAdapter(adapter, 
						new OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								if(mListener != null)
									mListener.onIntentSelected(mListResolves.get(which));
							}
						})
			.setInverseBackgroundForced(true)
			.create();
		return this;
	}
	public void show() { mDialog.show(); }
	
    public OpenIntentChooser setOnIntentSelectedListener(IntentSelectedListener listener) {
        mListener = listener;
        return this;
    }
    
    public OpenIntentChooser setOnCancelListener(DialogInterface.OnCancelListener onCancelListener) {
    	mDialog.setOnCancelListener(onCancelListener);
        return this;
    }
    
    public OpenIntentChooser setOnDismissListener(DialogInterface.OnDismissListener onDismissListener) {
    	mDialog.setOnDismissListener(onDismissListener);
        return this;
    }
    
    public OpenIntentChooser setTitle(CharSequence title) {
    	mDialog.setTitle(title);
        return this;
    }
    
    public OpenIntentChooser setTitle(int titleId) {
    	mDialog.setTitle(titleId);
        return this;
    }
}
