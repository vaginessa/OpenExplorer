
package org.brandroid.openmanager.util;

import org.brandroid.openmanager.R;

import java.lang.reflect.Field;
import java.util.ArrayList;

public class HelpStringHelper {
    private static ArrayList<String> mStringResourceIDs = null;
    private static ArrayList<String> mHelpSuffs = null;

    public static ArrayList<String> getHelpSuffs() {
        if (mHelpSuffs == null) {
            mHelpSuffs = new ArrayList<String>();
            for (String res : getAllStringResourceNames())
                if (res.startsWith("help_") && !res.endsWith("_title"))
                    mHelpSuffs.add(res.substring(5));
        }
        return mHelpSuffs;
    }

    public static ArrayList<String> getAllStringResourceNames() {
        if (mStringResourceIDs == null) {
            mStringResourceIDs = new ArrayList<String>();
            for (Field f : R.string.class.getFields()) {
                mStringResourceIDs.add(f.getName());
            }
        }
        return mStringResourceIDs;
    }
}
