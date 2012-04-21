package android.app.actionbarcompat;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.ActionBar.TabListener;
import android.graphics.drawable.Drawable;
import android.view.View;

@TargetApi(11)
public class ActionBarHelperTabHoneycomb extends android.app.ActionBar.Tab implements ActionBarHelperTab
{
	private ActionBar.Tab me;
	private static Boolean isInstantiated = false;
	
	public ActionBarHelperTabHoneycomb(ActionBarHelper bar)
	{
		ActionBar realBar = bar.mActivity.getActionBar();
		if(!isInstantiated)
		{
			realBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
			isInstantiated = true;
		}
		me = realBar.newTab();
	}
	
	@Override
	public CharSequence getContentDescription() {
		return me.getContentDescription();
	}

	@Override
	public View getCustomView() {
		return me.getCustomView();
	}

	@Override
	public Drawable getIcon() {
		return me.getIcon();
	}

	@Override
	public int getPosition() {
		return me.getPosition();
	}

	@Override
	public Object getTag() {
		return me.getTag();
	}

	@Override
	public CharSequence getText() {
		return me.getText();
	}

	@Override
	public void select() {
		me.select();
	}

	@Override
	public Tab setContentDescription(int resId) {
		me.setContentDescription(resId);
		return this;
	}

	@Override
	public Tab setContentDescription(CharSequence contentDesc) {
		me.setContentDescription(contentDesc);
		return this;
	}

	@Override
	public Tab setCustomView(View view) {
		me.setCustomView(view);
		return this;
	}

	@Override
	public Tab setCustomView(int layoutResId) {
		me.setCustomView(layoutResId);
		return this;
	}

	@Override
	public Tab setIcon(Drawable icon) {
		me.setIcon(icon);
		return this;
	}

	@Override
	public Tab setIcon(int resId) {
		me.setIcon(resId);
		return this;
	}

	@Override
	public Tab setTabListener(TabListener listener) {
		me.setTabListener(listener);
		return this;
	}

	@Override
	public Tab setTag(Object obj) {
		me.setTag(obj);
		return this;
	}

	@Override
	public Tab setText(CharSequence text) {
		me.setText(text);
		return this;
	}

	@Override
	public Tab setText(int resId) {
		me.setText(resId);
		return this;
	}

	public ActionBarHelperTab setText(String title) {
		me.setText(title);
		return this;
	}

}
