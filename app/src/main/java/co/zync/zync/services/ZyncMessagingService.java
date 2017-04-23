package co.zync.zync.services;

import co.zync.zync.R;
import co.zync.zync.ZyncApplication;
import co.zync.zync.api.ZyncAPI;
import co.zync.zync.api.ZyncClipType;
import co.zync.zync.api.ZyncError;
import co.zync.zync.api.callback.ZyncCallback;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
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
        if (remoteMessage.getData().size() != 2) {
            return;
        }

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
            return;
        }

        if (!application.getConfig().syncDown()) {
            return;
        }

        if (!remoteMessage.getData().containsKey("type") ||
                !remoteMessage.getData().containsKey("size")) {
            return;
        }

        // TODO filter if already in history

        ZyncClipType clipType;

        try {
            clipType = ZyncClipType.valueOf(remoteMessage.getData().get("type"));
        } catch (IllegalArgumentException ignored) {
            return;
        }

        if (!application.isTypeSupported(clipType)) {
            return;
        }

        long size = Long.valueOf(remoteMessage.getData().get("size"));
        long maxSize = application.getConfig().getMaxSize();

        if (maxSize != 0 && size > maxSize) {
            return;
        }

        application.syncDown();
        application.sendNotification(
                ZyncApplication.CLIPBOARD_UPDATED_ID,
                getString(R.string.clipboard_changed_notification),
                getString(R.string.clipboard_changed_notification_desc)
        );
    }
}
