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
import android.widget.ImageView;
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
			((ImageView)convertView.findViewById(android.R.id.icon)).setImageResource(bw.getNotifIconResId());
			final View view = convertView;
			final ProgressBar pb = (ProgressBar)view.findViewById(android.R.id.progress);
			final TextView text1 = (TextView)view.findViewById(android.R.id.text1);
			bw.updateView(view);
			bw.setWorkerUpdateListener(new OnWorkerUpdateListener() {
				public void onWorkerThreadComplete(int type, ArrayList<String> results) {
					bw.setWorkerUpdateListener(null);
					view.post(new Runnable(){public void run(){
						pb.setVisibility(View.GONE);
						text1.setText(R.string.s_toast_complete);
						notifyDataSetChanged();
					}});
				}
				public void onWorkerProgressUpdate(final int pos, final int total) {
					view.post(new Runnable(){public void run(){
						Logger.LogDebug("Operations onProgressUpdate(" + pos + "," + total + " :: " + bw.getProgressA() + "," + bw.getProgressB() + ")");
						pb.setProgress(bw.getProgressA());
						pb.setSecondaryProgress(bw.getProgressB());
						text1.setText(bw.getLastRate());
					}});
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
		return getDrawable(R.drawable.sm_gear_anim);
	}

	@Override
	public CharSequence getTitle() {
		if(getExplorer() != null)
			return getExplorer().getString(R.string.s_title_operations);
		else
			return "File Operations";
	}

}
