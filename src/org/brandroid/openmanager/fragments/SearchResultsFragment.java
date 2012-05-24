package org.brandroid.openmanager.fragments;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.brandroid.openmanager.R;
import org.brandroid.openmanager.activities.OpenExplorer;
import org.brandroid.openmanager.adapters.ArrayPagerAdapter;
import org.brandroid.openmanager.adapters.ContentAdapter;
import org.brandroid.openmanager.data.OpenFile;
import org.brandroid.openmanager.data.OpenPath;
import org.brandroid.openmanager.data.OpenPath.OpenPathThreadUpdater;
import org.brandroid.openmanager.data.OpenSearch;
import org.brandroid.openmanager.data.OpenSearch.SearchProgressUpdateListener;
import org.brandroid.openmanager.util.FileManager;
import org.brandroid.utils.Logger;

import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

public class SearchResultsFragment
		extends ContentFragment
		implements OnItemClickListener, OnItemLongClickListener, SearchProgressUpdateListener
{
	private TextView mTextSummary;
	private ProgressBar mProgressBar;
	private Button mCancel;
	private SearchTask myTask = new SearchTask();
	private int lastNotedCount = 0;
	
	private SearchResultsFragment() {
		setArguments(new Bundle());
	}
	
	private class SearchTask extends AsyncTask<Void, Void, Void>
	{

		@Override
		protected Void doInBackground(Void... params) {
			getSearch().setThreadUpdateCallback(new OpenPathThreadUpdater() {
				@Override
				public void update(String status) {
					publishProgress();
				}
				@Override
				public void update(int progress, int total) {
					publishProgress();
				}
			});
			getSearch().start();
			return null;
		}
		
		@Override
		protected void onProgressUpdate(Void... values) {
			notifyDataSetChanged();
		}
		
		@Override
		protected void onPostExecute(Void result) {
			notifyDataSetChanged();
		}
		
	}
	
	public static SearchResultsFragment getInstance(Bundle args)
	{
		SearchResultsFragment ret = new SearchResultsFragment();
		ret.setArguments(args);
		return ret;
	}
	public static SearchResultsFragment getInstance(OpenPath basePath, String query)
	{
		Bundle data = new Bundle();
		data.putString("query", query);
		data.putString("path", basePath.getPath());
		return getInstance(data);
	}
	
	public OpenSearch getSearch()
	{
		return (OpenSearch)mPath;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		if(myTask.getStatus() == Status.RUNNING)
			myTask.cancel(true);
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString("query", getSearch().getQuery());
		outState.putString("path", getSearch().getBasePath().getPath());
		if(myTask.getStatus() == Status.RUNNING)
			outState.putBoolean("running", true);
		else
			outState.putParcelableArrayList("results", getResults());
	}
	
	@Override
	public OpenSearch getPath() {
		return getSearch();
	}
	
	public ArrayList<OpenPath> getResults() {
		if(getSearch() != null)
			return (ArrayList<OpenPath>) getSearch().getResults();
		else return null;
	}
	
	@Override
	public boolean onBackPressed() {
		if(myTask.getStatus() == Status.RUNNING)
			myTask.cancel(true);
		else if(getExplorer() != null && getExplorer().isViewPagerEnabled())
			getExplorer().closeFragment(this);
		else if(getFragmentManager() != null && getFragmentManager().getBackStackEntryCount() > 0)
			getFragmentManager().popBackStack();
		else if(getActivity() != null)
			getActivity().finish();
		return true;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle b = getArguments();
		if(b == null)
			b = new Bundle();
		if(savedInstanceState != null && savedInstanceState.containsKey("path"))
		{
			Logger.LogDebug(b.toString() + " switching to " + savedInstanceState.toString());
			b = savedInstanceState;
		}
		String q = "";
		if(b.containsKey("query"))
			q = b.getString("query");
		OpenPath path = new OpenFile("/");
		if(b.containsKey("path"))
			path = FileManager.getOpenCache(b.getString("path"));
		if(b.containsKey("results"))
		{
			ArrayList<Parcelable> results = b.getParcelableArrayList("results");
			mPath = new OpenSearch(q, path, this, results);
			if(b.containsKey("running") && b.getBoolean("running"))
				myTask.execute();
		} else {
			mPath = new OpenSearch(q, path, this);
			myTask.execute();
		}
		mContentAdapter = new ContentAdapter(getExplorer(), OpenExplorer.VIEW_LIST, getSearch());
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
	
	@Override
	public void setProgressVisibility(boolean visible) {
		if(mProgressBar != null)
			mProgressBar.setVisibility(visible ? View.VISIBLE : View.GONE);
		super.setProgressVisibility(visible);
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		if(mTextSummary != null && getSearch() != null && getSearch().getBasePath() != null)
			mTextSummary.setText(getString(R.string.search_summary, getSearch().getQuery(), getSearch().getBasePath().getPath(), 0));
		mCancel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				myTask.cancel(true);
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
		return getDrawable(R.drawable.sm_folder_search);
	}

	@Override
	public CharSequence getTitle() {
		if(getSearch() != null)
			return "\"" + getSearch().getQuery() + "\"" + " (" + getResults().size() + ")";
		else return getString(android.R.string.search_go);
	}
	
	@Override
	public void onUpdate() {
		if(mGrid == null) return;
		if(lastNotedCount >= getSearch().getListLength())
		mGrid.post(new Runnable(){
			public void run() {
				mContentAdapter.notifyDataSetChanged();
				notifyPager();
		}});
	}
	@Override
	public void onFinish() {
		if(getView() == null) return;
		getView().post(new Runnable(){
			public void run() {
				setProgressVisibility(false);
				mContentAdapter.notifyDataSetChanged();
				notifyPager();
				mTextSummary.setText(getString(R.string.search_results, getResults().size(), getSearch().getQuery(), getSearch().getBasePath().getPath()));
				mCancel.setText(android.R.string.ok);
		}});
	}
}
