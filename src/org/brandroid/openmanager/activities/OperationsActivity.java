package org.brandroid.openmanager.activities;

import java.util.ArrayList;

import org.brandroid.openmanager.R;
import org.brandroid.openmanager.util.EventHandler;
import org.brandroid.openmanager.util.EventHandler.BackgroundWork;

import android.app.NotificationManager;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class OperationsActivity extends OpenFragmentActivity
{
	public final static int REQUEST_VIEW = 0;
	public final static int REQUEST_CANCEL = 1;
	
	private ListView mList = null;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle bundle = savedInstanceState;
		if(bundle == null || getIntent() != null)
			bundle = getIntent().getExtras();
		if(bundle != null && bundle.containsKey("TaskId"))
		{
			ArrayList<EventHandler.BackgroundWork> tasks = EventHandler.getTaskList();
			int taskId = bundle.getInt("TaskId");
			BackgroundWork task = null;
			if(taskId >= 0 && taskId < tasks.size())
			{
				task = tasks.get(taskId);
				task.cancel(true);
				NotificationManager mNotifier = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
				mNotifier.cancel(EventHandler.BACKGROUND_NOTIFICATION_ID);
				Toast.makeText(getApplicationContext(), R.string.s_msg_event_cancelled, Toast.LENGTH_LONG);
				finish();
			}
		}
		setContentView(R.layout.operations_layout);
		mList = (ListView)findViewById(android.R.id.list);
		mList.setAdapter(new ArrayAdapter<BackgroundWork>(this, R.layout.bookmark_layout, EventHandler.getRunningTasks()));
	}
	
	public class EventAdapter extends BaseAdapter
	{
		private final BackgroundWork[] mEvents;
		private final int mLayoutId; 
		private final Context mContext;
		
		public EventAdapter(Context c, int textId, BackgroundWork[] events)
		{
			mContext = c;
			mEvents = events;
			mLayoutId = textId;
		}

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return mEvents.length;
		}

		@Override
		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return mEvents[position];
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;
			if(view == null)
				view = getLayoutInflater().inflate(mLayoutId, parent, false);
			BackgroundWork mWork = (BackgroundWork)getItem(position);
			if(mWork != null)
				mWork.updateView(view);
			return null;
		}
		
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}
}
