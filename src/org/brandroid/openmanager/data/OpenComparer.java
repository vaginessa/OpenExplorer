package org.brandroid.openmanager.data;

import java.util.Comparator;

import org.brandroid.openmanager.FileManager.SortType;

public class OpenComparer implements Comparator<OpenFace>
{
	public Boolean FoldersFirst = true;
	private SortType SortType;
	
	public OpenComparer()
	{
		SortType = SortType.ALPHA;
	}
	
	public OpenComparer(SortType type)
	{
		SortType = type;
	}
	
	public OpenComparer(SortType type, Boolean foldersFirst)
	{
		SortType = type;
		FoldersFirst = foldersFirst;
	}
	
	public int compare(OpenFace fa, OpenFace fb) {
		if(FoldersFirst)
		{
			if(fa.isDirectory() && !fb.isDirectory())
				return 0;
			else if(!fa.isDirectory() && fb.isDirectory())
				return 1;
		}
		String a = fa.getName();
		String b = fb.getName();
		Long sa = fa.length();
		Long sb = fb.length();
		switch(SortType)
		{
			case ALPHA_DESC:
				return b.toLowerCase().compareTo(a.toLowerCase());
			case ALPHA:
				return a.toLowerCase().compareTo(b.toLowerCase());
			case SIZE_DESC:
				return sb.compareTo(sa);
			case SIZE:
				return sa.compareTo(sb);
			case DATE_DESC:
				return fb.lastModified().compareTo(fa.lastModified());
			case DATE:
				return fa.lastModified().compareTo(fb.lastModified());
			case TYPE:
				String ea = a.substring(a.lastIndexOf(".") + 1, a.length()).toLowerCase();
				String eb = b.substring(b.lastIndexOf(".") + 1, b.length()).toLowerCase();
				return ea.compareTo(eb);
			case NONE:
				return 0;
			default:
				return a.toLowerCase().compareTo(b.toLowerCase());
		}
	}	
}