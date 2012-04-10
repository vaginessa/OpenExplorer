package org.brandroid.openmanager.fragments;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.brandroid.openmanager.R;
import org.brandroid.openmanager.activities.OpenExplorer;
import org.brandroid.openmanager.adapters.FileSystemAdapter;
import org.brandroid.openmanager.adapters.IconContextMenu;
import org.brandroid.openmanager.adapters.IconContextMenu.IconContextItemSelectedListener;
import org.brandroid.openmanager.data.OpenClipboard;
import org.brandroid.openmanager.data.OpenCursor;
import org.brandroid.openmanager.data.OpenFile;
import org.brandroid.openmanager.data.OpenPath;
import org.brandroid.openmanager.util.ActionModeHelper;
import org.brandroid.openmanager.util.EventHandler;
import org.brandroid.openmanager.util.FileManager;
import org.brandroid.openmanager.util.InputDialog;
import org.brandroid.openmanager.util.IntentManager;
import org.brandroid.utils.Logger;
import org.brandroid.utils.MenuBuilder;
import org.brandroid.utils.Preferences;

import com.jcraft.jsch.UserInfo;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;

public abstract class OpenFragment
			extends Fragment
			implements View.OnClickListener, View.OnLongClickListener
{
	//public static boolean CONTENT_FRAGMENT_FREE = true;
	//public boolean isFragmentValid = true;
	protected OpenPath mPath;
	protected boolean mActionModeSelected = false;
	protected Object mActionMode = null;
	protected BaseAdapter mContentAdapter;
	protected int mMenuContextItemIndex = -1;

	//@Override
	public void onItemClick(AdapterView<?> list, View view, int pos, long id) {
		final OpenPath file = (OpenPath)list.getItemAtPosition(pos);
		
		Logger.LogDebug("File clicked: " + file.getPath());
		
		if(getClipboard().isMultiselect()) {
			if(getClipboard().contains(file))
			{
				getClipboard().remove(file);
				if(getClipboard().size() == 0)
					getClipboard().stopMultiselect();
				((BaseAdapter)list.getAdapter()).notifyDataSetChanged();
			} else {
				addToMultiSelect(file);
				((TextView)view.findViewById(R.id.content_text)).setTextAppearance(list.getContext(), R.style.Highlight);
			}
			return;
		}
		
		if(file.isDirectory() && !mActionModeSelected ) {
			/* if (mThumbnail != null) {
				mThumbnail.setCancelThumbnails(true);
				mThumbnail = null;
			} */
			
			
			//setContentPath(file, true);
			getExplorer().onChangeLocation(file);

		} else if (!file.isDirectory() && !mActionModeSelected) {
			
			if(file.requiresThread() && FileManager.hasOpenCache(file.getAbsolutePath()))
			{
				//getExplorer().showToast("Still need to handle this.");
				if(file.isTextFile())
					getExplorer().editFile(file);
				else {
					showCopyFromNetworkDialog(file);
					//getEventHandler().copyFile(file, mPath, mContext);
				}
				return;
			} else if(file.isTextFile() && Preferences.Pref_Text_Internal)
				getExplorer().editFile(file);
			else
				IntentManager.startIntent(file, getExplorer(), Preferences.Pref_Intents_Internal);
		}
	}
	
	public boolean onItemLongClick(AdapterView<?> list, final View view ,int pos, long id) {
		mMenuContextItemIndex = pos;
		//view.setBackgroundResource(R.drawable.selector_blue);
		//list.setSelection(pos);
		//if(list.showContextMenu()) return true;
		
		final OpenPath file = (OpenPath)((BaseAdapter)list.getAdapter()).getItem(pos);
		final String name = file.getName();
		
		if(!OpenExplorer.USE_PRETTY_CONTEXT_MENUS)
		{
			return list.showContextMenu();
		} else if(OpenExplorer.BEFORE_HONEYCOMB || !OpenExplorer.USE_ACTIONMODE) {
			
			try {
				//View anchor = view; //view.findViewById(R.id.content_context_helper);
				//if(anchor == null) anchor = view;
				//view.measure(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				//Rect r = new Rect(view.getLeft(),view.getTop(),view.getMeasuredWidth(),view.getMeasuredHeight());
				MenuBuilder cmm = IconContextMenu.newMenu(list.getContext(), R.menu.context_file);
				//if(!file.isArchive()) hideItem(cmm, R.id.menu_context_unzip);
				if(getClipboard().size() > 0)
					OpenExplorer.setMenuVisible(cmm, false, R.id.menu_multi);
				else
					OpenExplorer.setMenuVisible(cmm, false, R.id.menu_context_paste);
				OpenExplorer.setMenuEnabled(cmm, !file.isDirectory(), R.id.menu_context_edit, R.id.menu_context_view);
				final IconContextMenu cm = new IconContextMenu(
						list.getContext(), cmm, view, null, null);
				//cm.setAnchor(anchor);
				cm.setTitle(name);
				cm.setOnDismissListener(new android.widget.PopupWindow.OnDismissListener() {
					public void onDismiss() {
						//view.refreshDrawableState();
					}
				});
				cm.setOnIconContextItemSelectedListener(new IconContextItemSelectedListener() {	
					public void onIconContextItemSelected(MenuItem item, Object info, View view) {
						OpenPath path = null;
						if(mContentAdapter instanceof FileSystemAdapter)
							path = ((FileSystemAdapter)mContentAdapter).getItem((Integer)info);
						executeMenu(item.getItemId(), path);
						cm.dismiss();
					}
				});
				cm.setInfo(pos);
				cm.setTextLayout(R.layout.context_item);
				if(!cm.show()) //r.left, r.top);
					return list.showContextMenu();
				else return true;
			} catch(Exception e) {
				Logger.LogWarning("Couldn't show Iconified menu.", e);
				return list.showContextMenu();
			}
		}
		
		if(!OpenExplorer.BEFORE_HONEYCOMB&&OpenExplorer.USE_ACTIONMODE)
		{
			if(!file.isDirectory() && mActionMode == null && !getClipboard().isMultiselect()) {
				try {
					Method mStarter = getActivity().getClass().getMethod("startActionMode");
					mActionMode = mStarter.invoke(getActivity(),
							new ActionModeHelper.Callback() {
						//@Override
						public boolean onPrepareActionMode(android.view.ActionMode mode, Menu menu) {
							return false;
						}
						
						//@Override
						public void onDestroyActionMode(android.view.ActionMode mode) {
							mActionMode = null;
							mActionModeSelected = false;
						}
						
						//@Override
						public boolean onCreateActionMode(android.view.ActionMode mode, Menu menu) {
							mode.getMenuInflater().inflate(R.menu.context_file, menu);
				    		
				    		mActionModeSelected = true;
							return true;
						}

						//@Override
						public boolean onActionItemClicked(android.view.ActionMode mode, MenuItem item) {
							//ArrayList<OpenPath> files = new ArrayList<OpenPath>();
							
							//OpenPath file = mLastPath.getChild(mode.getTitle().toString());
							//files.add(file);
							
							if(item.getItemId() != R.id.menu_context_cut && item.getItemId() != R.id.menu_multi && item.getItemId() != R.id.menu_context_copy)
							{
								mode.finish();
								mActionModeSelected = false;
							}
							return executeMenu(item.getItemId(), mode, file);
						}
					});
					((android.view.ActionMode)mActionMode).setTitle(file.getName());
				} catch (NoSuchMethodException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return true;

		}
		
		if(file.isDirectory() && mActionMode == null && !getClipboard().isMultiselect()) {
			if(!OpenExplorer.BEFORE_HONEYCOMB && OpenExplorer.USE_ACTIONMODE)
			mActionMode = getActivity().startActionMode(new android.view.ActionMode.Callback() {
				
				//@Override
				public boolean onPrepareActionMode(android.view.ActionMode mode, Menu menu) {
					return false;
				}
				
				//@Override
				public void onDestroyActionMode(android.view.ActionMode mode) {
					mActionMode = null;
					mActionModeSelected = false;
				}
				
				//@Override
				public boolean onCreateActionMode(android.view.ActionMode mode, Menu menu) {
					mode.getMenuInflater().inflate(R.menu.context_file, menu);
					menu.findItem(R.id.menu_context_paste).setEnabled(getClipboard().size() > 0);
					//menu.findItem(R.id.menu_context_unzip).setEnabled(mHoldingZip);
		        	
		        	mActionModeSelected = true;
					
		        	return true;
				}
				
				//@Override
				public boolean onActionItemClicked(android.view.ActionMode mode, MenuItem item) {
					return executeMenu(item.getItemId(), mode, file);
				}
			});
			((android.view.ActionMode)mActionMode).setTitle(file.getName());
			
			return true;
		}
		
		return false;
	}

	protected String getViewSetting(OpenPath path, String key, String def)
	{
		return getExplorer().getPreferences().getSetting("views", key + "_" + path.getPath(), def);
	}
	protected Boolean getViewSetting(OpenPath path, String key, Boolean def)
	{
		if(getExplorer() != null && getExplorer().getPreferences() != null)
			return getExplorer().getPreferences().getSetting("views", key + "_" + path.getPath(), def);
		return def;
	}
	protected Integer getViewSetting(OpenPath path, String key, Integer def)
	{
		return getExplorer().getPreferences().getSetting("views", key + "_" + path.getPath(), def);
	}
	protected void setViewSetting(OpenPath path, String key, String value)
	{
		getExplorer().getPreferences().setSetting("views", key + "_" + path.getPath(), value);
	}
	protected void setViewSetting(OpenPath path, String key, Boolean value)
	{
		getExplorer().getPreferences().setSetting("views", key + "_" + path.getPath(), value);
	}
	protected void setViewSetting(OpenPath path, String key, Integer value)
	{
		if(path != null && path.getPath() != null && getExplorer() != null && getExplorer().getPreferences() != null)
			getExplorer().getPreferences().setSetting("views", key + "_" + path.getPath(), value);
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		if(!OpenExplorer.BEFORE_HONEYCOMB && OpenExplorer.USE_ACTIONMODE) return;
		AdapterContextMenuInfo info = (AdapterContextMenuInfo)menuInfo;
		OpenPath file = null;
		if(mContentAdapter instanceof FileSystemAdapter)
			file = ((FileSystemAdapter)mContentAdapter).getItem(info != null ? info.position : mMenuContextItemIndex);
		else return;
		new MenuInflater(v.getContext()).inflate(R.menu.context_file, menu);
		OpenExplorer.setMenuEnabled(menu, !file.isDirectory(), R.id.menu_context_edit, R.id.menu_context_view);
		OpenExplorer.setMenuVisible(menu, getClipboard().size() > 0, R.id.menu_context_paste);
		//menu.findItem(R.id.menu_context_unzip).setVisible(file.isArchive());
		if(!mPath.isFile() || !IntentManager.isIntentAvailable(mPath, getExplorer()))
			OpenExplorer.setMenuVisible(menu, false, R.id.menu_context_edit, R.id.menu_context_view);
	}
	

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		OpenPath path = null;
		//Object o = mGrid.getSelectedItem();
		//if(o != null && o instanceof OpenPath)
		//	path = (OpenPath)o;
		//else
		if(mMenuContextItemIndex > -1 && mContentAdapter instanceof FileSystemAdapter)
			path = ((FileSystemAdapter)mContentAdapter).getItem(mMenuContextItemIndex);
		else return false;
		Logger.LogDebug("Showing context for " + path.getName() + "?");
		return executeMenu(item.getItemId(), path);
		//return super.onContextItemSelected(item);
	}
	

	public boolean executeMenu(final int id, OpenPath file)
	{
		return executeMenu(id, null, file);
	}
	public boolean executeMenu(final int id, Object mode, OpenPath file)
	{
		ArrayList<OpenPath> files = new ArrayList<OpenPath>();
		files.add(file);
		return executeMenu(id, mode, file, getClipboard());
	}
	public boolean executeMenu(final int id, final Object mode, final OpenPath file, List<OpenPath> fileList)
	{
		final String path = file != null ? file.getPath() : null;
		OpenPath parent = file != null ? file.getParent() : null;
		if(parent == null || parent instanceof OpenCursor)
			parent = OpenFile.getExternalMemoryDrive(true);
		final OpenPath folder = parent;
		String name = file != null ? file.getName() : null;
		if(fileList == null)
			fileList = getClipboard();
		final OpenPath[] fileArray = fileList.toArray(new OpenPath[fileList.size()]);
		
		onClick(id);
		
		switch(id)
		{
			case R.id.menu_context_view:
				Intent vintent = IntentManager.getIntent(file, getExplorer(), Intent.ACTION_VIEW);
				if(vintent != null)
					getActivity().startActivity(vintent);
				else {
					getExplorer().showToast(R.string.s_error_no_intents);
					if(file.length() < getResources().getInteger(R.integer.max_text_editor_size))
						getExplorer().editFile(file);
				}
				break;
			case R.id.menu_context_edit:
				Intent intent = IntentManager.getIntent(file, getExplorer(), Intent.ACTION_EDIT);
				if(intent != null)
				{
					if(intent.getPackage() != null && intent.getPackage().equals(getActivity().getPackageName()))
						getExplorer().editFile(file);
					else
						try {
							intent.setAction(Intent.ACTION_EDIT);
							Logger.LogVerbose("Starting Intent: " + intent.toString());
							getExplorer().startActivity(intent);
						} catch(ActivityNotFoundException e) {
							getExplorer().showToast(R.string.s_error_no_intents);
							getExplorer().editFile(file);
						}
				} else if(file.length() < getResources().getInteger(R.integer.max_text_editor_size)) {
					getExplorer().editFile(file);
				} else {
					getExplorer().showToast(R.string.s_error_no_intents);
				}
				break;

			case R.id.menu_multi:
				changeMultiSelectState(!getClipboard().isMultiselect());
				getClipboard().add(file);
				return true;
			case R.id.menu_context_bookmark:
				getExplorer().addBookmark(file);
				finishMode(mode);
				return true;
				
			case R.id.menu_context_delete:
				fileList.add(file);
				getHandler().deleteFile(fileList, getActivity(), true);
				finishMode(mode);
				mContentAdapter.notifyDataSetChanged();
				return true;
				
			case R.id.menu_context_rename:
				getHandler().renameFile(file.getPath(), true, getActivity());
				finishMode(mode);
				return true;
				
			case R.id.menu_context_copy:
			case R.id.menu_context_cut:
				if(id == R.id.menu_context_cut)
					getClipboard().DeleteSource = true;
				else
					getClipboard().DeleteSource = false;

				getClipboard().add(file);
				return false;

			case R.id.menu_context_paste:
			case R.id.menu_paste:
				OpenPath into = file;
				if(!file.isDirectory())
				{
					Logger.LogWarning("Can't paste into file (" + file.getPath() + "). Using parent directory (" + folder.getPath() + ")");
					into = folder;
				}
				OpenClipboard cb = getClipboard();
				if(cb.size() > 0)
					if(cb.DeleteSource)
						getHandler().cutFile(cb, into, getActivity());
					else
						getHandler().copyFile(cb, into, getActivity());
				
				cb.DeleteSource = false;
				if(cb.ClearAfter)
					getClipboard().clear();
				getExplorer().updateTitle(path);
				finishMode(mode);
				return true;
				
			case R.id.menu_context_zip:
				getClipboard().add(file);
				getClipboard().ClearAfter = true;
				String zname = file.getName().replace("." + file.getExtension(), "") + ".zip";
				if(getClipboard().size() > 1)
				{
					OpenPath last = getClipboard().get(getClipboard().getCount() - 1);
					if(last != null && last.getParent() != null)
					{
						if(last.getParent() instanceof OpenCursor)
							zname = folder.getPath();
						zname = last.getParent().getName() + ".zip";
					}
				}
				final String def = zname;
				
				final InputDialog dZip = new InputDialog(getExplorer())
					.setIcon(R.drawable.sm_zip)
					.setTitle(R.string.s_menu_zip)
					.setMessageTop(R.string.s_prompt_path)
					.setDefaultTop(folder.getPath())
					.setMessage(R.string.s_prompt_zip)
					.setCancelable(true)
					.setNegativeButton(android.R.string.no, null);
				dZip
					.setPositiveButton(android.R.string.ok,
						new OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								OpenPath zFolder = new OpenFile(dZip.getInputTopText());
								if(zFolder == null || !zFolder.exists())
									zFolder = folder;
								OpenPath zipFile = zFolder.getChild(dZip.getInputText());
								Logger.LogVerbose("Zipping " + getClipboard().size() + " items to " + zipFile.getPath());
								getHandler().zipFile(zipFile, getClipboard(), getExplorer());
								finishMode(mode);
							}
						})
					.setDefaultText(def);
				dZip.create().show();
				return true;
				
			//case R.id.menu_context_unzip:
			//	getHandler().unzipFile(file, getExplorer());
			//	return true;
			
			case R.id.menu_context_info:
				getExplorer().showFileInfo(file);
				finishMode(mode);
				return true;
				
			case R.id.menu_context_share:
				
				// TODO: WTF is this?
				Intent mail = new Intent();
				mail.setType("application/mail");
				
				mail.setAction(android.content.Intent.ACTION_SEND);
				mail.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(path)));
				startActivity(mail);
				
				//mode.finish();
				return true;
	
	//			this is for bluetooth
	//			files.add(path);
	//			getHandler().sendFile(files);
	//			mode.finish();
	//			return true;
			}
		return true;
	}
	
	public void changeMultiSelectState(boolean multiSelectOn) {
		if(multiSelectOn)
			getClipboard().startMultiselect();
		else
			getClipboard().stopMultiselect();
		//mMultiSelectDrawer.setVisibility(multiSelectOn ? View.VISIBLE : View.GONE);
	}


	
	private void finishMode(Object mode)
	{
		getClipboard().clear();
		if(!OpenExplorer.BEFORE_HONEYCOMB && mode != null && mode instanceof android.view.ActionMode)
			((android.view.ActionMode)mode).finish();
	}
	

	private void addToMultiSelect(final OpenPath file)
	{
		getClipboard().add(file);
	}

	
	private void showCopyFromNetworkDialog(OpenPath source)
	{
		/// TODO Implement Copy From Network
		getExplorer().showToast("Not yet implemented (" + source.getMimeType() + ")");
		return;
		/*
		final View view = FolderPickerActivity.createPickerView(mContext);
		new DialogBuilder(mContext)
			.setTitle("Choose a folder to copy " + source.getName() + " into:")
			.setView(view)
			.setPositiveButton(android.R.string.ok, new OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					
				}
			});
			*/
	}
	
	public String getClassName()
	{
		return this.getClass().getSimpleName();
	}
	
	public OpenPath getPath() { return mPath; }
	
	public static void setAlpha(float alpha, View... views)
	{
		for(View kid : views)
			setAlpha(kid, alpha);
	}
	public static void setAlpha(View v, float alpha)
	{
		if(v == null) return;
		if(!OpenExplorer.BEFORE_HONEYCOMB)
			v.setAlpha(alpha);
		else if(v instanceof ImageView)
			((ImageView)v).setAlpha((int)(255 * alpha));
		else if(v instanceof TextView)
			((TextView)v).setTextColor(((TextView)v).getTextColors().withAlpha((int)(255 * alpha)));
	}
	public static void setAlpha(float alpha, View root, int... ids)
	{
		for(int id : ids)
			setAlpha(root.findViewById(id), alpha);
	}

	
	protected EventHandler getHandler()
	{
		return OpenExplorer.getEventHandler();
	}
	
	protected FileManager getManager()
	{
		return OpenExplorer.getFileManager();
	}
	
	public OpenExplorer getExplorer() { return (OpenExplorer)getActivity(); }
	public static EventHandler getEventHandler() { return OpenExplorer.getEventHandler(); }
	public static FileManager getFileManager() { return OpenExplorer.getFileManager(); }
	protected OpenClipboard getClipboard() {
		return OpenExplorer.getClipboard();
	}
	
	public void onClick(View v) {
		Logger.LogDebug("View onClick(" + v.getId() + ") - " + v.toString());
	}
	
	public void onClick(int id) {
		Logger.LogDebug("View onClick(" + id + ") / " + getClassName());
	}
	
	public boolean onLongClick(View v) {
		Logger.LogDebug("View onLongClick(" + v.getId() + ") - " + v.toString());
		return false;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Logger.LogDebug("<-- onCreate - " + getClassName());
		//CONTENT_FRAGMENT_FREE = false;
		super.onCreate(savedInstanceState);
	}

	public abstract CharSequence getTitle();
	
	/*
	 * 
	@Override
	public void onDestroy() {
		Logger.LogDebug("--> onDestroy - " + getClassName());
		super.onDestroy();
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		Logger.LogDebug("<-- onActivityCreated - " + getClassName());
		super.onActivityCreated(savedInstanceState);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		Logger.LogDebug("<-- onCreateView - " + getClassName());
		return super.onCreateView(inflater, container, savedInstanceState);
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		Logger.LogDebug("<-- onViewCreated - " + getClassName());
		super.onViewCreated(view, savedInstanceState);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		Logger.LogDebug("<-onCreateView - " + getClassName());
		//CONTENT_FRAGMENT_FREE = false;
		return super.onCreateView(inflater, container, savedInstanceState);
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		Logger.LogDebug("<-onViewCreated - " + getClassName());
	}
	
	@Override
	public void onPause() {
		super.onPause();
		Logger.LogDebug("->onPause - " + getClassName());
	}
	
	@Override
	public void onResume() {
		super.onResume();
		Logger.LogDebug("<-onResume - " + getClassName());
	}
	
	@Override
	public void onStart() {
		super.onStart();
		Logger.LogDebug("<-onStart - " + getClassName());
	}
	
	@Override
	public void onStop() {
		super.onStop();
		Logger.LogDebug("->onStop - " + getClassName());
		//CONTENT_FRAGMENT_FREE = true;
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		Logger.LogDebug("->onSaveInstanceState - " + getClassName());
	}
	*/
}
