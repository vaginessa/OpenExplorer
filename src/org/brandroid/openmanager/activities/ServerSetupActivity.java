
package org.brandroid.openmanager.activities;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Locale;

import org.brandroid.openmanager.R;
import org.brandroid.openmanager.R.array;
import org.brandroid.openmanager.R.drawable;
import org.brandroid.openmanager.R.id;
import org.brandroid.openmanager.R.layout;
import org.brandroid.openmanager.R.string;
import org.brandroid.openmanager.data.BookmarkHolder;
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

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener;
import com.box.androidlib.Box;
import com.box.androidlib.BoxAuthentication;
import com.box.androidlib.BoxConstants;
import com.box.androidlib.DAO;
import com.box.androidlib.GetAuthTokenListener;
import com.box.androidlib.GetTicketListener;
import com.box.androidlib.LogoutListener;
import com.box.androidlib.User;

import android.R.anim;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.text.method.SingleLineTransformationMethod;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewStub;
import android.view.WindowManager.BadTokenException;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class ServerSetupActivity
        extends SherlockActivity
        implements OnCheckedChangeListener, OnClickListener, OnItemSelectedListener,
        OnMenuItemClickListener, LogoutListener {

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
    private int iServersIndex = -1;
    private boolean mAuthTokenFound = false;
    private WebView mLoginWebView;

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

        iServersIndex = mArgs.getInt("server_index", -1);
        int serverType = mArgs.getInt("server_type", -1);

        final Context context = this;
        servers = LoadDefaultServers(context);
        server = iServersIndex > -1 ? servers.get(iServersIndex)
                : new OpenServer().setName("New Server");

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

        DialogHandler.showServerWarning(context);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OpenExplorer.REQ_AUTHENTICATE && data != null
                && data.getExtras() != null && data.getExtras().containsKey("AUTH_TOKEN"))
        {
            String token = data.getStringExtra("AUTH_TOKEN");
            server.setPassword(token);
            server.setUser(token);
            ViewUtils.setText(mBaseView, token, R.id.text_password, R.id.text_user);
            if (data.getExtras().containsKey("AUTH_LOGIN"))
            {
                String login = data.getStringExtra("AUTH_LOGIN");
                server.setName(login);
                ViewUtils.setText(mBaseView, login, R.id.text_name);
            }
            ViewUtils.setViewsVisible(mBaseView, false, R.id.server_webview);
            ViewUtils.setViewsEnabled(mBaseView, false, R.id.server_authenticate);
            ViewUtils.setViewsEnabled(mBaseView, true, R.id.server_logout);
        }
    }

    private int getServerTypeDrawable(int serverType) {
        switch (serverType)
        {
            case 3:
                return R.drawable.icon_box;
            default:
                return R.drawable.lg_ftp;
        }
    }

    public void setIcon(int res)
    {
        getSupportActionBar().setIcon(res);
    }

    @Override
    public void setTitle(int titleId) {
        getSupportActionBar().setTitle(titleId);
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
        switch (id)
        {
            case android.R.string.ok:
                //onSaveInstanceState(mArgs);
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
                            final String sig = SettingsActivity.GetSignatureKey(this);
                            try {
                                val = SimpleCrypto.encrypt(sig, val);
                            } catch (Exception e) {
                                Logger.LogError("Unable to encrypt password!", e);
                            }
                        }
                        server.setSetting(key, val);
                    }
                }
                if (iServersIndex > -1)
                    servers.set(iServersIndex, server);
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
                if (iServersIndex > -1)
                {
                    servers.remove(iServersIndex);
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
                if (server.getType().equals("box"))
                {
                    Box box = Box.getInstance(PrivatePreferences.getBoxAPIKey());
                    box.getTicket(new GetTicketListener() {

                        @Override
                        public void onComplete(final String ticket, final String status) {
                            if (status.equals("get_ticket_ok")) {
                                ViewUtils.setViewsVisible(mBaseView, true, R.id.server_webview);
                                loadLoginWebview(ticket);
                            }
                            else {
                                // onGetTicketFail();
                            }
                        }

                        @Override
                        public void onIOException(final IOException e) {
                            // onGetTicketFail();
                        }
                    });
                }
                return true;
            case R.id.server_logout:
                Box.getInstance(PrivatePreferences.getBoxAPIKey())
                        .logout(server.getPassword(), this);
                return true;
        }
        return false;
    }

    /**
     * Load the login webview.
     * 
     * @param ticket Ticket from Box API action get_ticket
     */
    private void loadLoginWebview(final String ticket) {
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
                getAuthToken(ticket, 0);
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
    private void getAuthToken(final String ticket, final int tries) {
        if (tries >= 5) {
            return;
        }
        final Handler handler = new Handler();
        Box.getInstance(PrivatePreferences.getBoxAPIKey()).getAuthToken(ticket,
                new GetAuthTokenListener() {

                    @Override
                    public void onComplete(final User user, final String status) {
                        if (status.equals("get_auth_token_ok") && user != null) {
                            onAuthTokenRetreived(user);
                        }
                        else if (status.equals("error_unknown_http_response_code")) {
                            handler.postDelayed(new Runnable() {

                                @Override
                                public void run() {
                                    getAuthToken(ticket, tries + 1);
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
    private void onAuthTokenRetreived(final User authToken) {
        if (mAuthTokenFound) {
            return;
        }
        mAuthTokenFound = true;
        Intent intent = getIntent();
        intent.putExtra("AUTH_TOKEN", authToken.getAuthToken());
        intent.putExtra("AUTH_LOGIN", authToken.getLogin());
        server.setSetting("dao", DAO.toJSON(authToken));
        setIntent(intent);
        onActivityResult(OpenExplorer.REQ_AUTHENTICATE, RESULT_OK, intent);
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
            ViewUtils.setViewsVisible(v, true, R.id.server_texts, R.id.check_port, R.id.text_port);
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
        else if (position == 3)
        {
            server.setType("box");
            ViewUtils.setViewsVisible(v, false, R.id.server_texts, R.id.check_port, R.id.text_port);
            ViewUtils.setViewsVisible(v, true, R.id.server_auth_buttons);
            if (!server.getPassword().equals(""))
            {
                ViewUtils.setViewsEnabled(v, true, R.id.server_logout);
                ViewUtils.setViewsEnabled(v, false, R.id.server_authenticate);
            } else {
                ViewUtils.setViewsEnabled(v, false, R.id.server_logout);
                ViewUtils.setViewsEnabled(v, true, R.id.server_authenticate);
            }
        }
    }

    @Override
    public void onIOException(IOException e) {
        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        Logger.LogError("Couldn't log out of Box", e);
    }

    @Override
    public void onComplete(String status) {
        Toast.makeText(this, status, Toast.LENGTH_SHORT).show();
        ViewUtils.setViewsEnabled(mBaseView, false, R.id.server_logout);
        ViewUtils.setViewsEnabled(mBaseView, true, R.id.server_authenticate);
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
        return showServerDialog(app, mPath.getServersIndex());
    }

    public static boolean showServerDialog(final OpenApp app, final OpenNetworkPath mPath) {
        return showServerDialog(app, mPath.getServersIndex());
    }

    public static boolean showServerDialog(final OpenApp app, final int iServersIndex) {
        final Context context = app.getContext();

        DialogHandler.showServerWarning(context);
        Intent intent = new Intent(context, ServerSetupActivity.class);
        intent.putExtra("server_index", iServersIndex);
        app.startActivityForResult(intent, SettingsActivity.MODE_SERVER);

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
                if (SettingsActivity.DEBUG)
                    Logger.LogDebug("Server JSON: " + data);
                OpenServers.DefaultServers = new OpenServers(new JSONArray(data),
                        SettingsActivity.GetMasterPassword(context));
                if (SettingsActivity.DEBUG)
                    Logger.LogDebug("Loaded " + OpenServers.DefaultServers.size() + " servers @ "
                            + data.length() + " bytes from " + f.getPath());
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

    public static void SaveToDefaultServers(OpenServers servers, Context context) {
        final OpenFile f = ServerSetupActivity.GetDefaultServerFile(context);
        final String data = servers.getJSONArray(true, context).toString();
        new Thread(new Runnable() {
            public void run() {
                Writer w = null;
                try {
                    f.delete();
                    f.create();
                    w = new BufferedWriter(new FileWriter(f.getFile()));
                    if (SettingsActivity.DEBUG)
                        Logger.LogDebug("Writing to " + f.getPath() + ": " + data);
                    // data = SimpleCrypto.encrypt(GetSignatureKey(context), data);
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
