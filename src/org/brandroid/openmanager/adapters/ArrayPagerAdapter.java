package org.brandroid.openmanager.adapters;

import java.util.ArrayList;
import java.util.List;

import org.brandroid.openmanager.data.OpenPath;
import org.brandroid.openmanager.fragments.ContentFragment;
import org.brandroid.utils.Logger;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;

public class ArrayPagerAdapter extends FragmentStatePagerAdapter {
	private List<Fragment> mExtraFrags = new ArrayList<Fragment>();

	public ArrayPagerAdapter(FragmentManager fm) {
		super(fm);
	}
	
	@Override
	public Fragment getItem(int pos) {
		return mExtraFrags.get(pos);
	}
	
	public Fragment getLastItem() {
		return mExtraFrags.get(getCount() - 1);
	}
	
	@Override
	public int getCount() {
		return mExtraFrags.size();
	}
	
	@Override
	public CharSequence getPageTitle(int position) {
		if(getItem(position) instanceof ContentFragment)
			return ((ContentFragment)getItem(position)).getPath().getName();
		else
			return super.getPageTitle(position);
	}
	
	public boolean checkForContentFragmentWithPath(OpenPath path)
	{
		for(Fragment f : mExtraFrags)
			if(f instanceof ContentFragment && ((ContentFragment)f).getPath().equals(path))
				return true;
		return false;
	}
	
	public boolean add(Fragment frag)
	{
		if(frag == null) return false;
		if(mExtraFrags.contains(frag)) return false;
		if(frag instanceof ContentFragment && checkForContentFragmentWithPath(((ContentFragment)frag).getPath()))
		Logger.LogVerbose("MyPagerAdapter Count: " + (getCount() + 1));
		boolean ret = mExtraFrags.add(frag);
		notifyDataSetChanged();
		return ret;
	}
	public void add(int index, Fragment frag)
	{
		if(frag == null) return;
		mExtraFrags.add(index, frag);
		notifyDataSetChanged();
	}
	
	public boolean remove(Fragment frag)
	{
		boolean ret = mExtraFrags.remove(frag);
		notifyDataSetChanged();
		return ret;
	}
	
	public void clear()
	{
		mExtraFrags.clear();
	}

	public List<Fragment> getFragments() {
		return mExtraFrags;
	}

	public void set(int index, Fragment frag) {
		mExtraFrags.set(index, frag);
	}
}
