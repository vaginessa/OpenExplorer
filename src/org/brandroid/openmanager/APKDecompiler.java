package org.brandroid.openmanager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.brandroid.utils.Logger;

//import brut.androlib.Androlib;
//import brut.androlib.AndrolibException;
//import brut.androlib.ApkDecoder;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

public class APKDecompiler extends Activity
{
	private Intent mIntent;
	private TextView mTxtAPK, mTxtStats;
	private ProgressBar mProgress;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.decompile);
		mIntent = getIntent();
		//Logger.LogInfo("APKDecompiler Intent: " + mIntent.toString());
		// Starting: Intent { act=android.intent.action.VIEW dat=file:///sdcard/Download/asphault6.apk typ=application/vnd.android.package-archive cmp=com.android.packageinstaller/.PackageInstallerActivity } from pid 16980
		if(!ExecuteAsRootBase.canRunRootCommands())
			finish();
		File apkFile = new File(mIntent.getDataString().replace("file://", ""));
		Logger.LogInfo("Decompiling " + apkFile.getAbsolutePath());
		mTxtAPK = (TextView)findViewById(R.id.txtApk);
		mTxtStats = (TextView)findViewById(R.id.txtStats);
		mProgress = (ProgressBar)findViewById(R.id.progress);
		
		mTxtAPK.setText(apkFile.getAbsolutePath());
		mTxtStats.setText(getFileStats(apkFile));
		mProgress.setVisibility(View.GONE);
		
		new DecompileTask().execute(apkFile);
	}
	
	public static String getFileStats(File f)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("File Size: " + f.length());
		return sb.toString();
	}
	
	public class DecompileTask extends AsyncTask<File, Integer, File[]>
	{
		@Override
		protected File[] doInBackground(File... params)
		{
			ArrayList<File> ret = new ArrayList<File>();
			publishProgress(0, params.length);
			int i = 0;
			for(File apk : params)
			{
				File out = new File("/mnt/sdcard/download/" + apk.getName().replace(".apk", "") + "/");
				int iDir = 0;
				while(out.exists())
					out = new File("/mnt/sdcard/download/" + apk.getName().replace(".apk", "") + "-" + (++iDir) + "/");
				try {
					/*
					Androlib al = new Androlib();
					ApkDecoder decoder = new ApkDecoder(new Androlib());
					decoder.setApkFile(apk);
					decoder.setOutDir(out);
					decoder.setDecodeResources(ApkDecoder.DECODE_RESOURCES_NONE);
					decoder.setKeepBrokenResources(true);
					decoder.setDecodeSources(ApkDecoder.DECODE_SOURCES_JAVA);
					decoder.setDebugMode(true);
					decoder.decode();
					*/
					publishProgress(++i);
				} catch (Exception e) {
					Logger.LogError("Couldn't decode APK - " + out.getAbsolutePath() + " - " + e.getMessage(), e);
				}
			}
			if(ret.size() > 0)
				return (File[])ret.toArray();
			else
				return null;
		}
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			mProgress.setIndeterminate(true);
			mProgress.setVisibility(View.VISIBLE);
		}
		
		@Override
		protected void onPostExecute(File[] result) {
			super.onPostExecute(result);
			mProgress.setVisibility(View.GONE);
			if(result == null || result.length == 0)
				mTxtStats.setText("Error!");
			else
				mTxtStats.setText("DONE!");
		}
		
		@Override
		protected void onProgressUpdate(Integer... values) {
			super.onProgressUpdate(values);
			if(values.length > 1)
				mProgress.setMax(values[1]);
			if(values.length > 0)
			{
				mProgress.setProgress(values[0]);
				mProgress.setIndeterminate(false);
			} else
				mProgress.setIndeterminate(true);
		}
	}
}
