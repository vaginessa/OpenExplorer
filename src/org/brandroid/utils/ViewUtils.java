package org.brandroid.utils;

import java.lang.reflect.Method;
import java.util.ArrayList;

import org.brandroid.openmanager.activities.OpenExplorer;

import android.app.Activity;
import android.os.Build;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.TextView;

public class ViewUtils {

	public static int getOffsetLeft(View v) {
		int ret = getAbsoluteLeft(v);
		if(v.getParent() != null)
			ret -= getAbsoluteLeft((View)v.getParent());
		return ret;
	}

	public static int getAbsoluteLeft(View v) {
		try {
			int[] ret = new int[2];
			v.getLocationOnScreen(ret);
			return ret[0];
		} catch(Exception e) {
			int ret = v.getLeft();
			if(v.getParent() != null && v.getParent() instanceof View)
				ret += getAbsoluteLeft((View)v.getParent());
			return ret;
		}
	}

	public static int getAbsoluteTop(View v) {
		try {
			int[] ret = new int[2];
			v.getLocationInWindow(ret);
			return ret[1];
		} catch(Exception e) {
			int ret = v.getTop();
			if(v.getParent() != null && v.getParent() instanceof View)
				ret += getAbsoluteTop((View)v.getParent());
			return ret;
		}
	}

	public static void setViewsOnClick(Activity a, OnClickListener onclick, int... ids)
	{
		if(a == null) return;
		for(int id : ids)
			if(a.findViewById(id) != null)
				a.findViewById(id).setOnClickListener(onclick);
	}

	public static void setViewsEnabled(Activity a, final boolean enabled, int... ids)
	{
		if(a == null) return;
		for(int id : ids)
		{
			final View v = a.findViewById(id);
			if(v != null)
				//v.post(new Runnable(){public void run(){
					v.setEnabled(enabled);
				//}});
		}
	}
	
	public static void setViewsEnabled(View view, final boolean enabled, int... ids)
	{
		if(view == null) return;
		for(int id : ids)
		{
			final View v = view.findViewById(id);
			if(v != null)
				//v.post(new Runnable(){public void run(){
					v.setEnabled(enabled);
				//}});
		}
	}

	public static void setOnPrefChange(PreferenceManager pm, OnPreferenceChangeListener listener, String... keys)
	{
		for(String key : keys)
			if(pm.findPreference(key) != null)
				pm.findPreference(key).setOnPreferenceChangeListener(listener);
	}
	public static void setOnClicks(PreferenceManager pm, OnPreferenceClickListener listener, String... keys)
	{
		for(String key : keys)
			if(pm.findPreference(key) != null)
				pm.findPreference(key).setOnPreferenceClickListener(listener);
	}
	public static void setOnClicks(View parent, OnClickListener listener, int... ids)
	{
		for(int id : ids)
			if(parent.findViewById(id) != null)
				parent.findViewById(id).setOnClickListener(listener);
	}

	@SuppressWarnings("unchecked")
	public static <T> ArrayList<T> findChildByClass(ViewGroup parent, Class<T> class1) {
		ArrayList<T> ret = new ArrayList<T>();
		for(int i = 0; i < parent.getChildCount(); i++)
			if(parent.getChildAt(i).getClass().equals(class1))
				ret.add((T)parent.getChildAt(i));
		return ret;
	}

	public static void setOnTouchListener(View view, OnTouchListener l, boolean setChildrenToo) {
		view.setOnTouchListener(l);
		if(setChildrenToo)
		{
			ViewGroup vg = (ViewGroup)view;
			for(View v : vg.getTouchables())
				v.setOnTouchListener(l);
		}
	}

	public static void setText(Activity activity, String string, int... textViewID) {
		for(int id : textViewID)
		{
			View v = activity.findViewById(id);
			if(v != null && v instanceof TextView)
				((TextView)v).setText(string);
		}
	}

	public static void setText(View view, String text, int... textViewID) {
		for(int id : textViewID)
		{
			View v = view.findViewById(id);
			if(v != null && v instanceof TextView)
				((TextView)v).setText(text);
		}
	}

	public static void setViewsVisible(Activity a, final boolean visible, int... ids)
	{
		for(int id : ids)
		{
			if(a == null) return;
			final View v = a.findViewById(id);
			if(v != null)
				//v.post(new Runnable(){public void run(){
					v.setVisibility(visible ? View.VISIBLE : View.GONE);
				//}});
		}
	}

	public static void setViewsVisible(View a, boolean visible, int... ids)
	{
		for(int id : ids)
			if(a.findViewById(id) != null)
				a.findViewById(id).setVisibility(visible ? View.VISIBLE : View.GONE);
	}

	public static void toggleChecked(View view) {
		if(view instanceof CheckedTextView)
			((CheckedTextView)view).setChecked(!((CheckedTextView)view).isChecked());
	}

	/**
	 * Set alpha attribute for multiple views
	 * @param alpha Floating point from 0 to 1
	 * @param views Views to set alpha for 
	 */
	public static void setAlpha(float alpha, View... views)
	{
		for(View kid : views)
			ViewUtils.setAlpha(kid, alpha);
	}

	/**
	 * Set alpha attribute for single view (on pre-Honeycomb, this works
	 * for ImageView and TextView only). For SDK 11+, this works for any view. 
	 * @param v
	 * @param alpha Floating point from 0 to 1
	 */
	public static void setAlpha(View v, float alpha)
	{
		if(alpha > 1) alpha /= 255; // fix 8 bit int
		
		if(v == null) return;
		if(Build.VERSION.SDK_INT > 10)
		{
			Method m;
			try {
				m = View.class.getMethod("setAlpha", new Class[]{Float.class});
				m.invoke(v, alpha);
				return;
			} catch (Exception e) {
			}
		}
		if(v instanceof ImageView)
			((ImageView)v).setAlpha((int)(255 * alpha));
		else if(v instanceof TextView)
			((TextView)v).setTextColor(((TextView)v).getTextColors().withAlpha((int)(255 * alpha)));
	}

	/**
	 * Set alpha attribute for specified set of child views
	 * @param alpha Floating point from 0 to 1
	 * @param root Parent element
	 * @param ids List of view ids to set alpha for
	 */
	public static void setAlpha(float alpha, View root, int... ids)
	{
		for(int id : ids)
			setAlpha(root.findViewById(id), alpha);
	}
}
