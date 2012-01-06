package org.brandroid.openmanager.util;

import org.brandroid.utils.Logger;

import android.content.Context;
import android.view.View;
import android.widget.ShareActionProvider;

public class SortActionProvider extends ShareActionProvider
{

	public SortActionProvider(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public View onCreateActionView() {
		Logger.LogDebug("SortActionProvider onCreate");
		return super.onCreateActionView();
	}

}
