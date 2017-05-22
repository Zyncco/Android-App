package co.zync.android.listeners;

import android.content.SharedPreferences;
import co.zync.android.ZyncApplication;
import co.zync.android.services.ZyncClipboardService;

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
                boolean isConnected = app.isWifiConnected();

                if (!isConnected && sharedPreferences.getBoolean("use_on_data", true)) {
                    app.setupNetwork();
                } else if (!isConnected && !sharedPreferences.getBoolean("use_on_data", true)) {
                    app.removeNetworkUsages();
                }
                return;

            case "enable_persistent_notification":
                if (sharedPreferences.getBoolean("enable_persistent_notification", true)) {
                    ZyncClipboardService.getInstance().becomePersistent();
                } else {
                    ZyncClipboardService.getInstance().removeNotification();
                }
        }
    }
}
