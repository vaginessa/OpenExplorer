package org.brandroid.openmanager.fragments;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.CharBuffer;

import org.apache.commons.net.ftp.FTPFile;
import org.apache.http.util.ByteArrayBuffer;
import org.brandroid.openmanager.R;
import org.brandroid.openmanager.ftp.FTPManager;
import org.brandroid.utils.Logger;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class TextEditorFragment extends Fragment implements OnClickListener
{
	private EditText mEditText;
	
	private String mPath, mData;
	
	public TextEditorFragment(String path)
	{
		mPath = path;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.edit_text, null);
		return view;
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		mEditText = (EditText)view.findViewById(R.id.text_edit);
		((Button)view.findViewById(R.id.btn_save)).setOnClickListener(this);
		((Button)view.findViewById(R.id.btn_cancel)).setOnClickListener(this);
		((TextView)view.findViewById(R.id.label_path)).setText(mPath);
		new FileLoadTask().execute(mPath);
	}

	public void onClick(View v) {
		switch(v.getId())
		{
		case R.id.btn_cancel:
			
			break;
		}
	}
	
	public class FileLoadTask extends AsyncTask<String, Integer, String>
	{

		@Override
		protected String doInBackground(String... params) {
			Logger.LogDebug("Getting " + params[0]);
			String path = params[0];
			File f = new File(path);
			if(f.canRead()) {
				Logger.LogDebug("File is " + f.length() + " bytes.");
				try {
					FileReader fr = new FileReader(f);
					char[] buffer = new char[(int)f.length()];
					fr.read(buffer);
					StringBuilder sb = new StringBuilder();
					sb.append(buffer);
					return sb.toString();
				} catch (FileNotFoundException e) {
					Logger.LogError("Couldn't find file - " + path, e);
				} catch (IOException e) {
					Logger.LogError("Couldn't read file - " + path, e);
				}
			}
			else if(path.indexOf("ftp:/") > -1)
			{
				try {
					URL url = new URL(path);
					FTPManager ftp = FTPManager.getInstance(url);
					BufferedInputStream in = (BufferedInputStream)ftp.getInputStream(url.getPath());
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
				
				//return FTPManager.getData(path);
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(String result) {
			mEditText.setText(result);
			mData = result;
		}
	}
}
