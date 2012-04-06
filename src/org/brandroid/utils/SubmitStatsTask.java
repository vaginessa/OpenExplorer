package org.brandroid.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.zip.GZIPOutputStream;

import org.brandroid.openmanager.R;
import org.brandroid.openmanager.activities.OpenExplorer;
import org.brandroid.openmanager.fragments.DialogHandler;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.widget.Toast;

public class SubmitStatsTask extends AsyncTask<String, Void, Void>
{
	private final Context mContext;
	private boolean mEscalate = false;
	
	public SubmitStatsTask(Context c) { mContext = c; }
	
	public SubmitStatsTask escalate() {
		mEscalate = true;
		return this;
	}
	
	public void sendEmail(String subj, String msg)
	{
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType("text/plain");
		intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"brandroid64@gmail.com"});
		intent.putExtra(Intent.EXTRA_SUBJECT, subj);
		intent.putExtra(Intent.EXTRA_TEXT, msg);
		try {
			mContext.startActivity(intent);
		} catch(ActivityNotFoundException e) {
			Toast.makeText(mContext, R.string.s_error_no_intents, Toast.LENGTH_LONG);
		}
	}
	
	@Override
	protected Void doInBackground(String... params) {
		HttpURLConnection uc = null;
		try {
			uc = (HttpURLConnection)new URL("http://brandroid.org/stats.php").openConnection();
			uc.setReadTimeout(2000);
			PackageManager pm = mContext.getPackageManager();
			PackageInfo pi = pm.getPackageInfo(mContext.getPackageName(), 0);
			JSONObject device = getDeviceInfo();
			if(device == null) device = new JSONObject();
			String data = "{\"Version\":" + pi.versionCode + ",\"DeviceInfo\":" + device.toString() + ",\"Logs\":" + params[0] + ",\"App\":\"" + mContext.getPackageName() + "\"}";
			if(mEscalate)
				sendEmail("Crash Report " + pi.packageName.substring(pi.packageName.lastIndexOf(".") + 1) + " v" + pi.versionName, params[0] + "\n" + DialogHandler.getDeviceInfo());
			Logger.getCrashFile().delete();
			//uc.addRequestProperty("Accept-Encoding", "gzip, deflate");
			uc.addRequestProperty("App", mContext.getPackageName());
			uc.addRequestProperty("Version", ""+pi.versionCode);
			uc.setDoOutput(true);
			Logger.LogDebug("Sending logs...");
			GZIPOutputStream out = new GZIPOutputStream(uc.getOutputStream());
			out.write(data.getBytes());
			out.flush();
			out.close();
			uc.connect();
			if(uc.getResponseCode() == HttpURLConnection.HTTP_OK)
			{
				BufferedReader br = new BufferedReader(new InputStreamReader(uc.getInputStream()));
				String line = br.readLine();
				if(line == null)
				{
					Logger.LogWarning("No response on stat submit.");
				} else {
					Logger.LogDebug("Response: " + line);
					if(line.indexOf("Thanks") > -1)
					{
						while((line = br.readLine()) != null)
							Logger.LogDebug("Response: " + line);
						Logger.LogDebug("Sent logs successfully.");
						Logger.clearDb();
					} else {
						Logger.LogWarning("Logs not thanked");
					}
				}
			} else {
				Logger.LogWarning("Couldn't send logs (" + uc.getResponseCode() + ")");
			}
		} catch(Exception e)
		{
			Logger.LogError("Error sending logs.", e);
		}
		return null;
	}

	private static JSONObject getDeviceInfo()
	{
		JSONObject ret = new JSONObject();
		try {
			ret.put("SDK", Build.VERSION.SDK_INT);
			ret.put("Language", Locale.getDefault().getDisplayLanguage());
			ret.put("Country", Locale.getDefault().getDisplayCountry());
			ret.put("Brand", Build.BRAND);
			ret.put("Manufacturer", Build.MANUFACTURER);
			ret.put("Model", Build.MODEL);
			ret.put("Product", Build.PRODUCT);
			ret.put("Board", Build.BOARD);
			ret.put("Tags", Build.TAGS);
			ret.put("Type", Build.TYPE);
			ret.put("Bootloader", Build.BOOTLOADER);
			ret.put("Hardware", Build.HARDWARE);
			ret.put("User", Build.USER);
			if(Build.UNKNOWN != null)
				ret.put("Unknown", Build.UNKNOWN);
			ret.put("Display", Build.DISPLAY);
			ret.put("Fingerprint", Build.FINGERPRINT);
			ret.put("ID", Build.ID);
		} catch (JSONException e) {
			
		}
		return ret;
	}
	
	
}