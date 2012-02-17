package org.brandroid.openmanager.views;

import java.util.ArrayList;
import java.util.List;

import org.brandroid.openmanager.R;
import org.brandroid.openmanager.adapters.ArrayPagerAdapter;

import com.viewpagerindicator.PageIndicator;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.animation.AnimationUtils;

public class OpenViewPager extends ViewPager
{
	private PageIndicator mIndicator = null;
	private List<OnPageChangeListener> mListeners = new ArrayList<OnPageChangeListener>();
	private OnPageIndicatorChangeListener mIndicatorListener = null;

	public OpenViewPager(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
		super.setOnPageChangeListener(new OnPageChangeListener() {
			
			@Override
			public void onPageSelected(int arg0) {
				for(OnPageChangeListener l : mListeners)
					l.onPageSelected(arg0);
			}
			
			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {

				for(OnPageChangeListener l : mListeners)
					l.onPageScrolled(arg0, arg1, arg2);
			}
			
			@Override
			public void onPageScrollStateChanged(int arg0) {

				for(OnPageChangeListener l : mListeners)
					l.onPageScrollStateChanged(arg0);
			}
		});
	}
	
	public void setOnPageIndicatorChangeListener(OnPageIndicatorChangeListener l)
	{
		mIndicatorListener = l;
	}
	
	@Override
	public void setAdapter(PagerAdapter a) {
		super.setAdapter(a);
		setIndicator(mIndicator);
	}
	
	@Override
	public void setOnPageChangeListener(OnPageChangeListener listener) {
		//super.setOnPageChangeListener(listener);
		mListeners.add(listener);
	}

	public void setIndicator(PageIndicator indicator)
	{
		mIndicator = indicator;
		if(mIndicatorListener != null)
			mIndicatorListener.onPageIndicatorChange();
		if(mIndicator == null) return;
		if(getAdapter() != null)
		{
			mIndicator.setViewPager(this);
			mIndicator.notifyDataSetChanged();
		}
	}
	
	public interface OnPageIndicatorChangeListener
	{
		public void onPageIndicatorChange();
	}

	public Fragment getCurrentFragment() {
		// TODO Auto-generated method stub
		return ((ArrayPagerAdapter)getAdapter()).getItem(getCurrentItem());
	}
}
