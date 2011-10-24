package android.app.actionbarcompat;

import android.app.ActionBar.TabListener;

public interface ActionBarHelperTab {

	public Object setText(String title);
	public Object setTabListener(TabListener tabListener);
	public void select();

}
