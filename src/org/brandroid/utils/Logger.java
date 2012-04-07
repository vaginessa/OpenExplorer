package org.brandroid.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;

import org.brandroid.openmanager.activities.OpenExplorer;
import org.brandroid.openmanager.data.OpenFile;
import org.brandroid.openmanager.util.FileManager;
import org.json.JSONArray;

import android.content.Context;
import android.util.Log;

public class Logger
{
	private static String[] sLastMessage = new String[] {"", "", "", "", ""};
	private static Integer[] iLastCount = new Integer[] {0,0,0,0,0};
	private final static Boolean DO_LOG = true; // global static
	private static Boolean bLoggingEnabled = true; // this can be set view preferences
	public final static Integer MIN_DB_LEVEL = Log.WARN;
	private final static String LOG_KEY = "OpenExplorer";
	private static LoggerDbAdapter dbLog;
	
	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		if(dbLog != null)
			dbLog.close();
	}

	public static Boolean isLoggingEnabled() { return bLoggingEnabled && DO_LOG; }
	public static void setLoggingEnabled(Boolean enable) { bLoggingEnabled = enable; }
	private static boolean CheckLastLog(String msg, int level)
	{
		if(OpenExplorer.IS_DEBUG_BUILD) return false;
		if(!isLoggingEnabled()) return true;
		level -= 2;
		if(level < 0 || level > 4) return false;
		if(sLastMessage[level] != null && msg != null && msg.equalsIgnoreCase(sLastMessage[level]))
		{
			iLastCount[level]++;
			return true;
		} else if(iLastCount[level] > 0)
		{
			Log.println(level, LOG_KEY, "The last message repeated " + iLastCount[level] + " times");
			iLastCount[level] = 0;
		}
		sLastMessage[level] = msg;
		return false;
	}
	private static int getMyStackTraceCount(StackTraceElement[] els)
	{
		int ret = 0;
		for(int i = 0; i < els.length; i++)
			if(els[i].getClassName().contains("brandroid"))
				ret++;
		return ret;
	}
	private static StackTraceElement[] getMyStackTrace(Throwable e)
	{
		StackTraceElement[] elArray = e.getStackTrace();
		StackTraceElement[] ret = new StackTraceElement[getMyStackTraceCount(elArray)];
		int j = 0;
		for(int i = 0; i < elArray.length; i++)
			if(elArray[i].getClassName().contains("brandroid"))
				ret[j++] = elArray[i];
		return ret;
	}
	private static StackTraceElement[] getMyStackTrace(Exception e)
	{
		StackTraceElement[] elArray = e.getStackTrace();
		StackTraceElement[] ret = new StackTraceElement[getMyStackTraceCount(elArray)];
		int j = 0;
		for(int i = 0; i < elArray.length; i++)
			if(elArray[i].getClassName().contains("brandroid"))
				ret[j++] = elArray[i];
		return ret;
	}
	private static void LogToDB(int level, String msg, String stack)
	{
		if(level < MIN_DB_LEVEL) return;
		if(dbLog == null) return;
		dbLog.createItem(msg, level, stack);
	}
	public static void openDb(Context c) {
		dbLog = new LoggerDbAdapter(c);
	}
	public static Boolean hasDb() { return dbLog != null; }
	public static void setDb(LoggerDbAdapter newDb) { dbLog = newDb; }
	public static JSONArray getDbLogArray() { return dbLog != null ? dbLog.getAllItemsJSON() : new JSONArray(); }
	public static String getDbLogs(Boolean clear) {
		if(dbLog == null) return "";
		String ret = dbLog.getAllItems();
		if(clear)
			dbLog.clear();
		return ret;
	}
	public static int countLevel(int level)
	{
		if(dbLog == null) return 0;
		return dbLog.countLevel(level);
	}
	public static void clearDb()
	{
		if(dbLog != null)
			dbLog.clear();
	}
	public static void LogError(String msg)
	{
		if(CheckLastLog(msg, Log.ERROR)) return;
		LogToDB(Log.ERROR, msg, "");
		Log.e(LOG_KEY, msg);
	}
	public static void LogError(String msg, Error ex)
	{
		if(CheckLastLog(ex.getMessage(), Log.ERROR)) return;
		StackTraceElement[] trace = getMyStackTrace(ex);
		ex.setStackTrace(trace);
		LogToDB(Log.ERROR, msg, Log.getStackTraceString(ex));
		Log.e(LOG_KEY, msg + (trace.length > 0 ? " (" + trace[0].getFileName() + ":" + trace[0].getLineNumber() + ")" : ""), ex);
	}
	public static void LogError(String msg, Exception ex)
	{
		if(CheckLastLog(ex.getMessage(), Log.ERROR)) return;
		StackTraceElement[] trace = getMyStackTrace(ex);
		if(trace.length == 0)
			trace = ex.getStackTrace();
		ex.setStackTrace(trace);
		LogToDB(Log.ERROR, msg, Log.getStackTraceString(ex));
		Log.e(LOG_KEY, msg + (trace.length > 0 ? " (" + trace[0].getFileName() + ":" + trace[0].getLineNumber() + ")" : ""), ex);
	}
	public static boolean checkWTF()
	{
		OpenFile crp = getCrashFile();
		if(crp != null && crp.exists() && crp.length() > 0)
			return true;
		return countLevel(Log.ASSERT) > 0;
	}
	public static OpenFile getCrashFile()
	{
		OpenFile ext = OpenFile.getExternalMemoryDrive(true);
		if(ext != null)
		{
			ext = (OpenFile)ext.getChild(".openexplorer_crash");
			if(ext.exists() && !ext.canWrite())
				ext.getFile().setWritable(true);
			return ext;
		}
		ext = new OpenFile("/mnt/sdcard/.openexplorer_crash");
		if(ext.exists() && !ext.canWrite())
			ext.getFile().setWritable(true);
		if(ext.canWrite())
			return ext;
		return null;
	}
	public static String getCrashReport(boolean json)
	{
		OpenFile crp = getCrashFile();
		if(crp == null || !crp.exists()) return null;
		return json ?
				"{crash:\"" + crp.readAscii().replace("\"", "\\\"") + "}" : 
				crp.readAscii();
	}
	public static void LogWTF(String msg, Throwable ex)
	{
		if(hasDb())
			LogToDB(Log.ASSERT, msg, Log.getStackTraceString(ex));
		OpenFile crp = getCrashFile();
		if(crp != null)
		{
			crp.write(Log.getStackTraceString(ex));
			if(crp.exists())
				Logger.LogVerbose("Crash file created!");
			else
				Logger.LogWarning("Crash file written, but we can't find it!");
		} else Logger.LogWarning("Crash file is null!");
		StackTraceElement[] trace = getMyStackTrace(ex);
		Log.wtf(LOG_KEY, msg + (trace.length > 0 ? " (" + trace[0].getFileName() + ":" + trace[0].getLineNumber() + ")" : ""), ex);
	}
	public static void LogWarning(String msg)
	{
		if(CheckLastLog(msg, Log.WARN)) return;
		LogToDB(Log.WARN, msg, "");
		Log.w(LOG_KEY, msg);
	}
	public static void LogWarning(String msg, Error w)
	{
		if(CheckLastLog(w.getMessage(), Log.WARN)) return;
		StackTraceElement[] trace = getMyStackTrace(w);
		w.setStackTrace(trace);
		LogToDB(Log.WARN, msg, Log.getStackTraceString(w));
		Log.w(LOG_KEY, msg + (trace.length > 0 ? " (" + trace[0].getFileName() + ":" + trace[0].getLineNumber() + ")" : ""), w);
	}
	public static void LogWarning(String msg, Exception w)
	{
		if(CheckLastLog(w.getMessage(), Log.WARN)) return;
		StackTraceElement[] trace = getMyStackTrace(w);
		w.setStackTrace(trace);
		LogToDB(Log.WARN, msg, Log.getStackTraceString(w));
		Log.w(LOG_KEY, msg + (trace.length > 0 ? " (" + trace[0].getFileName() + ":" + trace[0].getLineNumber() + ")" : ""), w);
	}
	public static void LogInfo(String msg)
	{
		if(CheckLastLog(msg, Log.INFO)) return;
		LogToDB(Log.INFO, msg, "");
		Log.i(LOG_KEY, msg);
	}
	public static void LogInfo(String msg, String stack)
	{
		if(CheckLastLog(msg, Log.INFO)) return;
		LogToDB(Log.DEBUG, msg, stack);
		Log.d(LOG_KEY, msg);
	}
	public static void LogInfo(String msg, Error e)
	{
		if(CheckLastLog(e.getMessage(), Log.INFO)) return;
		StackTraceElement[] trace = getMyStackTrace(e);
		e.setStackTrace(trace);
		LogToDB(Log.INFO, msg, Log.getStackTraceString(e));
		Log.i(LOG_KEY, msg + (trace.length > 0 ? " (" + trace[0].getFileName() + ":" + trace[0].getLineNumber() + ")" : ""), e);
	}
	public static void LogInfo(String msg, Exception e)
	{
		if(CheckLastLog(e.getMessage(), Log.INFO)) return;
		StackTraceElement[] trace = getMyStackTrace(e);
		e.setStackTrace(trace);
		LogToDB(Log.INFO, msg, Log.getStackTraceString(e));
		Log.i(LOG_KEY, msg + (trace.length > 0 ? " (" + trace[0].getFileName() + ":" + trace[0].getLineNumber() + ")" : ""), e);
	}
	public static void LogDebug(String msg)
	{
		if(CheckLastLog(msg, Log.DEBUG)) return;
		LogToDB(Log.DEBUG, msg, "");
		Log.d(LOG_KEY, msg);
	}
	public static void LogDebug(String msg, String stack)
	{
		if(CheckLastLog(msg, Log.DEBUG)) return;
		LogToDB(Log.DEBUG, msg, stack);
		Log.d(LOG_KEY, msg);
	}
	public static void LogDebug(String msg, Error e)
	{
		if(CheckLastLog(e.getMessage(), Log.DEBUG)) return;
		StackTraceElement[] trace = getMyStackTrace(e);
		e.setStackTrace(trace);
		LogToDB(Log.DEBUG, msg, Log.getStackTraceString(e));
		Log.d(LOG_KEY, msg + (trace.length > 0 ? " (" + trace[0].getFileName() + ":" + trace[0].getLineNumber() + ")" : ""), e);
	}
	public static void LogDebug(String msg, Exception e)
	{
		if(CheckLastLog(e.getMessage(), Log.DEBUG)) return;
		StackTraceElement[] trace = getMyStackTrace(e);
		e.setStackTrace(trace);
		LogToDB(Log.DEBUG, msg, Log.getStackTraceString(e));
		Log.d(LOG_KEY, msg + (trace.length > 0 ? " (" + trace[0].getFileName() + ":" + trace[0].getLineNumber() + ")" : ""), e);
	}
	
	public static void LogVerbose(String msg)
	{
		if(CheckLastLog(msg, Log.VERBOSE)) return;
		LogToDB(Log.VERBOSE, msg, "");
		Log.v(LOG_KEY, msg);
	}

	public static void closeDb() {
		if(hasDb())
			dbLog.close();
	}
}
