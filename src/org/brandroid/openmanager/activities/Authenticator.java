package org.brandroid.openmanager.activities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.Header;
import org.brandroid.openmanager.R;
import org.brandroid.openmanager.adapters.LinedArrayAdapter;
import org.brandroid.openmanager.adapters.LinesAdapter;
import org.brandroid.openmanager.data.OpenDropBox;
import org.brandroid.openmanager.data.OpenDropBox.OnAPIResponseListener;
import org.brandroid.openmanager.data.OpenDropBox.RequestType;
import org.brandroid.openmanager.data.OpenPath;
import org.brandroid.utils.Logger;
import org.brandroid.utils.Preferences;

import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.TokenPair;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

public class Authenticator extends Activity
	implements OnClickListener
{
	private ListView mOutput;
	private Button mSubmit;
	private LinedArrayAdapter mOutputAdapter;
	private OpenDropBox mDrop;
	private boolean mLoggedIn = false;
	private final ArrayList<CharSequence> mData = new ArrayList<CharSequence>();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.authenticate);
		mSubmit = (Button)findViewById(R.id.auth_test);
		mSubmit.setOnClickListener(this);
		Preferences.setContext(getApplicationContext());
		mDrop = new OpenDropBox();
		mOutputAdapter = new LinedArrayAdapter(this, R.layout.edit_text_view_row, mData);
		mOutput = (ListView)findViewById(R.id.auth_output);
		mOutput.setAdapter(mOutputAdapter);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
        AndroidAuthSession session = mDrop.getSession();

        // The next part must be inserted in the onResume() method of the
        // activity from which session.startAuthentication() was called, so
        // that Dropbox authentication completes properly.
        if (session.authenticationSuccessful()) {
            try {
                // Mandatory call to complete the auth
                session.finishAuthentication();

                // Store it locally in our app for later use
                TokenPair tokens = session.getAccessTokenPair();
                storeKeys(tokens.key, tokens.secret);
                setLoggedIn(true);
            } catch (IllegalStateException e) {
            	Toast.makeText(this, "Couldn't authenticate with Dropbox:" + e.getLocalizedMessage(), Toast.LENGTH_LONG);
                //Log.i(TAG, "Error authenticating", e);
            }
        }
	}
	

    private void logOut() {
        // Remove credentials from the session
        mDrop.getSession().unlink();

        // Clear our stored keys
        clearKeys();
        // Change UI state to display logged out version
        setLoggedIn(false);
    }

    /**
     * Convenience function to change UI state based on being logged in
     */
    private void setLoggedIn(boolean loggedIn) {
    	mLoggedIn = loggedIn;
    	if (loggedIn) {
    		mSubmit.setText("Unlink from Dropbox");
    	} else {
    		mSubmit.setText("Link with Dropbox");
    	}
    }
	
	private SharedPreferences getPreferences()
	{
		return Preferences.getPreferences(this, "dropbox");
	}

    /**
     * Shows keeping the access keys returned from Trusted Authenticator in a local
     * store, rather than storing user name & password, and re-authenticating each
     * time (which is not to be done, ever).
     */
    private void storeKeys(String key, String secret) {
        // Save the access key for later
        getPreferences().edit()
        	.putString("key", key)
        	.putString("secret", secret)
        	.commit();
    }

    private void clearKeys() {
        getPreferences().edit()
        	.clear()
        	.commit();
    }

	@Override
	public void onClick(View v) {
		switch(v.getId())
		{
		case R.id.auth_test:
			if(mLoggedIn)
				try {
					for(OpenPath kid : mDrop.list())
						printLine(kid.getName());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			else
				mDrop.start(Authenticator.this);
			break;
		}
	}
	
	public void printLine(final String txt)
	{
		mOutput.post(new Runnable(){public void run(){
			mData.add(0, txt);
			mOutputAdapter.notifyDataSetChanged();
		}});
	}
}
