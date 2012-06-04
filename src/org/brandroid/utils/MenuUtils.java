package org.brandroid.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

import org.brandroid.openmanager.activities.OpenExplorer;

import android.support.v4.view.MenuItemCompat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.TouchDelegate;
import android.widget.Button;
import android.widget.ImageButton;

public class MenuUtils {
	public static void transferMenu(Menu from, Menu to) { transferMenu(from, to, true); }
	public static void transferMenu(Menu from, Menu to, Boolean clearFrom) {
		if(from == null || to == null) return;
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
			.setAlphabeticShortcut(item.getAlphabeticShortcut())
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
		if(ids.length == 0)
			setMenuVisible(menu, visible, getMenuIDs(menu));
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
		if(OpenExplorer.BEFORE_HONEYCOMB) return;
		for(int id : ids)
			if(menu.findItem(id) != null)
			{
				MenuItem item = (MenuItem)menu.findItem(id);
				MenuItemCompat.setShowAsAction(item, show);
			}
		/*
		for(int id : ids)
			if(menu.findItem(id) != null)
			{
				MenuItem item = menu.findItem(id);
				item.setShowAsAction(show);
			}*/
	}
	
	public static int[] getMenuIDs(Menu menu)
	{
		int[] ret = new int[menu.size()];
		for(int i = 0; i < ret.length; i++)
			ret[i] = menu.getItem(i).getItemId();
		return ret;
	}
	
	public static void setMenuEnabled(Menu menu, boolean enabled, int... ids)
	{
		if(ids.length == 0)
			setMenuEnabled(menu, enabled, getMenuIDs(menu));
		for(int id : ids)
		{
			MenuItem item = menu.findItem(id);
			if(item == null) continue;
			item.setEnabled(enabled);
			if(enabled)
				item.setVisible(true);
		}
	}
	public static void mergeDuplicateMenus(Menu menu)
	{
		ArrayList<Integer> holder = new ArrayList<Integer>();
		//ArrayList<Integer> buffer = new ArrayList<int>();
		for(int i = menu.size() - 1; i >= 0; i--)
		{
			MenuItem item = menu.getItem(i);
			if(holder.contains(item.getItemId())) {
				
			}
		}
	}
	public static void fillSubMenus(int[] search, int[] replace, Menu menu, MenuInflater inflater)
	{
		if(search == null || replace == null || menu == null || inflater == null) return;
		for(int i = 0; i < menu.size(); i++)
		{
			MenuItem item = menu.getItem(i);
			try {
				int index = Utils.getArrayIndex(search, item.getItemId());
				if(index > -1 && item.getSubMenu() != null)
				{
					inflater.inflate(replace[index], item.getSubMenu());
					//Logger.LogDebug("Inflating 0x" + Integer.toHexString(replace[index]) + " to " + item.getTitle());
				}
			} catch(Exception e) { Logger.LogWarning("Couldn't fill submenu (0x" + Integer.toHexString(item.getItemId()) + ")"); }
		}
	}
	public static int countVisibleMenus(Menu menu) {
		int ret = 0;
		for(int i = 0; i < menu.size(); i++)
			if(menu.getItem(i).isVisible())
				ret++;
		return ret;
	}
	public static void hideMenuGrandChildren(MenuBuilder menu) {
		for(int i = 0; i < menu.size(); i++)
		{
			MenuItem item = menu.getItem(i);
			if(item.hasSubMenu() && item.getSubMenu() != null && item.getSubMenu().size() > 0)
			{
				item.setOnMenuItemClickListener(new OnMenuItemClickListener() {
					public boolean onMenuItemClick(MenuItem item) {
						return true;
					}
				});
				Menu sub = item.getSubMenu();
				sub.clear();
			}
		}
	}
	
}
