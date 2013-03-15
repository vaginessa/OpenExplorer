
package org.brandroid.openmanager.util;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class PrivatePreferences {
    private static final Map<String, String> privateKeys = new HashMap<String, String>();

    public static final String getKey(String name) {
        return getKey(name, "");
    }
    
    public static final String getKey(String name, String defValue) {
        if(privateKeys.containsKey(name.toLowerCase(Locale.US)))
            return privateKeys.get(name.toLowerCase(Locale.US));
        else return defValue;
    }

    public static final String getBoxAPIKey() {
        return getKey("box_api");
    }

    static {
        // All Private Keys should go here like this:
        privateKeys.put("box_api", "zqjxn1m3i4eg4iud158e0nz7u9oi2cpu");
        privateKeys.put("box_secret", "BcTh1GpJpma1cJc58sqcfZSjDZeuiYZ2");
        privateKeys.put("dropbox_key", "vajaedmhzkkp3sw");
        privateKeys.put("dropbox_secret", "plkrfrygu17glgn");
        privateKeys.put("oauth_drive_client_id", "645291897772.apps.googleusercontent.com");
        privateKeys.put("oauth_drive_secret", "xo9-oPP7P7Rj5er3J1qmzhoG");
        privateKeys.put("debug_drive_client_id", "645291897772-j66m64an4a8kj8he60ciarpmj12428ul.apps.googleusercontent.com");
        privateKeys.put("drive_client_id", "645291897772-c540moos4f9bo15jg7aqfat5c0cj5d0v.apps.googleusercontent.com");
        privateKeys.put("drive_secret", "4Q7GP50Vc412WEx0T2LY40p_");
        privateKeys.put("google_api_key", "AIzaSyCaxVyRcOV5nej6x-6iONgKOsrNGmrk4ZI");
        privateKeys.put("master_key", "I'm not sure why I'm going " +
                "through the trouble of making this " +
                "encryption mechanism so secure. " +
                "I doubt anyone will ever try to hack it.");
    }
}
