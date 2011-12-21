package org.brandroid.openmanager.fragments;

import org.brandroid.openmanager.activities.OpenExplorer;
import org.brandroid.utils.Logger;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

public class OpenFragmentActivity
			extends FragmentActivity
			implements View.OnClickListener, View.OnLongClickListener
{
	//public static boolean CONTENT_FRAGMENT_FREE = true;
	//public boolean isFragmentValid = true;
	
	public String getClassName()
	{
		return this.getClass().getSimpleName();
	}
	
	public void onClick(View v) {
		Logger.LogDebug("View onClick(" + v.getId() + ") - " + v.toString());
	}
	
	public void onClick(int id) {
		Logger.LogDebug("View onClick(" + id + ") / " + getClassName());
	}
	
	public boolean onLongClick(View v) {
		Logger.LogDebug("View onLongClick(" + v.getId() + ") - " + v.toString());
		return false;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Logger.LogDebug("Menu selected(" + item.getItemId() + ") - " + item.toString());
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Logger.LogDebug("<-onCreate - " + getClassName());
		//CONTENT_FRAGMENT_FREE = false;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		//Logger.LogDebug("->onDestroy - " + getClassName());
	}
	
	/*
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		Logger.LogDebug("<-onCreateView - " + getClassName());
		//CONTENT_FRAGMENT_FREE = false;
		return super.onCreateView(inflater, container, savedInstanceState);
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		Logger.LogDebug("<-onViewCreated - " + getClassName());
	}
	
	@Override
	public void onPause() {
		super.onPause();
		Logger.LogDebug("->onPause - " + getClassName());
	}
	
	@Override
	public void onResume() {
		super.onResume();
		Logger.LogDebug("<-onResume - " + getClassName());
	}
	
	@Override
	public void onStart() {
		super.onStart();
		Logger.LogDebug("<-onStart - " + getClassName());
	}
	
	@Override
	public void onStop() {
		super.onStop();
		Logger.LogDebug("->onStop - " + getClassName());
		//CONTENT_FRAGMENT_FREE = true;
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		Logger.LogDebug("->onSaveInstanceState - " + getClassName());
	}
	*/
}
