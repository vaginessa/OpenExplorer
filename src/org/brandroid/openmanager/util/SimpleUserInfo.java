package org.brandroid.openmanager.util;

import org.brandroid.utils.Logger;

import org.brandroid.openmanager.R;
import org.brandroid.openmanager.data.OpenServers;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.jcraft.jsch.UserInfo;

public class SimpleUserInfo implements UserInfo
{
	//private final Uri mUri;
	private final Activity mActivity;
	private String mPassword = null;
	
	public SimpleUserInfo(Activity activity)
	{
		//mUri = uri;
		mActivity = activity;
	}

	@Override
	public String getPassphrase() {
		return mPassword;
	}

	@Override
	public String getPassword() {
		return mPassword;
	}
	
	public void setPassword(String password) { mPassword = password; }

	@Override
	public boolean promptPassword(final String message) {
		if(mPassword != null) return true;
		final boolean[] result = new boolean[]{false,false}; 
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
			
				View view = ((LayoutInflater)mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
						.inflate(R.layout.prompt_password, null);
				TextView tv = (TextView)view.findViewById(android.R.id.message);
				tv.setText(message);
				final EditText text1 = ((EditText)view.findViewById(android.R.id.text1));
				AlertDialog dlg = new AlertDialog.Builder(mActivity)
					.setTitle(R.string.s_prompt_password)
					.setView(view)
					.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							mPassword = text1.getText().toString();
							result[0] = result[1] = true;
						}
					})
					.setNegativeButton(android.R.string.no, new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							result[1] = false;
							result[0] = true;
						}
					})
					.create();
				dlg.show();
			}};
		mActivity.runOnUiThread(runnable);
		
		while(!result[0]){}
		return result[1];
	}

	@Override
	public boolean promptPassphrase(final String message) {
		return promptPassword(message);
	}

	@Override
	public boolean promptYesNo(final String message) {
		final boolean[] result = new boolean[]{false,false};
		mActivity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				AlertDialog dlg = new AlertDialog.Builder(mActivity)
					.setMessage(message)
					.setPositiveButton(android.R.string.yes, new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							result[0] = result[1] = true;
						}
					})
					.setNegativeButton(android.R.string.no, new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							result[1] = false;
							result[0] = true;
						}
					})
					.create();
				dlg.show();
			}
		});
		while(!result[0]){}
		Logger.LogVerbose("YesNo result: " + (result[1] ? "true" : "false"));
		return result[1];
	}

	@Override
	public void showMessage(final String message) {
		promptPassword(message);
	}

}
