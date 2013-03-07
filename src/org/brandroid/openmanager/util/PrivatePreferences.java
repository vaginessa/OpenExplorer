
package org.brandroid.openmanager.util;

import java.util.Hashtable;
import java.util.Locale;

public class PrivatePreferences {
    private static final Hashtable<String, String> privateKeys = new Hashtable<String, String>();

    public static final String getKey(String name) {
        return privateKeys.get(name.toLowerCase(Locale.US));
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
    }
}
