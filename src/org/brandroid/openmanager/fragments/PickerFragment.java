package org.brandroid.openmanager.fragments;

import org.brandroid.openmanager.data.OpenPath;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;

public class PickerFragment extends OpenFragment
{
	private Context mContext;
	private OnOpenPathPickedListener mPickListener;
	
	public PickerFragment(Context context)
	{
		mContext = context;
	}
	
	public interface OnOpenPathPickedListener
	{
		public void onOpenPathPicked(OpenPath path);
	}
	
	public void setOnOpenPathPickedListener(OnOpenPathPickedListener listener)
	{
		mPickListener = listener;
	}
	
	@Override
	public View getView() {
		// TODO Auto-generated method stub
		return super.getView();
	}
	
	@Override
	public Drawable getIcon() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CharSequence getTitle() {
		// TODO Auto-generated method stub
		return null;
	}

}
