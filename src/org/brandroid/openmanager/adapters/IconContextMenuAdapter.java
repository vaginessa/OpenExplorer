package org.brandroid.openmanager.adapters;

import org.brandroid.openmanager.R;
import org.brandroid.utils.MenuBuilder;
import org.brandroid.utils.ViewUtils;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ScaleDrawable;
import android.view.*;
import android.widget.*;

public class IconContextMenuAdapter extends BaseAdapter {
	private Context context;
    private MenuBuilder myMenu;
    private boolean hasIcons = false;
    private boolean hasChecks = false;
    private int textLayoutId = R.layout.simple_list_item_multiple_choice;
    
    public IconContextMenuAdapter(Context context, Menu menu) {
		this.context = context;
		myMenu = new MenuBuilder(context);
		setMenu(menu);
	}
    
    public void setMenu(Menu menu)
    {
    	if(menu == null) return;
    	myMenu.clear();
    	for(int i=0; i<menu.size(); i++)
    	{
    		MenuItem item = menu.getItem(i);
    		if(!item.isVisible()) continue;
    		if(item.getIcon() != null) hasIcons = true;
    		if(item.isCheckable() || item.isChecked()) hasChecks = true;
    		myMenu.add(item.getGroupId(), item.getItemId(), item.getOrder(), item.getTitle())
    			.setIcon(item.getIcon())
    			.setCheckable(item.isCheckable())
    			.setChecked(item.isChecked())
    			.setVisible(item.isVisible())
    			.setEnabled(item.isEnabled());
    	}
    	notifyDataSetChanged();
    }
    public void setTextLayout(int layoutId) {
    	textLayoutId = layoutId;
    	notifyDataSetChanged();
    }

	//@Override
	public int getCount() {
		return myMenu.size();
	}
	
	//@Override
	public MenuItem getItem(int position) {
		if(position < myMenu.size() && position >= 0)
			return myMenu.getItem(position);
		else
			return null;
	}

	public MenuItem findItem(int id) {
		return myMenu.findItem(id);
	}
	
	//@Override
	public long getItemId(int position) {
		return position;
		//return getItem(position).getItemId();
	}
    
    //@Override
    public View getView(int position, View convertView, ViewGroup parent) {
        MenuItem item = getItem(position);
        TextView res = (TextView)LayoutInflater.from(context).inflate(
       				textLayoutId
       				, parent, false);
        
        if(!item.isVisible())
        	res.setVisibility(View.GONE);
        if(!item.isEnabled())
        	res.setEnabled(false);
        else
        	res.setEnabled(true);
        
        int checkId = android.R.drawable.checkbox_off_background;
        if(item.isCheckable() || item.isChecked())
        	checkId = item.isChecked() ?
        			android.R.drawable.checkbox_on_background :
        			android.R.drawable.checkbox_off_background;
        Drawable check = null;
        if(res instanceof CheckedTextView)
        {
        	if(item.getTitle().toString().indexOf("View") > -1 || item.getTitle().toString().indexOf("Sort") > -1)
        		((CheckedTextView)res).setCheckMarkDrawable(android.R.drawable.btn_radio);
        	checkId = 0;
	        if(item.isCheckable() || item.isChecked())
	        {
	        	((CheckedTextView)res).setChecked(item.isChecked());
	        } else {
	        	((CheckedTextView)res).setCheckMarkDrawable(null);
	        }
        } else if(checkId > 0)
        	check = res.getResources().getDrawable(checkId);
        
        //else if(item.isChecked())
        
        //res.setTextSize(context.getResources().getDimension(R.dimen.large_text_size));
        //res.setTextColor(context.getResources().getColor(android.R.color.primary_text_dark));
        
        int sz = (int)(res.getResources().getDimension(R.dimen.one_dp) * 32f);
        Drawable src = item.getIcon();
        if(src != null && src instanceof BitmapDrawable)
        	((BitmapDrawable)src).setGravity(Gravity.CENTER);
        Drawable icon = new LayerDrawable(new Drawable[]{src != null ? src : new ColorDrawable(android.R.color.white)});
        		//Gravity.CENTER, sz, sz);

    	ViewUtils.setAlpha(res, item.isEnabled() ? 1f : 0.4f);
        
    	res.setEnabled(item.isEnabled());
        res.setTag(item);
        res.setText(item.getTitle());
        res.setCompoundDrawablesWithIntrinsicBounds(icon, null, check, null);
        
        if(position == 0)
        	res.setNextFocusUpId(getCount() - 1);
        else if(position == getCount() - 1)
        	res.setNextFocusDownId(0);
              
        return res;
    }
}
