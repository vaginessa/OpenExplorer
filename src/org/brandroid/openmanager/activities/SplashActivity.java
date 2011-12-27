package org.brandroid.openmanager.activities;

import org.brandroid.openmanager.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.RadioButton;

public class SplashActivity extends Activity implements OnClickListener
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);

		setContentView(R.layout.splash);
		
		findViewById(R.id.pref_start_videos).setOnClickListener(this);
		findViewById(R.id.pref_start_photos).setOnClickListener(this);
		findViewById(R.id.pref_start_external).setOnClickListener(this);
		findViewById(R.id.pref_start_root).setOnClickListener(this);
		
		Intent intent = getIntent();
		if(intent != null)
		{
			if(intent.hasExtra("start"))
			{
				String start = intent.getStringExtra("start");
				if("Videos".equals(start))
					((RadioButton)findViewById(R.id.pref_start_videos)).setChecked(true);
				else if("Photos".equals(start))
					((RadioButton)findViewById(R.id.pref_start_photos)).setChecked(true);
				else if("/".equals(start))
					((RadioButton)findViewById(R.id.pref_start_root)).setChecked(true);
				else
					((RadioButton)findViewById(R.id.pref_start_external)).setChecked(true);
			}
		}
	}
	
	public void onClick(View v) {
		Intent intent = getIntent();
		if(intent == null)
			intent = new Intent();
		switch(v.getId())
		{
		case R.id.pref_start_videos:
			intent.putExtra("start", "Videos");
			break;
		case R.id.pref_start_photos:
			intent.putExtra("start", "Photos");
			break;
		case R.id.pref_start_external:
			intent.putExtra("start", "External");
			break;
		case R.id.pref_start_root:
			intent.putExtra("start", "Root");
			break;
		}
		setResult(RESULT_OK, intent);
		finish();
	}
}
