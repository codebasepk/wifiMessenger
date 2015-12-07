package com.byteshaft.wifimessenger.utils;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.provider.Settings.Secure;

public class AppGlobals extends Application {

    private static Context sContext;
    private static SharedPreferences sPreferences;
    private static final String VIRGIN_KEY = "virgin";
    private static final String USER_NAME = "user_name";
    private static final String SERVICE_KEY = "service_switch";
    public static boolean chatActivityOpen = false;

    @Override
    public void onCreate() {
        super.onCreate();
        sContext = getApplicationContext();
        sPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    }

    public static Context getContext() {
        return sContext;
    }

    public static boolean isVirgin() {
        return sPreferences.getBoolean(VIRGIN_KEY, true);
    }

    public static void setVirgin(boolean virgin) {
        sPreferences.edit().putBoolean(VIRGIN_KEY, virgin).apply();
    }

    public static String getName() {
        return sPreferences.getString(USER_NAME, null);
    }

    public static void putName(String username) {
        sPreferences.edit().putString(USER_NAME, username).apply();
    }

    public static boolean isServiceOn() {
        return sPreferences.getBoolean(SERVICE_KEY, false);
    }

    public static void setService(boolean service) {
        sPreferences.edit().putBoolean(SERVICE_KEY, service).apply();
    }

    public static String getDeviceId() {
        Context context = getContext();
        return Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);
    }
}
