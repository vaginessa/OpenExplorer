package org.brandroid.openmanager.adapters;

import org.brandroid.openmanager.R;

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
    private Menu menu;
    
    public IconContextMenuAdapter(Context context, Menu menu) {
		this.context = context;
		this.menu = menu;
	}

	//@Override
	public int getCount() {
		return menu.size();
	}
	
	//@Override
	public MenuItem getItem(int position) {
		if(position < menu.size() && position >= 0)
			return menu.getItem(position);
		else
			return null;
	}

	public MenuItem findItem(int id) {
		return menu.findItem(id);
	}
	
	//@Override
	public long getItemId(int position) {
		return getItem(position).getItemId();
	}
    
    //@Override
    public View getView(int position, View convertView, ViewGroup parent) {
        MenuItem item = getItem(position);
        if(item == null)
        	return convertView;
        
        TextView res = (TextView)convertView;
        if (res == null) {
       		res = (TextView)LayoutInflater.from(context).inflate(
       				//android.R.layout.simple_list_item_multiple_choice
       				R.layout.context_item
       				, null);
        }
        
        if(!item.isVisible())
        	res.setVisibility(View.GONE);
        if(!item.isEnabled())
        	res.setEnabled(false);
        else
        	res.setEnabled(true);
        
        if(CheckedTextView.class.equals(res.getClass()))
        {
	        if(item.isCheckable())
	        	((CheckedTextView)res).setChecked(item.isChecked());
	        else ((CheckedTextView)res).setCheckMarkDrawable(null);
        }
        
        //res.setTextSize(context.getResources().getDimension(R.dimen.large_text_size));
        //res.setTextColor(context.getResources().getColor(android.R.color.primary_text_dark));
        
        Drawable icon = item.getIcon();
        if(icon == null)
        	icon = new ColorDrawable(android.R.color.white);
        if(BitmapDrawable.class.equals(icon.getClass()))
        	((BitmapDrawable)icon).setGravity(Gravity.CENTER);
        
        res.setTag(item);
        res.setText(item.getTitle());
        res.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
              
        return res;
    }
}
