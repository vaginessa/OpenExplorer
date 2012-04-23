package org.brandroid.openmanager.fragments;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.brandroid.openmanager.R;
import org.brandroid.openmanager.activities.OpenExplorer;
import org.brandroid.openmanager.adapters.ArrayPagerAdapter;
import org.brandroid.openmanager.adapters.FileSystemAdapter;
import org.brandroid.openmanager.data.OpenPath;
import org.brandroid.openmanager.util.FileManager;
import org.brandroid.openmanager.util.IntentManager;
import org.brandroid.utils.Logger;
import org.brandroid.utils.Preferences;

import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

public class SearchResultsFragment
		extends OpenFragment
		implements OnItemClickListener, OnItemLongClickListener
{
	private String mQuery;
	private OpenPath mPath;
	private GridView mGrid = null;
	private ArrayList<OpenPath> mResultsArray;
	private SearchWithinTask mTask;
	private int mSearchedDirs = 0;
	private long mLastUpdate = 0;
	private TextView mTextSummary;
	private ProgressBar mProgressBar;
	private Button mCancel;
	private Integer mViewMode = OpenExplorer.VIEW_LIST;
	
	public class SearchWithinTask extends AsyncTask<Void, OpenPath, Void>
	{
		@Override
		protected Void doInBackground(Void... params) {
			try {
				SearchWithin(mPath);
			} catch (IOException e) {
				Logger.LogWarning("Couldn't search within " + mPath.getPath(), e);
			}
			return null;
		}
		
		private void SearchWithin(OpenPath dir) throws IOException
		{
			if(isCancelled()) return;
			mSearchedDirs++;
			for(OpenPath kid : dir.listFiles())
				if(isMatch(kid.getName().toLowerCase(), mQuery.toLowerCase()))
					mResultsArray.add(kid);
			if(new Date().getTime() - mLastUpdate > 500)
				publishProgress();
			for(OpenPath kid : dir.listFiles())
				if(kid.isDirectory())
					SearchWithin(kid);
		}
		
		@Override
		protected void onCancelled(Void result) {
			super.onCancelled();
			onPostExecute(result);
		}
		
		@Override
		protected void onCancelled() {
			super.onCancelled();
			onPostExecute(null);
		}
		
		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			if(mTextSummary != null)
				mTextSummary.setText(getString(R.string.search_results, mResultsArray.size(), mQuery, mPath.getPath()));
			if(mProgressBar != null)
				mProgressBar.setVisibility(View.GONE);
			if(mCancel != null)
				mCancel.setText(android.R.string.ok);
			Logger.LogDebug("Done Searching!");
		}
		
		private boolean isMatch(String a, String b)
		{
			return a.toLowerCase().indexOf(b.toLowerCase()) > -1 ? true : false;
		}
		
		@Override
		protected void onProgressUpdate(OpenPath... values) {
			for(OpenPath result : values)
				mResultsArray.add(result);
			mLastUpdate = new Date().getTime();
			if(mContentAdapter != null)
				mContentAdapter.notifyDataSetChanged();
			if(mTextSummary != null)
				mTextSummary.setText(getString(R.string.search_summary, mQuery, mPath.getPath(), mSearchedDirs));
		}
	}
	
	public SearchResultsFragment()
	{
		
	}
	public SearchResultsFragment(OpenPath searchIn, String query)
	{
		mPath = searchIn;
		mQuery = query;
		mResultsArray = new ArrayList<OpenPath>();
		mTask = new SearchWithinTask();
		mTask.execute();
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContentAdapter = new FileSystemAdapter(getExplorer(), R.layout.list_content_layout, mResultsArray);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View ret = inflater.inflate(R.layout.search_results, container, false);
		mGrid = (GridView) ret.findViewById(R.id.content_grid);
		mTextSummary = (TextView)ret.findViewById(R.id.search_summary);
		mProgressBar = (ProgressBar)ret.findViewById(android.R.id.progress);
		mCancel = (Button)ret.findViewById(R.id.search_cancel);
		mGrid.setOnItemClickListener(this);
		mGrid.setOnItemLongClickListener(this);
		if(!OpenExplorer.USE_PRETTY_CONTEXT_MENUS) //|| !USE_ACTIONMODE)
			registerForContextMenu(mGrid);
		updateGridView();
		return ret;
	}
	
	private void updateGridView()
	{
		int mLayoutID;
		if(getViewMode() == OpenExplorer.VIEW_GRID) {
			mLayoutID = R.layout.grid_content_layout;
			int iColWidth = getResources().getDimensionPixelSize(R.dimen.grid_width);
			mGrid.setColumnWidth(iColWidth);
		} else {
			mLayoutID = R.layout.list_content_layout;
			int iColWidth = getResources().getDimensionPixelSize(R.dimen.list_width);
			mGrid.setColumnWidth(iColWidth);
		}
		mContentAdapter = new FileSystemAdapter(getExplorer(), mLayoutID, mResultsArray);
		((FileSystemAdapter)mContentAdapter).setViewMode(getViewMode());
		mGrid.setAdapter(mContentAdapter);
		mContentAdapter.notifyDataSetChanged();
	}
	
	public int getViewMode() {
		return OpenExplorer.VIEW_LIST;
	}

	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		mTextSummary.setText(getString(R.string.search_summary, mQuery, mPath.getPath(), 0));
		mCancel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(mTask.getStatus() != Status.FINISHED && !mTask.isCancelled())
					mTask.cancel(false);
				if(getExplorer() != null && getExplorer().isViewPagerEnabled())
				{
					final ViewPager pager = (ViewPager)getExplorer().findViewById(R.id.content_pager);
					final ArrayPagerAdapter adapter = (ArrayPagerAdapter)pager.getAdapter();
					final int pos = pager.getCurrentItem() - 1;
					pager.post(new Runnable() {public void run() {
						adapter.remove(SearchResultsFragment.this);
						pager.setAdapter(adapter);
						pager.setCurrentItem(pos, false);
						}});
				} else if(getFragmentManager() != null)
					getFragmentManager().popBackStack();
			}
		});
	}
	
	public Drawable getIcon() {
		if(getActivity() != null)
			return getActivity().getResources().getDrawable(R.drawable.sm_folder_search);
		else return null;
	}

	@Override
	public CharSequence getTitle() {
		return "\"" + mQuery + "\"" + " (" + mResultsArray.size() + ")";
	}
}
