package org.brandroid.openmanager.data;

import java.util.ArrayList;

public class OpenClipboard extends ArrayList<OpenPath> {
	private static final long serialVersionUID = 8847538312028343319L;
	public boolean DeleteSource = false;
	public boolean ClearAfter = true;
	
	public OpenClipboard()
	{
		super();
	}
}
