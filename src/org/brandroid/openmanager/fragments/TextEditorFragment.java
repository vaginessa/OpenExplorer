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
import java.util.Date;

import org.brandroid.openmanager.R;
import org.brandroid.openmanager.activities.FolderPickerActivity;
import org.brandroid.openmanager.activities.OpenExplorer;
import org.brandroid.openmanager.activities.SettingsActivity;
import org.brandroid.openmanager.adapters.ArrayPagerAdapter;
import org.brandroid.openmanager.adapters.LinesAdapter;
import org.brandroid.openmanager.data.FTPManager;
import org.brandroid.openmanager.data.OpenContent;
import org.brandroid.openmanager.data.OpenFTP;
import org.brandroid.openmanager.data.OpenFile;
import org.brandroid.openmanager.data.OpenNetworkPath;
import org.brandroid.openmanager.data.OpenPath;
import org.brandroid.openmanager.data.OpenPath.NeedsTempFile;
import org.brandroid.openmanager.data.OpenServer;
import org.brandroid.openmanager.data.OpenServers;
import org.brandroid.openmanager.fragments.PickerFragment.OnOpenPathPickedListener;
import org.brandroid.openmanager.util.FileManager;
import org.brandroid.openmanager.util.ThumbnailCreator;
import org.brandroid.openmanager.views.SeekBarActionView;
import org.brandroid.utils.Logger;
import org.brandroid.utils.MenuUtils;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class TextEditorFragment extends OpenFragment
	implements OnClickListener, OpenPathFragmentInterface, TextWatcher,
		OpenFragment.OnFragmentTitleLongClickListener, OnSeekBarChangeListener
{
	private EditText mEditText;
	private ListView mViewList;
	private LinesAdapter mViewListAdapter = null;
	//private WebView mWebText;
	//private TableLayout mViewTable;
	//private ScrollView mViewScroller;
	private ProgressBar mProgress = null;
	private SeekBarActionView mFontSizeBar = null;
	
	private final static int REQUEST_SAVE_AS = 1; 
	
	private OpenPath mPath = null;
	private String mData = null;
	private boolean mDirty = false;
	private long lastClick = 0l;
	private float mTextSize = 10f;
	private boolean mSalvage = true;
	
	private final static boolean USE_SEEK_ACTIONVIEW = !OpenExplorer.BEFORE_HONEYCOMB && Build.VERSION.SDK_INT < 14;
	
	private AsyncTask<?, ?, ?> mTask = null;
	
	private boolean mEditMode = false;
	
	public TextEditorFragment() {
		if(getArguments() != null && getArguments().containsKey("edit_path"))
		{
			Logger.LogDebug("Creating TextEditorFragment @ " + getArguments().getString("edit_path") + " from scratch");
			setPath(getArguments().getString("edit_path"));
		} else Logger.LogWarning("Creating orphan TextEditorFragment");
	}
	
	public TextEditorFragment(OpenPath path)
	{
		mPath = path;
		Logger.LogDebug("Creating TextEditorFragment @ " + mPath + " from path");
		Bundle b = new Bundle();
		if(path != null && path.getPath() != null)
			b.putString("edit_path", path.getPath());
		//setArguments(b);
		setHasOptionsMenu(true);
	}
	
	public static TextEditorFragment getInstance(OpenPath path, Bundle args)
	{
		TextEditorFragment ret = getInstance(args);
		ret.setPath(path);
		return ret;
	}
	public static TextEditorFragment getInstance(Bundle args)
	{
		TextEditorFragment ret = new TextEditorFragment();
		ret.setArguments(args);
		return ret;
	}
	
	public void setSalvagable(boolean doSave)
	{
		mSalvage = doSave;
	}
	
	@Override
	public void onDetach() {
		super.onDetach();
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mViewListAdapter = new LinesAdapter(activity, new String[]{});
		mFontSizeBar = new SeekBarActionView(activity);
		mFontSizeBar.setProgress((int)mTextSize);
		mFontSizeBar.setOnCloseClickListener(new SeekBarActionView.OnCloseListener() {
			@Override
			public boolean onClose() {
				mViewListAdapter.notifySizeChanged();
				return false;
			}
		});
		mFontSizeBar.setOnSeekBarChangeListener(this);
	}

	private void setPath(String path)
	{
		if(path.startsWith("content://"))
			mPath = new OpenContent(Uri.parse(path), getActivity());
		else
			try {
				mPath = FileManager.getOpenCache(path, false, null);
			} catch (IOException e) {
				mPath = new OpenFile(path);
			}
	}
	private void setPath(OpenPath path)
	{
		mPath = path;
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
		if(mEditMode)
		{
			setEditable(false);
			return true;
		} else {
			doClose();
			return true;
		}
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//setHasOptionsMenu(true);
		Bundle bundle = savedInstanceState;
		if(getArguments() != null)
			bundle = getArguments();
		if(mPath == null && bundle != null && bundle.containsKey("edit_path"))
		{
			String path = bundle.getString("edit_path");
			mData = null;
			mPath = FileManager.getOpenCache(path);
			Logger.LogDebug("load text editor (" + path + ")");
			if(mData == null && bundle.containsKey("edit_data"))
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
		if(menu.size() > 0)
			menu.clear();
		inflater.inflate(R.menu.text_editor, menu);
		MenuItem mFontSize = menu.findItem(R.id.menu_view_font_size);
		if(mFontSize != null && USE_SEEK_ACTIONVIEW)
		{
			mFontSize
				.setActionView((SeekBarActionView)mFontSizeBar)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
		}
		if(menu.findItem(R.id.menu_file) != null && menu.findItem(R.id.menu_file).getSubMenu() != null && !menu.findItem(R.id.menu_file).getSubMenu().hasVisibleItems())
			inflater.inflate(R.menu.text_file, menu.findItem(R.id.menu_file).getSubMenu());
		if(menu.findItem(R.id.menu_view) != null && menu.findItem(R.id.menu_view).getSubMenu() != null && !menu.findItem(R.id.menu_view).getSubMenu().hasVisibleItems())
			inflater.inflate(R.menu.text_view, menu.findItem(R.id.menu_view).getSubMenu());
	}
	
	public void onPrepareOptionsMenu(Menu menu) {
		if(getActivity() == null) return;
		if(menu == null) return;
		super.onPrepareOptionsMenu(menu);
		MenuUtils.setMenuEnabled(menu, mPath.canWrite(), R.id.menu_save, R.id.menu_save_as);
		MenuUtils.setMenuChecked(menu, mEditMode, R.id.menu_view_keyboard_toggle);
		MenuUtils.setMenuVisible(menu, true, R.id.menu_view_font_size);
		//MenuUtils.setMenuVisible(menu, false);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId())
		{
			case R.id.menu_view_font_size:
				if(USE_SEEK_ACTIONVIEW)
					((SeekBarActionView)mFontSizeBar).onActionViewExpanded();
				else DialogHandler.showSeekBarDialog(getActivity(),
						getString(R.string.s_view_font_size),
						(int)mTextSize, 60,
						this);
				return true;
		}
		return onClickItem(item.getItemId());
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.edit_text, null);
		mEditText = (EditText)view.findViewById(R.id.text_edit);
		mViewList = (ListView)view.findViewById(R.id.text_view_list);
		//mViewTable = (TableLayout)view.findViewById(R.id.text_view_table);
		//mViewScroller = (ScrollView)view.findViewById(R.id.text_view_scroller);
		//mViewText = (TextView)view.findViewById(R.id.text_view);
		//mViewScroller = (ScrollView)view.findViewById(R.id.text_scroller);
		//mWebText = (WebView)view.findViewById(R.id.text_webview);
		//mWebText.getSettings().setJavaScriptEnabled(true);
		mProgress = ((ProgressBar)view.findViewById(android.R.id.progress));
		mEditText.addTextChangedListener(this);
		return view;
	}
	
	public void setTextSize(float sz)
	{
		mTextSize = sz;
		mEditText.setTextSize(sz);
		if(mViewListAdapter != null)
			mViewListAdapter.setTextSize(sz);
		//mViewText.setTextSize(sz);
	}
	
	private void refreshList()
	{
		if(mViewListAdapter != null)
			mViewListAdapter.setLines(mData.split("\n"));
		
	}
	public void setText(final String txt)
	{
		if(txt == null) return;
		mData = txt;
		if(mEditMode)
			mEditText.post(new Runnable() {
				public void run() {
					mEditText.setText(txt);
				}
			});
		else
			refreshList();
	}
	
	@Override
	public int getPagerPriority() {
		return 6;
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		setEditable(mEditMode);
		if(mData == null && savedInstanceState != null && savedInstanceState.containsKey("edit_data") && (mPath == null || mPath.getPath().equals(savedInstanceState.get("edit_path"))))
			mData = savedInstanceState.getString("edit_data");
		if(mPath != null && mData == null)
		{
			//mData = getString(R.string.s_status_loading);
			if(mPath instanceof OpenFile)
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
				//}}).start();
			} else
				mTask = new FileLoadTask().execute(mPath);
		} else if (mData != null)
			setText(mData);
		if(savedInstanceState != null && savedInstanceState.containsKey("size"))
			setTextSize(savedInstanceState.getFloat("size"));
		mViewList.setAdapter(mViewListAdapter);
		mViewList.setLongClickable(true);
		mViewList.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
				long time = new Date().getTime();
				if(time - lastClick < 500)
					setEditable(true);
				else lastClick = time;
			}
		});
		mViewList.setOnItemLongClickListener(new OnItemLongClickListener() {
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {
				setEditable(true);
				return true;
			}
		});
	}
	
	@Override
	public void onPause() {
		super.onPause();
	}
	
	@Override
	public void onStop() {
		mData = null;
		super.onStop();
	}
	
	@Override
	public void setInitialSavedState(SavedState state) {
		super.setInitialSavedState(state);
		Logger.LogInfo("setInitialSavedState @ TextEditor (" + mPath.getPath() + ")");
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if(!mSalvage) return;
		Logger.LogInfo("saveInstanceState @ TextEditor (" + mPath.getPath() + ")");
		outState.putString("edit_path", mPath.getPath());
		if(mData != null && mData.length() < 500000)
			outState.putString("edit_data", mData);
		if(mPath instanceof OpenNetworkPath)
		{
			if(((OpenNetworkPath)mPath).getServersIndex() > -1)
			{
				Logger.LogDebug("Saving server #" + ((OpenNetworkPath)mPath).getServersIndex());
				outState.putInt("edit_server", ((OpenNetworkPath)mPath).getServersIndex());
			} else Logger.LogWarning("No server #");
		}
		outState.putFloat("size", mTextSize);
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
		mSalvage = false;
		cancelTask();
		if(getExplorer() != null && getExplorer().isViewPagerEnabled())
			getExplorer().closeEditor(this);				
		else if(getFragmentManager() != null && getFragmentManager().getBackStackEntryCount() > 0)
			getFragmentManager().popBackStack();
		else if(getActivity() != null)
			getActivity().finish();
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
	public boolean onClick(int id) {
		return onClickItem(id);
	}
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(requestCode == REQUEST_SAVE_AS)
		{
			if(data.hasExtra("path"))
				mPath = (OpenPath)data.getParcelableExtra("path");
			else if(data.getData() != null)
				mPath = FileManager.getOpenCache(data.getDataString());
			cancelTask();
			mTask = new FileSaveTask(mPath);
			if(mDirty)
				mData = mEditText.getText().toString();
			((FileSaveTask)mTask).execute(mData);
			notifyPager();
		}
	}
	public boolean onClickItem(int id) {
		Context c = getActivity();
		View from = null; 
		if(getExplorer() != null)
			from = getExplorer().findViewById(id);
		Logger.LogDebug("TextEditorFragment.onClickItem(0x" + Integer.toHexString(id) + ")");
		switch(id)
		{
		case R.id.menu_context_info:
			if(c != null)
				DialogHandler.showFileInfo(c, getPath());
			return true;
		case R.id.menu_save:
			doSave();
			return true;
			
		case R.id.menu_save_as:
			
			Intent intent = new Intent(getActivity(), FolderPickerActivity.class);
			intent.putExtra("start", (Parcelable)mPath);
			intent.putExtra("name", mPath.getName());
			intent.setData(mPath.getUri());
			startActivityForResult(intent, REQUEST_SAVE_AS);
			return true;
			
		case R.id.menu_close:
			doClose();
			return true;
			
		case R.id.menu_view_font_size:
			if(USE_SEEK_ACTIONVIEW)
				((SeekBarActionView)mFontSizeBar).onActionViewExpanded();
			else DialogHandler.showSeekBarDialog(getActivity(),
					getString(R.string.s_view_font_size),
					(int)mTextSize, 60,
					this);
			return true;
			
		case R.id.menu_view:
			if(OpenExplorer.BEFORE_HONEYCOMB)
				showMenu(R.menu.text_view, from);
			return true;
			
		case R.id.menu_file:
			if(OpenExplorer.BEFORE_HONEYCOMB)
				showMenu(R.menu.text_file, from);
			return true;
			
		case R.id.menu_view_keyboard_toggle:
			setEditable(!mEditMode);
			return true;
		}
		return false;
	}
	
	private void setEditable(boolean editable)
	{
		if(mEditMode == editable) return;
		mEditMode = editable;
		if(editable)
		{
			//mWebText.setVisibility(View.GONE);
			//mViewScroller.setVisibility(View.GONE);
			//mViewScroller.setVisibility(View.GONE);
			mViewList.setVisibility(View.GONE);
			mEditText.setVisibility(View.VISIBLE);
			mEditText.removeTextChangedListener(this);
			setText(mData);
			mEditText.addTextChangedListener(this);
		} else {
			mEditText.removeTextChangedListener(this);
			if(mPath.canWrite() && mDirty)
			{
				DialogHandler.showConfirmationDialog(getActivity(),
						getString(R.string.s_alert_dirty, mPath.getName()),
						getText(R.string.s_save).toString() + "?",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								doSave();
								mEditText.setVisibility(View.GONE);
								mViewList.setVisibility(View.VISIBLE);
							}
						},
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								dialog.cancel();
								mEditText.setVisibility(View.GONE);
								mViewList.setVisibility(View.VISIBLE);
							}
						},
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								setEditable(true);
							}
						}
					);
			} else {
				mEditText.setVisibility(View.GONE);
				mViewList.setVisibility(View.VISIBLE);
			}
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
				if(mPath instanceof OpenNetworkPath)
					((OpenNetworkPath)mPath).disconnect();
				if(mPath instanceof NeedsTempFile)
					((NeedsTempFile)mPath).tempUpload();
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
				} catch(SecurityException s) {
					Logger.LogError("Couldn't open file due to security. " + path, s);
					doClose();
				} catch (RuntimeException r) {
					Logger.LogError("File too large?", r);
					if(getExplorer() != null)
						getExplorer().showToast("Unable to open file. File too large?");
					doClose();
				} catch (Exception e) {
					Logger.LogError("Couldn't find file - " + path, e);
					doClose();
				} finally {
					try {
						if(is != null)
							is.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
					if(mPath instanceof OpenNetworkPath)
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
				BufferedInputStream in = null;
				try {
					in = new BufferedInputStream(mPath.getInputStream());
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
				} finally {
					if(in != null)
						try {
							in.close();
						} catch(Exception e) { }
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
			if(result != null)
				setText(result);
			setProgressVisibility(false);
			setEnabled(true, mEditText);
		}
	}

	public OpenPath getPath() {
		return mPath;
	}
	@Override
	public Drawable getIcon() {
		if(getActivity() == null) return null;
		return new BitmapDrawable(getResources(),
				ThumbnailCreator.getFileExtIcon(getPath().getExtension(), getActivity(), false));
	}
	@Override
	public CharSequence getTitle() {
		SpannableString ret = new SpannableString(getPath().getName());
		if(mDirty)
			ret.setSpan(new StyleSpan(Typeface.ITALIC), 0, ret.length(), 0);
		//ret.setSpan(new ImageSpan(getActivity(), R.drawable.ic_menu_save), 0, 1, 0);
		//Logger.LogDebug("TextEditorFragment.getTitle() = " + ret.toString());
		return ret;
	}
	
	public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		if(count == 0) return;
		mData = s.toString();
		mDirty = true;
		if(getExplorer() != null)
			getExplorer().notifyPager();
	}
	public void afterTextChanged(Editable s) {
	}
	
	@Override
	public boolean onTitleLongClick(View titleView) {
		return showMenu(R.menu.text_editor, titleView);
	}

	public boolean isSalvagable() {
		return mSalvage;
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		if(!fromUser) return;
		float fsize = (float)(progress + 1) / 2;
		setTextSize(fsize);
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		Logger.LogDebug("refresh!");
		mViewListAdapter.notifySizeChanged();
	}
	
}
