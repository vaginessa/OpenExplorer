package org.brandroid.openmanager.fragments;

import org.brandroid.openmanager.OpenExplorer;
import org.brandroid.openmanager.R;
import org.brandroid.openmanager.SettingsActivity;
import org.brandroid.openmanager.data.OpenPath;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceScreen;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public class PreferenceFragment extends Fragment
{
	private OpenPath mBase = null;
	private Context mContext = null;
	private android.preference.PreferenceFragment mPrefs;
	
	public PreferenceFragment(Context context)
	{
		mContext = context;
	}
	public PreferenceFragment(Context context, OpenPath base)
	{
		this(context);
		mBase = base;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		PreferenceScreen ps = new SettingsActivity().inflatePreferenceScreenFromResource(R.xml.preferences);
		mPrefs = new PreferenceFragmentV11(mBase);
		//mPrefs.
		//pf.getView();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		LinearLayout v = new LinearLayout(mContext);
		View frag = new View(mContext);
		frag.setId(100000);
		FragmentManager man = getActivity().getFragmentManager();
		FragmentTransaction ft = man.beginTransaction();
		ft.add(mPrefs, "prefs");
		return mPrefs.getView();
	}
}
