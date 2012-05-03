package org.brandroid.openmanager.data;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;

import android.net.Uri;

public class OpenZipEntry extends OpenPath
{
	private final OpenZip p;
	private final ZipEntry ze;
	
	public OpenZipEntry(OpenZip parent, ZipEntry entry)
	{
		p = parent;
		ze = entry;
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
		return null;
	}

	@Override
	public OpenPath[] list() throws IOException {
		return null;
	}

	@Override
	public OpenPath[] listFiles() throws IOException {
		return null;
	}

	@Override
	public Boolean isDirectory() {
		return ze.isDirectory();
	}

	@Override
	public Boolean isFile() {
		return !ze.isDirectory();
	}

	@Override
	public Boolean isHidden() {
		return false;
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
		return true;
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
