package org.brandroid.openmanager.adapters;

import org.brandroid.openmanager.R;
import org.brandroid.openmanager.util.BetterPopupWindow;
import org.brandroid.utils.MenuBuilderNew;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.*;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.Rect;
import android.view.*;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.PopupWindow;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class IconContextMenu
{
	
	public interface IconContextItemSelectedListener {
		void onIconContextItemSelected(MenuItem item, Object info, View view);
	}
	
	private GridView mList;
	//private Dialog dialog;
	protected final BetterPopupWindow popup;
	private MenuBuilderNew menu;
	protected final View anchor;
	private int maxColumns = 1;
	
	private IconContextItemSelectedListener iconContextItemSelectedListener;
	private Object info;

    public IconContextMenu(Context context, int menuId, View from) {
    	this(context, newMenu(context, menuId), from);
    }
    
    public static MenuBuilderNew newMenu(Context context, int menuId) {
    	MenuBuilderNew menu = new MenuBuilderNew(context);
    	new MenuInflater(context).inflate(menuId, menu);
    	return menu;
    }

	public IconContextMenu(Context context, Menu menu, final View from) {
		MenuBuilderNew newMenu = new MenuBuilderNew(context);
		for(int i = 0; i < menu.size(); i++)
		{
			MenuItem item = menu.getItem(i);
			if(item.isVisible())
				newMenu.add(item.getGroupId(), item.getItemId(), item.getOrder(), item.getTitle());
		}
        //menu = newMenu;
        anchor = from;
        //this.dialog = new AlertDialog.Builder(context);
        popup = new BetterPopupWindow(context, anchor);
        setAdapter(context, new IconContextMenuAdapter(context, menu));
	}
	
	public void setAdapter(Context context, final IconContextMenuAdapter adapter)
	{
		mList = new GridView(context);
		mList.setNumColumns(maxColumns);
		mList.setAdapter(adapter);
		mList.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> arg0, View v, int pos, long id) {
				if(iconContextItemSelectedListener != null)
				{
					iconContextItemSelectedListener.onIconContextItemSelected(
							adapter.getItem(pos), info, v);
				}
					
			}
			
		} );
		
		//popup.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.contextmenu_top_right));
		popup.setContentView(mList);
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
	
	public void setMaxColumns(int cols) {
		maxColumns = cols;
		if(mList != null)
			mList.setNumColumns(cols);
	}
	
	public void setInfo(Object info) {
		this.info = info;
	}

	public Object getInfo() {
		return info;
	}
	
	public MenuBuilderNew getMenu() {
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

    public void show()
    {
    	//popup.showLikeQuickAction();
    	popup.showLikePopDownMenu();
    }
    public void show(int left, int top)
    {
    	//popup.showLikeQuickAction();
    	popup.showLikePopDownMenu(left, top);
    }
    public void dismiss()
    {
    	popup.dismiss();
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