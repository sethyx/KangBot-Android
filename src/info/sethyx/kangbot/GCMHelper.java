
package info.sethyx.kangbot;

import info.sethyx.kangbot.R;

import java.text.DateFormat;
import java.util.Date;

import com.google.android.gcm.GCMRegistrar;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.widget.Toast;

public class GCMHelper {

    private static final String TAG = "GCMHelper";
    private static final String RESULT_OK = "success";
    private static final String RESULT_NOK = "failure";

    static AsyncTask<Void, Void, Void> mRegisterTask;

    public static SpannableStringBuilder getBuildStringWithColor(Context context, String result,
            String source) {
        int color = 0;
        String temp = "";
        if (result.equals(RESULT_OK)) {
            color = Color.argb(255, 153, 204, 0);
            temp = context.getString(R.string.build_completed, source);
        } else if (result.contains(RESULT_NOK)) {
            color = Color.argb(255, 255, 68, 88);
            temp = context.getString(R.string.build_failed, source);
        }
        SpannableStringBuilder stringBuilder = new SpannableStringBuilder(temp);
        stringBuilder.setSpan(new ForegroundColorSpan(color), 6,
                6 + temp.split(" ")[1].length(), 0);
        return stringBuilder;
    }

    public static String getBuildString(Context context, String result, String source) {
        if (result.equals(RESULT_OK)) {
            return context.getString(R.string.build_completed, source);
        } else if (result.contains(RESULT_NOK)) {
            return context.getString(R.string.build_failed, source);
        } else {
            return "";
        }
    }

    public static String getDateString(long time) {
        DateFormat df = DateFormat.getDateTimeInstance();
        Date resultdate = new Date(time);
        return df.format(resultdate);
    }

    public static String buildErrorText(Context context, String errLog, long time,
            String result, String device, String src) {
        StringBuilder builder = new StringBuilder();
        builder.append(getDateString(time) + "\n");
        builder.append(device + " " + getBuildString(context, result, src));
        builder.append(errLog);
        return builder.toString();
    }

    public static boolean checkResult(String result) {
        if (result.equals(RESULT_OK)) {
            return true;
        } else {
            return false;
        }
    }

    public static void displayMessage(final Context context, final String message) {
        Handler h = new Handler(Looper.getMainLooper());
        h.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, message, Toast.LENGTH_SHORT)
                        .show();
            }

        });
    }

    public static void register(final Context context) {
        if (!GCMRegistrar.getRegistrationId(context).equals("") && GCMRegistrar.isRegisteredOnServer(context)) {
            Log.d(TAG, "ALL OK");
            return;
        }
        final String regId = GCMRegistrar.getRegistrationId(context);
        if (regId.equals("")) {
            Log.i(TAG, "registering device on GCM!");
            GCMRegistrar.register(context, Secret.SENDER_ID);
        } else {
            // Device is already registered on GCM, check server.
            if (GCMRegistrar.isRegisteredOnServer(context)) {
                // Skip registration.
                Log.i(TAG, "device already registered on server!");
            } else {
                // Try to register again, but not in the UI thread.
                Log.i(TAG, "registering device on server!");
                mRegisterTask = new AsyncTask<Void, Void, Void>() {

                    @Override
                    protected Void doInBackground(Void... params) {
                        ServerUtilities
                                .register(context, regId,
                                        PrefsHelper.getID(context));
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void result) {
                        if (!GCMRegistrar.isRegisteredOnServer(context)) {
                            GCMRegistrar.unregister(context);
                        }
                        mRegisterTask = null;
                    }
                };
                mRegisterTask.execute(null, null, null);
            }
        }
    }

    public static void checkVersionUpdateOnStart(Context context) {
        try {
            if ((PrefsHelper.getVersion(context) < context.getPackageManager().
                    getPackageInfo(context.getPackageName(), 0).versionCode)
                    && GCMRegistrar.isRegisteredOnServer(context)) {
                GCMRegistrar.setRegisteredOnServer(context, false);
                GCMRegistrar.register(context, Secret.SENDER_ID);
                Log.d(TAG, "version changed, re-register");
            }
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
    }
}
