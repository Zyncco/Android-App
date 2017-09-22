package co.zync.android;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import co.zync.android.services.ZyncClipboardService;
import co.zync.android.services.ZyncInstanceIdService;
import co.zync.android.services.ZyncMessagingService;

public class ZyncBootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        ZyncConfiguration config = new ZyncConfiguration(context);

        startService(context, ZyncClipboardService.class);

        // if we are connected to wifi or allowed to use data, setup Zync network services
        if (ZyncApplication.isWifiConnected(context) || config.useOnData()) {
            startService(context, ZyncInstanceIdService.class);
            startService(context, ZyncMessagingService.class);
        }
    }

    // utility method to start a service
    private void startService(Context context, Class<? extends Service> cls) {
        Intent intent = new Intent(context, cls);
        context.startService(intent);
    }
}
