package co.zync.zync;

import android.app.*;
import android.content.*;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceActivity;
import android.util.Log;
import co.zync.zync.activities.SettingsActivity;
import co.zync.zync.api.ZyncAPI;
import co.zync.zync.api.ZyncClipData;
import co.zync.zync.api.ZyncClipType;
import co.zync.zync.api.ZyncError;
import co.zync.zync.api.callback.ZyncCallback;
import co.zync.zync.firebase.ZyncInstanceIdService;
import co.zync.zync.firebase.ZyncMessagingService;
import co.zync.zync.utils.NullDialogClickListener;
import co.zync.zync.utils.RequestStatusListener;
import co.zync.zync.utils.ZyncExceptionInfo;
import okhttp3.OkHttpClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class ZyncApplication extends Application {
    public static final List<String> SENSITIVE_PREFERENCE_FIELDS = Arrays.asList("zync_history", "zync_api_token", "encryption_pass");
    public static final List<ZyncExceptionInfo> LOGGED_EXCEPTIONS = Collections.synchronizedList(new ArrayList<ZyncExceptionInfo>());
    /* START NOTIFICATION IDS */
    public static int CLIPBOARD_UPDATED_ID = 281902;
    public static int CLIPBOARD_POSTED_ID = 213812;
    public static int CLIPBOARD_ERROR_ID = 9308312;
    public static int PERSISTENT_NOTIFICATION_ID = 329321;
    /* END NOTIFICATION IDS */
    // whether the last request was successful or not
    private AtomicBoolean lastRequestStatus = new AtomicBoolean(true);
    private RequestStatusListener requestStatusListener;
    private OkHttpClient httpClient;
    private ZyncDataManager dataManager;
    private ZyncAPI api;
    private ZyncWifiReceiver receiver; // do not remove, we have to retain the reference
    private final ZyncPreferenceChangeListener preferenceChangeListener = new ZyncPreferenceChangeListener(this);

    @Override
    public void onCreate() {
        super.onCreate();

        // if we are connected to wifi or allowed to use data, setup Zync network services
        if (isWifiConnected() || getPreferences().getBoolean("use_on_data", true)) {
            setupNetwork();
        }

        dataManager = new ZyncDataManager(this);

        // setup wifi receiver to get updates on network changes
        receiver = new ZyncWifiReceiver();
        registerReceiver(receiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        // set a preference listener to make changes to activity when user changes setting
        getPreferences().registerOnSharedPreferenceChangeListener(preferenceChangeListener);
    }

    public ZyncDataManager dataManager() {
        return dataManager;
    }

    // Set the history that is stored on file
    public void setHistory(List<ZyncClipData> history) {
        Set<String> historyText = new HashSet<>(history.size());

        for (ZyncClipData data : history) {
            data.encrypt(getEncryptionPass());
            historyText.add(data.toJson().toString());
        }

        getPreferences().edit().putStringSet("zync_history", historyText).apply();
    }

    // adds an item to history and writes changes to file
    public void addToHistory(ZyncClipData data) {
        List<String> history = new ArrayList<>(getPreferences().getStringSet("zync_history", new HashSet<String>()));

        if (history.size() == 10) {
            history.remove(9);
        }

        data.encrypt(getEncryptionPass());
        history.add(data.toJson().toString());
        getPreferences().edit().putStringSet("zync_history", new HashSet<>(history))
                .apply();
    }

    // utility method to effectively -> filter(timestamp).findFirst()
    public ZyncClipData clipFromTimestamp(long timestamp, List<ZyncClipData> history) {
        for (ZyncClipData data : history) {
            if (data.timestamp() == timestamp) {
                return data;
            }
        }

        return null;
    }

    public void setupNetwork() {
        // Zync services
        startService(ZyncInstanceIdService.class);
        startService(ZyncMessagingService.class);

        httpClient = new OkHttpClient();

        if (api != null) {
            enableClipboardService();
            api.setClient(httpClient);
        }
    }

    // remove network services
    public void removeNetworkUsages() {
        stopService(new Intent(this, ZyncInstanceIdService.class));
        stopService(new Intent(this, ZyncMessagingService.class));

        httpClient = null;

        if (api != null) {
            disableClipboardService();
            api.setClient(null);
        }
    }

    // go across all networks and test if the device is connected to the internet
    public boolean isWifiConnected() {
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean connected = false;

        for (Network network : connManager.getAllNetworks()) {
            NetworkInfo info = connManager.getNetworkInfo(network);

            if (info.getType() != ConnectivityManager.TYPE_WIFI) {
                continue;
            }

            if (info.isConnected()) {
                connected = true;
            }
        }

        return connected;
    }

    public void enableClipboardService() {
        if (ZyncClipboardService.getInstance() == null) {
            startService(ZyncClipboardService.class);
        }
    }

    public void disableClipboardService() {
        if (ZyncClipboardService.getInstance() != null) {
            stopService(new Intent(this, ZyncClipboardService.class));
            ZyncClipboardService.nullify();
        }
    }

    public void setRequestStatusListener(RequestStatusListener requestStatusListener) {
        this.requestStatusListener = requestStatusListener;
    }

    public void setLastRequestStatus(boolean val) {
        lastRequestStatus.set(val);

        if (requestStatusListener != null) {
            requestStatusListener.onStatusChange(val);
        }
    }

    public boolean lastRequestStatus() {
        return lastRequestStatus.get();
    }

    // utility method to start a service
    private void startService(Class<? extends Service> cls) {
        Intent intent = new Intent(this, cls);
        startService(intent);
    }

    // utility method to get a color which uses the best method
    // to get a color from resources based on version of device
    public int getColorSafe(int colorRes) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return getColor(colorRes);
        } else {
            return getResources().getColor(colorRes);
        }
    }

    /*       NOTIFICATION METHODS START        */

    public void sendClipErrorNotification() {
        sendNotification(
                CLIPBOARD_ERROR_ID,
                getString(R.string.clipboard_post_error_notification),
                getString(R.string.clipboard_post_error_notification_desc)
        );
    }

    public void sendClipPostedNotification() {
        if (getPreferences().getBoolean("clipboard_change_notification", true)) {
            sendNotification(
                    CLIPBOARD_POSTED_ID,
                    getString(R.string.clipboard_posted_notification),
                    getString(R.string.clipboard_posted_notification_desc)
            );
        }
    }

    // utility method to create a pending intent for a notification
    public PendingIntent createPendingIntent(Intent intent, Class<? extends Activity> activityClass) {
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(activityClass);
        stackBuilder.addNextIntent(intent);

        return stackBuilder.getPendingIntent(
                0,
                PendingIntent.FLAG_UPDATE_CURRENT
        );
    }

    public void sendNotification(int id, String title, String text) {
        sendNotification(id, title, text, null);
    }

    // sends a notification to the system with a general template
    public void sendNotification(int id, String title, String text, PendingIntent intent) {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.cancel(id);

        Notification.Builder builder = new Notification.Builder(this);
        builder.setSmallIcon(R.drawable.notification_icon);
        builder.setContentTitle(title);
        builder.setContentText(text);

        if (intent != null) {
            builder.setContentIntent(intent);
        }

        notificationManager.notify(id, builder.build());
    }

    /*       NOTIFICATION METHODS END        */

    /*
     * Sync cloud clipboard to local
     */
    public void syncDown() {
        if (getPreferences().getBoolean("sync_down", true)) {
            api.getClipboard(getEncryptionPass(), new ZyncCallback<ZyncClipData>() {
                @Override
                public void success(ZyncClipData value) {
                    byte[] data;

                    if (value != null && (data = value.data()) != null
                            && isTypeSupported(value.type())) {
                        if (value.type() == ZyncClipType.TEXT) {
                            ZyncClipboardService.getInstance().writeToClip(new String(data), false);
                        }
                    }
                }

                @Override
                public void handleError(ZyncError error) {
                    // ignored
                }
            });
        }
    }

    // max size of a payload allowed based on settings in bytes
    public long getMaxSize() {
        return getPreferences().getInt("max_size", 10) * 1000000;
    }

    // check if the clip type is supported in the app
    // modification will cause odd behaviour and errors
    // in the app
    public boolean isTypeSupported(ZyncClipType type) {
        return type == ZyncClipType.TEXT;
    }

    // utility method to open settings directly
    public void openSettings() {
        Intent settingsIntent = new Intent(getApplicationContext(), SettingsActivity.class);

        settingsIntent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, SettingsActivity.GeneralPreferenceFragment.class.getName());
        settingsIntent.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true);
        startActivity(settingsIntent);
    }

    public String getEncryptionPass() {
        return getPreferences().getString("encryption_pass", "default");
    }

    public OkHttpClient httpClient() {
        return httpClient;
    }

    public ZyncAPI getApi() {
        return api;
    }

    public SharedPreferences getPreferences() {
        return getSharedPreferences(SettingsActivity.PREFERENCES_NAME, 0);
    }

    public void setApi(ZyncAPI api) {
        this.api = api;
    }

    public void clearPreferences() {
        getPreferences().edit()
                .remove("zync_api_token")
                .remove("zync_history")
                .apply();
    }

    // creates the info file (zync_debug_info.txt) and fills it with a debug report
    public File createInfoFile() throws IOException {
        File file = new File(new File(getFilesDir(), "attachments/"), "zync_debug_info.txt");
        if (file.exists()) {
            file.delete();
        }

        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        file.createNewFile();
        FileOutputStream fos = new FileOutputStream(file);
        String fileContents = debugInfo();

        fos.write(fileContents.getBytes(Charset.forName("UTF-8")));
        fos.flush();
        fos.close();

        return file;
    }

    // creates debug report to figure out issues with bugs
    public String debugInfo() {
        StringBuilder builder = new StringBuilder();

        builder.append("-----------------------------------------\n");
        builder.append("Zync for Android Info report\n");
        builder.append("Unix time report generated: ").append(System.currentTimeMillis()).append('\n');
        builder.append("App Version: ").append(BuildConfig.VERSION_NAME).append('\n');
        builder.append("API Version: ").append(ZyncAPI.VERSION).append('\n');
        builder.append("API Base URL: ").append(ZyncAPI.BASE).append('\n');
        builder.append("Debug mode: ").append(BuildConfig.DEBUG).append("\n\n");

        builder.append("Device info:\n");
        builder.append("- Android Version: ").append(Build.VERSION.SDK_INT).append('\n');
        builder.append("- Device: ").append(Build.DEVICE).append('\n');
        builder.append("- Model: ").append(Build.MODEL).append('\n');
        builder.append("- Product: ").append(Build.PRODUCT).append('\n');
        builder.append("- User-Agent: ").append(System.getProperty("http.agent")).append("\n\n");

        builder.append("ZyncAPI initialized: ").append(api != null).append('\n');

        if (api != null) {
            builder.append("Zync Token: ").append(api.getToken()).append("\n");
        }

        builder.append('\n');
        builder.append("Zync Settings:\n");
        Map<String, ?> prefs = getPreferences().getAll();

        for (String key : prefs.keySet()) {
            // do NOT include sensitive information such as their history or encryption password
            if (SENSITIVE_PREFERENCE_FIELDS.contains(key.toLowerCase())) {
                continue;
            }

            builder.append("- ").append(key).append("=").append(prefs.get(key).toString()).append('\n');
        }

        builder.append('\n');
        builder.append("-----------------------------------------\n\n");

        if (LOGGED_EXCEPTIONS.isEmpty()) {
            builder.append("No exceptions to be found!");
        } else {
            builder.append("Listing exceptions...\n\n");

            for (ZyncExceptionInfo exceptionInfo : LOGGED_EXCEPTIONS) {
                builder.append("---------------------------------\n");
                builder.append("Exception Type: ").append(exceptionInfo.ex().getClass().getName()).append('\n');

                if (exceptionInfo.ex().getMessage() != null) {
                    builder.append("Message: ").append(exceptionInfo.ex().getMessage()).append('\n');
                }

                builder.append("Exception at ").append(exceptionInfo.timestamp()).append('\n');

                if ("Unknown".equals(exceptionInfo.action())) {
                    builder.append("Attempted action is unknown\n");
                } else {
                    builder.append("Application was attempting to ").append(exceptionInfo.action()).append('\n');
                }

                builder.append("\nFull Stacktrace:\n");
                builder.append(Log.getStackTraceString(exceptionInfo.ex())).append('\n');
            }

            builder.append("---------------------------------\n");
        }

        return builder.toString();
    }

    // open the prompt to direct the user to a URL
    // in their preferred web application
    // errorMessage=res id
    public void directToLink(String url, int errorMessage) {
        Uri webpage = Uri.parse(url);
        Intent intent = new Intent(Intent.ACTION_VIEW, webpage);

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            AlertDialog.Builder dialog = new AlertDialog.Builder(getApplicationContext());

            dialog.setTitle(R.string.error);
            dialog.setMessage(errorMessage);
            dialog.setPositiveButton(R.string.ok, new NullDialogClickListener());

            dialog.show();
        }
    }
}
