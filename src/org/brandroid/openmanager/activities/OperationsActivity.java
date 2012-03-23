package org.brandroid.openmanager.activities;

import java.util.ArrayList;

import org.brandroid.openmanager.fragments.OpenFragmentActivity;
import org.brandroid.openmanager.util.EventHandler;
import org.brandroid.openmanager.util.EventHandler.BackgroundWork;

import android.os.AsyncTask;
import android.os.Bundle;

public class OperationsActivity extends OpenFragmentActivity
{
	public final static int REQUEST_VIEW = 0;
	public final static int REQUEST_CANCEL = 1;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle bundle = savedInstanceState;
		if(bundle == null || getIntent() != null)
			bundle = getIntent().getExtras();
		ArrayList<EventHandler.BackgroundWork> tasks = EventHandler.getTaskList();
		int taskId = bundle.getInt("TaskId");
		BackgroundWork task = null;
		if(taskId >= 0 && taskId < tasks.size())
			task = tasks.get(taskId);
		task.cancel(true);
	}
}
