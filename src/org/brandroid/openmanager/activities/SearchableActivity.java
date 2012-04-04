package org.brandroid.openmanager.activities;

import java.util.ArrayList;

import org.brandroid.openmanager.R;
import org.brandroid.openmanager.data.OpenFile;
import org.brandroid.openmanager.util.EventHandler;
import org.brandroid.openmanager.util.EventHandler.OnWorkerThreadFinishedListener;
import org.brandroid.openmanager.util.FileManager;

import android.R.anim;
import android.app.ListActivity;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;


public class SearchableActivity extends ListActivity
{
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_fragments);
		handleIntent(getIntent());
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		setIntent(intent);
		handleIntent(intent);
	}
	
	private void handleIntent(Intent intent)
	{
		if(Intent.ACTION_SEARCH.equals(intent.getAction()))
		{
			FileManager fm = new FileManager();
			EventHandler eh = new EventHandler(fm);
			eh.setOnWorkerThreadFinishedListener(new OnWorkerThreadFinishedListener() {
				@Override
				public void onWorkerThreadComplete(int type, ArrayList<String> results) {
					setListAdapter(new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_single_choice, results));
				}
			});
			eh.searchFile(new OpenFile("/"), intent.getStringExtra(SearchManager.QUERY), this);
		}
	}
}
