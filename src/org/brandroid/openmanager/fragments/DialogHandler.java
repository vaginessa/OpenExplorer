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

import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.app.DialogFragment;
import android.support.v4.util.TimeUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;
import java.util.zip.ZipFile;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

import org.brandroid.openmanager.R;
import org.brandroid.openmanager.R.drawable;
import org.brandroid.openmanager.R.id;
import org.brandroid.openmanager.R.layout;
import org.brandroid.openmanager.activities.OpenExplorer;
import org.brandroid.openmanager.data.BookmarkHolder;
import org.brandroid.openmanager.data.OpenMediaStore;
import org.brandroid.openmanager.data.OpenPath;
import org.brandroid.openmanager.data.OpenFile;
import org.brandroid.openmanager.data.OpenSMB;
import org.brandroid.openmanager.util.IntentManager;
import org.brandroid.openmanager.util.OpenChromeClient;
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
		case FILEINFO_DIALOG:		return createFileInfoDialog(inflater, mPath);
		}

		return super.onCreateView(inflater, container, savedInstanceState);
	}
	
	@Override
	public void onSaveInstanceState(Bundle bundle) {
		bundle.putString("path", mPath.getPath());
		super.onSaveInstanceState(bundle);
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
		if(getDialog() == null || getDialog().getWindow() == null) return null;
		getDialog().getWindow().setGravity(Gravity.LEFT | Gravity.TOP);
		getDialog().setTitle(getResources().getString(R.string.s_title_holding_x_files).replace("xxx", "" + mFiles.size()));
		
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
				
				if(IntentManager.isIntentAvailable(selected, (OpenExplorer)getActivity()))
				{
					IntentManager.startIntent(selected, (OpenExplorer)getActivity());
					return;
				}
					
				
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
	
	public static View createFileInfoDialog(LayoutInflater inflater, OpenPath mPath) {
		View v = inflater.inflate(R.layout.info_layout, null);
		v.setBackgroundColor(0xcc000000);
		/*
		v.setOnTouchListener(new OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				getDialog().hide();
				return false;
			}
		});
		*/
		
		try {
			populateFileInfoViews(v, mPath);
		} catch (IOException e) {
			Logger.LogError("Couldn't create info dialog", e);
		}
		
		return v;
	}
	
	public static String formatSize(long size) { return formatSize(size, 2); }
	public static String formatSize(long size, int decimalPoints) {
		int kb = 1024;
		int mb = kb * 1024;
		int gb = mb * 1024;
		String ssize = "";
		
		int factor = (10^decimalPoints);
		
		if (size < kb)
			ssize = size + " B";
		else if (size > kb && size < mb)
			ssize = ((double)Math.round(((double)size / kb) * factor) / factor) + " KB";
		else if (size > mb && size < gb)
			ssize = ((double)Math.round(((double)size / mb) * factor) / factor) + " MB";
		else if(size > gb)
			ssize = ((double)Math.round(((double)size / gb) * factor) / factor) + " GB";
		
		return ssize;
	}
	
	public static void populateFileInfoViews(View v, OpenPath file) throws IOException {
			
		String apath = file.getAbsolutePath();
		if(file instanceof OpenMediaStore)
			file = ((OpenMediaStore)file).getFile();
		Date date = new Date(file.lastModified());
		
		TextView numDir = (TextView)v.findViewById(R.id.info_dirs_label);
		TextView numFile = (TextView)v.findViewById(R.id.info_files_label);
		TextView numSize = (TextView)v.findViewById(R.id.info_size);
		TextView numTotal = (TextView)v.findViewById(R.id.info_total_size);
		TextView numFree = (TextView)v.findViewById(R.id.info_free_size);
		
		//if (file.isDirectory()) {
			
			new CountAllFilesTask(numDir, numFile, numSize, numFree, numTotal).execute(file);
			
		//} else {
			//numFile.setText("-");
			//numDir.setText("-");
		//}
		
		((TextView)v.findViewById(R.id.info_name_label)).setText(file.getName());
		((TextView)v.findViewById(R.id.info_time_stamp)).setText(date.toString());
		((TextView)v.findViewById(R.id.info_path_label)).setText(file.getParent() != null ? file.getParent().getPath() : "");
		((TextView)v.findViewById(R.id.info_read_perm)).setText(file.canRead() + "");
		((TextView)v.findViewById(R.id.info_write_perm)).setText(file.canWrite() + "");
		((TextView)v.findViewById(R.id.info_execute_perm)).setText(file.canExecute() + "");
		
		if (file.isDirectory())
			((ImageView)v.findViewById(R.id.info_icon)).setImageResource(R.drawable.lg_folder);
		else
			((ImageView)v.findViewById(R.id.info_icon)).setImageDrawable(getFileIcon(file, false));
	}
	
	public static Drawable getFileIcon(OpenPath file, boolean largeSize) {
		return new BitmapDrawable(ThumbnailCreator.generateThumb(file, 96, 96).get());
	}
	
	public static class CountAllFilesTask extends AsyncTask<OpenPath, Integer, String[]>
	{
		private TextView mTextFiles, mTextDirs, mTextSize, mTextFree, mTextTotal;
		private int firstDirs = 0, firstFiles = 0;
		private int dirCount = 0, fileCount = 0;
		private long totalSize = 0, firstSize = 0;
		private long freeSize = 0l, diskTotal = 0l;
		
		public CountAllFilesTask(TextView mTextFiles, TextView mTextDirs,
				TextView mTextSize, TextView mTextFree, TextView mTextTotal) {
			this.mTextFiles = mTextFiles;
			this.mTextDirs = mTextDirs;
			this.mTextSize = mTextSize;
			this.mTextFree = mTextFree;
			this.mTextTotal = mTextTotal;
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
				try {
					for(OpenPath f : p.list())
						addPath(f, false);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if(fileCount + dirCount % 50 == 0)
				publishProgress();
		}
		
		@Override
		protected void onProgressUpdate(Integer... values) {
			updateTexts(mTextFiles, fileCount,
					mTextDirs, dirCount,
					mTextSize, formatSize(totalSize),
					mTextFree, formatSize(freeSize),
					mTextTotal, formatSize(diskTotal));
		}

		@Override
		protected String[] doInBackground(OpenPath... params) {
			OpenPath path = params[0];

			totalSize = path.length();
			if(!path.isDirectory())
			{
				firstFiles = fileCount = 1;
				firstDirs = dirCount = 0;
			}
			
			publishProgress();
			
			if(path instanceof OpenFile)
			{
				freeSize = ((OpenFile)path).getFreeSpace();
				diskTotal = ((OpenFile)path).getTotalSpace();
				publishProgress();
			} else if(path instanceof OpenMediaStore)
			{
				OpenMediaStore ms = (OpenMediaStore)path;
				freeSize = ms.getFile().getFreeSpace();
				diskTotal = ms.getFile().getTotalSpace();
				publishProgress();
			} else if(path instanceof OpenSMB)
			{
				try {
					SmbFile smb = ((OpenSMB)path).getFile();
					freeSize = smb.getDiskFreeSpace();
					String server = smb.getServer();
					if(server == null)
						diskTotal = smb.length();
					else
						diskTotal = new SmbFile((server.startsWith("smb://") ? "" : "smb://") + server + (server.endsWith("/") ? "" : "/")).length();
				} catch (SmbException e) {
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
				publishProgress();
			}
			
			if(path.isDirectory())
				addPath(path, true);
			
			String[] ret = new String[]{
					firstDirs + (dirCount > firstDirs ? " (" + dirCount + ")" : "")
					,firstFiles + (fileCount > firstFiles ? " (" + fileCount + ")" : "")
					,formatSize(totalSize)
					,formatSize(freeSize)
					,formatSize(diskTotal)
				};
			return ret;
		}
		
		public void updateTexts(Object... params)
		{
			for(int i = 0; i < params.length - 1; i += 2)
			{
				if(params[i] != null && params[i] instanceof TextView)
					((TextView)params[i]).setText(params[i+1].toString());
			}
		}
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			updateTexts(mTextFiles, "-", mTextDirs, "-", mTextSize, "-", mTextFree, "-", mTextTotal, "-");
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
			
			if(mTextFree != null && result != null && result.length > 3 && !result[3].equals("0"))
				mTextFree.setText(result[3]);
			else if(mTextFree != null) 
				((View)mTextFree.getParent()).setVisibility(View.GONE);
			
			if(mTextTotal != null && result != null && result.length > 4 && !result[4].equals("0"))
				mTextTotal.setText(result[4]);
			else if(mTextTotal != null)
				((View)mTextTotal.getParent()).setVisibility(View.GONE);
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
				view = inflater.inflate(R.layout.bookmark_layout, parent, false);
				mHolder = new BookmarkHolder(file, name, view, 0);				
				view.setTag(mHolder);
				
			} else {
				mHolder = (BookmarkHolder)view.getTag();
			}
			
			if (file.isDirectory())
				ext = "dir";
			else
				ext = name.substring(name.lastIndexOf(".") + 1);
			
			mHolder.setTitle(name);
			
			ThumbnailCreator.setThumbnail(mHolder.getIconView(), file, 96, 96);
			
			return view;
		}
	}
	


	public static void showFileInfo(final Context mContext, final OpenPath path) {
		new AlertDialog.Builder(mContext)
			.setView(createFileInfoDialog((LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE), path))
			.setTitle(path.getName())
			.setIcon(new BitmapDrawable(path.getThumbnail(ContentFragment.mListImageSize, ContentFragment.mListImageSize).get()))
			.create()
			.show();
		//DialogHandler dialogInfo = DialogHandler.newDialog(DialogHandler.DialogType.FILEINFO_DIALOG, this);
		//dialogInfo.setFilePath(path.getPath());
		//dialogInfo.show(fragmentManager, "info");
	}

	public static void showAboutDialog(final Context mContext)
	{
		LayoutInflater li = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = li.inflate(R.layout.about, null);

		String sVersionInfo = "";
		try {
			PackageInfo pi = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
			sVersionInfo += pi.versionName;
			if(!pi.versionName.contains(""+pi.versionCode))
				sVersionInfo += " (" + pi.versionCode + ")";
			if(OpenExplorer.IS_DEBUG_BUILD)
				sVersionInfo += " *debug*";
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String sBuildTime = "";
		try {
			sBuildTime = SimpleDateFormat.getInstance().format(new Date(
					new ZipFile(mContext.getPackageManager().getApplicationInfo(mContext.getPackageName(), 0).sourceDir)
					.getEntry("classes.dex").getTime()));
		} catch(Exception e) { 
			Logger.LogError("Couldn't get Build Time.", e);
		}
		
		WindowManager wm = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
		DisplayMetrics dm = new DisplayMetrics(); 
		wm.getDefaultDisplay().getMetrics(dm);
		Display d = wm.getDefaultDisplay();
		String sHardwareInfo = "Display:\n";
		sHardwareInfo += "Size: " + d.getWidth() + "x" + d.getHeight() + "\n";
		if(dm!=null)
			sHardwareInfo += "Density: " + dm.density + "\n";
		sHardwareInfo += "Rotation: " + d.getRotation() + "\n\n";
		sHardwareInfo += getNetworkInfo(mContext);
		sHardwareInfo += getDeviceInfo();
		((TextView)view.findViewById(R.id.about_hardware)).setText(sHardwareInfo);
		
		final String sSubject = "Feedback for OpenExplorer " + sVersionInfo; 
		OnClickListener email = new OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_SEND);
				intent.setType("text/plain");
				//intent.addCategory(Intent.CATEGORY_APP_EMAIL);
				intent.putExtra(android.content.Intent.EXTRA_TEXT, "\n" + getDeviceInfo());
				intent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{"brandroid64@gmail.com"});
				intent.putExtra(android.content.Intent.EXTRA_SUBJECT, sSubject);
				mContext.startActivity(Intent.createChooser(intent, mContext.getString(R.string.s_chooser_email)));
			}
		};
		OnClickListener viewsite = new OnClickListener() {
			public void onClick(View v) {
				mContext.startActivity(
					new Intent(android.content.Intent.ACTION_VIEW, Uri.parse("http://brandroid.org/open/"))
					);
			}
		};
		view.findViewById(R.id.about_email).setOnClickListener(email);
		view.findViewById(R.id.about_email_btn).setOnClickListener(email);
		view.findViewById(R.id.about_site).setOnClickListener(viewsite);
		view.findViewById(R.id.about_site_btn).setOnClickListener(viewsite);
		final View mRecentLabel = view.findViewById(R.id.about_recent_status_label);
		final WebView mRecent = (WebView)view.findViewById(R.id.about_recent);
		final OpenChromeClient occ = new OpenChromeClient();
		occ.mStatus = (TextView)view.findViewById(R.id.about_recent_status);
		mRecent.setWebChromeClient(occ);
		mRecent.setWebViewClient(new WebViewClient(){
			@Override
			public void onReceivedError(WebView view, int errorCode,
					String description, String failingUrl) {
				occ.mStatus.setVisibility(View.GONE);
				mRecent.setVisibility(View.GONE);
				mRecentLabel.setVisibility(View.GONE);
			}
		});
		mRecent.setBackgroundColor(Color.TRANSPARENT);
		mRecent.loadUrl("http://brandroid.org/open/?show=recent");
		
		((TextView)view.findViewById(R.id.about_version)).setText(sVersionInfo);
		if(sBuildTime != "")
			((TextView)view.findViewById(R.id.about_buildtime)).setText(sBuildTime);
		else
			((TableRow)view.findViewById(R.id.row_buildtime)).setVisibility(View.GONE);

		final View tab1 = view.findViewById(R.id.tab1);
		final View tab2 = view.findViewById(R.id.tab2);
		((Button)view.findViewById(R.id.btn_recent)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				tab1.setVisibility(View.VISIBLE);
				tab2.setVisibility(View.GONE);
			}
		});
		((Button)view.findViewById(R.id.btn_hardware)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				tab1.setVisibility(View.GONE);
				tab2.setVisibility(View.VISIBLE);
			}
		});
		
		AlertDialog mDlgAbout = new AlertDialog.Builder(mContext)
			.setTitle(R.string.app_name)
			.setView(view)
			.create();
		
		mDlgAbout.getWindow().getAttributes().windowAnimations = R.style.SlideDialogAnimation;
		mDlgAbout.getWindow().getAttributes().alpha = 0.9f;
		
		mDlgAbout.show();
	}
	
	private static String getNetworkInfoInfo(NetworkInfo info)
	{
		String ret = "";
		ret += info.getSubtypeName() + "/ ";
		if(info.getState() != null)
			ret += "s=" + info.getState().name() + "/ ";
		if(info.getDetailedState() != null)
			ret += "d=" + info.getDetailedState().name() + "/ ";
		if(info.getExtraInfo() != null)
			ret += "e=" + info.getExtraInfo();
		return ret;
	}
	
	private static String getReadableIP(int ip)
	{
		int[] bytes = new int[4];
	    bytes[0] = ip & 0xFF;
	    bytes[1] = (ip >> 8) & 0xFF;
	    bytes[2] = (ip >> 16) & 0xFF;
	    bytes[3] = (ip >> 24) & 0xFF;       
	    return bytes[3] + "." + bytes[2] + "." + bytes[1] + "." + bytes[0];   
	}
	private static String getNetworkInterfaces()
	{
		String ret = "IP Address:";
		try {
		for(Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();)
		{
			NetworkInterface ni = en.nextElement();
			for(Enumeration<InetAddress> enumIP = ni.getInetAddresses(); enumIP.hasMoreElements();)
			{
				InetAddress ip = enumIP.nextElement();
				if(!ip.isLoopbackAddress())
					ret += " " + ip.getHostAddress();
			}
		}
		} catch(SocketException e) {
			Logger.LogError("Couldn't get Network Interfaces", e);
		}
		ret += "\n";
		return ret;
	}
	
	public static String getNetworkInfo(Context c)
	{
		String ret = getNetworkInterfaces();
		
		ConnectivityManager conman = (ConnectivityManager)c.getSystemService(Context.CONNECTIVITY_SERVICE);
		if(conman == null)
			ret += "No Connectivity?\n";
		else {
			ret += "Connectivity Info:\n" + conman.toString() + "\n";
			for(NetworkInfo ni : conman.getAllNetworkInfo())
			{
				if(!ni.isAvailable()) continue;
				ret += "Network [" + ni.getTypeName() + "]: " +
						getNetworkInfoInfo(ni) + "\n";
			}
		}
		try {
			WifiManager wifi = (WifiManager) c.getSystemService(Context.WIFI_SERVICE);
			if(wifi == null)
				ret += "No wifi\n";
			else {
				ret += "Wifi Info:\n" + wifi.toString() + "\n";
				ret += "Status: " + (wifi.isWifiEnabled() ? "ENABLED" : "DISABLED") + "\n";
				ret += "ip=" + getReadableIP(wifi.getConnectionInfo().getIpAddress()) + "/ " +
						"mac=" + wifi.getConnectionInfo().getMacAddress() + "/ " +
						"b=" + wifi.getConnectionInfo().getBSSID() + "/ " + 
						"s=" + wifi.getConnectionInfo().getSSID();
				DhcpInfo dh = wifi.getDhcpInfo();
				if(dh == null)
					ret += "No DHCP\n";
				else
				{
					ret += "IP: " + getReadableIP(dh.ipAddress) + "\n";
					ret += "Gateway: " + getReadableIP(dh.gateway) + "\n";
					ret += "DNS: " + getReadableIP(dh.dns1) + " " + getReadableIP(dh.dns2) + "\n";
				}
			}
		} catch(SecurityException sec) {
			ret += "No Wifi permissions.\n";
		}
		return ret;
	}
	
	public static String getDeviceInfo()
	{
		String ret = "";
		String sep = "\n";
		ret += sep + "Build Info:" + sep;
		ret += "SDK: " + Build.VERSION.SDK_INT + sep;
		ret += "Fingerprint: " + Build.FINGERPRINT + sep;
		ret += "Manufacturer: " + Build.MANUFACTURER + sep;
		ret += "Model: " + Build.MODEL + sep;
		ret += "Product: " + Build.PRODUCT + sep;
		ret += "Brand: " + Build.BRAND + sep;
		ret += "Board: " + Build.BOARD + sep;
		ret += "Bootloader: " + Build.BOOTLOADER + sep;
		ret += "Hardware: " + Build.HARDWARE + sep;
		ret += "Display: " + Build.DISPLAY + sep;
		ret += "Language: " + Locale.getDefault().getDisplayLanguage() + sep;
		ret += "Country: " + Locale.getDefault().getDisplayCountry() + sep;
		ret += "Tags: " + Build.TAGS + sep;
		ret += "Type: " + Build.TYPE + sep;
		ret += "User: " + Build.USER + sep;
		if(Build.UNKNOWN != null)
			ret += "Unknown: " + Build.UNKNOWN + sep;
		ret += "ID: " + Build.ID;
		return ret;
	}


	public static String formatDuration(long ms) {
		int s = (int) (ms / 1000),
			m = s / 60,
			h = m / 60;
		m = m % 60;
		s = s % 60;
		return (ms > 360000 ? h + ":" : "") +
				(ms > 6000 ? (h == 0 || m >= 10 ? "" : "0") + m + ":" : "") +
				(ms > 6000 ? (s >= 10 ? "" : "0") + s : 
					(ms < 1000 ? ms + "ms" : s + "s"));
	}
}
