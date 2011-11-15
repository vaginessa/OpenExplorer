/*
    Open Explorer, an open source file explorer & text editor
    Copyright (C) 2011 Brandon Bowles <brandroid64@gmail.com>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.brandroid.openmanager.fragments;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.app.DialogFragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Date;
import java.io.File;
import java.io.IOException;

import org.brandroid.openmanager.OpenExplorer;
import org.brandroid.openmanager.R;
import org.brandroid.openmanager.R.drawable;
import org.brandroid.openmanager.R.id;
import org.brandroid.openmanager.R.layout;
import org.brandroid.openmanager.data.BookmarkHolder;
import org.brandroid.openmanager.data.OpenPath;
import org.brandroid.openmanager.data.OpenFile;
import org.brandroid.openmanager.util.IntentManager;
import org.brandroid.openmanager.util.ThumbnailCreator;
import org.brandroid.utils.Logger;

public class DialogHandler extends DialogFragment {
	
	public static enum DialogType {
		HOLDINGFILE_DIALOG,
		SEARCHRESULT_DIALOG,
		FILEINFO_DIALOG
	}
	
	private static DialogHandler instance = null;
	private static DialogType mDialogType;
	private static Context mContext;
	
	private OnSearchFileSelected mSearchListener;
	private ArrayList<OpenPath> mFiles;
	private OpenPath mPath;
	
	
	public interface OnSearchFileSelected {
		public void onFileSelected(String fileName);
	}
	
	public static DialogHandler newDialog(DialogType type, Context context) {
		instance = new DialogHandler();
		mDialogType = type;
		mContext = context;
		
		return instance;
	}
		
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
			
		switch(mDialogType) {
		case HOLDINGFILE_DIALOG:
			setStyle(DialogFragment.STYLE_NORMAL,
					!OpenExplorer.BEFORE_HONEYCOMB ?
							android.R.style.Theme_Holo_Dialog : android.R.style.Theme_Dialog);
			break;
		case SEARCHRESULT_DIALOG:
			setStyle(OpenExplorer.BEFORE_HONEYCOMB ? DialogFragment.STYLE_NORMAL : DialogFragment.STYLE_NO_TITLE, 
					!OpenExplorer.BEFORE_HONEYCOMB ?
							android.R.style.Theme_Holo_Panel : android.R.style.Theme_Panel);
			break;
			
		case FILEINFO_DIALOG:
			setStyle(OpenExplorer.BEFORE_HONEYCOMB ? DialogFragment.STYLE_NORMAL : DialogFragment.STYLE_NO_FRAME,
					!OpenExplorer.BEFORE_HONEYCOMB ?
							android.R.style.Theme_Holo_Panel : android.R.style.Theme_Panel);
			break;
		}
	}
	
	
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if(savedInstanceState != null && savedInstanceState.containsKey("path"))
			mPath = new OpenFile(savedInstanceState.getString("path"));
		switch(mDialogType) {
		case HOLDINGFILE_DIALOG:  	return createHoldingFileDialog();
		case SEARCHRESULT_DIALOG: 	return createSearchResultDialog(inflater);
		case FILEINFO_DIALOG:		return createFileInfoDialog(inflater);
		}

		return super.onCreateView(inflater, container, savedInstanceState);
	}
	
	@Override
	public void onSaveInstanceState(Bundle arg0) {
		super.onSaveInstanceState(arg0);
		arg0.putString("path", mPath.getPath());
	}
	
	public void setHoldingFileList(ArrayList<OpenPath> list) {
		mFiles = list;
	}
	
	public void setFilePath(String path) {
		mPath = new OpenFile(path);
	}
	
	public void setOnSearchFileSelected(OnSearchFileSelected s) {
		mSearchListener = s;
	}
	
	private View createHoldingFileDialog() {
		getDialog().getWindow().setGravity(Gravity.LEFT | Gravity.TOP);
		getDialog().setTitle("Holding " + mFiles.size() + " files");
		
		ListView list = new ListView(mContext);
		list.setAdapter(new DialogListAdapter(mContext, R.layout.bookmark_layout, mFiles));

		return list;
	}
	
	private View createSearchResultDialog(LayoutInflater inflater) {
		getDialog().getWindow().setGravity(Gravity.RIGHT);
		
		final View v = inflater.inflate(R.layout.search_grid, null);
		final Button launch_button = (Button)v.findViewById(R.id.search_button_open);
		final Button goto_button = (Button)v.findViewById(R.id.search_button_go);
		final LinearLayout layout = (LinearLayout)v.findViewById(R.id.search_button_view);
		layout.setBackgroundColor(0xee444444);

		ListView list = (ListView)v.findViewById(R.id.search_listview);
		list.setBackgroundColor(0xcc000000);
		list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			
			
			public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
				final OpenPath selected = mFiles.get(position);
				
				if (layout.getVisibility() == View.GONE)
					layout.setVisibility(View.VISIBLE);
				
				goto_button.setOnClickListener(new View.OnClickListener() {
					
					
					public void onClick(View v) {
						mSearchListener.onFileSelected(selected.getPath());
						dismiss();
					}
				});
				
				if(IntentManager.startIntent(selected, (OpenExplorer)getActivity(), ((OpenExplorer)getActivity()).getEventHandler()))
					return;
				
				
				if (!selected.isDirectory()) {
					launch_button.setVisibility(View.VISIBLE);
					launch_button.setOnClickListener(new View.OnClickListener() {
						
						
						public void onClick(View v) {
							String item_ext = "";
															
							try {
								item_ext = selected.getName().substring(selected.getName().lastIndexOf("."));
								
							} catch (StringIndexOutOfBoundsException e) {
								item_ext = "";
							}
							
							/*audio files*/
							if (item_ext.equalsIgnoreCase(".mp3") || 
								item_ext.equalsIgnoreCase(".m4a") ) {
					    		
					    		Intent i = new Intent();
				   				i.setAction(android.content.Intent.ACTION_VIEW);
				   				i.setDataAndType(selected.getUri(), "audio/*");
				   				startActivity(i);
							}
							
							/* image files*/
							else if(item_ext.equalsIgnoreCase(".jpeg") || 
					    			item_ext.equalsIgnoreCase(".jpg")  ||
					    			item_ext.equalsIgnoreCase(".png")  ||
					    			item_ext.equalsIgnoreCase(".gif")  || 
					    			item_ext.equalsIgnoreCase(".tiff")) {

								Intent picIntent = new Intent();
						    		picIntent.setAction(android.content.Intent.ACTION_VIEW);
						    		picIntent.setDataAndType(selected.getUri(), "image/*");
						    		startActivity(picIntent);
					    	}
							
							/*video file selected--add more video formats*/
					    	else if(item_ext.equalsIgnoreCase(".m4v") ||
					    			item_ext.equalsIgnoreCase(".mp4") ||
					    			item_ext.equalsIgnoreCase(".3gp") ||
					    			item_ext.equalsIgnoreCase(".wmv") || 
					    			item_ext.equalsIgnoreCase(".mp4") || 
					    			item_ext.equalsIgnoreCase(".ogg") ||
					    			item_ext.equalsIgnoreCase(".wav")) {
					    		
				    				Intent movieIntent = new Intent();
						    		movieIntent.setAction(android.content.Intent.ACTION_VIEW);
						    		movieIntent.setDataAndType(selected.getUri(), "video/*");
						    		startActivity(movieIntent);	
					    	}
							
							/*pdf file selected*/
					    	else if(item_ext.equalsIgnoreCase(".pdf")) {
					    		
					    		if(selected.exists()) {
						    		Intent pdfIntent = new Intent();
						    		pdfIntent.setAction(android.content.Intent.ACTION_VIEW);
						    		pdfIntent.setDataAndType(selected.getUri(), "application/pdf");
							    		
						    		try {
						    			startActivity(pdfIntent);
						    		} catch (ActivityNotFoundException e) {
						    			Toast.makeText(mContext, "Sorry, couldn't find a pdf viewer", 
												Toast.LENGTH_SHORT).show();
						    		}
						    	}
					    	}
							
							/*Android application file*/
					    	else if(item_ext.equalsIgnoreCase(".apk")){
					    		
					    		if(selected.exists()) {
					    			Intent apkIntent = new Intent();
					    			apkIntent.setAction(android.content.Intent.ACTION_VIEW);
					    			apkIntent.setDataAndType(selected.getUri(), 
					    									 "application/vnd.android.package-archive");
					    			startActivity(apkIntent);
					    		}
					    	}
							
							/* HTML XML file */
					    	else if(item_ext.equalsIgnoreCase(".html") || 
					    			item_ext.equalsIgnoreCase(".xml")) {
					    		
					    		if(selected.exists()) {
					    			Intent htmlIntent = new Intent();
					    			htmlIntent.setAction(android.content.Intent.ACTION_VIEW);
					    			htmlIntent.setDataAndType(selected.getUri(), "text/html");
					    			
					    			try {
					    				startActivity(htmlIntent);
					    			} catch(ActivityNotFoundException e) {
					    				Toast.makeText(mContext, "Sorry, couldn't find a HTML viewer", 
					    									Toast.LENGTH_SHORT).show();
						    			
					    			}
					    		}
					    	}
														
							/* text file*/
					    	else if(item_ext.equalsIgnoreCase(".txt")) {
				    			Intent txtIntent = new Intent();
				    			txtIntent.setAction(android.content.Intent.ACTION_VIEW);
				    			txtIntent.setDataAndType(selected.getUri(), "text/plain");
				    			
				    			try {
				    				startActivity(txtIntent);
				    			} catch(ActivityNotFoundException e) {
				    				txtIntent.setType("text/*");
				    				startActivity(txtIntent);
				    			}
					    	}
							
							/* generic intent */
					    	else {
					    		if(selected.exists()) {
						    		Intent generic = new Intent();
						    		generic.setAction(android.content.Intent.ACTION_VIEW);
						    		generic.setDataAndType(selected.getUri(), "application/*");
						    		
						    		try {
						    			startActivity(generic);
						    		} catch(ActivityNotFoundException e) {
						    			Toast.makeText(mContext, "Sorry, couldn't find anything " +
						    						   "to open " + selected.getName(), 
						    						   Toast.LENGTH_SHORT).show();
							    	}
					    		}
					    	}
							
							dismiss();
						}
					});
					
				} else {
					launch_button.setVisibility(View.INVISIBLE);
				}
				
				try {
					populateFileInfoViews(v, selected);
				} catch (IOException e) {
					Logger.LogError("Couldn't populate.", e);
				}
			}
		});
		list.setAdapter(new DialogListAdapter(mContext, R.layout.bookmark_layout, mFiles));
		
		return v;
	}
	
	private View createFileInfoDialog(LayoutInflater inflater) {
		View v = inflater.inflate(R.layout.info_layout, null);
		v.setBackgroundColor(0xcc000000);
		v.setOnTouchListener(new OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				getDialog().hide();
				return false;
			}
		});
		
		try {
			populateFileInfoViews(v, mPath);
		} catch (IOException e) {
			Logger.LogError("Couldn't create info dialog", e);
		}
		
		return v;
	}
	
	public static String formatSize(long size) {
		int kb = 1024;
		int mb = kb * 1024;
		int gb = mb * 1024;
		String ssize = "";
		
		if (size < kb)
			ssize = size + " B";
		else if (size > kb && size < mb)
			ssize = ((double)Math.round(((double)size / kb) * 100) / 100) + " KB";
		else if (size > mb && size < gb)
			ssize = ((double)Math.round(((double)size / mb) * 100) / 100) + " MB";
		else if(size > gb)
			ssize = ((double)Math.round(((double)size / gb) * 100) / 100) + " GB";
		
		return ssize;
	}
	
	private void populateFileInfoViews(View v, OpenPath file) throws IOException {
			
		String apath = file.getPath();
		Date date = new Date(file.lastModified());
		
		TextView numDir = (TextView)v.findViewById(R.id.info_dirs_label);
		TextView numFile = (TextView)v.findViewById(R.id.info_files_label);
		TextView numSize = (TextView)v.findViewById(R.id.info_total_size);
		
		if (file.isDirectory()) {
			
			new CountAllFilesTask(numDir, numFile, numSize).execute((OpenFile)file);
			
		} else {
			numFile.setText("-");
			numDir.setText("-");
		}
		
		((TextView)v.findViewById(R.id.info_name_label)).setText(file.getName());
		((TextView)v.findViewById(R.id.info_time_stamp)).setText(date.toString());
		((TextView)v.findViewById(R.id.info_path_label)).setText(apath.substring(0, apath.lastIndexOf("/") + 1));
		((TextView)v.findViewById(R.id.info_read_perm)).setText(file.canRead() + "");
		((TextView)v.findViewById(R.id.info_write_perm)).setText(file.canWrite() + "");
		((TextView)v.findViewById(R.id.info_execute_perm)).setText(file.canExecute() + "");
		
		if (file.isDirectory())
			((ImageView)v.findViewById(R.id.info_icon)).setImageResource(R.drawable.folder);
		else
			((ImageView)v.findViewById(R.id.info_icon)).setImageDrawable(getFileIcon(file, false));
	}
	
	private Drawable getFileIcon(OpenPath file, boolean largeSize) {
		return new BitmapDrawable(ThumbnailCreator.generateThumb(file, 96, 96).get());
	}
	
	private class CountAllFilesTask extends AsyncTask<OpenFile, Integer, String[]>
	{
		private TextView mTextFiles, mTextDirs, mTextSize;
		private int firstDirs = 0, firstFiles = 0;
		private int dirCount = 0, fileCount = 0;
		private long totalSize = 0, firstSize = 0;
		
		public CountAllFilesTask(TextView mTextFiles, TextView mTextDirs, TextView mTextSize) {
			this.mTextFiles = mTextFiles;
			this.mTextDirs = mTextDirs;
			this.mTextSize = mTextSize;
		}
		
		private void addPath(OpenPath p, boolean bFirst)
		{
			if(!p.isDirectory())
			{
				fileCount++;
				totalSize += p.length();
				if(bFirst)
				{
					firstFiles++;
					firstSize += p.length();
				}
			} else {
				dirCount++;
				if(bFirst)
					firstDirs++;
				for(OpenPath f : p.list())
					addPath(f, false);
			}
			if(fileCount + dirCount % 50 == 0)
				publishProgress(fileCount, dirCount);
		}
		
		@Override
		protected void onProgressUpdate(Integer... values) {
			updateTexts(mTextFiles, fileCount, mTextDirs, dirCount, mTextSize, totalSize);
		}

		@Override
		protected String[] doInBackground(OpenFile... params) {
			OpenPath path = params[0];
			
			addPath(path, true);
			
			String[] ret = new String[3];
			ret[0] = firstFiles + " (" + fileCount + ")";
			ret[1] = firstDirs + " (" + dirCount + ")";
			ret[2] = formatSize(totalSize);
			return ret;
		}
		
		public void updateTexts(Object... params)
		{
			for(int i = 0; i < params.length - 1; i += 2)
			{
				if(TextView.class.equals(params[i].getClass()))
					((TextView)params[i]).setText(params[i+1].toString());
			}
		}
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			updateTexts(mTextFiles, "-", mTextDirs, "-", mTextSize, "0");
		}
		
		@Override
		protected void onPostExecute(String[] result) {
			super.onPostExecute(result);
			if(mTextFiles != null && result != null && result.length > 0)
				mTextFiles.setText(result[0]);
			if(mTextDirs != null && result != null && result.length > 1)
				mTextDirs.setText(result[1]);
			if(mTextSize != null && result != null && result.length > 2)
				mTextSize.setText(result[2]);
		}
		
	}
	
	/*
	 * 
	 */
	private class DialogListAdapter extends ArrayAdapter<OpenPath> {
		private BookmarkHolder mHolder;
		
		public DialogListAdapter(Context context, int layout, ArrayList<OpenPath> data) {
			super(context, layout, data);
			
		}
		
		
		public View getView(int position, View view, ViewGroup parent) {
			String ext;
			OpenPath file = mFiles.get(position);
			//String file.getName();
			String name = file.getName(); // file.substring(file.lastIndexOf("/") + 1, file.length());
			
			
			if (view == null) {
				LayoutInflater inflater = (LayoutInflater)mContext
											.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				
				mHolder = new BookmarkHolder(file, name, inflater.inflate(R.layout.bookmark_layout, parent, false));				
				view.setTag(mHolder);
				
			} else {
				mHolder = (BookmarkHolder)view.getTag();
			}
			
			if (file.isDirectory())
				ext = "dir";
			else
				ext = name.substring(name.lastIndexOf(".") + 1);
			
			mHolder.setText(name);
			
			if(ext.equalsIgnoreCase("dir")) {	
				mHolder.setIconResource(R.drawable.folder);
				
			} else if(ext.equalsIgnoreCase("doc") || ext.equalsIgnoreCase("docx")) {
				mHolder.setIconResource(R.drawable.doc);
				
			} else if(ext.equalsIgnoreCase("xls")  || 
					  ext.equalsIgnoreCase("xlsx") ||
					  ext.equalsIgnoreCase("xlsm")) {
				mHolder.setIconResource(R.drawable.excel);
				
			} else if(ext.equalsIgnoreCase("ppt") || ext.equalsIgnoreCase("pptx")) {
				mHolder.setIconResource(R.drawable.powerpoint);
				
			} else if(ext.equalsIgnoreCase("zip") || ext.equalsIgnoreCase("gzip")) {
				mHolder.setIconResource(R.drawable.zip);
				
			} else if(ext.equalsIgnoreCase("apk")) {
				mHolder.setIconResource(R.drawable.apk);
				
			} else if(ext.equalsIgnoreCase("pdf")) {
				mHolder.setIconResource(R.drawable.pdf);
				
			} else if(ext.equalsIgnoreCase("xml") || ext.equalsIgnoreCase("html")) {
				mHolder.setIconResource(R.drawable.xml_html);
				
			} else if(ext.equalsIgnoreCase("mp4") || 
					  ext.equalsIgnoreCase("3gp") ||
					  ext.equalsIgnoreCase("webm") || 
					  ext.equalsIgnoreCase("m4v")) {
				mHolder.setIconResource(R.drawable.movie);
				
			} else if(ext.equalsIgnoreCase("mp3") || ext.equalsIgnoreCase("wav") ||
					  ext.equalsIgnoreCase("wma") || ext.equalsIgnoreCase("m4p") ||
					  ext.equalsIgnoreCase("m4a") || ext.equalsIgnoreCase("ogg")) {
				mHolder.setIconResource(R.drawable.music);
				
			} else if(ext.equalsIgnoreCase("jpeg") || ext.equalsIgnoreCase("png") ||
					  ext.equalsIgnoreCase("jpg")  || ext.equalsIgnoreCase("gif")) {
				mHolder.setIconResource(R.drawable.photo);
				
			} else {
				mHolder.setIconResource(R.drawable.unknown);
			}
			
			return view;
		}
	}
}
