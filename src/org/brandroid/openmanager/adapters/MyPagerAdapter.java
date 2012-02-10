package org.brandroid.openmanager.adapters;

import java.util.ArrayList;
import java.util.List;

import org.brandroid.openmanager.fragments.ContentFragment;
import org.brandroid.utils.Logger;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

public class MyPagerAdapter extends android.support.v4.app.FragmentPagerAdapter {
	private List<Fragment> mExtraFrags = new ArrayList<Fragment>();

	public MyPagerAdapter(FragmentManager fm) {
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
	
	public boolean add(Fragment frag)
	{
		if(frag == null) return false;
		Logger.LogVerbose("MyPagerAdapter Count: " + (getCount() + 1));
		return mExtraFrags.add(frag);
		//notifyDataSetChanged();
		//return ret;
	}
	
	public boolean remove(Fragment frag)
	{
		return mExtraFrags.remove(frag);
		//notifyDataSetChanged();
		//return ret;
	}
	
	public void clear()
	{
		mExtraFrags.clear();
	}
}
