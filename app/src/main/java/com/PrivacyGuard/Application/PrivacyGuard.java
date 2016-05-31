package com.PrivacyGuard.Application;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.content.ContextCompat;

/**
 * Created by justinhu on 16-01-15.
 */
public class PrivacyGuard extends Application {
    public final static String EXTRA_DATA = "PrivacyGuard.DATA";
    public final static String EXTRA_ID = "PrivacyGuard.id";
    public final static String EXTRA_APP_NAME = "PrivacyGuard.appName";
    public final static String EXTRA_PACKAGE_NAME = "PrivacyGuard.packageName";
    public final static String EXTRA_CATEGORY = "PrivacyGuard.category";
    public final static String EXTRA_IGNORE = "PrivacyGuard.ignore";
    public final static String EXTRA_SIZE = "PrivacyGuard.SIZE";
    public final static String EXTRA_DATE_FORMAT = "PrivacyGuard.DATE";
    public static boolean doFilter = false;
    public static boolean asynchronous = false; //TODO: so all are synchronous? not sure what's the effect
    public static int tcpForwarderWorkerRead = 0;
    public static int tcpForwarderWorkerWrite = 0;
    public static int socketForwarderWrite = 0;
    public static int socketForwarderRead = 0;

    private static Application sApplication;

    public static Application getApplication() {
        return sApplication;
    }

    public static Context getAppContext() {
        return getApplication().getApplicationContext();
    }//TODO:Nullable?

    @Override
    public void onCreate() {
        super.onCreate();
        sApplication = this;
    }
}
