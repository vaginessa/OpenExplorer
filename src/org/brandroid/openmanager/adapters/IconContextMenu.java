package org.brandroid.openmanager.adapters;

import java.util.Hashtable;

import org.brandroid.openmanager.R;
import org.brandroid.openmanager.util.BetterPopupWindow;
import org.brandroid.openmanager.util.BetterPopupWindow.OnPopupShownListener;
import org.brandroid.utils.Logger;
import org.brandroid.utils.MenuBuilder;
import org.brandroid.utils.Utils;
import org.brandroid.utils.ViewUtils;

import android.content.Context;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.View.OnKeyListener;
import android.widget.PopupWindow;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class IconContextMenu implements OnKeyListener
{
	
	public interface IconContextItemSelectedListener {
		void onIconContextItemSelected(IconContextMenu menu, MenuItem item, Object info, View view);
	}
	
	private ScrollView mScroller;
	private TableLayout mTable;
	//private Dialog dialog;
	protected final BetterPopupWindow popup;
	private MenuBuilder menu;
	protected View anchor;
	private int mPosition;
	private int maxColumns = 1;
	private int mWidth = 0;
	private int rotation = 0;
	private CharSequence mTitle = null;
	private int textLayoutId = R.layout.simple_list_item_multiple_choice;
	private static final Hashtable<Integer, IconContextMenu> mInstances = new Hashtable<Integer, IconContextMenu>();
	private static final Hashtable<Integer, Integer> mHeights = new Hashtable<Integer, Integer>();
	private static final int[] DOUBLE_WIDTH_IDS = new int[]{R.id.menu_context_download};
	
	private OnKeyListener mKeyListener;
	private IconContextItemSelectedListener iconContextItemSelectedListener;
	private Object info;

    public IconContextMenu(Context context, int menuId, View from) {
    	this(context, newMenu(context, menuId), from);
    }
    
    public static MenuBuilder newMenu(Context context, int menuId) {
    	MenuBuilder menu = new MenuBuilder(context);
    	try {
    		new MenuInflater(context).inflate(menuId, menu);
    	} catch(ClassCastException e) {
    		Logger.LogWarning("Couldn't inflate menu (0x" + Integer.toHexString(menuId) + ")", e);
    		return null;
    	}
    	return menu;
    }

	public IconContextMenu(Context context, MenuBuilder newMenu, final View from) {
		//root.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        menu = newMenu;
		anchor = from;
		rotation = ((WindowManager)context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
        //this.dialog = new AlertDialog.Builder(context);
        popup = new BetterPopupWindow(context, anchor);
        mScroller = (ScrollView)LayoutInflater.from(context)
        		.inflate(R.layout.icon_menu, null);
        mTable = (TableLayout)mScroller.findViewById(R.id.icon_menu_table);
        refreshTable();
        //mTable.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        //mGrid.setNumColumns(maxColumns);
        mTable.setOnKeyListener(this);
        popup.setOnKeyListener(this);
        //mTable.setColumnStretchable(0, true);
        //setAdapter(context, new IconContextMenuAdapter(context, menu));
        if(mWidth == 0)
        {
        	//mGrid.measure(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        	//mWidth = mGrid.getMeasuredWidth();
        }
        if(mWidth == 0)
        	mWidth = context.getResources().getDimensionPixelSize(R.dimen.popup_width);
        if(mWidth > 0)
        	popup.setPopupWidth(mWidth);
        popup.setContentView(mScroller);
        //if(mWidth > 0)
        //	popup.setPopupWidth(mWidth);
	}
	
	/*
	private IconContextMenuAdapter getAdapter() { return (IconContextMenuAdapter) mGrid.getAdapter(); }
	public void setAdapter(Context context, final IconContextMenuAdapter adapter)
	{
		mGrid.setAdapter(adapter);
		Logger.LogInfo("mGrid Adapter set");
		mGrid.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> arg0, View v, int pos, long id) {
				if(!adapter.getItem(pos).isEnabled()) return;
				if(iconContextItemSelectedListener != null)
				{
					iconContextItemSelectedListener.onIconContextItemSelected(
							IconContextMenu.this, adapter.getItem(pos), info, v);
				}
			}
			
		} );
		
		
		//root.
		//popup.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.contextmenu_top_right));
		//popup.setContentView(mList);
		/*this.dialog = new AlertDialog.Builder(context)
	        .setAdapter(adapter, new DialogInterface.OnClickListener() {
		        //@Override
		        public void onClick(DialogInterface dialog, int which) {
		        	if (iconContextItemSelectedListener != null) {
		        		iconContextItemSelectedListener.onIconContextItemSelected(adapter.getItem(which), info);
		        	}
		        }
	        })
	        .setInverseBackgroundForced(true)
	        .create();
	        * /
    }
	*/
	
	public void refreshTable()
	{
		final Context context = mTable.getContext();
		mTable.post(new Runnable(){public void run(){
			mTable.setStretchAllColumns(false);
			mTable.setColumnStretchable(0, false);
			mTable.removeAllViews();
			//mTable.setShowDividers(TableLayout.SHOW_DIVIDER_MIDDLE);
			//mTable.setDividerDrawable(context.getResources().getDrawable(android.R.drawable.divider_horizontal_dark));
			if(mTitle != null)
			{
				if(mScroller.findViewById(R.id.icon_menu_top) != null &&
						mScroller.findViewById(R.id.icon_menu_top).findViewById(android.R.id.title) != null)
					ViewUtils.setText(mScroller.findViewById(R.id.icon_menu_top), mTitle, android.R.id.title);
				else {
					View ttl = LayoutInflater.from(context)
								.inflate(R.layout.popup_title, (ViewGroup)mScroller.findViewById(R.id.icon_menu_top), false);
					((TextView)ttl.findViewById(android.R.id.title)).setText(mTitle);
					((ViewGroup)mScroller.findViewById(R.id.icon_menu_top)).addView(ttl);
				}
			}
			TableRow row = new TableRow(context);
			for(int i = 0; i < menu.size(); i++)
			{
				final MenuItem item = menu.getItem(i);
				if(!item.isVisible()) continue;
				int col = i % maxColumns;
				boolean dbl = Utils.getArrayIndex(DOUBLE_WIDTH_IDS, item.getItemId()) > -1;
				if(col == 0)
				{
					if(i > 0)
						mTable.addView(row);
					row = new TableRow(context);
					row.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
				}
				View kid = IconContextMenuAdapter.createView(row, item, textLayoutId);
				if(i == menu.size() - 1)
					kid.setNextFocusDownId(menu.getItem(0).getItemId());
				else if(i == 0)
					kid.setNextFocusUpId(menu.getItem(menu.size() - 1).getItemId());
				kid.setId(item.getItemId());
				kid.setBackgroundResource(R.drawable.list_selector_background);
				kid.setFocusable(true);
				//kid.setFocusableInTouchMode(true);
				if(i == 0)
					kid.requestFocus();
				kid.setOnKeyListener(IconContextMenu.this);
				kid.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if(iconContextItemSelectedListener != null)
							iconContextItemSelectedListener
								.onIconContextItemSelected(
									IconContextMenu.this, item, menu, v);
					}
				});
				if(textLayoutId == R.layout.simple_list_item_multiple_choice)
					kid.setPadding(8, 8, 8, 8);
				else kid.setPadding(2, 2, 2, 2);
				((TextView)kid).setCompoundDrawablePadding(8);
				row.addView(kid);
			}
			if(row.getChildCount() > 0)
				mTable.addView(row);
			mTable.setStretchAllColumns(true);
		}});
	}
	
	public void setOnKeyListener(OnKeyListener listener)
	{
		mKeyListener = listener;
		//popup.setOnKeyListener(listener);
		//if(mTable != null)
		//	mTable.setOnKeyListener(listener);
	}
	
	public void setNumColumns(int cols) {
		maxColumns = cols;
		//if(mGrid != null)
		//	mGrid.setNumColumns(cols);
	}
	
	public void setInfo(Object info) {
		this.info = info;
	}

	public Object getInfo() {
		return info;
	}
	
	public MenuBuilder getMenu() {
		return menu;
	}
	
	public void setOnDismissListener(PopupWindow.OnDismissListener listener)
	{
		popup.setOnDismissListener(listener);
	}
    public void setOnIconContextItemSelectedListener(IconContextItemSelectedListener iconContextItemSelectedListener) {
        this.iconContextItemSelectedListener = iconContextItemSelectedListener;
    }
    
    public void setTitle(CharSequence title) {
    	//popup.setTitle(title);
    	mTitle = title;
    }
	
	private int getMenuSignature() {
		return
			rotation * 1000 +
			maxColumns * 100 +
			getMenuSignature(menu);
	}
	
	private int getMenuSignature(Menu menu)
	{
		if(menu == null) return 0;
		int ret = 0;
		for(int i = 0; i < menu.size(); i++)
			ret += getMenuSignature(menu.getItem(i));
		return ret;
	}
	
	private int getMenuSignature(MenuItem item)
	{
		if(item == null) return 0;
		if(!item.isVisible()) return 0;
		if(item.getIcon() != null) return 3;
		if(item.isCheckable()) return 2;
		return 1;
	}

    public boolean show()
    {
    	return show(0, 0);
    }
    public boolean show(int left, int top)
    {
    	refreshTable();
    	//popup.showLikeQuickAction();
    	final int menuSig = getMenuSignature();
    	if(mHeights.containsKey(menuSig))
    	{
    		Logger.LogDebug("Menu Signature (" + menuSig + ") found = " + mHeights.get(menuSig));
    		popup.setPopupHeight(mHeights.get(menuSig));
    	}
    	popup.setPopupShownListener(new OnPopupShownListener() {
			@Override
			public void OnPopupShown(int width, int height) {
				Logger.LogVerbose("Popup Height: " + height);
				mHeights.put(menuSig, height);
			}
		});
    	return popup.showLikePopDownMenu(left, top);
    }
    public void dismiss()
    {
    	popup.dismiss();
    }
	
	public void setPopupWidth(int w)
	{
		mWidth = w;
		if(popup != null)
			popup.setPopupWidth(w);
	}

	public static IconContextMenu getInstance(Context c, int menuId, View from) {
		if(!mInstances.containsKey(menuId))
		{
			MenuBuilder menu = newMenu(c, menuId);
			if(menu == null) {
				Logger.LogWarning("IconContextMenu getInstance(0x" + Integer.toHexString(menuId) + ") is null");
				return null;
			}
			mInstances.put(menuId, new IconContextMenu(c, menu, from));
		}
		else Logger.LogDebug("IContextMenu Instance Height: " + mInstances.get(menuId).popup.getPopupHeight());
		IconContextMenu ret = mInstances.get(menuId);
		ret.setAnchor(from);
		return ret;
	}

	public static void clearInstances() {
		mInstances.clear();
		
	}

	public void setMenu(MenuBuilder newMenu) {
		/*getAdapter().setMenu(newMenu);*/
		menu = newMenu;
	}

	public void setAnchor(View from) {
		this.anchor = from;
		popup.setAnchor(from);
	}

	public void setTextLayout(int layoutId) {
		//getAdapter().setTextLayout(layoutId);
		textLayoutId = layoutId;
		refreshTable();
	}
	
	public int getSelectedItemPosition() { return mPosition; }

	@Override
	public boolean onKey(View v, int keyCode, KeyEvent event) {
		Logger.LogDebug("IconContextMenu.onKey(" + v + "," + keyCode + "," + event + ")");
		if(event.getAction() != KeyEvent.ACTION_DOWN) return false;
		if(keyCode == KeyEvent.KEYCODE_MENU)
		{
			dismiss();
			return true;
		}
		if(mKeyListener != null)
			if(mKeyListener.onKey(v, keyCode, event))
				return true;
		int pos = getSelectedItemPosition();
		int col = pos % maxColumns;
		if(col == 0 && keyCode == KeyEvent.KEYCODE_DPAD_LEFT)
			return true;
		if(col == maxColumns - 1 && keyCode == KeyEvent.KEYCODE_DPAD_RIGHT)
			return true;
		return false;
	}

    
    /*
    public void setOnCancelListener(DialogInterface.OnCancelListener onCancelListener) {
    	popup.setOnCancelListener(onCancelListener);
    }
    
    public void setOnDismissListener(DialogInterface.OnDismissListener onDismissListener) {
    	dialog.setOnDismissListener(onDismissListener);
    }
    
    public void show() {
    	dialog.show();
    }
    
    public void dismiss() {
    	dialog.dismiss();
    }
    
    public void cancel() {
    	dialog.cancel();
    }
    
    public Dialog getDialog() {
    	return dialog;
    }
    */
}