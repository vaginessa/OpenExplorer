
package org.brandroid.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;

public class SubmitStatsTask extends AsyncTask<String, Void, Void> {
    private final Context mContext;

    public SubmitStatsTask(Context c) {
        mContext = c;
    }

    @Override
    protected Void doInBackground(String... params) {
        HttpURLConnection uc = null;
        try {
            String url = "http://brandroid.org/stats.php";
            if(params.length > 1 && params[1].startsWith("http"))
                url = params[1];
            uc = (HttpURLConnection)new URL(url).openConnection();
            uc.setReadTimeout(2000);
            //if(params.length > 1)
              //  uc.addRequestProperty("Set-Cookie", params[1]);
            PackageManager pm = mContext.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(mContext.getPackageName(), 0);
            String data = "{\"Version\":" + pi.versionCode;
            data += ",\"Runs\":" + Preferences.Run_Count;
            JSONObject device = getDeviceInfo();
            if(device != null)
                data += ",\"DeviceInfo\":" + device.toString();
            for(String pref : new String[]{"global","views","bookmarks"})
            {
                SharedPreferences sp = Preferences.getPreferences(pref);
                if(sp == null || sp.getAll() == null) continue;
                JSONObject j = new JSONObject(sp.getAll());
                if(j == null) continue;
                data += ",\"" + pref + "\":" + j.toString();
            }
            data += ",\"Logs\":" + params[0] + ",\"App\":\"" + mContext.getPackageName() + "\"}";
            // uc.addRequestProperty("Accept-Encoding", "gzip, deflate");
            uc.addRequestProperty("App", mContext.getPackageName());
            uc.addRequestProperty("Version", "" + pi.versionCode);
            uc.setDoOutput(true);
            //Logger.LogVerbose("Sending logs: " + data + "...");
            GZIPOutputStream out = new GZIPOutputStream(uc.getOutputStream());
            out.write(data.getBytes());
            out.flush();
            out.close();
            uc.connect();
            if (uc.getResponseCode() == HttpURLConnection.HTTP_OK) {
                Map m = uc.getHeaderFields();
                if(m != null)
                    Logger.LogVerbose("Stats Response Headers: " + m.toString());
                BufferedReader br = new BufferedReader(new InputStreamReader(uc.getInputStream()));
                String line = br.readLine();
                if (line == null) {
                    Logger.LogWarning("No response on stat submit.");
                } else {
                    Logger.LogDebug("Response: " + line);
                    if (line.indexOf("document.cookie=") > -1)
                    {
                        if(params.length == 1)
                        {
                            Logger.LogWarning("Server(" + uc.getURL().getHost() + ") responding improperly. Retrying");
                            doInBackground(params[0], "http://dev2.brandroid.org/stats.php");
                            return null;
                        } else {
                            Logger.LogError("Server(" + uc.getURL().getHost() + ") response invalid again.");
                        }
                    }
                    if (line.indexOf("Thanks") > -1) {
                        while ((line = br.readLine()) != null)
                            Logger.LogDebug("Response: " + line);
                        Logger.LogDebug("Sent logs successfully.");
                        new Preferences(mContext).setSetting("flags", "last_stat_submit",
                                new Date().getTime());
                        Logger.clearDb();
                    } else {
                        Logger.LogWarning("Logs not thanked");
                    }
                }
            } else {
                Logger.LogWarning("Couldn't send logs (" + uc.getResponseCode() + ")");
            }
        } catch (Exception e) {
            Logger.LogError("Error sending logs.", e);
        }
        return null;
    }

    private static JSONObject getDeviceInfo() {
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
            ret.put("Display", Build.DISPLAY);
            ret.put("Fingerprint", Build.FINGERPRINT);
            ret.put("ID", Build.ID);
        } catch (JSONException e) {

        }
        return ret;
    }

}
