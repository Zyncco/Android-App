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
        boolean useOnData = app.getPreferences().getBoolean("use_on_data", true);

        if (netInfo != null && netInfo.getType() == ConnectivityManager.TYPE_WIFI) {
            if (!useOnData && app.httpClient() == null) {
                app.setupNetwork();
            }
        } else if (app.httpClient() != null && !useOnData) {
            app.removeNetworkUsages();
        }
    }
}
