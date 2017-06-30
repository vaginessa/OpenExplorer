
package org.brandroid.openmanager.util;

import android.content.Context;
import android.view.ActionProvider;
import android.view.View;

import org.brandroid.utils.Logger;

public class SortActionProvider extends ActionProvider {

    public SortActionProvider(Context context) {
        super(context);
        // TODO Auto-generated constructor stub
    }

    @Override
    public View onCreateActionView() {
        Logger.LogDebug("SortActionProvider onCreate");
        // return super.onCreateActionView();
        return null;
    }

}
