package org.brandroid.openmanager.fragments;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.net.ftp.FTPFile;
import org.brandroid.openmanager.R;
import org.brandroid.openmanager.activities.SettingsActivity;
import org.brandroid.openmanager.adapters.ArrayPagerAdapter;
import org.brandroid.openmanager.adapters.IconContextMenu;
import org.brandroid.openmanager.adapters.IconContextMenu.IconContextItemSelectedListener;
import org.brandroid.openmanager.data.OpenContent;
import org.brandroid.openmanager.data.OpenFTP;
import org.brandroid.openmanager.data.OpenFile;
import org.brandroid.openmanager.data.OpenNetworkPath;
import org.brandroid.openmanager.data.OpenPath;
import org.brandroid.openmanager.data.OpenServer;
import org.brandroid.openmanager.data.OpenServers;
import org.brandroid.openmanager.ftp.FTPManager;
import org.brandroid.openmanager.util.FileManager;
import org.brandroid.openmanager.util.ThumbnailCreator;
import org.brandroid.utils.Logger;
import org.brandroid.utils.MenuBuilder;
import org.brandroid.utils.MenuUtils;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.InputMethodService.InputMethodImpl;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class TextEditorFragment extends OpenFragment
	implements OnClickListener, OpenPathFragmentInterface, TextWatcher
{
	private EditText mEditText;
	private WebView mWebText;
	private ProgressBar mProgress = null;
	
	private OpenPath mPath = null;
	private String mData = null;
	private float mTextSize = 10f;
	
	private AsyncTask mTask = null;
	
	private boolean mEditMode = false;
	
	public TextEditorFragment() { }
	public TextEditorFragment(OpenPath path)
	{
		mPath = path;
		Bundle b = new Bundle();
		if(path != null && path.getPath() != null)
			b.putString("edit_path", path.getPath());
		setArguments(b);
		setHasOptionsMenu(true);
	}

	public void setProgressVisibility(boolean visible)
	{
		if(mProgress != null)
			mProgress.setVisibility(visible ? View.VISIBLE : View.GONE);
		if(getExplorer() != null)
			getExplorer().setProgressBarVisibility(visible);
	}
	
	@Override
	public boolean onBackPressed() {
		doClose();
		return true;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle bundle = savedInstanceState;
		if(getArguments() != null)
			bundle = getArguments();
		if(mPath == null && bundle != null && bundle.containsKey("edit_path"))
		{
			String path = bundle.getString("edit_path");
			if(path.startsWith("content://"))
				mPath = new OpenContent(Uri.parse(path), getActivity());
			else
				try {
					mPath = FileManager.getOpenCache(path, false, null);
				} catch (IOException e) {
					Logger.LogWarning("Couldn't get cache to edit.", e);
					mPath = new OpenFile(path);
				}
			Logger.LogDebug("load text editor (" + path + ")");
			if(bundle.containsKey("edit_data"))
				mData = bundle.getString("edit_data");
			if(bundle.containsKey("edit_server"))
			{
				int serverIndex = bundle.getInt("edit_server");
				Logger.LogDebug("Loading server #" + serverIndex);
				if(serverIndex > -1)
				{
					OpenServers servers = SettingsActivity.LoadDefaultServers(getActivity());
					if(serverIndex < servers.size())
					{
						OpenServer server = servers.get(serverIndex);
						FTPManager man = new FTPManager(server.getHost(), server.getUser(), server.getPassword(), server.getPath()); 
						Logger.LogDebug("Found server - " + server.getName());
						mPath = new OpenFTP(mPath.getPath(), null, man);
					}
				}
			} else //if (path.indexOf("ftp:/") > -1)
			{
				try {
					mPath = FileManager.getOpenCache(path, false, null);
				} catch (IOException e) {
					Logger.LogError("Couldn't get Path to edit.", e);
				}
			}
		}
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		if(!menu.hasVisibleItems())
			inflater.inflate(R.menu.text_editor, menu);
		if(Build.VERSION.SDK_INT > 10 && menu.findItem(R.id.menu_view) != null && !menu.findItem(R.id.menu_view).getSubMenu().hasVisibleItems())
			inflater.inflate(R.menu.text_view, menu.findItem(R.id.menu_view).getSubMenu());
	}
	
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		MenuUtils.setMenuVisible(menu, mPath.canWrite(), R.id.menu_save);
		MenuUtils.setMenuChecked(menu, mEditMode, R.id.menu_view_keyboard_toggle);
		if(mTextSize >= 30f)
			MenuUtils.setMenuChecked(menu, true, R.id.menu_view_font_large, R.id.menu_view_font_medium, R.id.menu_view_font_small);
		else if(mTextSize >= 20f)
			MenuUtils.setMenuChecked(menu, true, R.id.menu_view_font_medium, R.id.menu_view_font_large, R.id.menu_view_font_small);
		else
			MenuUtils.setMenuChecked(menu, true, R.id.menu_view_font_small, R.id.menu_view_font_medium, R.id.menu_view_font_large);
		//MenuUtils.setMenuVisible(menu, false);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return onClickItem(item.getItemId());
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.edit_text, null);
		mEditText = (EditText)view.findViewById(R.id.text_edit);
		//mViewText = (TextView)view.findViewById(R.id.text_view);
		//mViewScroller = (ScrollView)view.findViewById(R.id.text_scroller);
		mWebText = (WebView)view.findViewById(R.id.text_webview);
		mWebText.getSettings().setJavaScriptEnabled(true);
		mProgress = ((ProgressBar)view.findViewById(android.R.id.progress));
		mEditText.addTextChangedListener(new TextWatcher() {
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				//mData = s.toString();
			}
			
			public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
			public void afterTextChanged(Editable s) { }
		});
		return view;
	}
	
	public void setTextSize(float sz)
	{
		mTextSize = sz;
		mEditText.setTextSize(sz);
		//mViewText.setTextSize(sz);
	}
	private String getDataHTML()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(getHTMLHead());
		sb.append(TextUtils.htmlEncode(mData));
		sb.append(getHTMLFoot());
		return sb.toString();
	}
	private String getLanguage() {
		String ext = getPath().getExtension().toLowerCase();
		if(ext.endsWith("ml")) return "xml";
		if(ext.equals("js")) return "js";
		if(ext.equals("css")) return "css";
		if(ext.equals("php")) return "php";
		if(ext.equals("java")) return "java";
		return "text";
	}
	private String getHTMLHead() {
		return getHTMLHead(getLanguage());
	}
	private String getHTMLHead(String language)
	{
		return "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n" +
			"<html><head>" +
			"<script src=\"sh/scripts/shCore.js\" type=\"text/javascript\"></script>"
			+ "<script type=\"text/javascript\" src=\"sh/scripts/shBrushJScript.js\"></script>"
			+ "<link href=\"sh/styles/shCore.css\" type=\"text/css\" rel=\"stylesheet\" />"
			+ "<link href=\"sh/styles/shThemeDefault.css\" type=\"text/css\" rel=\"stylesheet\" />"
			//+ "<style>body,textarea,pre{margin:0px;padding:0px;border:0px;}textarea{width:100%;height:100%;}</style>"
			+ "<script type=\"text/javascript\">SyntaxHighlighter.all()</script>"
			+ "</head><body>"
			+ "<pre class=\"brush: " + language + ";\">";
	}
	private String getHTMLFoot()
	{
		return "</pre></body></html>";
	}
	private void refreshWebText()
	{
		new Thread(new Runnable(){
			public void run() {
				final String mDataHTML = getDataHTML();
				mWebText.post(new Runnable() {
					public void run() {
						mWebText.loadDataWithBaseURL("file:///android_asset/",
								mDataHTML, "text/html", "utf-8", null);
					}
				});
			}
		}).start();
	}
	public void setText(final String txt)
	{
		mData = txt;
		if(mEditMode)
			mEditText.post(new Runnable() {
				public void run() {
					mEditText.setText(txt);
				}
			});
		else
			refreshWebText();
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		setEditable(mEditMode);
		if(savedInstanceState != null && savedInstanceState.containsKey("edit_data"))
			mData = savedInstanceState.getString("edit_data");
		if(mPath != null && mData == null)
		{
			if(mPath instanceof OpenFile && mPath.length() < 500000)
			{
				//new Thread(new Runnable() {public void run() {
					try {
						FileReader fr = new FileReader(((OpenFile)mPath).getFile());
						char[] data = new char[(int) mPath.length()];
						fr.read(data);
						setText(new String(data));
					} catch (FileNotFoundException e) {
						Logger.LogError("Couldn't find file to load - " + mPath.getPath(), e);
					} catch (IOException e) {
						Logger.LogError("Couldn't read from file - " + mPath.getPath(), e);
					}
				}
				//}).start();
			else
				mTask = new FileLoadTask().execute(mPath);
		} else if (mData != null)
			setText(mData);
	}
	
	@Override
	public void onPause() {
		super.onPause();
	}
	
	@Override
	public void onStop() {
		super.onStop();
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		Logger.LogInfo("saveInstanceState @ TextEditor (" + mPath.getPath() + ")");
		outState.putString("edit_path", mPath.getPath());
		if(mData != null && mData.length() < 500000)
			outState.putString("edit_data", mData);
		if(mPath instanceof OpenNetworkPath && ((OpenNetworkPath)mPath).getServersIndex() > -1)
		{
			Logger.LogDebug("Saving server #" + ((OpenNetworkPath)mPath).getServersIndex());
			outState.putInt("edit_server", ((OpenNetworkPath)mPath).getServersIndex());
		} else Logger.LogDebug("No server #");
	}
	
	public void doSave()
	{
		if(!mEditMode) return;
		cancelTask();
		mTask = new FileSaveTask(mPath);
		((FileSaveTask)mTask).execute(mEditText.getText().toString());
	}
	
	public void doClose()
	{
		cancelTask();
		if(getExplorer() != null && getExplorer().isViewPagerEnabled())
		{
			final ViewPager pager = (ViewPager)getExplorer().findViewById(R.id.content_pager);
			final ArrayPagerAdapter adapter = (ArrayPagerAdapter)pager.getAdapter();
			final int pos = pager.getCurrentItem() - 1;
			if(pos < 0)
				getActivity().finish();
			else
				pager.post(new Runnable() {public void run() {
					adapter.remove(TextEditorFragment.this);
					pager.setAdapter(adapter);
					pager.setCurrentItem(pos, false);
				}});
		} else if(getFragmentManager() != null && getFragmentManager().getBackStackEntryCount() > 0)
			getFragmentManager().popBackStack();
		else getActivity().finish();
	}
	
	public void cancelTask() {
		if(mTask != null)
			mTask.cancel(true);
	}

	@Override
	public void onClick(View v) {
		onClickItem(v.getId());
	}
	@Override
	public void onClick(int id) {
		onClickItem(id);
	}
	public boolean onClickItem(int id) {
		switch(id)
		{
		case R.id.menu_context_info:
			DialogHandler.showFileInfo(getExplorer(), getPath());
			return true;
		case R.id.menu_save:
			doSave();
			return true;
			
		case R.id.menu_view_font_large:
			setTextSize(30f);
			return true;
		case R.id.menu_view_font_medium:
			setTextSize(20f);
			return true;
		case R.id.menu_view_font_small:
			setTextSize(10f);
			return true;
			
		case R.id.menu_close:
			doClose();
			return true;
		case R.id.menu_view:
			View from = getView().findViewById(id);
			showMenu(R.menu.text_view, from);
			return true;
			
		case R.id.menu_view_keyboard_toggle:
			setEditable(!mEditMode);
			return true;
		}
		return false;
	}
	
	private void setEditable(boolean editable)
	{
		mEditMode = editable;
		if(editable)
		{
			mWebText.setVisibility(View.GONE);
			//mViewScroller.setVisibility(View.GONE);
			mEditText.setVisibility(View.VISIBLE);
			mEditText.removeTextChangedListener(this);
			setText(mData);
			mEditText.addTextChangedListener(this);
		} else {
			mEditText.removeTextChangedListener(this);
			mEditText.setVisibility(View.GONE);
			//mViewScroller.setVisibility(View.VISIBLE);
			mWebText.setVisibility(View.VISIBLE);
		}
	}

	
	public static void setEnabled(boolean enabled, View... views)
	{
		for(View v : views)
			if(v != null)
				v.setEnabled(enabled);
	}
	
	
	public class FileSaveTask extends AsyncTask<String, Integer, String>
	{
		private OpenPath mPath;
		
		public FileSaveTask(OpenPath path)
		{
			mPath = path;
		}

		@Override
		protected String doInBackground(String... datas) {
			String data = datas[0];
			OutputStream fos = null;
			try {
				fos = new BufferedOutputStream(mPath.getOutputStream());
				fos.write(data.getBytes());
				fos.close();
				if(OpenFTP.class.equals(mPath.getClass()))
					((OpenFTP)mPath).getManager().disconnect();
			} catch(Exception e) {
				Logger.LogError("Couldn't save file.", e);
			}
			return null;
		}
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			setProgressVisibility(true);
			setEnabled(false, mEditText);
		}
		
		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			setProgressVisibility(false);
			setEnabled(true, mEditText);
		}
		
	}
	
	public class FileLoadTask extends AsyncTask<OpenPath, Integer, String>
	{
		@Override
		protected String doInBackground(OpenPath... params) {
			OpenPath mPath = params[0];
			String path = mPath.getPath();
			Logger.LogDebug("Getting " + path);
			if(mPath.canRead()) {
				Logger.LogDebug("File is " + mPath.length() + " bytes.");
				InputStream is = null;
				StringBuilder sb = new StringBuilder();
				try {
					is = mPath.getInputStream();
					BufferedReader br = new BufferedReader(new InputStreamReader(is));
					String line;
					while((line = br.readLine()) != null)
						sb.append(line + "\n");
				} catch (RuntimeException r) {
					Logger.LogError("File too large?", r);
					getExplorer().showToast("Unable to open file. File too large?");
					cancelTask();
					getFragmentManager().popBackStack();
				} catch (Exception e) {
					Logger.LogError("Couldn't find file - " + path, e);
				} finally {
					try {
						if(is != null)
							is.close();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					if(OpenFTP.class.equals(mPath.getClass()))
						((OpenFTP)mPath).getManager().disconnect();
					else if(mPath instanceof OpenNetworkPath)
						((OpenNetworkPath)mPath).disconnect();
				}
				return sb.toString();
			}
			else if(path.indexOf("ftp:/") > -1)
			{
				BufferedInputStream in = null;
				try {
					URL url = new URL(path);
					FTPManager ftp = FTPManager.getInstance(url);
					in = new BufferedInputStream(ftp.getInputStream(url.getPath()));
					byte[] buffer = new byte[4096];
					StringBuilder sb = new StringBuilder();
					while(in.read(buffer) > 0)
					{
						for(byte b : buffer)
							sb.append((char)b);
					}
					return sb.toString();
				} catch (MalformedURLException e) {
					Logger.LogError("Bad URL for FTP - " + path, e);
				} catch (IOException e) {
					Logger.LogError("Couldn't read from FTP - " + path, e);
				}
				finally {
					if(in != null)
						try {
							in.close();
						} catch(IOException e) { }
				}
				
				//return FTPManager.getData(path);
			} else if(path.indexOf("sftp:/") > -1)
			{
				try {
					BufferedInputStream in = (BufferedInputStream)mPath.getInputStream();
					byte[] buffer = new byte[4096];
					StringBuilder sb = new StringBuilder();
					while(in.read(buffer) > 0)
					{
						for(byte b : buffer)
							sb.append((char)b);
					}
					return sb.toString();
				} catch(IOException e) {
					Logger.LogError("Couldn't read from SFTP - " + path, e);
				}
			}
			return null;
		}
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			setEnabled(false, mEditText);
			setProgressVisibility(true);
		}
		
		@Override
		protected void onPostExecute(String result) {
			setText(result);
			setProgressVisibility(false);
			setEnabled(true, mEditText);
			mData = result;
		}
	}

	public OpenPath getPath() {
		return mPath;
	}
	@Override
	public Drawable getIcon() {
		if(getActivity() != null)
			return new BitmapDrawable(getResources(),
				ThumbnailCreator.getFileExtIcon(getPath().getExtension(), getActivity(), false));
		else return null;
	}
	@Override
	public CharSequence getTitle() {
		// TODO Auto-generated method stub
		return getPath().getName();
	}
	
	public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
	public void onTextChanged(CharSequence s, int start, int before, int count) { }
	public void afterTextChanged(Editable s) {
		mData = s.toString();
	}
}
