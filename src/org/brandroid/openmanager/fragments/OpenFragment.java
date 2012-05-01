package org.brandroid.openmanager.fragments;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.brandroid.openmanager.R;
import org.brandroid.openmanager.activities.OpenExplorer;
import org.brandroid.openmanager.activities.OpenFragmentActivity;
import org.brandroid.openmanager.adapters.ContentAdapter;
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
import org.brandroid.utils.MenuUtils;
import org.brandroid.utils.Preferences;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;

public abstract class OpenFragment
			extends Fragment
			implements View.OnClickListener, View.OnLongClickListener
				, Comparable<OpenFragment>
				, Comparator<OpenFragment>
{
	//public static boolean CONTENT_FRAGMENT_FREE = true;
	//public boolean isFragmentValid = true;
	protected boolean mActionModeSelected = false;
	protected Object mActionMode = null;
	protected BaseAdapter mContentAdapter;
	protected int mMenuContextItemIndex = -1;
	private boolean mHasOptions = false;
	
	public interface OnFragmentTitleLongClickListener
	{
		public boolean onTitleLongClick(View titleView);
	}
	
	@Override
	public int compareTo(OpenFragment b) {
		return compare(this, b);
	}
	
	@Override
	public int compare(OpenFragment a, OpenFragment b) {
		Logger.LogDebug("Comparing " + a.getTitle() + " to " + b.getTitle());
		if(b instanceof ContentFragment && !(a instanceof ContentFragment)) return 1;
		if(a instanceof ContentFragment && !(b instanceof ContentFragment)) return -1;
		if(!(a instanceof ContentFragment) || !(b instanceof ContentFragment)) return 0;
		OpenPath pa = ((ContentFragment)a).getPath();
		OpenPath pb = ((ContentFragment)b).getPath();
		return pa.getPath().compareTo(pb.getPath());
	}

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
				//Animation anim = Animation.
				/*
				Drawable dIcon = ((ImageView)view.findViewById(R.id.content_icon)).getDrawable();
				if(dIcon instanceof BitmapDrawable)
				{
					IconAnimationPanel panel = new IconAnimationPanel(getExplorer())
						.setIcon(((BitmapDrawable)dIcon).getBitmap())
						.setStart(new Point(view.getLeft(), view.getRight()))
						.setEnd(new Point(getActivity().getWindow().getWindowManager().getDefaultDisplay().getWidth() / 2, getActivity().getWindowManager().getDefaultDisplay().getHeight()))
						.setDuration(500);
					((ViewGroup)getView()).addView(panel);
				}
				*/
				
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
			else if(!IntentManager.startIntent(file, getExplorer(), Preferences.Pref_Intents_Internal))
				getExplorer().editFile(file);
		}
	}
	
	public boolean showMenu(int menuId, View from)
	{
		Logger.LogDebug("Showing menu 0x" + Integer.toHexString(menuId) + (from != null ? " near 0x" + Integer.toHexString(from.getId()) : " by itself"));
		if(getActivity() == null) return false;
		MenuBuilder menu = IconContextMenu.newMenu(getActivity(), menuId);
		if(menu == null) return false;
		onPrepareOptionsMenu(menu);
		IconContextMenu mOpenMenu = new IconContextMenu(getActivity(), menu, from, null, null);
		mOpenMenu.setMenu(menu);
		mOpenMenu.setAnchor(from);
		mOpenMenu.setNumColumns(1);
		mOpenMenu.setOnIconContextItemSelectedListener(new IconContextItemSelectedListener() {
			public void onIconContextItemSelected(MenuItem item, Object info, View view) {
				//showToast(item.getTitle().toString());
				if(item.getItemId() == R.id.menu_sort)
					showMenu(R.menu.menu_sort, view);
				else if(item.getItemId() == R.id.menu_view)
					showMenu(R.menu.menu_view, view);
				else
					onClick(item.getItemId());
				//mOpenMenu.dismiss();
				//mMenuPopup.dismiss();
			}
		});
		return true;
	}
	
	@Override
	public void setHasOptionsMenu(boolean hasMenu) {
		if(!OpenExplorer.BEFORE_HONEYCOMB)
			super.setHasOptionsMenu(hasMenu);
		mHasOptions = hasMenu;
	}
	public boolean hasOptionsMenu()
	{
		return mHasOptions;
	}
	
	public boolean onBackPressed() {
		if(getExplorer() != null)
		{
			try {
				getExplorer().removeFragment(this);
				return true;
			} catch(Exception e) { }
		}
		return false;
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
					MenuUtils.setMenuVisible(cmm, false, R.id.menu_multi);
				else
					MenuUtils.setMenuVisible(cmm, false, R.id.menu_context_paste);
				MenuUtils.setMenuEnabled(cmm, !file.isDirectory(), R.id.menu_context_edit, R.id.menu_context_view);
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
						if(mContentAdapter instanceof ContentAdapter)
							path = ((ContentAdapter)mContentAdapter).getItem((Integer)info);
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
		if(getExplorer() != null && getExplorer().getPreferences() != null && path != null && path.getPath() != null)
			return getExplorer().getPreferences().getSetting("views", key + "_" + path.getPath(), def);
		return def;
	}
	protected Boolean getViewSetting(OpenPath path, String key, Boolean def)
	{
		if(getExplorer() != null && getExplorer().getPreferences() != null && path != null && path.getPath() != null)
			return getExplorer().getPreferences().getSetting("views", key + "_" + path.getPath(), def);
		return def;
	}
	protected Integer getViewSetting(OpenPath path, String key, Integer def)
	{
		if(getExplorer() != null && getExplorer().getPreferences() != null && path != null && path.getPath() != null)
			return getExplorer().getPreferences().getSetting("views", key + "_" + path.getPath(), def);
		return def;
	}
	protected void setViewSetting(OpenPath path, String key, String value)
	{
		if(getExplorer() != null && getExplorer().getPreferences() != null && path != null && path.getPath() != null)
			getExplorer().getPreferences().setSetting("views", key + "_" + path.getPath(), value);
	}
	protected void setViewSetting(OpenPath path, String key, Boolean value)
	{
		if(getExplorer() != null && getExplorer().getPreferences() != null && path != null && path.getPath() != null)
			getExplorer().getPreferences().setSetting("views", key + "_" + path.getPath(), value);
	}
	protected void setViewSetting(OpenPath path, String key, Integer value)
	{
		if(path != null && path.getPath() != null && getExplorer() != null && getExplorer().getPreferences() != null)
			getExplorer().getPreferences().setSetting("views", key + "_" + path.getPath(), value);
	}
	protected Integer getSetting(OpenPath file, String key, Integer defValue)
	{
		if(getActivity() == null) return defValue; 
		return getFragmentActivity().getSetting(file, key, defValue);
	}
	protected String getSetting(OpenPath file, String key, String defValue)
	{
		if(getActivity() == null) return defValue;
		return getFragmentActivity().getSetting(file, key, defValue);
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		Logger.LogDebug("}-- onAttach :: " + getClassName() + (this instanceof OpenPathFragmentInterface && ((OpenPathFragmentInterface)this).getPath() != null ? " @ " + ((OpenPathFragmentInterface)this).getPath().getPath() : ""));
	}
	
	@Override
	public void onDetach() {
		super.onDetach();
		Logger.LogDebug("{-- onDetach :: " + getClassName() + (this instanceof OpenPathFragmentInterface && ((OpenPathFragmentInterface)this).getPath() != null ? " @ " + ((OpenPathFragmentInterface)this).getPath().getPath() : ""));
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		if(!isVisible()) return;
		super.onCreateContextMenu(menu, v, menuInfo);
		//Logger.LogDebug("OpenFragment.onCreateContextMenu");
		if(!OpenExplorer.BEFORE_HONEYCOMB && OpenExplorer.USE_ACTIONMODE) return;
		AdapterContextMenuInfo info = (AdapterContextMenuInfo)menuInfo;
		OpenPath file = null;
		if(mContentAdapter instanceof ContentAdapter)
			file = ((ContentAdapter)mContentAdapter).getItem(info != null ? info.position : mMenuContextItemIndex);
		else return;
		new MenuInflater(v.getContext()).inflate(R.menu.context_file, menu);
		MenuUtils.setMenuEnabled(menu, !file.isDirectory(), R.id.menu_context_edit, R.id.menu_context_view);
		MenuUtils.setMenuVisible(menu, getClipboard().size() > 0, R.id.menu_context_paste);
		//menu.findItem(R.id.menu_context_unzip).setVisible(file.isArchive());
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		Logger.LogDebug(getClassName() + ".onCreateOptionsMenu");
		super.onCreateOptionsMenu(menu, inflater);
	}
	

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		OpenPath path = null;
		//Object o = mGrid.getSelectedItem();
		//if(o != null && o instanceof OpenPath)
		//	path = (OpenPath)o;
		//else
		if(mMenuContextItemIndex > -1 && mContentAdapter instanceof ContentAdapter)
			path = ((ContentAdapter)mContentAdapter).getItem(mMenuContextItemIndex);
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
					if(getExplorer() != null)
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
				//fileList.add(file);
				getHandler().deleteFile(file, getActivity(), true);
				finishMode(mode);
				mContentAdapter.notifyDataSetChanged();
				return true;
				
			case R.id.menu_context_rename:
				getHandler().renameFile(file, true, getActivity());
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
				DialogHandler.showFileInfo(getExplorer(), file);
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
	
	public Drawable getDrawable(int resId)
	{
		if(getActivity() == null) return null;
		if(getResources() == null) return null;
		return getResources().getDrawable(resId);
	}
	public OpenFragmentActivity getFragmentActivity() { return (OpenFragmentActivity)getActivity(); }
	public OpenExplorer getExplorer() { return (OpenExplorer)getActivity(); }
	public Context getApplicationContext() { return getActivity().getApplicationContext(); }
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
		Logger.LogDebug("<-- onCreate - " + getClassName() + (this instanceof OpenPathFragmentInterface && ((OpenPathFragmentInterface)this).getPath() != null ? " @ " + ((OpenPathFragmentInterface)this).getPath().getPath() : ""));
		//CONTENT_FRAGMENT_FREE = false;
		super.onCreate(savedInstanceState);
	}

	public abstract Drawable getIcon();
	public abstract CharSequence getTitle();
	
	public void notifyPager()
	{
		if(getExplorer() != null)
			getExplorer().notifyPager();
	}
	
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
