package org.brandroid.openmanger.services;

import org.brandroid.utils.Logger;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;

public class EventService extends Service
{
	private Looper mLooper;
	private ServiceHandler mHandler;
	private AsyncTask[] mQueue;
	
	private final class ServiceHandler extends Handler {
		public ServiceHandler(Looper looper) {
	          super(looper);
	      }
	      @Override
	      public void handleMessage(Message msg) {
	          // Normally we would do some work here, like download a file.
	          // For our sample, we just sleep for 5 seconds.
	          long endTime = System.currentTimeMillis() + 5*1000;
	          while (System.currentTimeMillis() < endTime) {
	              synchronized (this) {
	                  try {
	                      wait(endTime - System.currentTimeMillis());
	                  } catch (Exception e) {
	                  }
	              }
	          }
	          // Stop the service using the startId, so that we don't stop
	          // the service in the middle of handling another job
	          stopSelf(msg.arg1);
	      }
	}
	
	@Override
	public void onCreate() {
		HandlerThread thread = new HandlerThread("EventHandler", Process.THREAD_PRIORITY_BACKGROUND);
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
