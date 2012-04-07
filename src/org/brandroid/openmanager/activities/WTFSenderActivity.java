package org.brandroid.openmanager.activities;

import org.brandroid.openmanager.R;
import org.brandroid.utils.Logger;
import org.brandroid.utils.Preferences;
import org.brandroid.utils.SubmitStatsTask;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.EditText;

public class WTFSenderActivity extends Activity
		implements OnClickListener
{
	private Preferences prefs;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.wtf_sender);
		prefs = new Preferences(this);
		findViewById(R.id.wtf_yes).setOnClickListener(this);
		findViewById(R.id.wtf_no).setOnClickListener(this);
		findViewById(R.id.wtf_preview).setOnClickListener(this);
		findViewById(R.id.wtf_remember).setOnClickListener(this);
		findViewById(R.id.wtf_report).setVisibility(View.GONE);
		prefs.setSetting("global", "prefs_autowtf", true);
	}
	
	private void sendReport()
	{
		new SubmitStatsTask(this).escalate().execute(getReport());
	}
	
	private String getReport()
	{
		return Logger.getCrashReport(false) + "\n" + Logger.getDbLogs(false);
	}

	@Override
	public void onClick(View v) {
		switch(v.getId())
		{
		case R.id.wtf_yes:
			sendReport();
			Logger.getCrashFile().delete();
			finish();
			break;
		case R.id.wtf_no:
			Logger.getCrashFile().delete();
			finish();
			break;
		case R.id.wtf_preview:
			EditText mPreview = ((EditText)findViewById(R.id.wtf_report));
			mPreview.setText(getReport());
			mPreview.setVisibility(View.VISIBLE);
			v.setEnabled(false);
			break;
		case R.id.wtf_remember:
			prefs.setSetting("global", "prefs_autowtf", ((CheckBox)v).isChecked());
			break;
		}
	}
}
