package org.brandroid.openmanager.util;

import org.brandroid.openmanager.OpenExplorer;
import org.brandroid.openmanager.data.OpenPath;
import org.brandroid.utils.Logger;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.widget.Toast;

public class IntentManager
{
	public static Intent getIntent(OpenPath file, OpenExplorer activity, EventHandler mHandler)
	{
		String name = file.getName();
		final String ext = name.substring(name.lastIndexOf(".") + 1).toLowerCase();
		
		Intent ret = new Intent();
		ret.setAction(Intent.ACTION_VIEW);
		//ret.set
		
		/*audio files*/
		
		if (ext.equalsIgnoreCase("mp3") || 
			ext.equalsIgnoreCase("m4a") ) {
    		
    		ret.setDataAndType(file.getUri(), "audio/*");
		}
		
		/* image files*/
		else if(ext.equalsIgnoreCase("jpeg") || 
    			ext.equalsIgnoreCase("jpg")  ||
    			ext.equalsIgnoreCase("png")  ||
    			ext.equalsIgnoreCase("gif")  || 
    			ext.equalsIgnoreCase("tiff")) {

			ret.setDataAndType(file.getUri(), "image/*");
    	}
		
		/*video file selected--add more video formats*/
    	else if(ext.equalsIgnoreCase("m4v") ||
    			ext.equalsIgnoreCase("mp4") ||
    			ext.equalsIgnoreCase("3gp") ||
    			ext.equalsIgnoreCase("wmv") || 
    			ext.equalsIgnoreCase("mp4") || 
    			ext.equalsIgnoreCase("avi") || 
    			ext.equalsIgnoreCase("ogg") ||
    			ext.equalsIgnoreCase("wav")) {
    		
			ret.setDataAndType(file.getUri(), "video/*");
    	}
		
		/*pdf file selected*/
    	else if(ext.equalsIgnoreCase("pdf")) {
    		
    		if(file.exists()) {
	    		ret.setDataAndType(file.getUri(), "application/pdf");
	    	}
    	}
		
		/*Android application file*/
    	else if(ext.equalsIgnoreCase("apk")){
    		
    		if(file.exists()) {
    			ret.setDataAndType(file.getUri(), 
    									 "application/vnd.android.package-archive");
    		}
    	}
		
		/* HTML XML file */
    	else if(ext.equalsIgnoreCase("html") || 
    			ext.equalsIgnoreCase("xml")) {
    		
    		if(file.exists()) {
    			ret.setDataAndType(file.getUri(), "text/html");
    		}
    	}
		
		/* ZIP files */
    	else if(ext.equalsIgnoreCase("zip")) {
    		//mHandler.unzipFile(file, );
    		return null;
    	}
		
		/* text file*/
    	else if(ext.equalsIgnoreCase("txt")) {
    		Boolean bUseIntent = false;
    		if(!bUseIntent)
    		{
    			activity.editFile(file);
    			return null;
    		} else {
    			ret.setDataAndType(file.getUri(), "text/plain");
    		}
    	}
		
		/* generic intent */
    	else {
    		ret.setDataAndType(file.getUri(), "application/*");
    	}
		return ret;
	}

	public static boolean startIntent(OpenPath file, OpenExplorer activity, EventHandler mHandler)
	{
		Intent intent = getIntent(file, activity, mHandler);
		//intent.addFlags(Intent.FL);
		if(intent != null)
		{
			try {
				activity.startActivity(intent);
			} catch(ActivityNotFoundException e) {
				Logger.LogWarning("Couldn't launch intent for " + file.getPath(), e);
				return false;
			}
			return true;
		}
		return false;
	}
}
