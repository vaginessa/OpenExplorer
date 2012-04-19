package org.brandroid.openmanager.adapters;

import java.sql.Date;

import org.brandroid.openmanager.data.OpenPath;
import org.brandroid.openmanager.util.FileManager.SortType;
import org.brandroid.utils.Logger;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class OpenPathDbAdapter
{
	public static final String KEY_ID = "_id";
    public static final String KEY_FOLDER = "folder";
    public static final String KEY_NAME = "name";
    public static final String KEY_SIZE = "size";
    public static final String KEY_MTIME = "mtime";
    public static final String KEY_STAMP = "stamp";
    public static final String KEY_ATTRIBUTES = "atts";
    public static final String[] KEYS = {KEY_FOLDER,KEY_NAME,KEY_SIZE,KEY_MTIME,KEY_STAMP,KEY_ATTRIBUTES};
    
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;
    
    private static final String DATABASE_NAME = "files.db";
    private static final String DATABASE_TABLE = "files";
    private static final int DATABASE_VERSION = 6;

    private static final String DATABASE_CREATE =
        "create table " + DATABASE_TABLE + " (" + KEY_ID + " integer primary key autoincrement, "
        + KEY_FOLDER + " text null, "
        + KEY_NAME + " text null, "
        + KEY_SIZE + " text not null, "
        + KEY_MTIME + " int not null, "
        + KEY_STAMP + " int not null, "
        + KEY_ATTRIBUTES + " int null);";

    private final Context mCtx;

    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db)
        {
        	Logger.LogVerbose("Creating table [" + DATABASE_NAME + "]");
            db.execSQL(DATABASE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        	if(oldVersion < 4 && newVersion >= 5)
        	{
        		Logger.LogVerbose("We can do this upgrade [" + DATABASE_TABLE + "] from " + oldVersion + " to " + newVersion + " and retain old data");
        		db.execSQL("ALTER TABLE " + DATABASE_TABLE + " ADD COLUMN [" + KEY_ATTRIBUTES + "] int null");
        		return;
        	}
            Logger.LogVerbose("Upgrading table [" + DATABASE_TABLE + "] from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE);
            onCreate(db);
        }
    }

    public OpenPathDbAdapter(Context ctx)
    {
    	this.mCtx = ctx;
    }
    
    public OpenPathDbAdapter open() throws SQLException
    {
    	if(mDb != null && mDb.isOpen()) return this;
    	if(mDbHelper == null)
    		mDbHelper = new DatabaseHelper(mCtx);
    	try {
	    	if(mDb == null)
	    		mDb = mDbHelper.getWritableDatabase();
    	} catch(IllegalStateException ise) {
    		Logger.LogError("Couldn't open logger", ise);
    	}
    	return this;
    }
    
    public void close() { 
    	if(mDb != null && mDb.isOpen())
        	mDb.close();
    }
    
    @Override
    protected void finalize() throws Throwable {
    	super.finalize();
    	if(mDb != null && mDb.isOpen())
    		close();
    }
    
    public static int getKeyIndex(String keyName)
    {
    	int ret = 0;
    	for(String key : KEYS)
    	{
    		if(key.equals(keyName))
    			return ret;
    		ret++;
    	}
    	return -1;
    }
    
    public long createItem(OpenPath path) {
    	if(mDb == null || !mDb.isOpen()) open();
    	if(mDb == null) return -1;
    	ContentValues initialValues = new ContentValues();
    	OpenPath parent = path.getParent();
    	String sParent = "";
    	if(parent != null) sParent = parent.getPath();
    	initialValues.put(KEY_FOLDER, sParent);
    	initialValues.put(KEY_NAME, path.getName());
        initialValues.put(KEY_SIZE, path.length());
        initialValues.put(KEY_MTIME, path.lastModified());
        initialValues.put(KEY_STAMP, new java.util.Date().getTime());
        initialValues.put(KEY_ATTRIBUTES, path.getAttributes());
        //initialValues.put(KEY_STAMP, (new java.util.Date().getTime() - new Date(4,9,2011).getTime()) / 1000);
        //Logger.LogVerbose("Adding " + path.getPath() + " to files.db");
        
		try {
			//mDb.delete(DATABASE_TABLE, KEY_FOLDER + " = '" + sParent + "' AND " + KEY_NAME + " = '" + path.getName() + "'", null);
			if(mDb.replace(DATABASE_TABLE, null, initialValues) > -1)
				return 1;
			else return 0;
		} catch(Exception e) {
			Logger.LogError("Couldn't write to Files DB.", e);
			return 0;
		}
    }
    
    public int deleteFolder(OpenPath parent)
    {
    	try {
    		if(mDb != null && parent != null) {
    			String sParent = parent.getPath();
    			int ret = mDb.delete(DATABASE_TABLE, KEY_FOLDER + " = '" + sParent.replace("'", "\\'") + "'", null);
    			//Logger.LogDebug("deleteFolder(" + parent.getPath() + ") removed " + ret + " rows");
    			return ret;
    		} else return 0;
    	} catch(Exception e) {
    		Logger.LogError("Coudln't delete folder from Files DB.", e);
    		return -1;
    	}
    }
    
    private String getSortString(SortType sort)
    {
    	boolean asc = true;
    	String col = "";
    	switch(sort)
    	{
	    	case ALPHA_DESC:
	    		asc = false;
	    	case ALPHA:
	    		col = KEY_NAME;
	    		break;
	    	case DATE_DESC:
	    		asc = false;
	    	case DATE:
	    		col = KEY_MTIME;
	    		break;
	    	case SIZE_DESC:
	    		asc = false;
	    	case SIZE:
	    		col = KEY_SIZE;
	    		break;
	    	default: return "";
    	}
    	return col + " " + (asc ? "asc" : "desc");
    }
    
    public Cursor fetchItemsFromFolder(String folder, SortType sort)
    {
    	open();
    	try {
    		return mDb.query(true, DATABASE_TABLE,
    				KEYS, KEY_FOLDER + " = '" + folder.replace("'", "\\'") + "'",
    				null, null, null, getSortString(sort), null);
    	} catch(Exception e)
    	{
    		Logger.LogError("Couldn't fetch from folder " + folder + ". " + e.getLocalizedMessage(), e);
    		return null;
    	}
    }
    
    public void cleanParents() {
    	//mDb.rawQuery("UPDATE , selectionArgs)
    }

    public Cursor fetchAllItems() {
    	open();
    	try {
	    	return mDb.query(DATABASE_TABLE,
	    			KEYS, null, null, null, null, null);
    	} catch(Exception e) { return null; }
    }
    
    public int clear() {
    	if(mDb != null && mDb.isOpen())
    		return mDb.delete(DATABASE_TABLE, null, null);
    	else return -1;
    }
    
}
