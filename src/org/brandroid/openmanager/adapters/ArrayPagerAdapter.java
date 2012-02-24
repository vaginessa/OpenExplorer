package org.brandroid.openmanager.adapters;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.brandroid.openmanager.activities.OpenExplorer;
import org.brandroid.openmanager.data.OpenFile;
import org.brandroid.openmanager.data.OpenPath;
import org.brandroid.openmanager.fragments.ContentFragment;
import org.brandroid.openmanager.fragments.TextEditorFragment;
import org.brandroid.openmanager.util.ThumbnailCreator;
import org.brandroid.utils.Logger;
import com.viewpagerindicator.TitleProvider;
import com.viewpagerindicator.TabPageIndicator.TabView;

import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.view.Gravity;
import android.view.TextureView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.TextView;

public class ArrayPagerAdapter extends FragmentStatePagerAdapter
		implements TitleProvider {
	private List<Fragment> mExtraFrags = new ArrayList<Fragment>();
	private OnPageTitleClickListener mListener = null;

	public ArrayPagerAdapter(FragmentManager fm) {
		super(fm);
	}
	
	public interface OnPageTitleClickListener
	{
		public boolean onPageTitleLongClick(int position, View view);
	}
	
	public void setOnPageTitleClickListener(OnPageTitleClickListener l)
	{
		mListener = l;
	}

	@Override
	public Fragment getItem(int pos) {
		if(pos < getCount())
			return mExtraFrags.get(pos);
		else return null;
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
		Fragment f = getItem(position);
		OpenPath path = null;
		if(f instanceof ContentFragment)
			path = ((ContentFragment)f).getPath();
		else if(f instanceof TextEditorFragment)
			return ((TextEditorFragment)f).getPath().getName();
		
		if(path == null)
			return super.getPageTitle(position);
		else if(path instanceof OpenFile && path.isDirectory())
			return path.getName() + "/";
		
		else return path.getName();
	}
	
	public boolean checkForContentFragmentWithPath(OpenPath path) {
		for (Fragment f : mExtraFrags)
			if (f instanceof ContentFragment
					&& ((ContentFragment) f).getPath().equals(path))
				return true;
		return false;
	}

	public boolean add(Fragment frag) {
		if (frag == null)
			return false;
		if (mExtraFrags.contains(frag))
			return false;
		if (frag instanceof ContentFragment
				&& checkForContentFragmentWithPath(((ContentFragment) frag).getPath()))
			return false;
		Logger.LogVerbose("MyPagerAdapter Count: " + (getCount() + 1));
		boolean ret = mExtraFrags.add(frag);
		notifyDataSetChanged();
		return ret;
	}

	public void add(int index, Fragment frag) {
		if (frag == null)
			return;
		if (mExtraFrags.contains(frag))
			return;
		if (frag instanceof ContentFragment
				&& checkForContentFragmentWithPath(((ContentFragment) frag)
						.getPath()))
			return;
		mExtraFrags.add(index, frag);
		try {
			notifyDataSetChanged();
		} catch(IllegalStateException eew) { }
	}

	public boolean remove(Fragment frag) {
		boolean ret = mExtraFrags.remove(frag);
		notifyDataSetChanged();
		return ret;
	}

	public <T extends Fragment> List<T> getItemsOfType(T c) {
		List<T> ret = new ArrayList<T>();
		for (Fragment f : mExtraFrags)
			if (f.getClass().equals(c))
				ret.add((T) f);
		return ret;
	}

	public int removeOfType(Class c) {
		int ret = 0;
		for (int i = getCount() - 1; i >= 0; i--) {
			Fragment f = getItem(i);
			if (f.getClass().equals(c))
				mExtraFrags.remove(i);
		}
		return ret;
	}

	public void clear() {
		mExtraFrags.clear();
	}

	public List<Fragment> getFragments() {
		return mExtraFrags;
	}

	public void set(int index, Fragment frag) {
		if(index < getCount())
			mExtraFrags.set(index, frag);
		else mExtraFrags.add(frag);
	}

	@Override
	public String getTitle(int position) {
		if (getPageTitle(position) == null)
			return "";
		return getPageTitle(position).toString();
	}

	public int getLastPositionOfType(Class class1) {
		for(int i = getCount() - 1; i > 0; i--)
			if(getItem(i).getClass().equals(class1))
				return i;
		return 0;
	}

	@Override
	public Drawable[] getIcons(int position) {
		Fragment f = getItem(position);
		if(f == null) return new Drawable[0];
		if (f instanceof TextEditorFragment)
			return new Drawable[]{new BitmapDrawable(ThumbnailCreator.getFileExtIcon(
					((TextEditorFragment) f).getPath().getExtension(), f
							.getActivity().getApplicationContext(), false))};
		return new Drawable[0];
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
}
