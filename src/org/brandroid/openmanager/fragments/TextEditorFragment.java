package org.brandroid.openmanager.fragments;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
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
import org.brandroid.openmanager.data.OpenFTP;
import org.brandroid.openmanager.data.OpenFile;
import org.brandroid.openmanager.data.OpenNetworkPath;
import org.brandroid.openmanager.data.OpenPath;
import org.brandroid.openmanager.data.OpenServer;
import org.brandroid.openmanager.data.OpenServers;
import org.brandroid.openmanager.ftp.FTPManager;
import org.brandroid.openmanager.util.FileManager;
import org.brandroid.utils.Logger;
import org.brandroid.utils.MenuUtils;

import android.content.Context;
import android.inputmethodservice.InputMethodService.InputMethodImpl;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

public class TextEditorFragment extends OpenFragment
	implements OnClickListener, OpenPathFragmentInterface
{
	private EditText mEditText;
	private ProgressBar mProgress;
	
	private OpenPath mPath = null;
	private String mData = null;
	
	private AsyncTask mTask = null;
	
	private boolean bShowKeyboard = true;
	
	public static int[] USED_MENUS = new int[]{R.id.menu_save, R.id.menu_close, R.id.menu_context_info, R.id.menu_view,
			R.id.menu_view_font_large, R.id.menu_view_font_medium, R.id.menu_view_font_small,
			R.id.menu_view_keyboard_toggle, R.id.menu_settings};
	
	public TextEditorFragment() { }
	public TextEditorFragment(OpenPath path)
	{
		mPath = path;
		Bundle b = new Bundle();
		if(path != null && path.getPath() != null)
			b.putString("edit_path", path.getPath().toString());
		setArguments(b);
		setHasOptionsMenu(true);
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle bundle = savedInstanceState;
		if(savedInstanceState == null && getArguments() != null)
			bundle = getArguments();
		if(bundle != null && bundle.containsKey("edit_path"))
		{
			String path = bundle.getString("edit_path");
			mPath = new OpenFile(path);
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
			} else if (path.indexOf("ftp:/") > -1)
			{
				path = path.substring(path.lastIndexOf("/", path.indexOf(":/") + 2) + 1);
				String host = path;
				if(path.indexOf("/") > -1)
				{
					host = path.substring(0, path.indexOf("/"));
					path = path.substring(path.indexOf("/"));
				}
				OpenServers servers = SettingsActivity.LoadDefaultServers(getActivity());
				for(int i=0; i < servers.size(); i++)
					if(servers.get(i).getHost().equals(host))
					{
						OpenServer server = servers.get(i);
						FTPManager man = new FTPManager(server.getHost(), server.getUser(), server.getPassword(), server.getPath());
						Logger.LogDebug("Searched & Found server - " + server.getName());
						mPath = new OpenFTP(mPath.getPath(), null, man);
					}
			}
		}
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.text_editor, menu);
	}
	
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		MenuUtils.setMenuVisible(menu, false);
		MenuUtils.setMenuVisible(menu, true, USED_MENUS);
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
		mProgress = ((ProgressBar)view.findViewById(android.R.id.progress));
		return view;
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		mEditText.setOnFocusChangeListener(new OnFocusChangeListener() {
			public void onFocusChange(View v, boolean hasFocus) {
				/*if(hasFocus)
				{
					InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.showSoftInput(v, 0);
				}*/
			}
		});
		if(mPath != null && mData == null)
			mTask = new FileLoadTask().execute(mPath);
		else if (mData != null)
			mEditText.setText(mData);
	}
	
	@Override
	public void onPause() {
		super.onPause();
		mData = mEditText.getText().toString();
	}
	
	@Override
	public void onStop() {
		super.onStop();
		mData = mEditText.getText().toString();
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
	
	public void save()
	{
		cancelTask();
		mTask = new FileSaveTask(mPath);
		((FileSaveTask)mTask).execute(mEditText.getText().toString());
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
			save();
			return true;
			
		case R.id.menu_close:
			cancelTask();
			if(getExplorer().isViewPagerEnabled())
			{
				ViewPager pager = (ViewPager)getExplorer().findViewById(R.id.content_pager);
				ArrayPagerAdapter adapter = (ArrayPagerAdapter)pager.getAdapter();
				if(pager.getCurrentItem() > 0)
					pager.setCurrentItem(pager.getCurrentItem() - 1);
				adapter.remove(this);
				pager.setAdapter(adapter);
			} else
				getFragmentManager().popBackStack();
			return true;
		case R.id.menu_view_keyboard_toggle:
			//if(((InputMethodManager)getActivity()).getInputMethodList().
			((InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(mEditText, 0);
			return true;
		}
		return false;
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
			if(mProgress != null)
				mProgress.setVisibility(View.VISIBLE);
			setEnabled(false, mEditText);
		}
		
		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			if(mProgress != null)
				mProgress.setVisibility(View.GONE);
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
			if(mProgress != null)
				mProgress.setVisibility(View.VISIBLE);
		}
		
		@Override
		protected void onPostExecute(String result) {
			if(mEditText != null)
				mEditText.setText(result);
			if(mProgress != null)
				mProgress.setVisibility(View.GONE);
			setEnabled(true, mEditText);
			mData = result;
		}
	}

	public OpenPath getPath() {
		// TODO Auto-generated method stub
		return mPath;
	}
	@Override
	public CharSequence getTitle() {
		// TODO Auto-generated method stub
		return getPath().getName();
	}
}
