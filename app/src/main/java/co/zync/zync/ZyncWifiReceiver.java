package co.zync.zync;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class ZyncWifiReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        ZyncApplication app = (ZyncApplication) context.getApplicationContext();
        ConnectivityManager conMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = conMan.getActiveNetworkInfo();

        if (netInfo != null && netInfo.getType() == ConnectivityManager.TYPE_WIFI) {
            if (!app.getPreferences().getBoolean("use_on_data", true)
                    && app.httpRequestQueue() == null) {
                app.setupNetwork();
            }
        } else if (app.httpRequestQueue() != null) {
            app.removeNetworkUsages();
        }
    }
}
