
package info.sethyx.kangbot;

import android.content.Context;
import android.preference.PreferenceManager;

public class PrefsHelper {

    public static void putID(Context context, String id) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString("REG_ID", id)
                .commit();
    }

    public static String getID(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString("REG_ID", "");
    }

    public static void putVersion(Context context, int version) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putInt("APP_VER", version)
                .commit();
    }

    public static int getVersion(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt("APP_VER", 0);
    }

    public static void putUser(Context context, String user) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString("KB_USER", user)
                .commit();
    }

    public static String getUser(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString("KB_USER", "");
    }

    public static void putPwd(Context context, String pwd) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString("KB_PWD", pwd)
                .commit();
    }

    public static String getPwd(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString("KB_PWD", "");
    }

}
