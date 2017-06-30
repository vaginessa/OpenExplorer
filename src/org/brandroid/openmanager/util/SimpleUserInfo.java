
package org.brandroid.openmanager.util;

import com.jcraft.jsch.UserInfo;

public class SimpleUserInfo implements UserInfo {
    // private final Uri mUri;
    // private final Activity mActivity;
    private String mPassword = null;
    private static UserInfoInteractionCallback callback;

    public interface UserInfoInteractionCallback {
        boolean promptPassword(final String message);

        boolean promptYesNo(final String message);

        void onPasswordEntered(String password);

        void onYesNoAnswered(boolean yes);
    }

    public static void setInteractionCallback(UserInfoInteractionCallback listener) {
        callback = listener;
    }

    public SimpleUserInfo() {
        // mUri = uri;
        // mActivity = activity;
    }

    // public Activity getActivity() { return mActivity; }

    @Override
    public String getPassphrase() {
        return mPassword;
    }

    @Override
    public String getPassword() {
        return mPassword;
    }

    public void setPassword(String password) {
        mPassword = password;
    }

    /*@Override
    public void resetPassword() {
        mPassword = null;
    }*/

    @Override
    public boolean promptPassword(final String message) {
        if (mPassword != null)
            return true;
        return callback.promptPassword(message);
    }

    @Override
    public boolean promptPassphrase(final String message) {
        return promptPassword(message);
    }

    @Override
    public boolean promptYesNo(final String message) {
        return callback.promptYesNo(message);
    }

    @Override
    public void showMessage(final String message) {
        promptPassword(message);
    }

}
