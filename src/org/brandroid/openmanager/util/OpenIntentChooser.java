package org.brandroid.openmanager.util;

import java.util.List;

import org.brandroid.openmanager.R;
import org.brandroid.openmanager.activities.OpenExplorer;
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
import android.widget.CheckBox;

public class OpenIntentChooser implements android.view.View.OnClickListener
{
	private AlertDialog mDialog;
	private Object mTag;
	private List<ResolveInfo> mListResolves;
	private IntentSelectedListener mListener;
	private boolean mDefaultSelected = true;

	public interface IntentSelectedListener {
		void onIntentSelected(ResolveInfo item, boolean defaultSelected);
	}

	public OpenIntentChooser(final OpenExplorer activity, final OpenPath file)
	{
		this(activity,IntentManager.getIntent(file, activity));
	}
	public OpenIntentChooser(final OpenExplorer activity, final Intent intent)
	{
		mListResolves = activity.getPackageManager().queryIntentActivities(
				intent,
				PackageManager.MATCH_DEFAULT_ONLY);
		setAdapter(activity, new OpenIntentAdapter(mListResolves));
	}
	public OpenIntentChooser(final Context activity, List<ResolveInfo> mResolves)
	{
		mListResolves = mResolves;
		setAdapter(activity, new OpenIntentAdapter(mResolves));
	}
	public OpenIntentChooser setAdapter(Context context, final OpenIntentAdapter adapter)
	{
		View v = LayoutInflater.from(context).inflate(R.layout.chooser_layout, null);
		v.findViewById(R.id.chooser_default).setOnClickListener(this);
		mDialog = new AlertDialog.Builder(context)
			.setView(v)
			.setAdapter(adapter, 
						new OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								if(mListener != null)
									mListener.onIntentSelected(mListResolves.get(which), mDefaultSelected);
							}
						})
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
	@Override
	public void onClick(View v) {
		switch(v.getId())
		{
		case R.id.chooser_default:
			mDefaultSelected = ((CheckBox)v).isChecked();
			break;
		}
	}
}
