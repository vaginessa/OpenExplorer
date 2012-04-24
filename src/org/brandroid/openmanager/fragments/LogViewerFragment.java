package org.brandroid.openmanager.fragments;

import java.util.ArrayList;
import java.util.List;

import org.brandroid.openmanager.R;
import org.brandroid.openmanager.adapters.LinedArrayAdapter;

import android.content.Context;
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
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class LogViewerFragment extends OpenFragment implements OnClickListener
{
	private ArrayList<CharSequence> mData = new ArrayList<CharSequence>();
	private ArrayAdapter<CharSequence> mAdapter = null;
	
	public LogViewerFragment() {
	}
	
	public void print(final String txt, final int color)
	{
		getActivity().runOnUiThread(new Runnable(){public void run(){
		mData.add(0, colorify(txt, color));
		if(mData.size() > 100)
			mData.removeAll(mData.subList(80, mData.size()));
		if(mAdapter != null)
			mAdapter.notifyDataSetChanged();
		}});
		/*
		builder.insert(0, colorify(txt, color));
		if(builderLines++ > 100)
		{
			builder.delete(80, 100);
			builderLines -= 20;
		}
		if(isVisible())
			getActivity().runOnUiThread(new Runnable() {public void run() {
				if(mTextLog != null)
					mTextLog.setText(builder);
			}});*/
	}
	private CharSequence colorify(String txt, int color)
	{
		if(color != 0)
		{
			SpannableString line = new SpannableString(txt + "\n");
			line.setSpan(new ForegroundColorSpan(color), 0, line.length(), Spanned.SPAN_COMPOSING);
			return line;
		} else return txt + "\n";
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		if(menu == null || inflater == null) return;
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.text_editor, menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		onClick(item.getItemId(), item, null);
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.log_viewer, container, false);
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		mAdapter = new LinedArrayAdapter(getActivity(), R.layout.edit_text_view_row, mData);
		getListView().setAdapter(mAdapter);
	}
	
	public ListView getListView() { return (ListView)getView().findViewById(android.R.id.list); }
	
	@Override
	public void onClick(View v) {
		onClick(v.getId(), null, v);
	}
	
	public void onClick(int id, MenuItem item, View from)
	{
		switch(id)
		{
			default: if(getExplorer() != null) getExplorer().onClick(id, item, from);
		}
	}

	@Override
	public Drawable getIcon() {
		return getResources().getDrawable(R.drawable.ic_paper);
	}

	@Override
	public CharSequence getTitle() {
		return getString(R.string.s_pref_logview);
	}
	
	
}
