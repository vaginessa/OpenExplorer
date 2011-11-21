package org.brandroid.openmanager.util;

import android.os.AsyncTask;

public class ThumbnailTask extends AsyncTask<ThumbnailStruct, Void, ThumbnailStruct[]>
{
	private int iPending = 0;
	
	public ThumbnailTask() {
		
	}
	
	@Override
	protected ThumbnailStruct[] doInBackground(ThumbnailStruct... params) {
		ThumbnailStruct[] ret = new ThumbnailStruct[params.length];
		for(int i = 0; i < params.length; i++)
		{
			ret[i] = params[i];
			if(ret == null) continue;
			//Logger.LogDebug("Getting thumb for " + ret[i].File.getName());
			ret[i].setBitmap(ThumbnailCreator.generateThumb(ret[i].File, ret[i].Width, ret[i].Height));
		}
		return ret;
	}
	
	@Override
	protected void onPostExecute(ThumbnailStruct[] result) {
		super.onPostExecute(result);
		for(ThumbnailStruct t : result)
			t.updateHolder();
	}
	
}