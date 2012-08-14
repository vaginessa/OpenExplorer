package org.brandroid.openmanger.services;

import org.brandroid.openmanager.data.OpenFile;
import org.brandroid.openmanager.data.OpenPath;
import org.brandroid.openmanager.util.FileManager;
import org.brandroid.utils.Logger;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;

public class EventService extends Service {
	private Looper mLooper;
	private ServiceHandler mHandler;
	
	public static final int TYPE_COPY = 1;

	private final class ServiceHandler extends Handler {
		public ServiceHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			
			Bundle data = msg.getData();
			
			int type = -1;
			if(data.containsKey("event_type"))
				type = data.getInt("event_type");
			
			OpenPath src = null, dst = null;
			if(data.containsKey("source"))
				src = (OpenPath)data.getParcelable("source");
			if(data.containsKey("destination"))
				dst = (OpenPath)data.getParcelable("destination");
			
			switch(type)
			{
			case TYPE_COPY:
				if(src instanceof OpenFile && dst instanceof OpenFile)
					((OpenFile)dst).copyFrom((OpenFile)src);
				break;
			}
			
			// Stop the service using the startId, so that we don't stop
			// the service in the middle of handling another job
			stopSelf(msg.arg1);
		}
	}

	@Override
	public void onCreate() {
		HandlerThread thread = new HandlerThread("EventHandler",
				Process.THREAD_PRIORITY_BACKGROUND);
		thread.start();
		mLooper = thread.getLooper();
		mHandler = new ServiceHandler(mLooper);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Logger.LogDebug("EventService Started");
		Message msg = mHandler.obtainMessage();
		msg.arg1 = startId;
		mHandler.sendMessage(msg);
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onDestroy() {
		Logger.LogDebug("EventService done");
	}

}
