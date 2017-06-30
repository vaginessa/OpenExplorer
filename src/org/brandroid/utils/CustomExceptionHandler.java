
package org.brandroid.utils;

import java.lang.Thread.UncaughtExceptionHandler;

public class CustomExceptionHandler implements UncaughtExceptionHandler {
    public void uncaughtException(Thread t, Throwable e) {
        Logger.LogWTF("Uncaught Exception in Thread: " + t.getName() + " (" + t.toString() + ")", e);
        System.exit(1);
    }
}
