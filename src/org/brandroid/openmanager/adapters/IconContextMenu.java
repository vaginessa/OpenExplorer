package org.brandroid.openmanager.adapters;

import org.brandroid.utils.MenuBuilder;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.*;
import android.view.*;

public class IconContextMenu
{
	public interface IconContextItemSelectedListener {
		void onIconContextItemSelected(MenuItem item, Object info);
	}
	
	private Dialog dialog;
	private MenuBuilder menu;
	
	private IconContextItemSelectedListener iconContextItemSelectedListener;
	private Object info;
	
    public IconContextMenu(Context context, int menuId) {
    	this(context, newMenu(context, menuId));
    }
    
    public static MenuBuilder newMenu(Context context, int menuId) {
    	MenuBuilder menu = new MenuBuilder(context);
    	new MenuInflater(context).inflate(menuId, menu);
    	return menu;
    }

	public IconContextMenu(Context context, Menu menu) {
        this.menu = (MenuBuilder)menu;
        //this.dialog = new AlertDialog.Builder(context);
        setAdapter(context, new IconContextMenuAdapter(context, menu));
	}
	public void setAdapter(Context context, final IconContextMenuAdapter adapter)
	{
		this.dialog = new AlertDialog.Builder(context)
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
	
    public void setOnIconContextItemSelectedListener(IconContextItemSelectedListener iconContextItemSelectedListener) {
        this.iconContextItemSelectedListener = iconContextItemSelectedListener;
    }
    
    public void setOnCancelListener(DialogInterface.OnCancelListener onCancelListener) {
    	dialog.setOnCancelListener(onCancelListener);
    }
    
    public void setOnDismissListener(DialogInterface.OnDismissListener onDismissListener) {
    	dialog.setOnDismissListener(onDismissListener);
    }
    
    public void setTitle(CharSequence title) {
    	dialog.setTitle(title);
    }
    
    public void setTitle(int titleId) {
    	dialog.setTitle(titleId);
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
}