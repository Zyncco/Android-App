package co.zync.android.services;

import co.zync.android.ZyncApplication;
import co.zync.android.api.ZyncAPI;
import co.zync.android.api.ZyncClipData;
import co.zync.android.api.ZyncClipType;
import co.zync.android.api.ZyncError;
import co.zync.android.api.callback.ZyncCallback;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import okhttp3.OkHttpClient;
import org.json.JSONException;

/**
 * Listens for messages from Firebase and acts accordingly
 * as outlined in the Spec doc.
 *
 * @author Mazen Kotb
 */
public class ZyncMessagingService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        final ZyncApplication application = ((ZyncApplication) getApplication());

        if (remoteMessage.getData().containsKey("zync-token")) {
            String zyncToken = remoteMessage.getData().get("zync-token");
            String randomToken = remoteMessage.getData().get("random-token");

            try {
                ZyncAPI.validateDevice(application.httpClient(), zyncToken, randomToken, new ZyncCallback<ZyncAPI>() {
                    @Override
                    public void success(ZyncAPI value) {
                        application.setApi(value);
                        application.authenticateCallback().callback(value);
                    }

                    @Override
                    public void handleError(ZyncError error) {
                    }
                });
            } catch (JSONException ignord) {
            }
            System.out.println("validate message");
            return;
        }

        if (!application.getConfig().syncDown()) {
            return;
        }

        // TODO filter if already in history

        ZyncClipType clipType;

        try {
            clipType = ZyncClipType.valueOf(remoteMessage.getData().get("payload-type"));
        } catch (IllegalArgumentException ignored) {
            return;
        }

        if (!application.isTypeSupported(clipType)) {
            return;
        }

        /*long size = Long.valueOf(remoteMessage.getData().get("size"));
        long maxSize = application.getConfig().getMaxSize();

        if (maxSize != 0 && size > maxSize) {
            return;
        }*/

        long timestamp = Long.valueOf(remoteMessage.getData().get("timestamp"));

        // (we might be getting our own message here)
        for (ZyncClipData data : application.getConfig().getHistory()) {
            if (data.timestamp() == timestamp) {
                return;
            }
        }

        // if for some reason our api and client have unloaded, reload them
        if (application.getApi() == null) {
            if (application.httpClient() == null) {
                application.setHttpClient(new OkHttpClient());
            }

            application.setApi(new ZyncAPI(application.httpClient(), application.getConfig().apiToken()));
        }

        application.syncDown();
    }
}
