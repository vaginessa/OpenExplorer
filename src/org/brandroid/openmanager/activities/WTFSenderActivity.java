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
		prefs = new org.brandroid.utils.Preferences(this);
		findViewById(R.id.wtf_yes).setOnClickListener(this);
		findViewById(R.id.wtf_no).setOnClickListener(this);
		findViewById(R.id.wtf_preview).setOnClickListener(this);
		findViewById(R.id.wtf_remember).setOnClickListener(this);
	}
	
	private void sendReport()
	{
		String report = "";
		if(((CheckBox)findViewById(R.id.wtf_preview)).isChecked())
			report = ((EditText)findViewById(R.id.wtf_report)).getText().toString();
		if(report == "")
			report = Logger.getDbLogs(false);
		new SubmitStatsTask(this).execute(report);
	}

	@Override
	public void onClick(View v) {
		switch(v.getId())
		{
		case R.id.wtf_yes:
			sendReport();
			break;
		case R.id.wtf_no:
			finish();
			break;
		case R.id.wtf_preview:
			((EditText)findViewById(R.id.wtf_report)).setText(Logger.getDbLogs(false));
			break;
		case R.id.wtf_remember:
			prefs.setSetting("global", "prefs_autowtf", ((CheckBox)v).isChecked());
			break;
		}
	}
}
