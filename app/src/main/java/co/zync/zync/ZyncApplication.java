package co.zync.zync;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.preference.PreferenceActivity;
import co.zync.zync.activities.SettingsActivity;
import co.zync.zync.api.ZyncAPI;
import co.zync.zync.api.ZyncClipData;
import co.zync.zync.api.ZyncClipType;
import co.zync.zync.api.ZyncError;
import co.zync.zync.firebase.ZyncInstanceIdService;
import co.zync.zync.firebase.ZyncMessagingService;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import java.util.Random;

public class ZyncApplication extends Application {
    public static int PERSISTENT_NOTIFICATION_ID = 329321;
    private RequestQueue httpRequestQueue;
    private ZyncAPI api;

    @Override
    public void onCreate() {
        super.onCreate();

        // Zync services
        startService(ZyncClipboardService.class);
        startService(ZyncInstanceIdService.class);
        startService(ZyncMessagingService.class);

        httpRequestQueue = Volley.newRequestQueue(getApplicationContext());

        if (getPreferences().getBoolean("enable_persistent_notification", true)) {
            createPersistentNotification();
        }
    }

    private void startService(Class<? extends Service> cls) {
        Intent intent = new Intent(this, cls);
        startService(intent);
    }

    public void createPersistentNotification() {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification.Builder builder = new Notification.Builder(this);
        builder.setOngoing(true);
        builder.setSmallIcon(R.drawable.notification_icon);
        builder.setLargeIcon(Icon.createWithResource("mipmap", R.mipmap.ic_launcher));
        builder.setContentTitle(getString(R.string.zync_persistentnotif_title));
        builder.setContentText(getString(R.string.zync_persistentnotif_descr));
        builder.setPriority(-2);

        notificationManager.notify(PERSISTENT_NOTIFICATION_ID, builder.build());
    }

    public void sendNotification(String title, String text) {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification.Builder builder = new Notification.Builder(this);
        builder.setSmallIcon(R.drawable.notification_icon);
        builder.setLargeIcon(Icon.createWithResource("mipmap", R.mipmap.ic_launcher));
        builder.setContentTitle(title);
        builder.setContentText(text);

        notificationManager.notify(new Random().nextInt(), builder.build());
    }

    /*
     * Sync cloud clipboard to local
     */
    public void syncDown() {
        if (getPreferences().getBoolean("sync_down", true)) {
            api.getClipboard(getEncryptionPass(), new ZyncAPI.ZyncCallback<ZyncClipData>() {
                @Override
                public void success(ZyncClipData value) {
                    String data;

                    if (value != null && (data = value.data()) != null
                            && isTypeSupported(value.type())) {
                        ZyncClipboardService.getInstance().writeToClip(data, false);
                    }
                }

                @Override
                public void handleError(ZyncError error) {
                    // ignored
                }
            });
        }
    }

    public boolean isTypeSupported(ZyncClipType type) {
        return type == ZyncClipType.TEXT;
    }

    public void openSettings(Context context) {
        Intent settingsIntent = new Intent(context, SettingsActivity.class);

        settingsIntent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, SettingsActivity.GeneralPreferenceFragment.class.getName());
        settingsIntent.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true);
        startActivity(settingsIntent);
    }

    public String getEncryptionPass() {
        SharedPreferences preferences = getPreferences();
        return preferences.getBoolean("encryption_enabled", false) ?
                preferences.getString("encryption_pass", "default") : null;
    }

    public RequestQueue httpRequestQueue() {
        return httpRequestQueue;
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
}
