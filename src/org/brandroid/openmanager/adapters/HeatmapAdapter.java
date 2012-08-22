package org.brandroid.openmanager.adapters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.brandroid.openmanager.R;
import org.brandroid.openmanager.data.OpenPath;
import org.brandroid.openmanager.fragments.DialogHandler;
import org.brandroid.openmanager.interfaces.OpenApp;
import org.brandroid.openmanager.util.ThumbnailCreator;
import org.brandroid.utils.Logger;

import com.android.gallery3d.common.Utils;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class HeatmapAdapter extends BaseAdapter
{
	private long mTotalBytes = 0l;
	private final OpenPath mParent;
	private final OpenApp mApp;
	private final List<Pair<Long, OpenPath>> mPairs;
	private final Hashtable<OpenPath, ScanSizeTask> mTasks = new Hashtable<OpenPath, HeatmapAdapter.ScanSizeTask>();
	
	public HeatmapAdapter(OpenApp app, OpenPath parent)
	{
		mApp = app;
		mParent = parent;
		OpenPath[] items = new OpenPath[0];
		try {
			items = mParent.list();
		} catch (IOException e) {
			Logger.LogError("Couldn't list for Heatmap.", e);
		}
		mPairs = new ArrayList<Pair<Long,OpenPath>>();
		for(OpenPath kid : items)
			mPairs.add(new Pair<Long, OpenPath>(kid.length(), kid));
	}
	
	@Override
	public void notifyDataSetChanged() {
		Collections.sort(mPairs, new Comparator<Pair<Long, OpenPath>>() {
			@Override
			public int compare(Pair<Long, OpenPath> lhs,
					Pair<Long, OpenPath> rhs) {
				if(lhs.first.equals(rhs.first))
					return lhs.second.getName().compareTo(rhs.second.getName());
				return rhs.first.compareTo(lhs.first);
			}
		});
		super.notifyDataSetChanged();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = convertView;
		if(view == null)
			view = LayoutInflater.from(parent.getContext())
						.inflate(R.layout.heatmap_row, parent, false);
		
		ImageView mIcon = (ImageView)view.findViewById(R.id.content_icon);
		TextView mText = (TextView)view.findViewById(R.id.content_text);
		TextView mSize = (TextView)view.findViewById(R.id.content_count);
		ProgressBar mBar = (ProgressBar)view.findViewById(android.R.id.progress);
		
		OpenPath path = getItem(position);
		mIcon.setImageBitmap(ThumbnailCreator.generateThumb(mApp, path, 32, 32, parent.getContext()).get());
		mText.setText(path.getName());
		
		mBar.setMax((int)mTotalBytes);
		
		if(!path.isDirectory())
		{
			mSize.setText("Size: " + DialogHandler.formatSize(path.length()));
			mBar.setProgress((int)path.length());
		} else if(!mTasks.containsKey(path))
		{
			mBar.setProgress(0);
			ScanSizeTask task = new ScanSizeTask(mSize, mBar);
			mTasks.put(path, task);
			task.execute(path);
		}
		
		return view;
	}
	
	public class ScanSizeTask extends AsyncTask<OpenPath, Long, Long>
	{
		private final ProgressBar mBar;
		private final TextView mSizeText;
		
		public ScanSizeTask(TextView sizeText, ProgressBar bar)
		{
			mSizeText = sizeText;
			mBar = bar;
		}

		@Override
		protected Long doInBackground(OpenPath... params) {
			return ScanDir(params[0]);
		}
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			mBar.setIndeterminate(true);
		}
		
		@Override
		protected void onProgressUpdate(Long... values)
		{
			super.onProgressUpdate(values);
			mSizeText.setText(values[0].toString());
		}
		
		@Override
		protected void onPostExecute(Long result) {
			super.onPostExecute(result);
			mSizeText.setText("Size: " + DialogHandler.formatSize(result));
			mBar.setMax((int)mTotalBytes);
			mBar.setProgress((int)(long)result);
			mBar.setIndeterminate(false);
			notifyDataSetChanged();
		}
		
		private Long ScanDir(OpenPath path)
		{
			Long mTotal = 0l;
			if(path.isDirectory())
			{
				OpenPath[] kids = null;
				try {
					kids = path.list();
				} catch (IOException e) {
				}
				if(kids != null)
					for(OpenPath kid : kids)
						mTotal += ScanDir(kid);
			} else {
				long bytes = path.length();
				mTotalBytes += bytes;
				mTotal += bytes;
			}
			return mTotal;
		}
	}

	@Override
	public int getCount() {
		return mPairs.size();
	}

	@Override
	public OpenPath getItem(int position) {
		return mPairs.get(position).second;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

}
