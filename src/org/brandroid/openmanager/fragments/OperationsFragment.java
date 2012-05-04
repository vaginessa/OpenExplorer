package org.brandroid.openmanager.fragments;

import java.util.ArrayList;

import org.brandroid.openmanager.R;
import org.brandroid.openmanager.util.EventHandler;
import org.brandroid.openmanager.util.EventHandler.BackgroundWork;
import org.brandroid.openmanager.util.EventHandler.OnWorkerUpdateListener;
import org.brandroid.utils.Logger;

import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class OperationsFragment extends OpenFragment
{
	private ListView mList;
	
	class BackgroundTaskAdapter extends BaseAdapter
	{

		@Override
		public int getCount() {
			return EventHandler.getTaskList().size();
		}

		@Override
		public BackgroundWork getItem(int position) {
			return EventHandler.getTaskList().get((getCount() - 1) - position);
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			final BackgroundWork bw = getItem(position);
			if(convertView == null)
			{
				convertView = getLayoutInflater(getArguments()).inflate(
						R.layout.notification, parent, false);
				//convertView.findViewById(android.R.id.icon).setVisibility(View.GONE);
			}
			final View view = convertView;
			bw.updateView(view);
			bw.setWorkerUpdateListener(new OnWorkerUpdateListener() {
				public void onWorkerThreadComplete(int type, ArrayList<String> results) {
					bw.setWorkerUpdateListener(null);
					view.findViewById(android.R.id.progress).setVisibility(View.GONE);
					((TextView)view.findViewById(android.R.id.text1)).setText(R.string.s_toast_complete);
				}
				public void onWorkerProgressUpdate(int pos, int total) {
					Logger.LogDebug("Operations onProgressUpdate(" + pos + "," + total + ")");
					bw.updateView(view);
				}
			});
			return view;
		}
		
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.operations_layout, container, false);
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		mList = (ListView)view.findViewById(android.R.id.list);
		mList.setAdapter(new BackgroundTaskAdapter());
	}
	
	private BaseAdapter getAdapter() { return (BaseAdapter)mList.getAdapter(); }
	private void notifyDataSetChanged()
	{
		getAdapter().notifyDataSetChanged();
	}
	

	@Override
	public Drawable getIcon() {
		LayerDrawable ld = (LayerDrawable)getDrawable(R.drawable.sm_gear);
		AnimationDrawable ad = (AnimationDrawable)ld.getDrawable(ld.getNumberOfLayers() - 1);
		ad.start();
		return ld;
	}

	@Override
	public CharSequence getTitle() {
		return getString(R.string.s_title_operations);
	}

}
