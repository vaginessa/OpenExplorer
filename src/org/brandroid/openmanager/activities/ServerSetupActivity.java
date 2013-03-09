
package org.brandroid.openmanager.activities;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Locale;

import org.brandroid.openmanager.R;
import org.brandroid.openmanager.data.OpenDropBox;
import org.brandroid.openmanager.data.OpenFTP;
import org.brandroid.openmanager.data.OpenFile;
import org.brandroid.openmanager.data.OpenNetworkPath;
import org.brandroid.openmanager.data.OpenServer;
import org.brandroid.openmanager.data.OpenServers;
import org.brandroid.openmanager.fragments.DialogHandler;
import org.brandroid.openmanager.interfaces.OpenApp;
import org.brandroid.openmanager.util.PrivatePreferences;
import org.brandroid.utils.Logger;
import org.brandroid.utils.Preferences;
import org.brandroid.utils.SimpleCrypto;
import org.brandroid.utils.ViewUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener;
import com.box.androidlib.Box;
import com.box.androidlib.DAO;
import com.box.androidlib.GetAuthTokenListener;
import com.box.androidlib.GetTicketListener;
import com.box.androidlib.LogoutListener;
import com.box.androidlib.User;
import com.dropbox.client2.DropboxAPI.Account;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.android.AuthActivity;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.text.method.SingleLineTransformationMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class ServerSetupActivity
        extends SherlockActivity
        implements OnCheckedChangeListener, OnClickListener, OnItemSelectedListener,
        OnMenuItemClickListener {

    private final int[] mMapIDs = new int[] {
            R.id.text_server, R.id.text_user, R.id.text_password,
            R.id.text_path, R.id.text_name,
            R.id.text_port, R.id.server_type
    };
    private final String[] mMapKeys = new String[] {
            "host", "user", "password",
            "dir", "name",
            "port", "type"
    };
    final static int[] OnlyOnSMB = new int[] {}; // R.id.server_drop,
    // R.id.server_scan};
    final static int[] NotOnSMB = new int[] {
            R.id.text_path, R.id.text_path_label, R.id.text_port, R.id.label_port,
            R.id.check_port
    };
    private String[] mServerTypes;
    private OpenServers servers;
    private OpenServer server;
    private View mBaseView;
    private Bundle mArgs;
    private boolean mAuthTokenFound = false;
    private WebView mLoginWebView;
    private static boolean DEBUG = OpenExplorer.IS_DEBUG_BUILD && false;

    public int getThemeId() {
        String themeName = new Preferences(this)
                .getString("global", "pref_themes", "dark");
        if (themeName.equals("dark"))
            return R.style.AppTheme_Dark;
        else if (themeName.equals("light"))
            return R.style.AppTheme_Light;
        else if (themeName.equals("lightdark"))
            return R.style.AppTheme_LightAndDark;
        else if (themeName.equals("custom"))
            return R.style.AppTheme_Custom;
        return 0;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int theme = getThemeId();
        setTheme(theme);

        mArgs = savedInstanceState;
        if (getIntent() != null && getIntent().getExtras() != null)
            mArgs = getIntent().getExtras();
        if (mArgs == null)
            mArgs = new Bundle();

        int iServersIndex = mArgs.getInt("server_index", -1);
        int serverType = mArgs.getInt("server_type", -1);

        final Context context = this;
        servers = LoadDefaultServers(context);
        server = iServersIndex > -1 ? servers.get(iServersIndex)
                : new OpenServer().setName("New Server");

        server.setServerIndex(iServersIndex);

        String t2 = server.getType().toLowerCase(Locale.US);
        if (serverType > -1) {
            if (serverType == 0)
                server.setType("ftp");
            else if (serverType == 1)
                server.setType("sftp");
            else if (serverType == 2)
                server.setType("smb");
            else if (serverType == 3)
                server.setType("box");
        } else if (t2.startsWith("ftp"))
            serverType = 0;
        else if (t2.startsWith("sftp"))
            serverType = 1;
        else if (t2.startsWith("smb"))
            serverType = 2;
        else if (t2.startsWith("box"))
            serverType = 3;

        mBaseView = getLayoutInflater().inflate(R.layout.server, null);
        setContentView(mBaseView);

        mLoginWebView = (WebView)mBaseView.findViewById(R.id.server_webview);

        setIcon(getServerTypeDrawable(serverType));
        setTitle(server.getName());

        for (int i = 0; i < mMapIDs.length; i++)
        {
            int id = mMapIDs[i];
            String map = mMapKeys[i];
            if (mArgs.containsKey("server_" + map))
                server.setSetting(map, mArgs.getString("server_" + map));
            ViewUtils.setText(mBaseView, server.get(map, ""), id);
        }
        ViewUtils.setOnChangeListener(mBaseView, (OnCheckedChangeListener)this,
                R.id.check_password, R.id.check_port);
        ViewUtils.setOnClicks(mBaseView, this, R.id.server_authenticate, R.id.server_logout);

        for (int i = 0; i < mMapIDs.length; i++)
        {
            final int id = mMapIDs[i];
            final String key = mMapKeys[i];
            final View v = mBaseView.findViewById(id);
            if (v instanceof TextView)
            {
                ((TextView)v).addTextChangedListener(new TextWatcher() {
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        v.setTag("dirty");
                    }

                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    public void afterTextChanged(Editable s) {
                    }
                });
                v.setOnFocusChangeListener(new OnFocusChangeListener() {
                    public void onFocusChange(View v, boolean hasFocus) {
                        if (!hasFocus && v.getTag() != null
                                && v.getTag() instanceof String
                                && ((String)v.getTag()).equals("dirty"))
                        {
                            String val = ((TextView)v).getText().toString();
                            server.setSetting(key, val);
                            v.setTag(null);
                        }
                    }
                });
            }
        }

        CheckBox mCheckPort = (CheckBox)mBaseView.findViewById(R.id.check_port);
        TextView mTextPort = (TextView)mBaseView.findViewById(R.id.text_port);
        if (server.getPort() > 0) {
            if (mCheckPort != null)
                mCheckPort.setChecked(false);
            if (mTextPort != null)
                mTextPort.setText("" + server.getPort());
        } else if (mCheckPort != null)
            mCheckPort.setChecked(true);

        Spinner mTypeSpinner = (Spinner)mBaseView.findViewById(R.id.server_type);
        mServerTypes = getResources().getStringArray(R.array.server_types_values);
        int pos = 0;
        for (int i = 0; i < mServerTypes.length; i++)
            if (server.getType().toLowerCase(Locale.US).startsWith(mServerTypes[i])) {
                pos = i;
                break;
            }
        mTypeSpinner.setOnItemSelectedListener(this);
        mTypeSpinner.setSelection(pos);
    }

    @Override
    protected void onResume() {
        super.onResume();
        handleIntent(getIntent());
        DialogHandler.showServerWarning(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null)
        {
            handleIntent(data);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    private void handleIntent(Intent data) {
        if (DEBUG)
            Logger.LogDebug("ServerSetupActivity.handleIntent(" + data.getExtras() + ")");
        if (data != null && data.getExtras() != null && data.getExtras().containsKey("AUTH_TOKEN"))
        {
            String token = data.getStringExtra("AUTH_TOKEN");
            server.setPassword(token);
            server.setUser(token);
            ViewUtils.setText(mBaseView, token, R.id.text_password);
            if (data.getExtras().containsKey("AUTH_LOGIN"))
            {
                String login = data.getStringExtra("AUTH_LOGIN");
                server.setName(login);
                ViewUtils.setText(mBaseView, login, R.id.text_name);
            }
            ViewUtils.setViewsVisible(mBaseView, false, R.id.server_webview,
                    R.id.server_authenticate);
            ViewUtils.setViewsVisible(mBaseView, true, R.id.server_logout);
        } else {
            if (OpenDropBox.handleIntent(data, server))
            {
                ViewUtils.setText(mBaseView, server.getPassword(), R.id.text_password);
                ViewUtils.setViewsVisible(mBaseView, false, R.id.server_webview,
                        R.id.server_authenticate);
                ViewUtils.setViewsVisible(mBaseView, true, R.id.server_logout);
            }
        }
        SharedPreferences sp = getSharedPreferences("dropbox", Context.MODE_PRIVATE);
        if (sp != null && sp.contains("token"))
        {
            String pass = sp.getString("token", "") + "," + sp.getString("secret", "");
            String uid = sp.getString("uid", "dropbox");
            boolean found = false;
            for (OpenServer s : servers)
                if (s.getPassword().equals(pass) || s.getUser().equals(uid))
                    found = true;
            if (!found)
            {
                server.setType("db");
                server.setName("DropBox");
                server.setPassword(pass);
                server.setUser(uid);
                ViewUtils.setText(mBaseView, server.getPassword(), R.id.text_password);
                ViewUtils.setViewsVisible(mBaseView, false, R.id.server_webview,
                        R.id.server_authenticate);
                ViewUtils.setViewsVisible(mBaseView, true, R.id.server_logout);
                AndroidAuthSession sess = OpenDropBox.buildSession(server);
                sess.finishAuthentication();
                getDropboxAccountInfo();
            }
            sp.edit().clear().commit();
        }
    }

    private void getDropboxAccountInfo()
    {
        final OpenDropBox db = (OpenDropBox)(server.getOpenPath());
        if (db.getAccountInfo() != null)
        {
            db.getAccountInfo(new OpenDropBox.GetAccountInfoCallback() {
                public void onException(Exception e) {
                    // TODO Auto-generated method stub

                }

                public void onGetAccountInfo(Account account) {
                    server.setName(account.displayName);
                    db.setName(account.displayName);
                    ViewUtils.setText(mBaseView, account.displayName, R.id.text_name);
                    server.setSetting("quota", account.quota);
                    SaveToDefaultServers(servers, ServerSetupActivity.this);
                }
            });
        }
    }

    private int getServerTypeDrawable(int serverType) {
        switch (serverType)
        {
            case 3:
                return R.drawable.icon_box;
            case 4:
                return R.drawable.icon_dropbox;
            default:
                return R.drawable.lg_ftp;
        }
    }

    public void setIcon(int res)
    {
        // getSupportActionBar().setIcon(res);
    }

    @Override
    public void setTitle(int titleId) {
        super.setTitle(titleId);
        // getSupportActionBar().setTitle(titleId);
    }

    // @Override
    // protected void onSaveInstanceState(Bundle outState) {
    // super.onSaveInstanceState(outState);
    // for (int i = 0; i < mMapIDs.length; i++)
    // {
    // int id = mMapIDs[i];
    // String key = mMapKeys[i];
    // String val = server.get(key, "");
    // if (mBaseView.findViewById(id) != null)
    // {
    // if(mBaseView.findViewById(id) instanceof TextView)
    // val = ((TextView)mBaseView.findViewById(id)).getText().toString();
    // else if(mBaseView.findViewById(id) instanceof Spinner)
    // val = ((Spinner)mBaseView.findViewById(id)).getSelectedItem().toString();
    // else continue;
    // server.setSetting(key, val);
    // outState.putString("server_" + key, val);
    // }
    // }
    // }

    @Override
    public void onClick(View v) {
        onClick(v.getId());
    }

    public boolean onClick(int id)
    {
        String t2 = server.getType().toLowerCase(Locale.US);
        switch (id)
        {
            case android.R.string.ok:
                // onSaveInstanceState(mArgs);
                for (int i = 0; i < mMapIDs.length; i++)
                {
                    int vid = mMapIDs[i];
                    String key = mMapKeys[i];
                    View v = mBaseView.findViewById(vid);
                    if (v.getTag() != null && v.getTag() instanceof String
                            && ((String)v.getTag()).equals("dirty"))
                    {
                        String val = null;
                        if (v instanceof TextView)
                            val = ((TextView)v).getText().toString();
                        else if (v instanceof Spinner)
                            val = ((Spinner)v).getSelectedItem().toString();
                        else
                            continue;
                        mArgs.putString("server_" + key, val);
                        if (id == R.id.text_password)
                        {
                            final String sig = SettingsActivity.GetMasterPassword(this, true, false);
                            try {
                                val = SimpleCrypto.encrypt(sig, val);
                            } catch (Exception e) {
                                Logger.LogError("Unable to encrypt password!", e);
                            }
                        }
                        server.setSetting(key, val);
                    }
                }
                if (server.getServerIndex() > -1)
                    servers.set(server.getServerIndex(), server);
                else
                    servers.add(server);
                SaveToDefaultServers(servers, this);
                Intent intent = getIntent();
                if (intent == null)
                    intent = new Intent();
                intent.putExtras(mArgs);
                setResult(OpenExplorer.RESULT_OK, intent);
                finish();
                return true;
            case R.string.s_remove:
                if (server.getServerIndex() > -1)
                {
                    servers.remove(server.getServerIndex());
                    SaveToDefaultServers(servers, this);
                    Toast.makeText(this,
                            server.getName() + " " + getString(R.string.s_msg_deleted),
                            Toast.LENGTH_LONG).show();
                }
                setResult(OpenExplorer.RESULT_CANCELED);
                finish();
                return true;
            case android.R.string.cancel:
                setResult(OpenExplorer.RESULT_CANCELED);
                finish();
                return true;
            case R.id.server_authenticate:
                if (t2.startsWith("box"))
                {
                    enableAuthenticateButton(false);
                    Box box = Box.getInstance(PrivatePreferences.getBoxAPIKey());
                    box.getTicket(new GetTicketListener() {

                        @Override
                        public void onComplete(final String ticket, final String status) {
                            if (status.equals("get_ticket_ok")) {
                                ViewUtils.setViewsVisible(mBaseView, true, R.id.server_webview);
                                loadBoxLoginWebview(ticket);
                            }
                            else {
                                // onGetTicketFail();
                                enableAuthenticateButton(true);
                            }
                        }

                        @Override
                        public void onIOException(final IOException e) {
                            // onGetTicketFail();
                            enableAuthenticateButton(true);
                        }
                    });
                } else if (t2.startsWith("db")) {
                    // AppKeyPair kp = new
                    // AppKeyPair(PrivatePreferences.getKey("dropbox_key"),
                    // PrivatePreferences.getKey("dropbox_secret"));
                    // AndroidAuthSession dbAuth = new AndroidAuthSession(kp,
                    // AccessType.DROPBOX);
                    if (checkDropBoxAppKeySetup())
                        OpenDropBox.startAuthentication(this);
                }
                return true;
            case R.id.server_logout:
                if (t2.startsWith("box"))
                    Box.getInstance(PrivatePreferences.getBoxAPIKey())
                            .logout(server.getPassword(), new LogoutListener() {

                                @Override
                                public void onIOException(IOException e) {
                                    Toast.makeText(ServerSetupActivity.this, e.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                    Logger.LogError("Couldn't log out of Box", e);
                                }

                                @Override
                                public void onComplete(String status) {
                                    Toast.makeText(ServerSetupActivity.this, status,
                                            Toast.LENGTH_SHORT).show();
                                    enableAuthenticateButton(true);
                                }
                            });
                else if (t2.startsWith("db"))
                    ((OpenDropBox)server.getOpenPath()).unlink();
                enableAuthenticateButton(true);
                return true;
        }
        return false;
    }

    private void enableAuthenticateButton(boolean enable)
    {
        ViewUtils.setViewsVisible(mBaseView, false, R.id.server_logout);
        if (enable)
        {
            ViewUtils.setText(mBaseView, getString(R.string.s_authenticate),
                    R.id.server_authenticate);
            ViewUtils.setViewsEnabled(mBaseView, true, R.id.server_authenticate);
        } else {
            ViewUtils.setText(mBaseView, getString(R.string.s_authenticate) + " ...",
                    R.id.server_authenticate);
            ViewUtils.setViewsEnabled(mBaseView, false, R.id.server_authenticate);
        }
    }

    private boolean checkDropBoxAppKeySetup() {
        String APP_KEY = OpenDropBox.getAppKeyPair().key;

        // Check if the app has set up its manifest properly.
        Intent testIntent = new Intent(Intent.ACTION_VIEW);
        String scheme = "db-" + APP_KEY;
        String uri = scheme + "://" + AuthActivity.AUTH_VERSION + "/test";
        testIntent.setData(Uri.parse(uri));
        PackageManager pm = getPackageManager();
        if (1 != pm.queryIntentActivities(testIntent, 0).size()) {
            Toast.makeText(this, "URL scheme in your app's " +
                    "manifest is not set up correctly. You should have a " +
                    "com.dropbox.client2.android.AuthActivity with the " +
                    "scheme: " + scheme, Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    /**
     * Load the login webview.
     * 
     * @param ticket Ticket from Box API action get_ticket
     */
    private void loadBoxLoginWebview(final String ticket) {
        // Load the login webpage. Note how the ticket must be appended to the
        // login url.
        String loginUrl = "https://m.box.net/api/1.0/auth/" + ticket;
        mLoginWebView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        mLoginWebView.getSettings().setJavaScriptEnabled(true);
        mLoginWebView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageFinished(final WebView view, final String url) {
                // Listen for page loads and execute Box.getAuthToken() after
                // each one to see if the user has successfully logged in.
                getBoxAuthToken(ticket, 0);
            }

            @Override
            public boolean shouldOverrideUrlLoading(final WebView view, final String url) {
                if (url != null && url.startsWith("market://")) {
                    try {
                        view.getContext().startActivity(
                                new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                        return true;
                    }
                    catch (ActivityNotFoundException e) {
                        // e.printStackTrace();
                    }
                }
                return false;
            }
        });
        mLoginWebView.loadUrl(loginUrl);
    }

    /**
     * Try to get an auth token. Due to a bug with Android webviews, it is
     * possible for WebViewClient.onPageFinished to be called before the page
     * has actually loaded. So we may have to try the getAuthToken request
     * several times.
     * http://stackoverflow.com/questions/3702627/onpagefinished-not
     * -firing-correctly-when-rendering-web-page
     * 
     * @param ticket Box ticket
     * @param tries the number of attempts that have been made
     */
    private void getBoxAuthToken(final String ticket, final int tries) {
        if (tries >= 5) {
            return;
        }
        final Handler handler = new Handler();
        Box.getInstance(PrivatePreferences.getBoxAPIKey()).getAuthToken(ticket,
                new GetAuthTokenListener() {

                    @Override
                    public void onComplete(final User user, final String status) {
                        if (status.equals("get_auth_token_ok") && user != null) {
                            onBoxAuthTokenRetreived(user);
                        }
                        else if (status.equals("error_unknown_http_response_code")) {
                            handler.postDelayed(new Runnable() {

                                @Override
                                public void run() {
                                    getBoxAuthToken(ticket, tries + 1);
                                }
                            }, 500);
                        }
                    }

                    @Override
                    public void onIOException(final IOException e) {
                    }
                });
    }

    /**
     * Called when an auth token has been obtained.
     * 
     * @param authToken Box auth token
     */
    private void onBoxAuthTokenRetreived(final User authToken) {
        if (mAuthTokenFound) {
            return;
        }
        mAuthTokenFound = true;
        Intent intent = getIntent();
        intent.putExtra("AUTH_TOKEN", authToken.getAuthToken());
        intent.putExtra("AUTH_LOGIN", authToken.getLogin());
        server.setSetting("dao", DAO.toJSON(authToken));
        setIntent(intent);
        onActivityResult(OpenExplorer.REQ_AUTHENTICATE_BOX, RESULT_OK, intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.dialog_buttons, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (onClick(item.getItemId()))
            return true;
        return super.onOptionsItemSelected(item);
    }

    // @Override
    // public boolean onCreateOptionsMenu(android.view.Menu menu) {
    // getMenuInflater().inflate(R.menu.dialog_buttons, menu);
    // return super.onCreateOptionsMenu(menu);
    // }
    //
    // @Override
    // public boolean onOptionsItemSelected(android.view.MenuItem item) {
    // if(onClick(item.getItemId()))
    // return true;
    // return super.onOptionsItemSelected(item);
    // }

    @Override
    public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id) {
        if (position >= mServerTypes.length || position < 0)
            return;
        String type = mServerTypes[position];
        server.setType(type);
        setIcon(getServerTypeDrawable(position));
        final View v = mBaseView;
        if (position < 3)
        {
            ViewUtils.setViewsVisible(v, false, R.id.server_auth_buttons);
            ViewUtils.setViewsVisible(v, true, R.id.server_texts, R.id.check_port, R.id.text_port,
                    R.id.label_port);
        }
        if (OnlyOnSMB.length > 0)
            ViewUtils.setViewsVisible(v, position == 2, OnlyOnSMB);
        if (NotOnSMB.length > 0)
            ViewUtils.setViewsVisible(v, position != 2, NotOnSMB);
        server.setType("ftp");
        if (position == 1)
            server.setType("sftp");
        else if (position == 2)
            server.setType("smb");
        else if (position >= 3)
        {
            if (position == 3)
                server.setType("box");
            else if (position == 4)
            {
                server.setType("db");
                if (!server.get("account", "").equals(""))
                    getDropboxAccountInfo();
            }
            ViewUtils.setViewsVisible(v, false, R.id.server_texts, R.id.check_port, R.id.text_port,
                    R.id.label_port);
            ViewUtils.setViewsVisible(v, true, R.id.server_auth_buttons);
            if (!server.getPassword().equals(""))
            {
                ViewUtils.setViewsVisible(v, true, R.id.server_logout);
                ViewUtils.setViewsVisible(v, false, R.id.server_authenticate);
            } else {
                enableAuthenticateButton(true);
            }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
    }

    @Override
    public void onCheckedChanged(CompoundButton v, boolean isChecked) {
        if (v.getId() == R.id.check_port)
        {
            ViewUtils.setEnabled(mBaseView, !isChecked, R.id.text_port);
            if (!isChecked)
                server.setPort(-1);
            else {
                TextView mTextPort = (TextView)mBaseView.findViewById(R.id.text_port);
                try {
                    server.setPort(Integer.parseInt(mTextPort.getText().toString()));
                } catch (Exception e) {
                    Logger.LogWarning("Invalid Port: " + mTextPort.getText().toString());
                }
            }
        } else if (v.getId() == R.id.check_password) {
            TextView mPassword = (TextView)mBaseView.findViewById(R.id.text_password);
            if (isChecked) {
                mPassword.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                mPassword.setTransformationMethod(new SingleLineTransformationMethod());
            } else {
                mPassword.setRawInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
                mPassword.setTransformationMethod(new PasswordTransformationMethod());
            }
        }
    }

    public static boolean showServerDialog(final OpenApp app, final OpenFTP mPath) {
        return showServerDialog(app, mPath.getServerIndex());
    }

    public static boolean showServerDialog(final OpenApp app, final OpenNetworkPath mPath) {
        return showServerDialog(app, mPath.getServerIndex());
    }

    public static boolean showServerDialog(final OpenApp app, final int iServersIndex) {
        final Context context = app.getContext();

        Intent intent = new Intent(context, ServerSetupActivity.class);
        intent.putExtra("server_index", iServersIndex);
        app.startActivityForResult(intent, SettingsActivity.MODE_SERVER);

        return true;
    }

    public static boolean showServerDialog(final Activity activity, final int iServersIndex) {
        final Context context = activity;

        Intent intent = new Intent(context, ServerSetupActivity.class);
        intent.putExtra("server_index", iServersIndex);
        activity.startActivityForResult(intent, SettingsActivity.MODE_SERVER);

        return true;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (onClick(item.getItemId()))
            return true;
        return false;
    }

    public static OpenFile GetDefaultServerFile(Context context) {
        OpenFile f2 = new OpenFile(context.getFilesDir().getPath(), "servers.json");
        Preferences prefs = new Preferences(context);
        try {
            OpenFile f = OpenFile.getInternalMemoryDrive().getChild("Android").getChild("data")
                    .getChild("org.brandroid.openmanager").getChild("files")
                    .getChild("servers.json");
            if (prefs.getSetting("global", "servers_private", false))
            {
                if (f.exists())
                {
                    if (f.length() > f2.length())
                        f2.copyFrom(f);
                    f.delete();
                }
                return f2;
            }
            if (!f.exists())
                f.touch();
            if (!f.exists() || !f.canWrite())
            {
                prefs.setSetting("global", "servers_private", true);
                return f2;
            }
            else if (f2.exists()) {
                if (OpenExplorer.IS_DEBUG_BUILD)
                    Logger.LogVerbose("Old servers.json(" + f2.length()
                            + ") found. Overwriting new servers.json(" + f.length() + ")!");
                if (!f.exists() || f2.length() > f.length())
                    f.copyFrom(f2);
                f2.delete();
            }
            if (OpenExplorer.isBlackBerry() && f.exists() && f.canWrite()) {
                f2 = OpenFile.getInternalMemoryDrive().getChild(".servers.json");
                if (f2.exists()) {
                    if (!f.exists() || f2.length() > f.length())
                        f.copyFrom(f2);
                    f2.delete();
                }
            }
            if (!f.exists() && !f.touch())
            {
                prefs.setSetting("global", "servers_private", true);
                return f2;
            }
            return f;
        } catch (Exception e) {
            prefs.setSetting("global", "servers_private", true);
            return f2;
        }
    }

    public static OpenServers LoadDefaultServers(Context context) {
        if (OpenServers.DefaultServers != null)
            return OpenServers.DefaultServers;
        OpenFile f = ServerSetupActivity.GetDefaultServerFile(context);
        try {
            if (!f.exists() && !f.create()) {
                Logger.LogWarning("Couldn't create default servers file (" + f.getPath() + ")");
                return new OpenServers();
            } else if (f.length() <= 1)
                return new OpenServers(); // Empty file
            else {
                // Logger.LogDebug("Created default servers file (" +
                // f.getPath() + ")");
                String data = f.readAscii();
                if (DEBUG)
                    Logger.LogDebug("Server JSON: " + data);
                JSONArray jarr = new JSONArray(data);
                final Preferences prefs = new Preferences(context);
                String dk = SettingsActivity.GetMasterPassword(context);
                if (//!prefs.getBoolean("warn", "master_key_fallback", false) &&
                        !canDecryptPasswords(jarr, dk))
                {
                    if(DEBUG)
                        Logger.LogDebug("ServerSetup can't decrypt. Trying to fall back!");
                    String dk2 = SettingsActivity.GetMasterPassword(context, false, true);
                    if (canDecryptPasswords(jarr, dk2))
                    {
                        try {
                            if(DEBUG)
                                Logger.LogDebug("ServerSetup decrypt success!");
                            encryptPasswords(jarr, dk);
                            //prefs.setSetting("warn", "master_key_fallback", true);
                            //f.write(jarr.toString());
                        } catch(Exception e) {
                            Logger.LogDebug("ServerSetup encrypt failed!");
                        }
                    }
                } else if(DEBUG) Logger.LogDebug("Server setup upgraded!");
                OpenServers.setDecryptKey(dk);
                OpenServers.DefaultServers = new OpenServers(jarr);
                return OpenServers.DefaultServers;
            }
        } catch (IOException e) {
            Logger.LogError("Error loading default server list.", e);
            return new OpenServers();
        } catch (JSONException e) {
            Logger.LogError("Error decoding JSON for default server list.", e);
            return new OpenServers();
        }
    }

    public static void encryptPasswords(JSONArray json, String key)
    {
        for (int i = 0; i < json.length(); i++)
        {
            JSONObject o = json.optJSONObject(i);
            if (o.has("password"))
            {
                String pw = o.optString("password");
                if (pw != null && !pw.equals(""))
                {
                    try {
                        pw = SimpleCrypto.encrypt(key, pw);
                        o.put("password", pw);
                    } catch (Exception e) {
                    }
                }
            }
            try {
                json.put(i, o);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private static boolean canDecryptPasswords(JSONArray json, String key) throws JSONException
    {
        for (int i = 0; i < json.length(); i++)
        {
            JSONObject o = json.optJSONObject(i);
            if (o.has("password"))
            {
                String pw = o.optString("password");
                if (pw != null && !pw.equals(""))
                {
                    try {
                        pw = SimpleCrypto.decrypt(key, pw);
                        o.put("password", pw);
                    } catch (Exception e) {
                        return false;
                    }
                }
            }
            json.put(i, o);
        }
        return true;
    }

    public static void SaveToDefaultServers(OpenServers servers, Context context) {
        SaveToDefaultServers(servers.getJSONArray(true, context), context);
    }
    public static void SaveToDefaultServers(JSONArray json, Context context) {
        final OpenFile f = ServerSetupActivity.GetDefaultServerFile(context);
        final String data = json.toString(); 
        new Thread(new Runnable() {
            public void run() {
                Writer w = null;
                try {
                    f.delete();
                    f.create();
                    w = new BufferedWriter(new FileWriter(f.getFile()));
                    if (SettingsActivity.DEBUG)
                        Logger.LogDebug("Writing to " + f.getPath() + ": " + data);
                    // data = SimpleCrypto.encrypt(GetSignatureKey(context),
                    // data);
                    w.write(data);
                    w.close();
                    if (SettingsActivity.DEBUG)
                        Logger.LogDebug("Wrote " + data.length() + " bytes to OpenServers ("
                                + f.getPath()
                                + ").");
                } catch (IOException e) {
                    Logger.LogError("Couldn't save OpenServers.", e);
                } catch (Exception e) {
                    Logger.LogError("Problem encrypting servers?", e);
                } finally {
                    try {
                        if (w != null)
                            w.close();
                    } catch (IOException e2) {
                        Logger.LogError("Couldn't close writer during error", e2);
                    }
                }
            }
        }).start();
    }
}
