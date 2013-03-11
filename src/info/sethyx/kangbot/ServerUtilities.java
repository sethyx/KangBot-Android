
package info.sethyx.kangbot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import android.content.Context;
import android.util.Log;

import com.google.android.gcm.GCMRegistrar;

/**
 * Helper class used to communicate with the demo server.
 */
public final class ServerUtilities {
    private static final String TAG = "ServerUtilities";

    private static final int MAX_ATTEMPTS = 5;
    private static final int BACKOFF_MILLI_SECONDS = 2000;
    private static final Random random = new Random();

    private static final String P_REGID = "regId";
    private static final String P_OLDID = "oldId";
    private static final String P_USER = "user";
    private static final String P_PWD = "pwd";

    public static boolean register(final Context context, final String regId, /* optional */
            final String oldId) {
        Log.i(TAG, "registering device");
        String serverUrl = Secret.SERVER_REG_URL;
        Map<String, String> params = new HashMap<String, String>();
        params.put(P_REGID, regId);
        params.put(P_USER, PrefsHelper.getUser(context));
        params.put(P_PWD, PrefsHelper.getPwd(context));
        if (oldId != "") {
            params.put(P_OLDID, oldId);
        }
        long backoff = BACKOFF_MILLI_SECONDS + random.nextInt(1000);
        // Once GCM returns a registration id, we need to register it in the
        // app server. As the server might be down, we will retry it a couple
        // times.
        for (int i = 1; i <= MAX_ATTEMPTS; i++) {
            if (GCMRegistrar.isRegisteredOnServer(context)) {
                // AsyncTask finished meanwhile, return
                return false;
            }
            Log.d(TAG, "Attempt #" + i + " to register");
            try {
                String message = post(serverUrl, params);
                if (message.contains("Successfully added")
                        || message.contains("already registered")) {
                    GCMRegistrar.setRegisteredOnServer(context, true);
                    GCMHelper.displayMessage(context, message);
                    return true;
                } else {
                    // dont backoff in case of a wrong password
                    GCMHelper.displayMessage(context, message);
                    return false;
                }
            } catch (IOException e) {
                Log.e(TAG, "Failed to register on attempt " + i, e);
                if (i == MAX_ATTEMPTS) {
                    break;
                }
                try {
                    Log.d(TAG, "Sleeping for " + backoff + " ms before retry");
                    Thread.sleep(backoff);
                } catch (InterruptedException e1) {
                    // Activity finished before we complete - exit.
                    Log.d(TAG, "Thread interrupted: abort remaining retries!");
                    Thread.currentThread().interrupt();
                    return false;
                }
                // increase backoff exponentially
                backoff *= 2;
            }
        }
        String message = context.getString(R.string.server_register_error,
                MAX_ATTEMPTS);
        GCMHelper.displayMessage(context, message);
        return false;
    }

    /**
     * Unregister this account/device pair within the server.
     */
    public static void unregister(final Context context, final String regId) {
        Log.i(TAG, "unregistering device");
        String serverUrl = Secret.SERVER_UNREG_URL;
        Map<String, String> params = new HashMap<String, String>();
        params.put(P_REGID, regId);
        params.put(P_USER, PrefsHelper.getUser(context));
        params.put(P_PWD, PrefsHelper.getPwd(context));

        try {
            String message = post(serverUrl, params);
            GCMHelper.displayMessage(context, message);
            if (message.contains("Success")) {
                GCMRegistrar.setRegisteredOnServer(context, false);
            }
        } catch (IOException e) {
            String message = context.getString(R.string.server_unregister_error,
                    e.getMessage());
            e.printStackTrace();
            GCMHelper.displayMessage(context, message);
        }
    }

    private static String post(String endpoint, Map<String, String> params)
            throws IOException {
        URL url;
        try {
            url = new URL(endpoint);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("invalid url: " + endpoint);
        }
        StringBuilder bodyBuilder = new StringBuilder();
        Iterator<Entry<String, String>> iterator = params.entrySet().iterator();
        // constructs the POST body using the parameters
        while (iterator.hasNext()) {
            Entry<String, String> param = iterator.next();
            bodyBuilder.append(param.getKey()).append('=')
                    .append(param.getValue());
            if (iterator.hasNext()) {
                bodyBuilder.append('&');
            }
        }
        String body = bodyBuilder.toString();
        // Log.v(TAG, "Posting '" + body + "' to " + url);
        byte[] bytes = body.getBytes();
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setFixedLengthStreamingMode(bytes.length);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded;charset=UTF-8");
            // post the request
            OutputStream out = conn.getOutputStream();
            out.write(bytes);
            out.close();
            // handle the response
            int status = conn.getResponseCode();
            if (status != 200) {
                throw new IOException("Post failed with error code " + status);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line = reader.readLine();
            return line;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
