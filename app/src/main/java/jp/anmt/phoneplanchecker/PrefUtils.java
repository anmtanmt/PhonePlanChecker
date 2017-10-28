package jp.anmt.phoneplanchecker;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by numata on 2017/10/15.
 */

public class PrefUtils {
    // 設定
    public static final String KEY_SETTING_REVIEW_DONE = "key_review_done"; //レビュー済か否か

    // int型の値の書き込み
    public static void writePrefInt(Context ctx, String key, int param) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);

        SharedPreferences.Editor editor = pref.edit();
        editor.putInt(key, param);
        editor.commit();
    }

    // int型の値の読み込み
    public static int readPrefInt(Context ctx, String key) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);

        return pref.getInt(key, -1);
    }

    // String型の値の書き込み
    public static void writePrefStr(Context ctx, String key, String param) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);

        SharedPreferences.Editor editor = pref.edit();
        editor.putString(key, param);
        editor.commit();
    }

    // String型の値の読み込み
    public static String readPrefStr(Context ctx, String key) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);

        return pref.getString(key, null);
    }

    // boolean型の値の書き込み
    public static void writePrefBool(Context ctx, String key, boolean param) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);

        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean(key, param);
        editor.commit();
    }

    // boolean型の値の読み込み
    public static boolean readPrefBool(Context ctx, String key) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);

        return pref.getBoolean(key, false);
    }

    // 削除
    public static void removePref(Context ctx, String key) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);

        SharedPreferences.Editor editor = pref.edit();
        editor.remove(key);
        editor.commit();
    }
}
