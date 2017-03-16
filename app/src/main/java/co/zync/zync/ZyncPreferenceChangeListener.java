package co.zync.zync;

import android.content.SharedPreferences;

/**
 * @author Mazen Kotb
 */
public class ZyncPreferenceChangeListener implements SharedPreferences.OnSharedPreferenceChangeListener {
    private ZyncApplication app;

    public ZyncPreferenceChangeListener(ZyncApplication app) {
        this.app = app;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case "use_on_data":
                if (!app.isWifiConnected() && sharedPreferences.getBoolean("use_on_data", true)) {
                    app.setupNetwork();
                }
                return;

            case "enable_persistent_notification":
                if (sharedPreferences.getBoolean("enable_persistent_notification", true)) {
                    app.createPersistentNotification();
                } else {
                    app.removePersistentNotification();
                }
        }
    }
}
