
package info.sethyx.kangbot;

import info.sethyx.kangbot.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.net.Uri;
import android.util.Log;

import com.google.android.gcm.GCMBaseIntentService;
import com.google.android.gcm.GCMRegistrar;

/**
 * IntentService responsible for handling GCM messages.
 */
public class GCMIntentService extends GCMBaseIntentService {

    private static final String TAG = "GCMIntentService";
    private static final String P_DEVICE = "dev";
    private static final String P_SOURCE = "src";
    private static final String P_RESULT = "result";
    private static final String P_TIME = "time";
    private static final String P_ERROR = "errlog";

    public GCMIntentService() {
        super(Secret.SENDER_ID);
    }

    @Override
    protected void onRegistered(Context context, String registrationId) {
        Log.i(TAG, "Device registered on GCM");
        GCMHelper.displayMessage(context, getString(R.string.gcm_registered));
        try {
            PrefsHelper.putID(this, registrationId);
            PrefsHelper.putVersion(this, getPackageManager().
                    getPackageInfo(getPackageName(), 0).versionCode);
            ServerUtilities.register(this, registrationId, PrefsHelper.getID(this));
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onUnregistered(Context context, String registrationId) {
        Log.i(TAG, "Device unregistered");
        GCMHelper.displayMessage(context, getString(R.string.gcm_unregistered));
        if (GCMRegistrar.isRegisteredOnServer(context)) {
            ServerUtilities.unregister(context, registrationId);
            GCMRegistrar.setRegisteredOnServer(context, false);
            PrefsHelper.putID(this, "");
        } else {
            Log.i(TAG, "Ignoring unregister callback");
        }
    }

    @Override
    protected void onMessage(Context context, Intent intent) {
        Log.i(TAG, "Received message");
        String device = intent.getExtras().getString(P_DEVICE);
        if (device != null) {
            logMessage(context, intent);
            generateNotification(context, device, GCMHelper.getBuildString(
                    getBaseContext(), intent.getExtras().getString(P_RESULT),
                    intent.getExtras().getString(P_SOURCE)));
        }
    }

    @Override
    protected void onDeletedMessages(Context context, int total) {
        Log.i(TAG, "Received deleted messages notification");
    }

    @Override
    public void onError(Context context, String errorId) {
        Log.i(TAG, "Received error: " + errorId);
    }

    @Override
    protected boolean onRecoverableError(Context context, String errorId) {
        // log message
        Log.i(TAG, "Received recoverable error: " + errorId);
        return super.onRecoverableError(context, errorId);
    }

    /**
     * Issues a notification to inform the user that server has sent a message.
     */
    private static void generateNotification(Context context, String device, String msg) {
        int icon = R.drawable.ic_notif;
        long when = System.currentTimeMillis();
        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        Intent notificationIntent = new Intent(context, KangBotActivity.class);
        // set intent so it does not start a new activity
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent intent =
                PendingIntent.getActivity(context, 0, notificationIntent, 0);
        Notification notification = new Notification.Builder(context)
                .setContentTitle(device)
                .setContentText(msg)
                .setSmallIcon(icon)
                .setWhen(when)
                .setDefaults(Notification.DEFAULT_SOUND)
                .setLights(Color.argb(255, 255, 122, 0), 500, 500)
                .setContentIntent(intent)
                .build();
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notificationManager.notify(0, notification);
    }

    private void logMessage(Context context, Intent intent) {
        String device = intent.getExtras().getString(P_DEVICE);
        long time = Long.parseLong(intent.getExtras().getString(P_TIME));
        String result = intent.getExtras().getString(P_RESULT);
        String src = intent.getExtras().getString(P_SOURCE);
        String errorLog = "";
        if (!GCMHelper.checkResult(result)) {
            errorLog = getErrorLog(intent.getExtras().getString(P_ERROR));
        }
        ContentValues mLogEntry = new ContentValues();

        mLogEntry.put(BuildLogProvider.C_DEVICE, device);
        mLogEntry.put(BuildLogProvider.C_TIME, time);
        mLogEntry.put(BuildLogProvider.C_SRC, src);
        mLogEntry.put(BuildLogProvider.C_RESULT, result);
        mLogEntry.put(BuildLogProvider.C_ERRLOG, errorLog);
        getContentResolver().insert(
                BuildLogProvider.CONTENT_URI,
                mLogEntry);
    }

    private String getErrorLog(String url) {
        String result = "";
        try {
            HttpClient httpclient = new DefaultHttpClient();
            HttpGet httpget = new HttpGet(url);
            HttpResponse response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();

            if (entity != null) {
                InputStream inputstream = entity.getContent();
                BufferedReader bufferedreader =
                        new BufferedReader(new InputStreamReader(inputstream));
                StringBuilder stringbuilder = new StringBuilder();

                String currentline = null;
                stringbuilder.append("\n\n");
                while ((currentline = bufferedreader.readLine()) != null) {
                    stringbuilder.append(currentline + "\n");
                }
                result = stringbuilder.toString();
                Log.v("HTTP REQUEST", result);
                inputstream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        return result;
    }

}
