package org.brandroid.openmanager.adapters;

import org.brandroid.openmanager.R;
import org.brandroid.openmanager.util.BetterPopupWindow;
import org.brandroid.utils.MenuBuilderNew;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.*;
import android.view.*;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class IconContextMenu
{
	
	public interface IconContextItemSelectedListener {
		void onIconContextItemSelected(MenuItem item, Object info, View view);
	}
	
	private ListView mList;
	//private Dialog dialog;
	private BetterPopupWindow popup;
	private Menu menu;
	private View anchor;
	
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

	public IconContextMenu(Context context, Menu menu, View from) {
        this.menu = menu;
        this.anchor = from;
        //this.dialog = new AlertDialog.Builder(context);
        setAdapter(context, new IconContextMenuAdapter(context, menu));
	}
	
	public void setAdapter(Context context, final IconContextMenuAdapter adapter)
	{
		mList = new ListView(context);
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
		popup = new BetterPopupWindow(context, anchor, R.style.Animations_GrowFromTop);
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
	
	public void setInfo(Object info) {
		this.info = info;
	}

	public Object getInfo() {
		return info;
	}
	
	public Menu getMenu() {
		return menu;
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
    	popup.showLikePopDownMenu();
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