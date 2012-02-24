package org.brandroid.openmanager.util;

import java.lang.ref.SoftReference;

import org.brandroid.openmanager.R;
import org.brandroid.utils.Logger;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
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
		this.popup.setWidth(mContext.getResources().getDimensionPixelSize(R.dimen.popup_width));


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
		int layout = yPos > getContentRect().centerY() ?
				R.layout.context_bottom : R.layout.contextmenu_layout;
		
		if(backgroundView == null)
		{
			backgroundView = ((LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
					.inflate(layout, null);
			((ViewGroup)backgroundView.findViewById(android.R.id.widget_frame)).addView(this.root);
		}
		this.popup.setContentView(backgroundView);
		
		this.popup.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
		this.popup.setTouchable(true);
		this.popup.setFocusable(true);
		this.popup.setOutsideTouchable(true);
	}
	
	private void placeArrow(int arrowOffset, int rootWidth)
	{
		View indicator = backgroundView.findViewById(R.id.indicator);
		//indicator.setMinimumHeight(((ImageView)indicator).getDrawable().getIntrinsicHeight());
		//indicator.setMinimumWidth(((ImageView)indicator).getDrawable().getIntrinsicWidth());
		int arrowWidth = ((ImageView)indicator).getDrawable().getIntrinsicWidth();
		//indicator.measure(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		//int arrowWidth = indicator.getMeasuredWidth();
		int pos1 = Math.min(rootWidth - arrowWidth, Math.max(0, arrowOffset % rootWidth));
		int pos2 = Math.min(rootWidth - arrowWidth, Math.max(0, (rootWidth + arrowOffset) % rootWidth));
		Logger.LogVerbose("Arrow: " + arrowOffset + ", " + rootWidth + " -> " + pos1 + " / " + pos2);
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

	public void showLikePopDownMenu() {
		this.showLikePopDownMenu(0,0);
	}

	public void showLikePopDownMenu(int xOffset, int yOffset) {
		this.preShow((anchor != null ? anchor.getLeft() : 0) + xOffset,
				(anchor != null ? anchor.getTop() : 0) + yOffset);

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
			popup.showAsDropDown(new View(mContext), xOffset, yOffset);
		} else {
			int arrowOffset = 20;
			int windowWidth = getWindowWidth(),
				contentWidth = getContentWidth(),
				contentHeight = getContentHeight(),
				widgetWidth = contentWidth,
				widgetHeight = contentHeight,
				rootWidth = contentWidth,
				rootHeight = contentHeight,
				bgWidth = contentWidth,
				bgHeight = contentHeight;
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
			if(popup.getMaxAvailableHeight(anchor) < widgetHeight)
				popup.setHeight(widgetHeight + 20);
			try {
				root.measure(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				rootWidth = root.getMeasuredWidth();
				rootHeight = root.getMeasuredHeight();
				if(rootWidth > getWindowWidth())
					rootWidth = widgetWidth;
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
			
			int ancLeft = anchor.getLeft(),
				ancTop = anchor.getTop();

			Logger.LogVerbose("Widths: " +
						"root=" + rootWidth + "x" + rootHeight + "/" +
						"bg=" + bgWidth + "x" + bgHeight + "/" +
						"pop=" + popup.getWidth() + "x" + popup.getHeight() + ":" + popup.getMaxAvailableHeight(anchor) + "/" +
						"widg=" + widgetWidth + "x" + widgetHeight + "/" +
						"cont=" + getContentRect() + "/" +
						//backgroundView.findViewById(R.id.indicator).getWidth() + "/" +
						"anch=" + ancLeft + "," + ancTop + "/" +
						"off=" + xOffset + "," + yOffset
						);
			
			float dp = mContext.getResources().getDimension(R.dimen.one_dp);
			
			//anchor.measure(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
			//arrowOffset = anchor.getMeasuredWidth() / 2;
			arrowOffset = 0;

			//anchor.getLocationOnScreen(anchorPos);
			boolean fromRight = anchor.getLeft() > getContentRect().centerX();
			boolean fromBottom = anchor.getTop() > getContentRect().centerY();
			//if(!fromBottom && pop)

			if(fromBottom)
				popup.setHeight(
						popup.getMaxAvailableHeight(anchor)
						//(R.dimen.large_text_height + (8 * dp)) * 
						//mContext.getResources().getDimensionPixelSize(R.dimen.popup_height)
						);
				//yOffset -= mContext.getResources().getDimensionPixelSize(R.dimen.popup_width);
			if(fromRight)
			{
				popup.setAnimationStyle(fromBottom ? R.style.Animations_GrowFromBottomRight : R.style.Animations_GrowFromTopRight);
				xOffset -= mContext.getResources().getDimensionPixelSize(R.dimen.popup_width);
				xOffset += anchor.getWidth();
				arrowOffset *= -1;
				if(anchor.getWidth() > 50 && anchor.getWidth() < windowWidth / 2)
					arrowOffset -= anchor.getWidth() / 3;
				//arrowOffset = -1 * (anchor.getWidth() / 2);
				//arrowOffset += 10;
			} else {
				if(anchor.getWidth() > 50 && anchor.getWidth() < windowWidth / 2)
				{
					arrowOffset = anchor.getWidth() / 2;
					xOffset -= anchor.getWidth() / 4;
				}
				else if(anchor.getWidth() > 20)
					arrowOffset += 20;
				popup.setAnimationStyle(fromBottom ? R.style.Animations_GrowFromBottomLeft : R.style.Animations_GrowFromTopLeft);
			}
			
			arrowOffset += anchorOffset;
	
			
			//int rootWidth = this.popup.getWidth(); //- (mContext.getResources().getDimensionPixelSize(R.dimen.popup_width) / 3);
			placeArrow(arrowOffset, popup.getWidth());
			

			if(fromBottom)
			{
				//backgroundView.findViewById(R.id.indicator).setVisibility(View.GONE);
				//backgroundView.setBackgroundDrawable(R.drawable.contextmenu_bottom);
				//yOffset += anchor.getHeight();
			}
			
			/*
			if(this.anchor.getY() > windowManager.getDefaultDisplay().getHeight() / 2)
				popup.showAtLocation(anchor, Gravity.BOTTOM, xOffset, yOffset);
			else
				*/
			this.popup.showAsDropDown(this.anchor, xOffset, yOffset);
			//popup.getContentView().measure(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			//Logger.LogVerbose("New popup size: " + popup.getContentView().getWidth() + "x" + popup.getContentView().getMeasuredHeight());
			
			//float dp = mContext.getResources().getDimension(R.dimen.one_dp);
			
			//*/
		}
	}
	
	public void setAnchorOffset(int amount) { anchorOffset = amount; } 

	public View getContentView()
	{
		if(anchor == null) return null;
		View root = anchor;
		while(root != null && root.getParent() != null)
		{
			if(root.getParent() != null && root.getParent() instanceof View)
				root = (View)root.getParent();
			else break;
			if(root.getId() == R.id.content_frag) break;
			if(GridView.class.equals(root.getClass())) break;
			if(root.getId() == R.id.view_root) break;
		}
		return root;
	}
	private Rect getContentRect()
	{
		Rect ret = new Rect();
		if(anchor == null)
		{
			if(Build.VERSION.SDK_INT >= 13)
				windowManager.getDefaultDisplay().getRectSize(ret);
			else
				ret = new Rect(0, 0, windowManager.getDefaultDisplay().getWidth(), windowManager.getDefaultDisplay().getHeight());
		} else if(getContentView() != null)
			getContentView().getDrawingRect(ret);
		return ret;
	}
	private int getContentWidth()
	{
		if(anchor == null)
			return getWindowWidth();
		try {
			View root = getContentView();
			//root.measure(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			return root.getWidth();
		} catch(Exception e) { return getWindowWidth(); }
	}
	private int getContentHeight()
	{
		if(anchor == null)
			return getWindowHeight();
		try {
			View root = getContentView();
			//root.measure(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			return root.getHeight();
		} catch(Exception e) { return getWindowHeight(); }
	}
	private int getWindowWidth()
	{
		return windowManager.getDefaultDisplay().getWidth();
	}

	private int getWindowHeight()
	{
		return windowManager.getDefaultDisplay().getHeight();
	}

	public void showLikeQuickAction(int arrowOffset) {
		showLikeQuickAction(0, 0, arrowOffset);
	}

	/**
	 * Displays like a QuickAction from the anchor view.
	 */
	public void showLikeQuickAction() {
		this.showLikeQuickAction(0, 0, anchor != null ? anchor.getLeft() : 20);
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
		}
	}

	public synchronized void setAnchor(View view) {
		anchor = view;
	}
}