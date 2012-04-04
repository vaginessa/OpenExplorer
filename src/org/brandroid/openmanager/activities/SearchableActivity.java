package org.brandroid.openmanager.activities;

import org.brandroid.openmanager.data.OpenFile;
import org.brandroid.openmanager.util.EventHandler;
import org.brandroid.openmanager.util.FileManager;

import android.app.ListActivity;
import android.os.Bundle;


public class SearchableActivity extends ListActivity
{
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
	}
	
	@Override
	public boolean onSearchRequested() {
		FileManager f = new FileManager();
		EventHandler h = new EventHandler(f);
		OpenFile path = new OpenFile("/");
		h.startSearch(path, this);
		//showToast("Sorry, not working yet.");
		return super.onSearchRequested();
	}
}
