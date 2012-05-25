package org.brandroid.openmanager.util;

import java.util.Hashtable;

import org.brandroid.openmanager.R;
import org.brandroid.openmanager.activities.OpenExplorer;
import org.brandroid.utils.Logger;
import org.brandroid.utils.ViewUtils;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.PopupWindow;
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
	private final WindowManager windowManager;
	private View backgroundView;
	private int anchorOffset = 0;
	private final int forcePadding = 0;
	public boolean USE_INDICATOR = true;
	private int mHeight = 0;
	private boolean forcedHeight = false;
	private static final boolean ALLOW_HORIZONTAL_MODE = false;
	private int layout;
	private Point exact = null;
	private boolean DEBUG = OpenExplorer.IS_DEBUG_BUILD && false;
	private OnKeyListener mKeyListener = null;
	private CharSequence mTitle = null;

	/**
	 * Create a BetterPopupWindow
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
		//popup.setClippingEnabled(true);
		//this.popup.setAnimationStyle(anim);
		//this.popup.setWidth(mContext.getResources().getDimensionPixelSize(R.dimen.popup_width));


		// when a touch even happens outside of the window
		// make the window go away
		this.popup.setTouchInterceptor(new OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				if(event.getAction() == MotionEvent.ACTION_OUTSIDE) {
						BetterPopupWindow.this.popup.dismiss();
						return true;
				} else if(event.getAction() == MotionEvent.ACTION_DOWN) {
					if(event.getY() < ViewUtils.getAbsoluteTop(getContentView()))
						BetterPopupWindow.this.popup.dismiss();
				}
				return false;
			}
		});

		this.windowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
		onCreate();
	}
	
	private OnPopupShownListener mShownListener;
	public interface OnPopupShownListener
	{
		public void OnPopupShown(int width, int height);
	}
	public void setPopupShownListener(OnPopupShownListener l) { mShownListener = l; }
	
	public int getPopupHeight()
	{
		if(popup.isShowing())
			return popup.getHeight();
		else return mHeight;
	}
	public void setPopupHeight(int h)
	{
		mHeight = h;
		popup.setHeight(h);
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
	protected void onShow() {
	}

	private void OnPopupShown(int w, int h)
	{
		//if(forcedHeight) return;
		if(mShownListener != null)
			mShownListener.OnPopupShown(w, h);
		if(Math.abs(popup.getHeight() - h) > 10 || popup.getHeight() >= getAvailableHeight() - mContext.getResources().getDimension(R.dimen.actionbar_compat_height))
		{
			if(exact != null && exact.y + h > getAvailableHeight())
				h -= Math.abs(getAvailableHeight() - exact.y) + 10;
			if(popup != null && popup.isShowing())
				popup.update(w, h);
		}
	}
	
	public void setOnKeyListener(OnKeyListener l) { mKeyListener = l; }
	
	private void preShow(int xPos, final int yPos) {
		if(this.root == null) {
				throw new IllegalStateException("setContentView was not called with a view to display.");
		}
		root.setOnKeyListener(mKeyListener);

		if(DEBUG)
			popup.setBackgroundDrawable(new ColorDrawable(mContext.getResources().getColor(R.color.translucent_gray)));
		else
			popup.setBackgroundDrawable(new BitmapDrawable());
				
		// if using PopupWindow#setBackgroundDrawable this is the only values of the width and hight that make it work
		// otherwise you need to set the background of the root viewgroup
		// and set the popupwindow background to an empty BitmapDrawable
		layout = yPos + (anchor != null ? anchor.getHeight() : 0) > getWindowRect().centerY() ?
				R.layout.context_bottom : R.layout.contextmenu_layout;
		
		if(anchor == null || (getAvailableHeight() > yPos + anchor.getHeight() + Math.max(mHeight, getPreferredMinHeight())))
			layout = R.layout.contextmenu_layout;
		else layout = R.layout.context_bottom;
		
		if(popup.isAboveAnchor())
			layout = R.layout.context_bottom;
		
		if(backgroundView == null)
		{
			
			backgroundView = ((LayoutInflater)mContext
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
					.inflate(layout, null);
			if(this.root.getParent() == null)
				((ViewGroup)backgroundView
					.findViewById(android.R.id.widget_frame))
					.addView(this.root);
			if(mTitle != null && mTitle.length() > 0)
				setTitle(mTitle);

			root.postDelayed(new Runnable(){public void run() {
				int h = root.getHeight();
				int w = popup.getWidth();
				int left = root.getLeft();
				int top = root.getTop();
				if(getArrow() != null)
					h += getArrow().getHeight();
				if(backgroundView.getPaddingTop() > 0)
					h += backgroundView.getPaddingTop();
				if(backgroundView.getPaddingBottom() > 0)
					h += backgroundView.getPaddingBottom();
				h += 4;
				//backgroundView.measure(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
				h = Math.max(h, backgroundView.getHeight());
				h = Math.max(getPreferredMinHeight(), h);
				int l = 0, t = 0;
				if(exact != null)
				{
					l = exact.x;
					t = exact.y;
				}
				if(mHeight > 0 && h <= mHeight) return;
				if(DEBUG)
					Logger.LogDebug("Popup Layout Change: (" + w + "x" + h + ":" + l + "," + t + ")@(" + left + "," + top + ")"); // from (" + (oldRight - oldLeft) + "x" + (oldBottom - oldTop) + ")@(" + oldLeft + "," + oldTop + ")");
				OnPopupShown(w, h);
			}}, mContext.getResources().getInteger(android.R.integer.config_mediumAnimTime));
		}
		this.popup.setContentView(backgroundView);
		
		//if(mHeight == 0) popup.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
		if(mHeight > 0)
			popup.setHeight(mHeight);
		else popup.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
		//this.popup.setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
		this.popup.setTouchable(true);
		this.popup.setFocusable(true);
		this.popup.setOutsideTouchable(true);
		
		onShow();
	}
	
	private int getPreferredMinHeight()
	{
		return Math.round(mContext.getResources().getDimension(R.dimen.one_dp) * (100));
	}
	
	private View getArrow()
	{
		View ret = null;
		if(backgroundView != null)
			ret = backgroundView.findViewById(R.id.indicator);
		return ret;
	}
	
	/**
	 * Place arrow in Popup.
	 * @param arrowOffset Requested X offset.
	 * @param rootWidth Container width.
	 * @return Percentage (0 to 1) of arrow position. Returns -1 if indicator is not used.
	 */
	private float placeArrow(int arrowOffset, int rootWidth)
	{
		View indicator = backgroundView.findViewById(R.id.indicator);
		if(indicator == null) return -1;
		if(!USE_INDICATOR && indicator != null)
		{
			indicator.setVisibility(View.GONE);
			return -1;
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
		//Logger.LogVerbose("Arrow (offset, width, arrow -> pos1 / pos2): " + arrowOffset + ", " + rootWidth + ", " + arrowWidth + " -> " + pos1 + " / " + pos2);
		indicator.setVisibility(View.VISIBLE);
		if(arrowOffset >= 0)
		{
			setViewWidth(backgroundView.findViewById(R.id.space_left), pos1);
			return (float)arrowOffset / (float)Math.min(1, rootWidth);
		} else {
			setViewWidth(backgroundView.findViewById(R.id.space_left), pos2);
			return (float)Math.abs(rootWidth + arrowOffset) / (float)Math.min(1, rootWidth);
		}
	}
	
	public BetterPopupWindow setContentView(View root) {
		//Logger.LogDebug("BetterPopupWindow.setContentView(" + root.toString() + ")");
		this.root = root;
		this.popup.setContentView(root);
		return this;
	}

	public void setContentView(int layoutResID) {
		this.setContentView(((LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
				.inflate(layoutResID, null));
	}

	public void setOnDismissListener(PopupWindow.OnDismissListener listener) {
		this.popup.setOnDismissListener(listener);
	}

	public boolean showLikePopDownMenu() {
		return showLikePopDownMenu(0,0);
	}

	public boolean showLikePopDownMenu(int xOffset, int yOffset) {
		this.preShow((anchor != null ? ViewUtils.getAbsoluteLeft(anchor) : 0) + xOffset,
				(anchor != null ? ViewUtils.getAbsoluteTop(anchor) : 0) + yOffset);

		if(anchor == null)
		{
			return false;
			/*
			Logger.LogWarning("Anchor is null");
			xOffset = (getWindowWidth() / 2) - (popup.getWidth() / 2);
			if(popup.getHeight() > 0)
				yOffset = (getWindowHeight() / 2) + (popup.getHeight() / 2);
			else
				yOffset = 100;
			//placeArrow(0, popup.getWidth());
			setViewWidth(backgroundView.findViewById(R.id.space_left), 0);
			setViewWidth(backgroundView.findViewById(R.id.space_right), LayoutParams.FILL_PARENT);
			backgroundView.findViewById(R.id.indicator).setVisibility(View.GONE);
			popup.showAtLocation(null, Gravity.CENTER, xOffset, yOffset);
			*/
			//popup.showAsDropDown(new View(mContext), xOffset, yOffset);
		} else {
			if(anchor.findViewById(R.id.content_icon) != null)
				anchor = anchor.findViewById(R.id.content_icon);
			int windowWidth = getWindowWidth(),
				availWidth = getAvailableWidth(),
				availHeight = getAvailableHeight(),
				widgetWidth = availWidth,
				widgetHeight = availHeight,
				rootWidth = availWidth,
				rootHeight = availHeight,
				bgWidth = availWidth,
				bgHeight = mHeight > 0 ? mHeight : availHeight,
				arrowOffset = 0;
			ViewGroup widget = (ViewGroup) backgroundView.findViewById(android.R.id.widget_frame);
			if(widget != null)
			{
				try {
				widget.measure(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
				widgetWidth = widget.getMeasuredWidth();
				widgetHeight = widget.getMeasuredHeight();
				} catch(Exception e) {
					Logger.LogError("Error while measuring widget", e);
				}
			}
			//if(popup.getMaxAvailableHeight(anchor) < widgetHeight)
			//	popup.setHeight(widgetHeight + 20);
			try {
				root.measure(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
				rootWidth = root.getMeasuredWidth();
				rootHeight = root.getMeasuredHeight();
				if(rootWidth > getWindowWidth())
					rootWidth = widgetWidth;
				if(rootHeight > getWindowHeight())
					rootHeight = widgetHeight;
				root.measure(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			} catch(Exception e) {
				Logger.LogError("Error measuring root", e);
			}
			
			try {
				backgroundView.measure(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				bgWidth = backgroundView.getMeasuredWidth();
				if(mHeight == 0)
					bgHeight = backgroundView.getMeasuredHeight();
			} catch(Exception e) {
				Logger.LogError("Error measuring background", e);
			}
			
			int ancLeft = ViewUtils.getAbsoluteLeft(anchor),
				ancTop = ViewUtils.getAbsoluteTop(anchor);
			
			int padLeft = 0;
			if(backgroundView.findViewById(R.id.indicator_line) != null)
				padLeft = backgroundView.findViewById(R.id.indicator_line).getLeft();

			//anchor.getLocationOnScreen(anchorPos);
			boolean fromRight = ancLeft + xOffset > getWindowRect().centerX();
			boolean fromBottom = layout == R.layout.context_bottom; // ancTop + yOffset > getWindowRect().centerY();
			//if(!fromBottom && pop)
			int spaceVertical = fromBottom ? ancTop : getAvailableHeight() - (ancTop + anchor.getHeight()); 
			int spaceHorizontal = fromRight ? ancLeft : getAvailableWidth() - (ancLeft + anchor.getWidth());
			
			if(popup.getWidth() == 0)
				popup.setWidth(mContext.getResources().getDimensionPixelSize(R.dimen.popup_width));
	
			int popWidth = this.popup.getWidth(); //- (mContext.getResources().getDimensionPixelSize(R.dimen.popup_width) / 3);
			int popLeft = ancLeft;

			popup.setAnimationStyle(R.style.Animations_Fade);

			//if(fromBottom)
			//	popup.setHeight(popup.getMaxAvailableHeight(anchor));
			
			float dp = mContext.getResources().getDimension(R.dimen.one_dp);
			
			if(spaceVertical < bgHeight && popup.getHeight() < bgHeight && mHeight == 0)
			{
				forcedHeight = true;
				popup.setHeight(bgHeight);
			}
			else if(mHeight > 0) popup.setHeight(mHeight);
			
			if(ALLOW_HORIZONTAL_MODE &&
				getWindowHeight() < 700 && (
				(ancLeft == 0 && ancTop > 0
					&& ancTop < getAvailableHeight() - mContext.getResources().getDimension(R.dimen.actionbar_compat_height)
					)
				|| (getAvailableHeight() / dp < 600 &&
					spaceHorizontal > spaceVertical * 1.2f &&
					spaceVertical < bgHeight * 2))) // Go Horizontal
			{
				if(mHeight == 0)
				{
					forcedHeight = true;
					popup.setHeight(getAvailableHeight());
				} else if(mHeight < getPreferredMinHeight())
				{
					popup.setHeight(getPreferredMinHeight());
				}
				
				int gravity = (fromBottom ? Gravity.BOTTOM : Gravity.TOP) | Gravity.LEFT;
				/*fromRight ? Gravity.RIGHT : Gravity.LEFT;
				if(fromBottom)
					gravity |= Gravity.TOP;
				else {
					gravity |= Gravity.BOTTOM;
					yOffset = getWindowHeight();
				}*/
				xOffset = fromRight ? ancLeft - popup.getWidth() : ancLeft + anchor.getWidth();
				yOffset = forcePadding;
				if(!fromBottom)
					yOffset += ancTop;
				
				//if(ancTop > getWindowHeight() * (3f / 4f))
					//popup.setHeight((int) (getWindowHeight() * (9f / 10f)));
				if(backgroundView.findViewById(R.id.indicator) != null)
					backgroundView.findViewById(R.id.indicator).setVisibility(View.GONE);
				anchor = getAnchorRoot();
				ancLeft = ViewUtils.getAbsoluteLeft(anchor);
				ancTop = ViewUtils.getAbsoluteTop(anchor);
				//yOffset = getWindowHeight() - getAvailableHeight();
				if(DEBUG)
					Logger.LogInfo("Switching to absolute popup! " +
						"anch=" + ancLeft + "," + ancTop + "-" + anchor.getWidth() + "x" + anchor.getHeight() + "/" +
						"win=" + getWindowWidth() + "x" + getWindowHeight() + "/" +
						"avail=" + getAvailableWidth() + "x" + getAvailableHeight() + "/" +
						"off=" + xOffset + "," + yOffset + "/" +
						"root=" + rootWidth + "x" + rootHeight + "/" +
						"bg=" + bgWidth + "x" + bgHeight + "," + padLeft + "/" +
						"pop=" + popup.getWidth() + "x" + getPopupHeight());
				//backgroundView.measure(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				
				exact = new Point(ancLeft + xOffset, ancTop + yOffset); 
				popup.showAtLocation(anchor, gravity, xOffset, yOffset);
				return true;
			}
			
			if(!popup.isAboveAnchor() && bgHeight + ancTop + anchor.getHeight() > getWindowHeight())
			{
				int newHeight = getPreferredMinHeight();
				newHeight = Math.max(newHeight, getWindowHeight() - (ancTop + anchor.getHeight()));
				newHeight = Math.max(newHeight, popup.getMaxAvailableHeight(anchor));
				newHeight -= forcePadding;
				if(newHeight > bgHeight && mHeight == 0)
				{
					if(DEBUG)
						Logger.LogDebug("Need to increase height (" + newHeight + ")");
					forcedHeight = true;
					popup.setHeight(newHeight);
					
					//root.measure(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
					widget.measure(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
					widgetWidth = widget.getMeasuredWidth();
					widgetHeight = widget.getMeasuredHeight();
				}
			}

			if(DEBUG)
				Logger.LogInfo("Widths: " +
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
			
			
			//anchor.measure(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
			//arrowOffset = anchor.getMeasuredWidth() / 2;
			
			if(popLeft + popWidth > getWindowWidth())
			{
				popLeft = getWindowWidth() - popWidth;
				arrowOffset = (ancLeft + (anchor.getWidth() / 2)) - popLeft;
			} else arrowOffset = Math.min(anchor.getWidth() / 2, popWidth / 2);
			
			
			/*if(anchor != null && anchor.findViewById(R.id.content_icon) != null)
			{
				View icon = anchor.findViewById(R.id.content_icon);
				arrowOffset = 0;
				//arrowOffset += getOffsetLeft(icon);
				arrowOffset += icon.getWidth() / 2;
			} //else */
			
			arrowOffset -= (int)(16 * mContext.getResources().getDimension(R.dimen.one_dp));
			
			//arrowOffset = ancLeft - popLeft;
			//if(ancLeft > anchor.getWidth() / 2 && anchor.getWidth() < getWindowRect().centerX())
			//	arrowOffset += anchor.getWidth() / 2;
			
			//Logger.LogDebug("Some More: pop(w/l)=(" + popWidth + "/" + popLeft + "),arr=" + arrowOffset);
			
			float pos = placeArrow(arrowOffset, popWidth);
			if(pos == -1)
				popup.setAnimationStyle(R.style.Animations_Fade);
			else if(pos > 0.8f && pos <= 1.0f)
				popup.setAnimationStyle(fromBottom ? R.style.Animations_GrowFromBottomLeft : R.style.Animations_GrowFromTopLeft);
			else if(pos < 0.2f && pos >= 0.0f)
				popup.setAnimationStyle(fromBottom ? R.style.Animations_GrowFromBottomRight : R.style.Animations_GrowFromTopRight);
			else
				popup.setAnimationStyle(fromBottom ? R.style.Animations_GrowFromBottom : R.style.Animations_GrowFromTop);
			
			
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
			return getWindowHeight() - forcePadding;
		try {
			View root = getAnchorRoot();
			//root.measure(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			return (root.getHeight() > 0 ? root.getHeight() : getWindowHeight()) - forcePadding;
		} catch(Exception e) { return getWindowHeight() - forcePadding; }
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
		this.showLikeQuickAction(0, 0, anchor != null ? ViewUtils.getAbsoluteLeft(anchor) : 10);
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
		
		//Logger.LogDebug("Showing Popup @ " + xPos + "," + yPos + " root:" + rootWidth + "x" + rootHeight + " screen:" + screenWidth + "x" + screenHeight + " anchor:" + anchorRect.toString());

		//popup.showAsDropDown(this.anchor, xPos, yPos);
		this.popup.showAtLocation(this.anchor, grav, xPos, yPos);
	}


	public void dismiss() {
		popup.dismiss();
	}


	public void setTitle(CharSequence title) {
		mTitle = title;
		if(backgroundView != null)
		{
			ViewUtils.setViewsVisible(backgroundView, true, R.id.contextmenu_title);
			ViewUtils.setText(backgroundView, title, android.R.id.title);
		}
	}


	public synchronized void setAnchor(View view) {
		anchor = view;
	}
}