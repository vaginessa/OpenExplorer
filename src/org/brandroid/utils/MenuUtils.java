package org.brandroid.utils;

import android.view.Menu;
import android.view.MenuItem;

public class MenuUtils {
	public static void transferMenu(Menu from, Menu to) { transferMenu(from, to, true); }
	public static void transferMenu(Menu from, Menu to, Boolean clearFrom) {
		to.clear();
		for(int i=0; i<from.size(); i++)
			transferMenu(from.getItem(i), to);
		if(clearFrom)
			from.clear();
	}
	public static void transferMenu(MenuItem item, Menu to)
	{
		if(!item.isVisible()) return;
		if(item.hasSubMenu())
			transferMenu(item.getSubMenu(),
					to.addSubMenu(item.getGroupId(), item.getItemId(), item.getOrder(), item.getTitle())
						.setIcon(item.getIcon())
					);
		else
			to.add(item.getGroupId(), item.getItemId(), item.getOrder(), item.getTitle())
			.setEnabled(item.isEnabled())
			.setCheckable(item.isCheckable())
			.setChecked(item.isChecked())
			.setVisible(item.isVisible())
			.setIcon(item.getIcon());
	}

	public static void setMenuChecked(Menu menu, boolean checked, int toCheck, int... toOppose)
	{
		for(int id : toOppose)
			if(menu.findItem(id) != null)
				menu.findItem(id).setChecked(!checked);
		if(menu.findItem(toCheck) != null)
			menu.findItem(toCheck).setChecked(checked);
	}

	public static void setMenuVisible(Menu menu, boolean visible, int... ids)
	{
		if(menu == null) return;
		for(int id : ids)
			if(menu.findItem(id) != null && !visible)
				menu.removeItem(id);
			else if(menu.findItem(id) != null && visible)
				menu.findItem(id).setVisible(visible);
			else for(int i=0; i<menu.size(); i++)
				if(menu.getItem(i).hasSubMenu())
					setMenuVisible(menu.getItem(i).getSubMenu(), visible, ids);
	}
	public static void setMenuShowAsAction(Menu menu, int show, int... ids)
	{
		for(int id : ids)
			if(menu.findItem(id) != null)
				menu.findItem(id).setShowAsAction(show);
	}
	
	public static void setMenuEnabled(Menu menu, boolean enabled, int... ids)
	{
		for(int id : ids)
			if(menu.findItem(id) != null)
				menu.findItem(id).setEnabled(enabled);
	}
	
}
