package org.brandroid.openmanager.adapters;

import org.brandroid.openmanager.R;

import android.content.Context;
import android.graphics.Color;
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
		return menu.getItem(position);
	}

	//@Override
	public long getItemId(int position) {
		return getItem(position).getItemId();
	}
    
    //@Override
    public View getView(int position, View convertView, ViewGroup parent) {
        MenuItem item = getItem(position);
        
        if(!item.isVisible()) return null;
        
        CheckedTextView res = (CheckedTextView)convertView;
        if (res == null) {
       		res = (CheckedTextView)LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_multiple_choice, null);
        }
        
        if(item.isCheckable())
        	res.setChecked(item.isChecked());
        else res.setCheckMarkDrawable(null);
        
        res.setTextSize(context.getResources().getDimension(R.dimen.large_text_size));
        res.setTextColor(context.getResources().getColor(android.R.color.primary_text_dark));
        
        res.setTag(item);
        res.setText(item.getTitle());
        res.setCompoundDrawablesWithIntrinsicBounds(item.getIcon(), null, null, null);
              
        return res;
    }
}
