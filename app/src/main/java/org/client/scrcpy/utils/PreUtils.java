package org.client.scrcpy.utils;

import android.content.Context;
import android.content.SharedPreferences;


/**
 * Created by hasee on 2017/6/10.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class PreUtils {
    private static final String User = "default";

    public static boolean put(Context context, String key, int value) {
        SharedPreferences preferences = context.getSharedPreferences(User, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(key, value);
        return editor.commit();
    }

    public static int get(Context context, String key, int defaultInt) {
        SharedPreferences preferences = context.getSharedPreferences(User, Context.MODE_PRIVATE);
        return preferences.getInt(key, defaultInt);

    }

    public static boolean put(Context context, String key, long value) {
        SharedPreferences preferences = context.getSharedPreferences(User, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong(key, value);
        return editor.commit();
    }

    public static long get(Context context, String key, long defaultInt) {
        SharedPreferences preferences = context.getSharedPreferences(User, Context.MODE_PRIVATE);
        return preferences.getLong(key, defaultInt);
    }

    public static boolean put(Context context, String key, String value) {
        SharedPreferences preferences = context.getSharedPreferences(User, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(key, value);
        return editor.commit();
    }

    public static String get(Context context, String key, String defaultStr) {
        SharedPreferences preferences = context.getSharedPreferences(User, Context.MODE_PRIVATE);
        return preferences.getString(key, defaultStr);
    }

    public static boolean put(Context context, String key, boolean value) {
        SharedPreferences preferences = context.getSharedPreferences(User, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(key, value);
        return editor.commit();
    }

    public static boolean get(Context context, String key, boolean defaultStr) {
        SharedPreferences preferences = context.getSharedPreferences(User, Context.MODE_PRIVATE);
        return preferences.getBoolean(key, defaultStr);
    }
}
