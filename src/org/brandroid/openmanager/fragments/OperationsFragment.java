package org.brandroid.openmanager.fragments;

import java.util.ArrayList;

import org.brandroid.openmanager.R;
import org.brandroid.openmanager.fragments.OpenFragment.Poppable;
import org.brandroid.openmanager.util.BetterPopupWindow;
import org.brandroid.openmanager.util.EventHandler;
import org.brandroid.openmanager.util.EventHandler.BackgroundWork;
import org.brandroid.openmanager.util.EventHandler.OnWorkerUpdateListener;
import org.brandroid.utils.Logger;
import org.brandroid.utils.MenuUtils;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class OperationsFragment
	extends OpenFragment
	implements Poppable
{
	private ListView mList;
	private BetterPopupWindow mPopup = null;
	
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
			MenuUtils.setViewsVisible(convertView, false, android.R.id.icon);
			final View view = convertView;
			final ProgressBar pb = (ProgressBar)view.findViewById(android.R.id.progress);
			final TextView text1 = (TextView)view.findViewById(android.R.id.text1);
			final ImageButton closeButton = (ImageButton)view.findViewById(android.R.id.closeButton);
			if(closeButton != null)
			{
				closeButton.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						bw.cancel(true);
					}
				});
			}
			bw.updateView(view);
			bw.setWorkerUpdateListener(new OnWorkerUpdateListener() {
				public void onWorkerThreadComplete(int type, ArrayList<String> results) {
					notifyDataSetChanged();
				}
				public void onWorkerProgressUpdate(final int pos, final int total) {
					notifyDataSetChanged();
				}
			});
			return view;
		}
		
	}
	
	@Override
	public boolean hasOptionsMenu() {
		return true;
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		menu.clear();
		inflater.inflate(R.menu.operations, menu);
	}
	
	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		boolean isRunning = getHandler().getRunningTasks().length > 0;
		MenuUtils.setMenuEnabled(menu, isRunning, R.id.menu_ops_stop);
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
		mList.setAdapter(getTaskAdapter());
	}
	
	private BackgroundTaskAdapter getTaskAdapter()
	{
		if(mList != null && mList.getAdapter() != null && mList.getAdapter() instanceof BackgroundTaskAdapter)
			return (BackgroundTaskAdapter)mList.getAdapter();
		return new BackgroundTaskAdapter();
	}
	
	public ListView getListView() { return mList; }
	public void setListView(ListView lv) { mList = lv; }  
	
	public BaseAdapter getAdapter() {
		return getTaskAdapter();
	}
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

	@Override
	public void setupPopup(Context c, View anchor) {
		if(mPopup == null)
		{
			mPopup = new BetterPopupWindow(c, anchor);
			View v = getView();
			if(v == null)
				v = ((LayoutInflater)c.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
					.inflate(R.layout.operations_layout, null);
			if(mList == null)
				mList = (ListView)v.findViewById(android.R.id.list);
			if(mList.getAdapter() == null)
				mList.setAdapter(getAdapter());
			mPopup.setContentView(mList);
		} else
			mPopup.setAnchor(anchor);
	}

	@Override
	public BetterPopupWindow getPopup() {
		return mPopup;
	}

}
