package org.brandroid.openmanager.views;

import java.util.ArrayList;
import java.util.List;

import com.viewpagerindicator.PageIndicator;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.support.v4.view.ViewPager.OnPageChangeListener;

public class OpenViewPager extends ViewPager
{
	private PageIndicator mIndicator = null;
	private List<OnPageChangeListener> mListeners = new ArrayList<OnPageChangeListener>();

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
	
	@Override
	public void setAdapter(PagerAdapter arg0) {
		super.setAdapter(arg0);
		if(mIndicator != null)
		{
			mIndicator.setViewPager(this);
			mIndicator.notifyDataSetChanged();
		}
	}
	
	@Override
	public void setOnPageChangeListener(OnPageChangeListener listener) {
		//super.setOnPageChangeListener(listener);
		mListeners.add(listener);
	}

	public void setIndicator(PageIndicator indicator)
	{
		mIndicator = indicator;
		if(getAdapter() != null)
			mIndicator.setViewPager(this);
	}
}
