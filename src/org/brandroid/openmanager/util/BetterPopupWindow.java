package org.brandroid.openmanager.util;

import java.lang.ref.SoftReference;

import org.brandroid.openmanager.R;
import org.brandroid.openmanager.activities.OpenExplorer;
import org.brandroid.utils.Logger;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewParent;
import android.view.WindowManager.BadTokenException;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;


/**
 * This class does most of the work of wrapping the {@link PopupWindow} so it's simpler to use.
 * 
 * @author qberticus
 * 
 */
public class BetterPopupWindow {
	private Context mContext;
	protected View anchor;
	private final PopupWindow popup;
	private View root;
	private Drawable background = null;
	private final WindowManager windowManager;
	private View backgroundView;
	private int anchorOffset = 0;
	public boolean USE_INDICATOR = true;


	/**
	 * Create a BetterPopupWindow
	 * 
	 * @param anchor
	 *		the view that the BetterPopupWindow will be displaying 'from'
	 */
	public BetterPopupWindow(Context mContext, View anchor) {
		this.mContext = mContext;
		this.anchor = anchor;
		if(anchor != null && anchor.findViewById(R.id.content_icon) != null)
			anchor = anchor.findViewById(R.id.content_icon);
		if(anchor != null && anchor.findViewById(android.R.id.icon) != null)
			anchor = anchor.findViewById(android.R.id.icon);
		this.popup = new PopupWindow(mContext);
		//this.popup.setAnimationStyle(anim);
		//this.popup.setWidth(mContext.getResources().getDimensionPixelSize(R.dimen.popup_width));


		// when a touch even happens outside of the window
		// make the window go away
		this.popup.setTouchInterceptor(new OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				if(event.getAction() == MotionEvent.ACTION_OUTSIDE) {
						BetterPopupWindow.this.popup.dismiss();
						return true;
				}
				return false;
			}
		});


		this.windowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
		onCreate();
	}
	
	public void setPopupWidth(int w)
	{
		popup.setWidth(w);
	}


	/**
	 * Anything you want to have happen when created. Probably should create a view and setup the event listeners on
	 * child views.
	 */
	protected void onCreate() {

		//backgroundView.addView(root);
	}


	/**
	 * In case there is stuff to do right before displaying.
	 */
	protected void onShow() {}


	private void preShow(int xPos, int yPos) {
		if(this.root == null) {
				throw new IllegalStateException("setContentView was not called with a view to display.");
		}
		onShow();

		this.popup.setBackgroundDrawable(new BitmapDrawable());
		
		// if using PopupWindow#setBackgroundDrawable this is the only values of the width and hight that make it work
		// otherwise you need to set the background of the root viewgroup
		// and set the popupwindow background to an empty BitmapDrawable
		int layout = yPos + (anchor != null ? anchor.getHeight() / 2 : 0) > getWindowRect().centerY() ?
				R.layout.context_bottom : R.layout.contextmenu_layout;
		
		if(backgroundView == null)
		{
			
			backgroundView = ((LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
					.inflate(layout, null);
			if(this.root.getParent() == null)
				((ViewGroup)backgroundView.findViewById(android.R.id.widget_frame)).addView(this.root);
		}
		this.popup.setContentView(backgroundView);
		
		this.popup.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
		//this.popup.setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
		this.popup.setTouchable(true);
		this.popup.setFocusable(true);
		this.popup.setOutsideTouchable(true);
	}
	
	private void placeArrow(int arrowOffset, int rootWidth)
	{
		View indicator = backgroundView.findViewById(R.id.indicator);
		if(!USE_INDICATOR && indicator != null)
		{
			indicator.setVisibility(View.GONE);
			return;
		}
		int arrowWidth = 30;
		if(indicator != null)
			arrowWidth = ((ImageView)indicator).getDrawable().getIntrinsicWidth() * 2;
		//indicator.setMinimumHeight(((ImageView)indicator).getDrawable().getIntrinsicHeight());
		//indicator.setMinimumWidth(((ImageView)indicator).getDrawable().getIntrinsicWidth());
		
		//indicator.measure(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		//int arrowWidth = indicator.getMeasuredWidth();
		//if(arrowOffset > 0)
		//	arrowOffset = (rootWidth / 2) - (arrowWidth / 2);
		int pos1 = arrowOffset;
		if(rootWidth > 0)
			pos1 = Math.min(rootWidth - arrowWidth, Math.max(0, arrowOffset));
		int pos2 = Math.min(rootWidth - arrowWidth, (rootWidth + arrowOffset) - arrowWidth);
		Logger.LogVerbose("Arrow (offset, width, arrow -> pos1 / pos2): " + arrowOffset + ", " + rootWidth + ", " + arrowWidth + " -> " + pos1 + " / " + pos2);
		if(arrowOffset >= 0)
		{
			//indicator.setLeft(arrowOffset);
			setViewWidth(backgroundView.findViewById(R.id.space_left), pos1);
			//setViewWidth(backgroundView.findViewById(R.id.space_right), LayoutParams.FILL_PARENT);
		} else {
			//arrowOffset *= -1;
			
			//setViewWidth(backgroundView.findViewById(R.id.space_left), rootWidth);
			setViewWidth(backgroundView.findViewById(R.id.space_left), pos2);
		}
		indicator.setVisibility(View.VISIBLE);
		
	}
	

	public void setBackgroundDrawable(Drawable background) {
		this.background = background;
	}

	public BetterPopupWindow setContentView(View root) {
		this.root = root;
		this.popup.setContentView(root);
		return this;
	}

	public void setContentView(int layoutResID) {
		LayoutInflater inflator =
					(LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.setContentView(inflator.inflate(layoutResID, null));
	}

	public void setOnDismissListener(PopupWindow.OnDismissListener listener) {
		this.popup.setOnDismissListener(listener);
	}

	public boolean showLikePopDownMenu() {
		return showLikePopDownMenu(0,0);
	}

	public boolean showLikePopDownMenu(int xOffset, int yOffset) {
		this.preShow((anchor != null ? getAbsoluteLeft(anchor) : 0) + xOffset,
				(anchor != null ? getAbsoluteTop(anchor) : 0) + yOffset);

		if(anchor == null)
		{
			Logger.LogWarning("Anchor is null");
			xOffset = (windowManager.getDefaultDisplay().getWidth() / 2) - (popup.getWidth() / 2);
			if(popup.getHeight() > 0)
				yOffset = (windowManager.getDefaultDisplay().getHeight() / 2) + (popup.getHeight() / 2);
			else
				yOffset = 100;
			//placeArrow(0, popup.getWidth());
			setViewWidth(backgroundView.findViewById(R.id.space_left), 0);
			setViewWidth(backgroundView.findViewById(R.id.space_right), LayoutParams.FILL_PARENT);
			backgroundView.findViewById(R.id.indicator).setVisibility(View.GONE);
			popup.showAtLocation(null, Gravity.CENTER, xOffset, yOffset);
			//popup.showAsDropDown(new View(mContext), xOffset, yOffset);
		} else {
			if(anchor.findViewById(R.id.content_icon) != null && anchor.getWidth() > getWindowWidth() / 2)
				anchor = anchor.findViewById(R.id.content_icon);
			int windowWidth = getWindowWidth(),
				availWidth = getAvailableWidth(),
				availHeight = getAvailableHeight(),
				widgetWidth = availWidth,
				widgetHeight = availHeight,
				rootWidth = availWidth,
				rootHeight = availHeight,
				bgWidth = availWidth,
				bgHeight = availHeight,
				arrowOffset = 0;
			ViewGroup widget = (ViewGroup) backgroundView.findViewById(android.R.id.widget_frame);
			if(widget != null)
			{
				try {
				widget.measure(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
				widgetWidth = widget.getMeasuredWidth();
				widgetHeight = widget.getMeasuredHeight();
				} catch(Exception e) {
					Logger.LogError("Error while measuring widget", e);
				}
			}
			//if(popup.getMaxAvailableHeight(anchor) < widgetHeight)
			//	popup.setHeight(widgetHeight + 20);
			try {
				root.measure(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				rootWidth = root.getMeasuredWidth();
				rootHeight = root.getMeasuredHeight();
				if(rootWidth > getWindowWidth())
					rootWidth = widgetWidth;
				if(rootHeight > getWindowHeight())
					rootHeight = widgetHeight;
			} catch(Exception e) {
				Logger.LogError("Error measuring root", e);
			}
			
			try {
				backgroundView.measure(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				bgWidth = backgroundView.getMeasuredWidth();
				bgHeight = backgroundView.getMeasuredHeight();
			} catch(Exception e) {
				Logger.LogError("Error measuring background", e);
			}
			
			int ancLeft = getAbsoluteLeft(anchor),
				ancTop = getAbsoluteTop(anchor);
			
			int padLeft = 0;
			if(backgroundView.findViewById(R.id.indicator_line) != null)
				padLeft = backgroundView.findViewById(R.id.indicator_line).getLeft();

			//anchor.getLocationOnScreen(anchorPos);
			boolean fromRight = ancLeft + xOffset > getWindowRect().centerX();
			boolean fromBottom = ancTop + yOffset > getWindowRect().centerY();
			//if(!fromBottom && pop)
			int spaceVertical = fromBottom ? ancTop : getWindowHeight() - (ancTop + anchor.getHeight()); 
			int spaceHorizontal = fromRight ? ancLeft : getWindowWidth() - (ancLeft + anchor.getWidth());
			
			if(popup.getWidth() == 0)
				popup.setWidth(mContext.getResources().getDimensionPixelSize(R.dimen.popup_width));
	
			int popWidth = this.popup.getWidth(); //- (mContext.getResources().getDimensionPixelSize(R.dimen.popup_width) / 3);
			int popLeft = ancLeft;

			if(fromRight)
			{
				popup.setAnimationStyle(fromBottom ? R.style.Animations_GrowFromBottomRight : R.style.Animations_GrowFromTopRight);
			} else {
				popup.setAnimationStyle(fromBottom ? R.style.Animations_GrowFromBottomLeft : R.style.Animations_GrowFromTopLeft);
			}
			//if(fromBottom)
			//	popup.setHeight(popup.getMaxAvailableHeight(anchor));
			
			if(spaceVertical < bgHeight)
			{
				popup.setHeight(bgHeight);
			}
			if((ancLeft == 0 && ancTop > 0) || (spaceHorizontal > spaceVertical * 1.2f && spaceVertical < bgHeight * 2)) // Go Horizontal
			{
				popup.setHeight(getAvailableHeight());
				int gravity = Gravity.TOP | Gravity.LEFT;
				/*fromRight ? Gravity.RIGHT : Gravity.LEFT;
				if(fromBottom)
					gravity |= Gravity.TOP;
				else {
					gravity |= Gravity.BOTTOM;
					yOffset = getWindowHeight();
				}*/
				xOffset = fromRight ? ancLeft - popup.getWidth() : ancLeft + anchor.getWidth();
				yOffset = 0;
				//if(ancTop > getWindowHeight() * (3f / 4f))
					//popup.setHeight((int) (getWindowHeight() * (9f / 10f)));
				if(backgroundView.findViewById(R.id.indicator) != null)
					backgroundView.findViewById(R.id.indicator).setVisibility(View.GONE);
				anchor = getAnchorRoot();
				ancLeft = getAbsoluteLeft(anchor);
				ancTop = getAbsoluteTop(anchor);
				yOffset = getWindowHeight() - popup.getHeight();
				Logger.LogInfo("Switching to absolute popup! " +
						"anch=" + ancLeft + "," + ancTop + "-" + anchor.getWidth() + "x" + anchor.getHeight() + "/" +
						"win=" + getWindowWidth() + "x" + getWindowHeight() + "/" +
						"off=" + xOffset + "," + yOffset + "/" +
						"pop=" + popup.getWidth() + "x" + popup.getHeight());
				backgroundView.measure(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				
				popup.showAtLocation(anchor, gravity, xOffset, yOffset);
				return true;
			}
			
			if(!popup.isAboveAnchor() && bgHeight + ancTop + anchor.getHeight() > getWindowHeight())
			{
				int newHeight = getWindowHeight() - (ancTop + anchor.getHeight());
				newHeight = Math.max(newHeight, popup.getMaxAvailableHeight(anchor));
				Logger.LogInfo("Need to increase height (" + newHeight + ")");
				if(newHeight > bgHeight)
					popup.setHeight(newHeight);
			}

			Logger.LogVerbose("Widths: " +
						"space(x,y)=" + spaceHorizontal + "," + spaceVertical + "/" +
						"anch=" + ancLeft + "," + ancTop + "-" + anchor.getWidth() + "x" + anchor.getHeight() + "/" +
						"win=" + getWindowWidth() + "x" + getWindowHeight() + "/" +
						"off=" + xOffset + "," + yOffset + "/" +
						"root=" + rootWidth + "x" + rootHeight + "/" +
						"bg=" + bgWidth + "x" + bgHeight + "," + padLeft + "/" +
						"pop=" + popup.getWidth() + "x" + popup.getHeight() + ":" + popup.getMaxAvailableHeight(anchor) + "/" +
						"widg=" + widgetWidth + "x" + widgetHeight
						//backgroundView.findViewById(R.id.indicator).getWidth() + "/" +
						);
			
			float dp = mContext.getResources().getDimension(R.dimen.one_dp);
			
			//anchor.measure(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
			//arrowOffset = anchor.getMeasuredWidth() / 2;
			

			if(popLeft + popWidth > getWindowWidth())
			{
				popLeft = getWindowWidth() - popWidth;
				arrowOffset = (ancLeft + (anchor.getWidth() / 2)) - popLeft;
			} else arrowOffset = Math.min(anchor.getWidth() / 2, popWidth / 2);
			
			arrowOffset -= (int)(16 * mContext.getResources().getDimension(R.dimen.one_dp));
			
			//arrowOffset = ancLeft - popLeft;
			//if(ancLeft > anchor.getWidth() / 2 && anchor.getWidth() < getWindowRect().centerX())
			//	arrowOffset += anchor.getWidth() / 2;
			
			//Logger.LogDebug("Some More: pop(w/l)=(" + popWidth + "/" + popLeft + "),arr=" + arrowOffset);
			
			placeArrow(arrowOffset, popWidth);
			
			/*
			if(this.anchor.getY() > windowManager.getDefaultDisplay().getHeight() / 2)
				popup.showAtLocation(anchor, Gravity.BOTTOM, xOffset, yOffset);
			else
				*/
			this.popup.showAsDropDown(this.anchor, xOffset, yOffset);
			//popup.getAnchorRoot().measure(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			//Logger.LogVerbose("New popup size: " + popup.getAnchorRoot().getWidth() + "x" + popup.getAnchorRoot().getMeasuredHeight());
			
			//float dp = mContext.getResources().getDimension(R.dimen.one_dp);
			
			//*/
		}
		return true;
	}
	
	private int getAbsoluteTop(View v) {
		try {
			int[] ret = new int[2];
			v.getLocationInWindow(ret);
			return ret[1];
		} catch(Exception e) {
			int ret = v.getTop();
			if(v.getParent() != null && v.getParent() instanceof View)
				ret += getAbsoluteTop((View)v.getParent());
			return ret;
		}
	}

	private int getAbsoluteLeft(View v) {
		try {
			int[] ret = new int[2];
			v.getLocationOnScreen(ret);
			return ret[0];
		} catch(Exception e) {
			int ret = v.getLeft();
			if(v.getParent() != null && v.getParent() instanceof View)
				ret += getAbsoluteLeft((View)v.getParent());
			return ret;
		}
	}

	public View getContentView()
	{
		return root;
	}
	
	public void setAnchorOffset(int amount) { anchorOffset = amount; } 

	public View getAnchorRoot()
	{
		if(anchor == null) return null;
		View root = anchor;
		while(root != null && root.getParent() != null)
		{
			if(root.getParent() != null && root.getParent() instanceof View)
				root = (View)root.getParent();
			else break;
			if(root.getId() == R.id.content_frag) break;
			//if(GridView.class.equals(root.getClass())) break;
			if(root.getId() == R.id.view_root) break;
		}
		return root;
	}
	private Rect getWindowRect()
	{
		Rect ret = new Rect();
		//if(anchor == null)
		{
			if(Build.VERSION.SDK_INT >= 13)
				windowManager.getDefaultDisplay().getRectSize(ret);
			else
			{
				DisplayMetrics m = new DisplayMetrics();
				windowManager.getDefaultDisplay().getMetrics(m);
				ret = new Rect(0, 0, m.widthPixels, m.heightPixels);
			}
		} //else if(getAnchorRoot() != null)
		//	getAnchorRoot().getDrawingRect(ret);
		return ret;
	}
	private int getAvailableWidth()
	{
		if(anchor == null)
			return getWindowWidth();
		try {
			View root = getAnchorRoot();
			//root.measure(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			return root.getWidth();
		} catch(Exception e) { return getWindowWidth(); }
	}
	private int getAvailableHeight()
	{
		if(anchor == null)
			return getWindowHeight();
		try {
			View root = getAnchorRoot();
			//root.measure(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			return root.getHeight();
		} catch(Exception e) { return getWindowHeight(); }
	}
	private int getWindowWidth()
	{
		return getWindowRect().width();
	}

	private int getWindowHeight()
	{
		return getWindowRect().height();
	}

	public void showLikeQuickAction(int arrowOffset) {
		showLikeQuickAction(0, 0, arrowOffset);
	}

	/**
	 * Displays like a QuickAction from the anchor view.
	 */
	public void showLikeQuickAction() {
		this.showLikeQuickAction(0, 0, anchor != null ? getAbsoluteLeft(anchor) : 20);
	}
	private View setViewWidth(View v, int w)
	{
		LayoutParams lp = v.getLayoutParams();
		lp.width = w;
		v.setLayoutParams(lp);
		return v;
	}


	/**
	 * Displays like a QuickAction from the anchor view.
	 * 
	 * @param xOffset
	 *		offset in the X direction
	 * @param yOffset
	 *		offset in the Y direction
	 */
	public void showLikeQuickAction(int xOffset, int yOffset, int arrowOffset) {
		
		this.preShow(anchor != null ? anchor.getLeft() : xOffset,
				anchor != null ? anchor.getTop() : yOffset);
		
		int[] location = new int[2];
		if(this.anchor == null)
		{
			showLikePopDownMenu(xOffset, yOffset);
			return;
		}
		this.anchor.getLocationOnScreen(location);


		Rect anchorRect =
					new Rect(location[0], location[1], location[0] + this.anchor.getWidth(), location[1]
						+ this.anchor.getHeight());


		this.backgroundView.measure(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

		int screenWidth = this.windowManager.getDefaultDisplay().getWidth();
		int screenHeight = this.windowManager.getDefaultDisplay().getHeight();

		int rootWidth = Math.min(this.backgroundView.getMeasuredWidth(), screenWidth);
		int rootHeight = Math.min(this.backgroundView.getMeasuredHeight(), screenHeight);
		
		int xPos = ((screenWidth - rootWidth) / 2) + xOffset;
		int yPos = anchorRect.top - rootHeight + yOffset;

		placeArrow(arrowOffset, rootWidth);

		// display on bottom
		if(rootHeight > anchorRect.top) {
				yPos = anchorRect.bottom + yOffset;
				//this.window.setAnimationStyle(anim == R.style.Animations_GrowFromBottom ? R.style.Animations_GrowFromTop : anim);
		}

		int grav = Gravity.NO_GRAVITY; 
		if(anchorRect.top > windowManager.getDefaultDisplay().getHeight() / 2)
			grav = Gravity.BOTTOM;
		
		Logger.LogDebug("Showing Popup @ " + xPos + "," + yPos + " root:" + rootWidth + "x" + rootHeight + " screen:" + screenWidth + "x" + screenHeight + " anchor:" + anchorRect.toString());

		//popup.showAsDropDown(this.anchor, xPos, yPos);
		this.popup.showAtLocation(this.anchor, grav, xPos, yPos);
	}


	public void dismiss() {
		this.popup.dismiss();
	}


	public void setTitle(CharSequence title) {
		if(backgroundView != null && backgroundView.findViewById(R.id.contextmenu_title) != null)
		{
			if(title != null && title.length() > 0)
			{
				backgroundView.findViewById(R.id.contextmenu_title).setVisibility(View.VISIBLE);
				((TextView)backgroundView.findViewById(android.R.id.title)).setText(title);
			} else backgroundView.findViewById(R.id.contextmenu_title).setVisibility(View.GONE);
		}
	}


	public void setTitle(int stringId) {
		if(backgroundView != null && backgroundView.findViewById(R.id.contextmenu_title) != null)
		{
			backgroundView.findViewById(R.id.contextmenu_title).setVisibility(View.VISIBLE);
			((TextView)backgroundView.findViewById(android.R.id.title)).setText(stringId);
			backgroundView.invalidate();
		}
	}

	public synchronized void setAnchor(View view) {
		anchor = view;
	}
}