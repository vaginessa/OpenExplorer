
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
        privateKeys.put("master_key", "I'm not sure why I'm going " +
                "through the trouble of making this " +
                "encryption mechanism so secure. " +
                "I doubt anyone will ever try to hack it.");
    }
}
