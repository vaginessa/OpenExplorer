package org.brandroid.openmanager.data;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.zip.ZipEntry;

import org.brandroid.openmanager.fragments.DialogHandler;

import android.net.Uri;

public class OpenZipEntry extends OpenPath
{
	private final OpenZip p;
	private final ZipEntry ze;
	private OpenPath[] mChildren = null;
	
	public OpenZipEntry(OpenZip parent, ZipEntry entry)
	{
		p = parent;
		ze = entry;
		if(ze.getName().endsWith("/") || ze.isDirectory())
		{
			try {
				mChildren = list();
			} catch(IOException e) { }
		}
	}

	@Override
	public String getName() {
		return ze.getName();
	}

	@Override
	public String getPath() {
		return p.getPath() + "/" + getName();
	}

	@Override
	public String getAbsolutePath() {
		return getPath();
	}

	@Override
	public void setPath(String path) {
		
	}

	@Override
	public long length() {
		return ze.getSize();
	}

	@Override
	public OpenPath getParent() {
		return p;
	}

	@Override
	public OpenPath getChild(String name) {
		try {
			for(OpenPath kid : list())
				if(kid.getName().equals(name))
					return kid;
		} catch(IOException e) { }
		return null;
	}

	@Override
	public OpenPath[] list() throws IOException {
		if(mChildren != null)
			return mChildren;
		return listFiles();
	}

	@Override
	public OpenPath[] listFiles() throws IOException
	{
		return p.listFiles(ze.getName());
	}
	
	@Override
	public int getListLength() {
		try {
			return list().length;
		} catch(IOException e) {
			return 0;
		}
	}
	
	@Override
	public String getDetails(boolean countHiddenChildren, boolean showLongDate) {
		String ret = super.getDetails(countHiddenChildren, showLongDate);
		if(!isDirectory())
			ret = ret.substring(0, ret.indexOf(" |")) + "(" + DialogHandler.formatSize(ze.getCompressedSize()) + ")" + ret.substring(ret.indexOf(" |"));
		return ret;
	}

	@Override
	public Boolean isDirectory() {
		return ze.isDirectory() || ze.getName().endsWith("/");
	}

	@Override
	public Boolean isFile() {
		return !ze.isDirectory();
	}

	@Override
	public Boolean isHidden() {
		return getName().startsWith(".");
	}

	@Override
	public Uri getUri() {
		return Uri.parse(getPath());
	}

	@Override
	public Long lastModified() {
		return ze.getTime();
	}

	@Override
	public Boolean canRead() {
		return true;
	}

	@Override
	public Boolean canWrite() {
		return false;
	}

	@Override
	public Boolean canExecute() {
		return false;
	}

	@Override
	public Boolean exists() {
		return true;
	}

	@Override
	public Boolean requiresThread() {
		return false;
	}

	@Override
	public Boolean delete() {
		return false;
	}

	@Override
	public Boolean mkdir() {
		return false;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return p.getZip().getInputStream(ze);
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return null;
	}

}
