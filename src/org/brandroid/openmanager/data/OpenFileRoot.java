package org.brandroid.openmanager.data;

public class OpenFileRoot extends OpenFile {

	public OpenFileRoot(String path) {
		super(path);
	}
	
	@Override
	public Boolean requiresThread() {
		return true;
	}
	
	@Override
	public Boolean exists() {
		return true;
	}
	
	@Override
	public boolean addToDb() {
		// TODO Auto-generated method stub
		return super.addToDb();
	}
	
	@Override
	public Boolean canRead() {
		return true;
	}

}
