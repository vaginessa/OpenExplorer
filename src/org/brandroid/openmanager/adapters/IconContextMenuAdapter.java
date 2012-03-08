package org.brandroid.openmanager.adapters;

import org.brandroid.openmanager.R;
import org.brandroid.utils.MenuBuilder;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.*;
import android.widget.*;

public class IconContextMenuAdapter extends BaseAdapter {
	private Context context;
    private MenuBuilder myMenu;
    private boolean hasIcons = false;
    private boolean hasChecks = false;
    private int textLayoutId = android.R.layout.simple_list_item_multiple_choice;
    
    public IconContextMenuAdapter(Context context, Menu menu) {
		this.context = context;
		myMenu = new MenuBuilder(context);
		setMenu(menu);
	}
    
    public void setMenu(Menu menu)
    {
    	myMenu.clear();
    	for(int i=0; i<menu.size(); i++)
    	{
    		MenuItem item = menu.getItem(i);
    		if(!item.isVisible()) continue;
    		if(item.getIcon() != null) hasIcons = true;
    		if(item.isCheckable() || item.isChecked()) hasChecks = true;
    		myMenu.add(item.getGroupId(), item.getItemId(), item.getOrder(), item.getTitle())
    			.setIcon(item.getIcon());
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
		return getItem(position).getItemId();
	}
    
    //@Override
    public View getView(int position, View convertView, ViewGroup parent) {
        MenuItem item = getItem(position);
        //if(item == null)
        //	return convertView;
        TextView res = (TextView)convertView;
        //if (res == null) {
       		res = (TextView)LayoutInflater.from(context).inflate(
       				textLayoutId
       				, parent, false);
        //}
        
        if(!item.isVisible())
        	res.setVisibility(View.GONE);
        if(!item.isEnabled())
        	res.setEnabled(false);
        else
        	res.setEnabled(true);
        
        Drawable check = null;
        if(res instanceof CheckedTextView)
        {
	        if(item.isCheckable() || item.isChecked())
	        	((CheckedTextView)res).setChecked(item.isChecked());
	        else if(!item.isCheckable())
	        	((CheckedTextView)res).setCheckMarkDrawable(null);
        } else if(item.isChecked())
        	check = res.getResources().getDrawable(android.R.drawable.checkbox_on_background);
        
        //res.setTextSize(context.getResources().getDimension(R.dimen.large_text_size));
        //res.setTextColor(context.getResources().getColor(android.R.color.primary_text_dark));
        
        Drawable icon = item.getIcon();
        if(icon == null)
        	icon = new ColorDrawable(android.R.color.white);
        if(icon instanceof BitmapDrawable)
        	((BitmapDrawable)icon).setGravity(Gravity.CENTER);
        
        res.setTag(item);
        res.setText(item.getTitle());
        res.setCompoundDrawablesWithIntrinsicBounds(icon, null, check, null);
              
        return res;
    }
}
