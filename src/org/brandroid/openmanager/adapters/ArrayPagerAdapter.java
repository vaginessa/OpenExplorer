package org.brandroid.openmanager.adapters;

import java.util.ArrayList;
import java.util.List;

import org.brandroid.openmanager.data.OpenFile;
import org.brandroid.openmanager.data.OpenNetworkPath;
import org.brandroid.openmanager.data.OpenPath;
import org.brandroid.openmanager.data.OpenServers;
import org.brandroid.openmanager.fragments.CarouselFragment;
import org.brandroid.openmanager.fragments.ContentFragment;
import org.brandroid.openmanager.fragments.OpenFragment;
import org.brandroid.openmanager.fragments.OpenPathFragmentInterface;
import org.brandroid.openmanager.fragments.SearchResultsFragment;
import org.brandroid.openmanager.fragments.TextEditorFragment;
import org.brandroid.openmanager.util.ThumbnailCreator;
import org.brandroid.utils.Logger;
import com.viewpagerindicator.TitleProvider;
import com.viewpagerindicator.TabPageIndicator.TabView;

import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnLongClickListener;

public class ArrayPagerAdapter extends FragmentStatePagerAdapter
		implements TitleProvider {
	//private static Hashtable<OpenPath, Fragment> mPathMap = new Hashtable<OpenPath, Fragment>();
	private List<Fragment> mFrags = new ArrayList<Fragment>();
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
			return mFrags.get(pos);
		else return null;
	}

	public Fragment getLastItem() {
		return mFrags.get(getCount() - 1);
	}

	@Override
	public int getCount() {
		return mFrags.size();
	}
	
	@Override
	public Parcelable saveState() {
		Bundle state = new Bundle();
		if(getCount() > 0)
		{
			OpenPath[] items = new OpenPath[getCount()];
			for(int i = 0; i < getCount(); i++)
				if(getItem(i) instanceof OpenPathFragmentInterface)
					items[i] = ((OpenPathFragmentInterface)getItem(i)).getPath();
			state.putParcelableArray("pages", items);
		}
		return state;
	}
	
	@Override
	public void restoreState(Parcelable state, ClassLoader loader) {
		if(state != null)
		{
			Bundle bundle = (Bundle)state;
			bundle.setClassLoader(loader);
			if(!bundle.containsKey("pages")) return;
			Parcelable[] items = bundle.getParcelableArray("pages");
			mFrags.clear();
			for(int i = 0; i < items.length; i++)
				if(items[i] != null && items[i] instanceof OpenPath)
				{
					OpenPath path = (OpenPath)items[i];
					if(path.isDirectory())
						mFrags.add(ContentFragment.getInstance(path));
					else if(path.isTextFile() || path.length() < 500000)
						mFrags.add(new TextEditorFragment(path));
				}
		}
	}
	
	@Override
	public int getItemPosition(Object object) {
		if(object instanceof OpenPathFragmentInterface)
		{
			OpenPath tofind = ((OpenPathFragmentInterface)object).getPath();
			if(tofind == null) return super.getItemPosition(object);
			for(int i = 0; i < getCount(); i++)
			{
				Fragment f = getItem(i);
				if(f instanceof OpenPathFragmentInterface)
				{
					OpenPath tocheck = ((OpenPathFragmentInterface)f).getPath();
					if(tocheck != null && tocheck.getPath().equals(tofind.getPath()))
						return i;
				}
			}
		}
		return super.getItemPosition(object);
	}

	@Override
	public CharSequence getPageTitle(int position) {
		Fragment f = getItem(position);
		
		CharSequence ret = null;
		
		if(f instanceof TextEditorFragment)
			return ((TextEditorFragment)f).getPath().getName();
		else if(f instanceof SearchResultsFragment)
			return ((SearchResultsFragment)f).getTitle();
		
		OpenPath path = null;
		if(f instanceof ContentFragment)
			path = ((ContentFragment)f).getPath();
		
		if(path == null)
			return ret;
		else if(path instanceof OpenNetworkPath && path.getParent() == null && ((OpenNetworkPath)path).getServersIndex() >= 0)
			return OpenServers.DefaultServers.get(((OpenNetworkPath)path).getServersIndex()).getName();
		else if(path.getName().equals(""))
			return path.getPath();
		else if(path.getParent() == null)
			return path.getName();
		else if(path.isDirectory() && !path.getName().endsWith("/"))
			return path.getName() + "/";
		else return path.getName();
	}
	
	public boolean checkForContentFragmentWithPath(OpenPath path) {
		if(mFrags == null) return false;
		for (Fragment f : mFrags)
			if (f instanceof OpenPathFragmentInterface
					&& ((OpenPathFragmentInterface) f).getPath() != null
					&& ((OpenPathFragmentInterface) f).getPath().equals(path))
				return true;
		return false;
	}

	public boolean add(Fragment frag) {
		if (frag == null)
			return false;
		if (mFrags.contains(frag))
			return false;
		if (frag instanceof ContentFragment
				&& checkForContentFragmentWithPath(((ContentFragment) frag).getPath()))
			return false;
		Logger.LogVerbose("MyPagerAdapter Count: " + (getCount() + 1));
		boolean ret = mFrags.add(frag);
		notifyDataSetChanged();
		return ret;
	}

	public void add(int index, Fragment frag) {
		if (frag == null)
			return;
		if (mFrags.contains(frag))
			return;
		if (frag instanceof ContentFragment
				&& checkForContentFragmentWithPath(((ContentFragment) frag)
						.getPath()))
			return;
		mFrags.add(Math.max(0, Math.min(getCount(), index)), frag);
		try { notifyDataSetChanged(); }
		catch(IllegalStateException eew) { }
		catch(IndexOutOfBoundsException e) {
			ArrayList<Fragment> recoveryArray = new ArrayList<Fragment>(mFrags);
			//recoveryArray.add(Math.max(0, Math.min(getCount() - 1, index)), frag);
			mFrags.clear();
			for(Fragment f : recoveryArray)
				mFrags.add(f);
			notifyDataSetChanged();
		}
	}

	public boolean remove(Fragment frag) {
		boolean ret = mFrags.remove(frag);
		notifyDataSetChanged();
		return ret;
	}
	public Fragment remove(int index) {
		return mFrags.remove(index);
	}

	public int removeOfType(Class c) {
		int ret = 0;
		for (int i = getCount() - 1; i >= 0; i--) {
			Fragment f = getItem(i);
			if (f.getClass().equals(c))
				mFrags.remove(i);
		}
		notifyDataSetChanged();
		return ret;
	}

	public void clear() {
		mFrags.clear();
	}

	public List<Fragment> getFragments() {
		return mFrags;
	}

	public void set(int index, Fragment frag) {
		if(index < getCount())
			mFrags.set(index, frag);
		else mFrags.add(frag);
	}

	@Override
	public String getTitle(int position) {
		if (getPageTitle(position) == null)
			return position + "?";
		return getPageTitle(position).toString();
	}

	public int getLastPositionOfType(Class class1) {
		for(int i = getCount() - 1; i > 0; i--)
			if(getItem(i).getClass().equals(class1))
				return i;
		return 0;
	}

	@SuppressWarnings("deprecation")
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

	public void replace(Fragment old, Fragment newFrag) {
		int pos = getItemPosition(old);
		if(pos == -1)
			mFrags.add(newFrag);
		else
			mFrags.set(pos, newFrag);
	}
}
