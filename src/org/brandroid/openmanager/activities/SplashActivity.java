package org.brandroid.openmanager.activities;

import org.brandroid.openmanager.R;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;

public class SplashActivity extends Activity implements OnClickListener
{
	@Override
	protected void onStart() {
		super.onStart();
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.splash);
		
		findViewById(R.id.pref_start_videos).setOnClickListener(this);
		findViewById(R.id.pref_start_photos).setOnClickListener(this);
		findViewById(R.id.pref_start_external).setOnClickListener(this);
		findViewById(R.id.pref_start_root).setOnClickListener(this);
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
