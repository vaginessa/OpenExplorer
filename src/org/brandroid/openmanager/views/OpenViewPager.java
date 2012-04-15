package org.brandroid.openmanager.views;

import java.util.ArrayList;
import java.util.List;

import org.brandroid.openmanager.R;
import org.brandroid.openmanager.adapters.ArrayPagerAdapter;
import org.brandroid.utils.Logger;

import com.viewpagerindicator.PageIndicator;
import com.viewpagerindicator.TabPageIndicator;
import com.viewpagerindicator.TabPageIndicator.TabView;
import com.viewpagerindicator.TitlePageIndicator;
import com.viewpagerindicator.TitleProvider;
import com.viewpagerindicator.TitlePageIndicator.IndicatorStyle;

import android.content.Context;
import android.graphics.drawable.Drawable;
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
	
	public interface OnPageIndicatorChangeListener
	{
		public void onPageIndicatorChange();
	}

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
		try {
			super.setAdapter(a);
			setIndicator(mIndicator);
		} catch(Exception e) { Logger.LogError("Couldn't set ViewPager adapter.", e); }
	}
	
	@Override
	public void setOnPageChangeListener(OnPageChangeListener listener) {
		//super.setOnPageChangeListener(listener);
		mListeners.add(listener);
	}

	public void setIndicator(PageIndicator indicator)
	{	
		if(mIndicator != null && indicator != null && !mIndicator.equals(indicator)) return;
		mIndicator = indicator;
		if(mIndicator != null && getAdapter() != null)
		{
			mIndicator.setViewPager(this);
			mIndicator.notifyDataSetChanged();
		}
		if(mIndicatorListener != null)
			mIndicatorListener.onPageIndicatorChange();
	}
	
	public Fragment getCurrentFragment() {
		// TODO Auto-generated method stub
		return ((ArrayPagerAdapter)getAdapter()).getItem(getCurrentItem());
	}
}
