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
import org.brandroid.openmanager.adapters.OpenClipboard;
import org.brandroid.openmanager.adapters.ContentAdapter.CheckClipboardListener;
import org.brandroid.openmanager.adapters.IconContextMenu.IconContextItemSelectedListener;
import org.brandroid.openmanager.data.OpenContent;
import org.brandroid.openmanager.data.OpenCursor;
import org.brandroid.openmanager.data.OpenFile;
import org.brandroid.openmanager.data.OpenPath;
import org.brandroid.openmanager.data.OpenZip;
import org.brandroid.openmanager.interfaces.OpenApp;
import org.brandroid.openmanager.util.ActionModeHelper;
import org.brandroid.openmanager.util.BetterPopupWindow;
import org.brandroid.openmanager.util.EventHandler;
import org.brandroid.openmanager.util.FileManager;
import org.brandroid.openmanager.util.InputDialog;
import org.brandroid.openmanager.util.IntentManager;
import org.brandroid.openmanager.util.ShellSession;
import org.brandroid.utils.DiskLruCache;
import org.brandroid.utils.Logger;
import org.brandroid.utils.MenuBuilder;
import org.brandroid.utils.MenuUtils;
import org.brandroid.utils.Preferences;

import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.DownloadCache;
import com.android.gallery3d.data.ImageCacheService;
import com.android.gallery3d.util.ThreadPool;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v4.app.Fragment.InstantiationException;
import android.support.v4.util.LruCache;
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

/*
 * Base class for all OpenExplorer fragments. Provides convenient methods to access
 * other sections of the application.
 */
public abstract class OpenFragment
			extends Fragment
			implements View.OnClickListener, View.OnLongClickListener
				, Comparator<OpenFragment>
				, Comparable<OpenFragment>
				, OpenApp, CheckClipboardListener
{
	//public static boolean CONTENT_FRAGMENT_FREE = true;
	//public boolean isFragmentValid = true;
	protected boolean mActionModeSelected = false;
	protected Object mActionMode = null;
	protected int mMenuContextItemIndex = -1;
	private boolean mHasOptions = false;
	protected boolean DEBUG = OpenExplorer.IS_DEBUG_BUILD && false;
	
	public interface OnFragmentTitleLongClickListener
	{
		public boolean onTitleLongClick(View titleView);
	}
	
	public interface Poppable
	{
		public void setupPopup(Context c, View anchor);
		public BetterPopupWindow getPopup();
	}
	
	public class OpenContextMenuInfo implements ContextMenuInfo
	{
		private final OpenPath file;
		public OpenContextMenuInfo(OpenPath path) { file = path; }
		public OpenPath getPath() { return file; }
	}
	
	public static OpenFragment instantiate(Context context, String fname, Bundle args) {
        String sPath = null;
    	if(args.containsKey("last"))
    		sPath = args.getString("last");
    	if(args.containsKey("edit_path"))
    		sPath = args.getString("edit_path");
    	OpenPath path = FileManager.getOpenCache(sPath, context);
    	if(sPath != null)
    	{
        	if(fname.endsWith("ContentFragment"))
        		return ContentFragment.getInstance(path, args);
        	else if(fname.endsWith("TextEditorFragment"))
        		return TextEditorFragment.getInstance(path, args);
        	else if(fname.endsWith("CarouselFragment"))
        		return CarouselFragment.getInstance(args);
    	}
        return null;
    }
	
	public int compareTo(OpenFragment b) {
		return compare(this, b);
	}
	
	@Override
	public int compare(OpenFragment a, OpenFragment b) {
		int priA = a.getPagerPriority();
		int priB = b.getPagerPriority();
		if(DEBUG)
			Logger.LogDebug("Comparing " + a.getTitle() + "(" + priA + ") to " + b.getTitle() + "(" + priB + ")");
		if(priA != priB)
		{
			if(priA > priB)
			{
				//Logger.LogDebug("Switch!");
				return 1;
			} else {
				//Logger.LogDebug("Stay!");
				return -1;
			}
		}
		if(a instanceof ContentFragment && b instanceof ContentFragment)
		{
			OpenPath pa = ((ContentFragment)a).getPath();
			OpenPath pb = ((ContentFragment)b).getPath();
			priA = pa.getPath().length();
			priB = pb.getPath().length();
			if(priA > priB)
			{
				//Logger.LogDebug("Switch 2!");
				return 1;
			} else {
				//Logger.LogDebug("Stay 2!");
				return -1;
			}
		} else {
			//Logger.LogDebug("0!");
			return 0;
		}
	}
	
	/*
	 * Return priority for ordering in ViewPager (Low to High)
	 */
	public int getPagerPriority() { return 5; }
	
	@Override
	public void startActivityForResult(Intent intent, int requestCode) {
		super.startActivityForResult(intent, requestCode);
		if(DEBUG)
			Logger.LogDebug(getClassName() + ".startActivityForResult(" + requestCode + "," + (intent != null ? intent.toString() : "null") + ")");
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(DEBUG)
			Logger.LogDebug(getClassName() + ".onActivityResult(" + requestCode + "," + resultCode + "," + (data != null ? data.toString() : "null") + ")");
	}

	//@Override
	public void onItemClick(AdapterView<?> list, View view, int pos, long id) {
		OpenPath file = (OpenPath)list.getItemAtPosition(pos);
		
		Logger.LogInfo("File clicked: " + file.getPath());
		
		if(file.isArchive() && file instanceof OpenFile && Preferences.Pref_Zip_Internal)
			file = new OpenZip((OpenFile)file);
		
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
	
	protected ContentAdapter getContentAdapter() { return null; }
	
	public boolean showMenu(int menuId, View from)
	{
		if(getActivity() == null) return false;
		final IconContextMenu mOpenMenu = IconContextMenu.getInstance(getActivity(), menuId, from, null, null);
		if(mOpenMenu == null) return false;
		if(DEBUG)
			Logger.LogDebug("Showing menu 0x" + Integer.toHexString(menuId) + (from != null ? " near 0x" + Integer.toHexString(from.getId()) : " by itself"));
		MenuBuilder menu = mOpenMenu.getMenu();
		if(getActivity() != null)
			getActivity().onPrepareOptionsMenu(menu);
		mOpenMenu.setMenu(menu);
		mOpenMenu.setAnchor(from);
		mOpenMenu.setNumColumns(1);
		mOpenMenu.setOnIconContextItemSelectedListener(getExplorer());
		return mOpenMenu.show();
	}
	
	@Override
	public void setHasOptionsMenu(boolean hasMenu) {
		//if(!OpenExplorer.BEFORE_HONEYCOMB) super.setHasOptionsMenu(hasMenu);
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
	
	public MenuInflater getMenuInflater()
	{
		if(getActivity() != null)
			return (MenuInflater)getActivity().getMenuInflater();
		else return null;
	}
	
	public boolean onItemLongClick(AdapterView<?> list, final View view ,int pos, long id) {
		mMenuContextItemIndex = pos;
		//view.setBackgroundResource(R.drawable.selector_blue);
		//list.setSelection(pos);
		//if(list.showContextMenu()) return true;
		
		final OpenPath file = (OpenPath)((BaseAdapter)list.getAdapter()).getItem(pos);
		final String name = file.getName();
		
		Logger.LogInfo(getClassName() + ".onItemLongClick: " + file);
		
		final OpenContextMenuInfo info = new OpenContextMenuInfo(file);
		
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
				if(!file.canRead())
				{
					MenuUtils.setMenuEnabled(cmm, false);
					MenuUtils.setMenuEnabled(cmm, true, R.id.menu_context_info);
				}
				MenuUtils.setMenuEnabled(cmm, file.canWrite(), R.id.menu_context_paste, R.id.menu_context_cut, R.id.menu_context_delete, R.id.menu_context_rename);
				onPrepareOptionsMenu(cmm);
				
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
				cm.setOnIconContextItemSelectedListener(getExplorer());
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
		
		if(!OpenExplorer.BEFORE_HONEYCOMB && OpenExplorer.USE_ACTIONMODE)
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
				} catch (IllegalArgumentException e) {
				} catch (IllegalAccessException e) {
				} catch (InvocationTargetException e) {
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
		if(getExplorer() == null || getExplorer().getPreferences() == null)
			return def;
		else
			return getFragmentActivity().getSetting(path, key, def);
	}
	protected Boolean getViewSetting(OpenPath path, String key, Boolean def)
	{
		if(getExplorer() == null || getExplorer().getPreferences() == null)
			return def;
		else
			return getFragmentActivity().getSetting(path, key, def);
	}
	protected Integer getViewSetting(OpenPath path, String key, Integer def)
	{
		if(getExplorer() == null || getExplorer().getPreferences() == null)
			return def;
		else
			return getFragmentActivity().getSetting(path, key, def);
	}
	protected Float getViewSetting(OpenPath path, String key, Float def)
	{
		if(getExplorer() == null || getExplorer().getPreferences() == null)
			return def;
		else
			return getFragmentActivity().getSetting(path, key, def);
	}
	protected void setViewSetting(OpenPath path, String key, String value)
	{
		if(getExplorer() == null || getExplorer().getPreferences() == null)
			Logger.LogWarning("Unable to setViewSetting");
		else
			getFragmentActivity().setSetting(path, key, value);
	}
	protected void setViewSetting(OpenPath path, String key, Boolean value)
	{
		if(getExplorer() == null || getExplorer().getPreferences() == null)
			Logger.LogWarning("Unable to setViewSetting");
		else
			getFragmentActivity().setSetting(path, key, value);
	}
	protected void setViewSetting(OpenPath path, String key, Integer value)
	{
		if(getExplorer() == null || getExplorer().getPreferences() == null)
			Logger.LogWarning("Unable to setViewSetting");
		else
			getFragmentActivity().setSetting(path, key, value);
	}
	protected void setViewSetting(OpenPath path, String key, Float value)
	{
		if(getExplorer() == null || getExplorer().getPreferences() == null)
			Logger.LogWarning("Unable to setViewSetting");
		else
			getFragmentActivity().setSetting(path, key, value);
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
	protected Boolean getSetting(String file, String key, Boolean defValue)
	{
		if(getActivity() == null) return defValue;
		if(getFragmentActivity().getPreferences() == null) return defValue;
		return getFragmentActivity().getPreferences().getSetting(file, key, defValue);
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if(DEBUG)
			Logger.LogDebug("}-- onAttach :: " + getClassName() + (this instanceof OpenPathFragmentInterface && ((OpenPathFragmentInterface)this).getPath() != null ? " @ " + ((OpenPathFragmentInterface)this).getPath().getPath() : ""));
	}
	
	@Override
	public void onDetach() {
		super.onDetach();
		if(DEBUG)
			Logger.LogDebug("{-- onDetach :: " + getClassName() + (this instanceof OpenPathFragmentInterface && ((OpenPathFragmentInterface)this).getPath() != null ? " @ " + ((OpenPathFragmentInterface)this).getPath().getPath() : ""));
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		if(!isVisible()) return;
		super.onCreateContextMenu(menu, v, menuInfo);
		//Logger.LogDebug("OpenFragment.onCreateContextMenu");
		if(!OpenExplorer.BEFORE_HONEYCOMB && OpenExplorer.USE_ACTIONMODE) return;
		OpenContextMenuInfo info = (OpenContextMenuInfo)menuInfo;
		OpenPath file = info.getPath();
		if(file == null && mMenuContextItemIndex > -1 && getContentAdapter() != null)
			file = getContentAdapter().getItem(mMenuContextItemIndex);
		new MenuInflater(v.getContext()).inflate(R.menu.context_file, menu);
		MenuUtils.setMenuEnabled(menu, !file.isDirectory(), R.id.menu_context_edit, R.id.menu_context_view);
		if(!file.canRead())
		{
			MenuUtils.setMenuEnabled(menu, false);
			MenuUtils.setMenuEnabled(menu, true, R.id.menu_context_info);
		}
		MenuUtils.setMenuEnabled(menu, file.canWrite(), R.id.menu_context_paste, R.id.menu_context_cut, R.id.menu_context_delete, R.id.menu_context_rename);
		MenuUtils.setMenuVisible(menu, getClipboard().size() > 0, R.id.menu_context_paste);
		//menu.findItem(R.id.menu_context_unzip).setVisible(file.isArchive());
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		if(DEBUG)
			Logger.LogDebug(getClassName() + ".onCreateOptionsMenu");
		super.onCreateOptionsMenu(menu, inflater);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		if(item == null) return false;
		if(item.getMenuInfo() == null) return false;
		if(!(item.getMenuInfo() instanceof OpenContextMenuInfo)) return false;
		OpenContextMenuInfo info = (OpenContextMenuInfo) item.getMenuInfo();
		OpenPath path = info.getPath();
		if(path == null && mMenuContextItemIndex > -1 && getContentAdapter() != null)
			path = getContentAdapter().getItem(mMenuContextItemIndex);
		return executeMenu(item.getItemId(), null, path);
	}
	
	public boolean executeMenu(final int id, final Object mode, final OpenPath file)
	{
		Logger.LogInfo("executeMenu(0x" + Integer.toHexString(id) + ") on " + file);
		final String path = file != null ? file.getPath() : null;
		OpenPath parent = file != null ? file.getParent() : null;
		if(parent == null || parent instanceof OpenCursor)
			parent = OpenFile.getExternalMemoryDrive(true);
		final OpenPath folder = parent;
		String name = file != null ? file.getName() : null;
		
		onClick(id);
		
		switch(id)
		{
			case R.id.menu_context_selectall:
				if(getContentAdapter() == null) return false;
				getClipboard().addAll(getContentAdapter().getAll());
				return true;
				
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
				if(getContentAdapter() != null)
					getContentAdapter().notifyDataSetChanged();
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
			case R.id.content_paste:
				OpenPath into = file;
				if(!file.isDirectory())
				{
					Logger.LogWarning("Can't paste into file (" + file.getPath() + "). Using parent directory (" + folder.getPath() + ")");
					into = folder;
				}
				OpenClipboard cb = getClipboard();
				if(cb.size() > 0)
				{
					if(cb.DeleteSource)
						getHandler().cutFile(cb, into, getActivity());
					else
						getHandler().copyFile(cb, into, getActivity());
					refreshOperations();
				}
				
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
					.setNegativeButton(android.R.string.no, new OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							if(getClipboard().size() <= 1)
								getClipboard().clear();
						}
					});
				dZip
					.setOnCancelListener(new OnCancelListener() {
						public void onCancel(DialogInterface dialog) {
							if(getClipboard().size() <= 1)
								getClipboard().clear();
						}
					})
					.setPositiveButton(android.R.string.ok,
						new OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								OpenPath zFolder = new OpenFile(dZip.getInputTopText());
								if(zFolder == null || !zFolder.exists())
									zFolder = folder;
								OpenPath zipFile = zFolder.getChild(dZip.getInputText());
								Logger.LogVerbose("Zipping " + getClipboard().size() + " items to " + zipFile.getPath());
								getHandler().zipFile(zipFile, getClipboard(), getExplorer());
								refreshOperations();
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
	
	private void refreshOperations()
	{
		if(getExplorer() != null)
			getExplorer().refreshOperations();
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
	public Context getApplicationContext() { if(getActivity() != null) return getActivity().getApplicationContext(); else return null; }
	public static EventHandler getEventHandler() { return OpenExplorer.getEventHandler(); }
	public static FileManager getFileManager() { return OpenExplorer.getFileManager(); }
	
	@Override
	public OpenClipboard getClipboard() {
		if(getExplorer() != null)
			return getExplorer().getClipboard();
		else return null;
	}
	
	@Override
	public boolean checkClipboard(OpenPath file) {
		if(getClipboard() != null)
			return getClipboard().contains(file);
		else return false;
	}
	
	public void onClick(View v) {
		Logger.LogInfo(getClassName() + ".onClick(" + v.getId() + ")");
	}
	
	public boolean onClick(int id) {
		Logger.LogInfo(getClassName() + ".onClick(" + id + ")");
		return false;
	}
	
	public boolean onLongClick(View v) {
		Logger.LogInfo(getClassName() + ".onLongClick(" + v.getId() + ") - " + v.toString());
		return false;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		//Logger.LogDebug("<-- onCreate - " + getClassName() + (this instanceof OpenPathFragmentInterface && ((OpenPathFragmentInterface)this).getPath() != null ? " @ " + ((OpenPathFragmentInterface)this).getPath().getPath() : ""));
		//CONTENT_FRAGMENT_FREE = false;
		setRetainInstance(true);
		super.onCreate(savedInstanceState);
	}
	
	public void invalidateOptionsMenu()
	{
		if(getExplorer() == null) return;
		getExplorer().invalidateOptionsMenu();
	}

	public abstract Drawable getIcon();
	public abstract CharSequence getTitle();
	
	public void notifyPager()
	{
		if(getExplorer() != null)
			getExplorer().notifyPager();
	}
	
	public void sendToLogView(String txt, int color)
	{
		if(getExplorer() != null)
			getExplorer().sendToLogView(txt, color);
	}
	
	@Override
	public Context getAndroidContext() {
		if(getExplorer() != null)
			return getExplorer().getAndroidContext();
		else return getApplicationContext();
	}
	
	@Override
	public DataManager getDataManager() {
		return getExplorer().getDataManager();
	}
	
	@Override
	public DiskLruCache getDiskCache() {
		return getExplorer().getDiskCache();
	}
	
	@Override
	public DownloadCache getDownloadCache() {
		return getExplorer().getDownloadCache();
	}
	
	@Override
	public ImageCacheService getImageCacheService() {
		return getExplorer().getImageCacheService();
	}
	
	@Override
	public ContentResolver getContentResolver() {
		return getExplorer().getContentResolver();
	}
	
	@Override
	public Looper getMainLooper() {
		return getExplorer().getMainLooper();
	}
	
	@Override
	public LruCache<String, Bitmap> getMemoryCache() {
		return getExplorer().getMemoryCache();
	}
	
	@Override
	public ThreadPool getThreadPool() {
		return getExplorer().getThreadPool();
	}
	
	@Override
	public ShellSession getShellSession() {
		return getExplorer().getShellSession();
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		if(DEBUG)
			Logger.LogDebug("<-- onViewCreated - " + getClassName());
		super.onViewCreated(view, savedInstanceState);
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
