package org.brandroid.openmanager.adapters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

import org.brandroid.openmanager.adapters.ArrayPagerAdapter.OnPageTitleClickListener;
import org.brandroid.openmanager.data.OpenCursor;
import org.brandroid.openmanager.data.OpenMediaStore;
import org.brandroid.openmanager.data.OpenPath;
import org.brandroid.openmanager.data.OpenSearch;
import org.brandroid.openmanager.data.OpenZip;
import org.brandroid.openmanager.fragments.ContentFragment;
import org.brandroid.openmanager.fragments.OpenFragment;
import org.brandroid.openmanager.fragments.SearchResultsFragment;
import org.brandroid.openmanager.fragments.TextEditorFragment;
import com.viewpagerindicator.TabPageIndicator.TabView;
import com.viewpagerindicator.TitleProvider;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.view.View;
import android.view.View.OnLongClickListener;

public class OpenPathPagerAdapter extends FragmentStatePagerAdapter
	implements TitleProvider
{
	private ArrayList<OpenPath> mChildren = new ArrayList<OpenPath>();
	private ArrayPagerAdapter.OnPageTitleClickListener mListener = null;

	public OpenPathPagerAdapter(FragmentManager fm) {
		super(fm);
	}

	public void setOnPageTitleClickListener(OnPageTitleClickListener l)
	{
		mListener = l;
	}
	
	public OpenFragment getLastItem() { return getItem(getCount() - 1); }
	
	@Override
	public CharSequence getPageTitle(int position) {
		OpenPath path = mChildren.get(position);
		if(path instanceof OpenMediaStore || path instanceof OpenCursor)
			return path.getName();
		if(path instanceof OpenZip)
			return path.getName();
		return path.getName() + (path.isDirectory() ? "/" : "");
	}

	@Override
	public OpenFragment getItem(int pos) {
		if(mChildren.size() <= pos || pos < 0) return null;
		OpenPath path = mChildren.get(pos);
		if(path instanceof OpenSearch)
		{
			return SearchResultsFragment.getInstance((OpenSearch)path);
		} else {
			if(!path.isDirectory())
				return TextEditorFragment.getInstance(path, new Bundle());
			else
				return ContentFragment.getInstance(path);
		}
	}
	
	public OpenPath getPath(int pos)
	{
		return mChildren.get(pos);
	}
	
	@Override
	public int getCount() {
		return mChildren.size();
	}

	@Override
	public boolean modifyTab(TabView tab, final int position) {
		if(mListener != null)
		{
			tab.setOnLongClickListener(new OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					return mListener.onPageTitleLongClick(position, v);
				}
			});
			tab.setLongClickable(true);
		}
		return true;
	}

	public void add(OpenPath path)
	{
		mChildren.add(path);
	}
	
	@Override
	public void notifyDataSetChanged() {
		Collections.sort(mChildren, new Comparator<OpenPath>() {
			public int compare(OpenPath lhs, OpenPath rhs) {
				return lhs.compareTo(rhs);
			}
		});
		super.notifyDataSetChanged();
	}
	
	public OpenFragment remove(int index)
	{
		OpenFragment ret = getItem(index);
		mChildren.remove(index);
		notifyDataSetChanged();
		return ret;
	}

	public int indexOf(OpenPath path) {
		for(int i = 0; i < mChildren.size(); i++)
			if(mChildren.get(i).equals(path))
				return i;
		return -1;
	}

	public void add(int index, OpenPath path) {
		if(mChildren.contains(path))
			mChildren.remove(mChildren.indexOf(path));
		if(index >= mChildren.size())
			add(path);
		else
			mChildren.add(index, path);
	}

}
