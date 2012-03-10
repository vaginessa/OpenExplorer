package org.brandroid.openmanager.adapters;

import java.util.Hashtable;

import org.brandroid.openmanager.R;
import org.brandroid.openmanager.util.BetterPopupWindow;
import org.brandroid.utils.Logger;
import org.brandroid.utils.MenuBuilder;
import org.brandroid.utils.MenuBuilderNew;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.*;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.Rect;
import android.view.*;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;

public class IconContextMenu
{
	
	public interface IconContextItemSelectedListener {
		void onIconContextItemSelected(MenuItem item, Object info, View view);
	}
	
	private GridView mGrid;
	//private Dialog dialog;
	protected final BetterPopupWindow popup;
	protected final ViewGroup root;
	private MenuBuilder menu;
	protected View anchor;
	private int maxColumns = 2;
	private int mWidth = 0;
	private static Hashtable<Integer, IconContextMenu> mInstances = new Hashtable<Integer, IconContextMenu>(); 
	
	private IconContextItemSelectedListener iconContextItemSelectedListener;
	private Object info;

    public IconContextMenu(Context context, int menuId, View from, View head, View foot) {
    	this(context, newMenu(context, menuId), from, head, foot);
    }
    
    public static MenuBuilder newMenu(Context context, int menuId) {
    	MenuBuilder menu = new MenuBuilder(context);
    	new MenuInflater(context).inflate(menuId, menu);
    	return menu;
    }

	public IconContextMenu(Context context, MenuBuilder menu, final View from, final View head, final View foot) {
		root = (ViewGroup) ((LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.paste_layout, null);
		//root.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        //menu = newMenu;
		this.menu = menu;
        anchor = from;
        //this.dialog = new AlertDialog.Builder(context);
        popup = new BetterPopupWindow(context, anchor);
        mGrid = new GridView(context);
        mGrid.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        mGrid.setNumColumns(maxColumns);
        setAdapter(context, new IconContextMenuAdapter(context, menu));
        if(mWidth == 0)
        {
        	//mGrid.measure(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        	//mWidth = mGrid.getMeasuredWidth();
        }
        if(mWidth == 0)
        	mWidth = context.getResources().getDimensionPixelSize(R.dimen.popup_width);
        if(mWidth > 0)
        	popup.setPopupWidth(mWidth);
        if(head != null)
        	root.addView(head);
		root.addView(mGrid);
		if(foot != null)
			root.addView(foot);
        popup.setContentView(root);
        //if(mWidth > 0)
        //	popup.setPopupWidth(mWidth);
	}
	
	private IconContextMenuAdapter getAdapter() { return (IconContextMenuAdapter) mGrid.getAdapter(); }
	public void setAdapter(Context context, final IconContextMenuAdapter adapter)
	{
		mGrid.setAdapter(adapter);
		Logger.LogInfo("mGrid Adapter set");
		mGrid.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> arg0, View v, int pos, long id) {
				if(iconContextItemSelectedListener != null)
				{
					iconContextItemSelectedListener.onIconContextItemSelected(
							adapter.getItem(pos), info, v);
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
	        */
    }
	
	public void setNumColumns(int cols) {
		maxColumns = cols;
		if(mGrid != null)
			mGrid.setNumColumns(cols);
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
    	popup.setTitle(title);
    }
	public void setTitle(int stringId) {
		popup.setTitle(stringId);
	}

    public boolean show()
    {
    	//popup.showLikeQuickAction();
    	return popup.showLikePopDownMenu();
    }
    public boolean show(int left, int top)
    {
    	//popup.showLikeQuickAction();
    	return popup.showLikePopDownMenu(left, top);
    }
    public void dismiss()
    {
    	popup.dismiss();
    }

    public void addView(View v)
    {
    	Logger.LogInfo("View added to IconContextMenu");
    	root.addView(v);
    }
	public void addView(View v, int index) {
		root.addView(v, index);
	}
	
	public void setPopupWidth(int w)
	{
		mWidth = w;
		if(popup != null)
			popup.setPopupWidth(w);
	}

	public static IconContextMenu getInstance(Context c, int menuId,
			View from, View top, View bottom) {
		if(!mInstances.containsKey(menuId))
			mInstances.put(menuId, new IconContextMenu(c, menuId, from, top, bottom));
		IconContextMenu ret = mInstances.get(menuId);
		ret.setAnchor(from);
		return ret;
	}

	public static void clearInstances() {
		mInstances.clear();
		
	}

	public void setMenu(MenuBuilder newMenu) {
		getAdapter().setMenu(newMenu);
	}

	public void setAnchor(View from) {
		this.anchor = from;
		popup.setAnchor(from);
	}

	public void setTextLayout(int layoutId) {
		getAdapter().setTextLayout(layoutId);
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