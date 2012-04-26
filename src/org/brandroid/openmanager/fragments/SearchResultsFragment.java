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
	private final OpenSearch mSearch;
	private TextView mTextSummary;
	private ProgressBar mProgressBar;
	private Button mCancel;
	
	public SearchResultsFragment()
	{
		Bundle b = getArguments();
		if(b != null)
		{
			String q = getArguments().getString("query");
			OpenPath path = null;
			try {
				path = FileManager.getOpenCache(getArguments().getString("path"), false, null);
			} catch(IOException e) { path = new OpenFile(getArguments().getString("path")); }
			ArrayList<Parcelable> results = null;
			if(b.containsKey("results"))
			{
				results = getParcelableArrayList("results");
				mSearch = new OpenSearch(q, path, results);
			} else {
				mSearch = new OpenSearch(q, path, this);
				mSearch.start();
			}
		}
		else mSearch = null;
	}
	private ArrayList<Parcelable> getParcelableArrayList(String string) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString("query", mSearch.getQuery());
		outState.putString("path", mSearch.getBasePath().getPath());
		if(mSearch.isRunning())
			mSearch.cancelSearch();
		else
			outState.putParcelableArrayList("results", getResults());
	}
	public SearchResultsFragment(OpenPath searchIn, String query)
	{
		mSearch = new OpenSearch(query, searchIn, this);
		mSearch.start();
	}
	
	@Override
	public OpenPath getPath() {
		return mSearch;
	}
	
	public ArrayList<OpenPath> getResults() { return (ArrayList<OpenPath>) mSearch.getResults(); }
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContentAdapter = new ContentAdapter(getExplorer(), R.layout.list_content_layout, mSearch.getResults());
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
		mTextSummary.setText(getString(R.string.search_summary, mSearch.getQuery(), mSearch.getBasePath().getPath(), 0));
		mCancel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mSearch.cancelSearch();
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
		return "\"" + mSearch.getQuery() + "\"" + " (" + getResults().size() + ")";
	}
	@Override
	public void onUpdate() {
		if(isVisible())
		{
			mGrid.post(new Runnable(){
				public void run() {
					mContentAdapter.notifyDataSetChanged();
			}});
		}
	}
	@Override
	public void onFinish() {
		setProgressVisibility(false);
		if(isVisible())
		{
			mGrid.post(new Runnable(){
				public void run() {
					mContentAdapter.notifyDataSetChanged();
			}});
		}
		if(mTextSummary != null)
			mTextSummary.post(new Runnable(){public void run(){
				mTextSummary.setText(getString(R.string.search_results, getResults().size(), mSearch.getQuery(), mSearch.getBasePath().getPath()));
			}});
		if(mCancel != null)
			mCancel.post(new Runnable(){public void run(){
				mCancel.setText(android.R.string.ok);
			}});
	}
}
