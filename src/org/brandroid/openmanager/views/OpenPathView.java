package org.brandroid.openmanager.views;

import org.brandroid.openmanager.data.OpenPath;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class OpenPathView extends LinearLayout {
	
	private OpenPath mFile;

	public OpenPathView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}

	public OpenPathView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	public OpenPathView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
	}

	public String getIdentifer() {
		return mFile.getAbsolutePath();
	}

	public void associateFile(OpenPath file) {
		mFile = file;
	}

}
