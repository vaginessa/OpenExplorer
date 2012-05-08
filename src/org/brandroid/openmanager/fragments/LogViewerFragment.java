package org.brandroid.openmanager.fragments;

import java.util.ArrayList;
import java.util.List;

import org.brandroid.openmanager.R;
import org.brandroid.openmanager.activities.OpenExplorer;
import org.brandroid.openmanager.adapters.LinedArrayAdapter;
import org.brandroid.openmanager.fragments.OpenFragment.Poppable;
import org.brandroid.openmanager.util.BetterPopupWindow;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class LogViewerFragment extends OpenFragment
	implements OnClickListener, Poppable
{
	private static ArrayList<CharSequence> mData = new ArrayList<CharSequence>();
	private LinedArrayAdapter mAdapter = null;
	private boolean mAdded;
	private BetterPopupWindow mPopup = null;
	private View myView = null;
	private ListView mListView = null;
	
	public LogViewerFragment() {
	}
	
	@Override
	public View getView() {
		return myView;
	}
	
	public static LogViewerFragment getInstance(Bundle args)
	{
		LogViewerFragment ret = new LogViewerFragment();
		ret.setArguments(args);
		return ret;
	}
	
	public boolean getAdded() { return mAdded; } 
	public void setAdded(boolean added) { mAdded = added; }
	
	public void print(final String txt, final int color)
	{
		if(getActivity() == null) return;

		mData.add(0, colorify(txt, color));

		//getActivity().runOnUiThread(
		Runnable doPrint = new Runnable(){public void run(){
			if(mAdapter != null)
				mAdapter.notifyDataSetChanged();
			}};
		if(!Thread.currentThread().equals(OpenExplorer.UiThread))
			getActivity().runOnUiThread(doPrint);
		else
			doPrint.run();
	}
	
	private CharSequence colorify(String txt, int color)
	{
		if(color != 0)
		{
			SpannableString line = new SpannableString(txt);
			line.setSpan(new ForegroundColorSpan(color), 0, line.length(), Spanned.SPAN_COMPOSING);
			return line;
		} else return txt;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//setHasOptionsMenu(true);
	}
	
	@Override
	public boolean hasOptionsMenu() {
		return true;
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		if(menu == null || inflater == null) return;
		if(!isVisible()) return;
		if(isDetached()) return;
		super.onCreateOptionsMenu(menu, inflater);
		menu.clear();
		inflater.inflate(R.menu.text_editor, menu);
	}
	
	@Override
	public int getPagerPriority() {
		return 100;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		onClick(item.getItemId(), item, null);
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		myView = getListView();
		return myView;
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		mAdapter = new LinedArrayAdapter(getActivity(), R.layout.edit_text_view_row, mData);
		mAdapter.setShowLineNumbers(false);
		getListView().setAdapter(mAdapter);
	}
	
	public ListView getListView() {
		
		if(mListView == null && getView() != null)
		{
			if(getView() instanceof ListView)
				mListView = (ListView)getView();
			else if(getView().findViewById(android.R.id.list) != null)
				mListView = (ListView)getView().findViewById(android.R.id.list);
		}
		return mListView;
	}
	
	@Override
	public void onClick(View v) {
		onClick(v.getId(), null, v);
	}
	
	public void onClick(int id, MenuItem item, View from)
	{
		switch(id)
		{
			//default: if(getExplorer() != null) getExplorer().onClick(id, item, from);
		}
	}

	@Override
	public Drawable getIcon() {
		if(isDetached()) return null;
		return getResources().getDrawable(R.drawable.ic_paper);
	}

	@Override
	public CharSequence getTitle() {
		if(isDetached()) return "Network Log";
		if(getActivity() == null) return "Network Log";
		String ret = getResources().getString(R.string.s_pref_logview);
		if(ret == null)
			ret = "Network Log";
		return ret;
	}

	public ListAdapter getAdapter(Context c) {
		if(mAdapter == null)
		{
			mAdapter = new LinedArrayAdapter(c, R.layout.edit_text_view_row, mData);
			mAdapter.setShowLineNumbers(false);
		}
		return mAdapter;
		
	}

	public void setupPopup(Context c, View anchor) {
		if(mPopup == null)
		{
			mPopup = new BetterPopupWindow(c, anchor);
			if(mListView == null)
			{
				myView = mListView = new ListView(c);
				LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
				mListView.setLayoutParams(lp);
			}
			mListView.setAdapter(getAdapter(c));
			mPopup.setContentView(mListView);
		}
	}
	
	public BetterPopupWindow getPopup() { return mPopup; }
	
	
	
}






















