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
import org.brandroid.openmanager.data.OpenSearch;
import org.brandroid.openmanager.data.OpenSearch.SearchProgressUpdateListener;
import org.brandroid.openmanager.util.FileManager;

import android.graphics.drawable.Drawable;
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
	
	public SearchResultsFragment()
	{
		
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
		if(getSearch().isRunning())
			getSearch().cancelSearch();
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString("query", getSearch().getQuery());
		outState.putString("path", getSearch().getBasePath().getPath());
		outState.putParcelableArrayList("results", getResults());
		outState.putBoolean("running", getSearch().isRunning());
	}
	
	@Override
	public OpenPath getPath() {
		return getSearch();
	}
	
	public ArrayList<OpenPath> getResults() {
		if(getSearch() != null)
			return (ArrayList<OpenPath>) getSearch().getResults();
		else return null;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle b = getArguments();
		if(b == null || (savedInstanceState != null && savedInstanceState.containsKey("path")))
			b = savedInstanceState;
		if(b != null)
		{
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
					getSearch().start();
			} else {
				mPath = new OpenSearch(q, path, this);
				getSearch().start();
			}
		}
		else {
			//throw new Exception("Couldn't search for emptiness");
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
				getSearch().cancelSearch();
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
		return "\"" + getSearch().getQuery() + "\"" + " (" + getResults().size() + ")";
	}
	
	@Override
	public void onUpdate() {
		if(mGrid == null) return;
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
