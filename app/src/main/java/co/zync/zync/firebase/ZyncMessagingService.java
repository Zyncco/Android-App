package co.zync.zync.firebase;

import co.zync.zync.R;
import co.zync.zync.ZyncApplication;
import co.zync.zync.ZyncClipboardService;
import co.zync.zync.api.ZyncAPI;
import co.zync.zync.api.ZyncClipData;
import co.zync.zync.api.ZyncClipType;
import co.zync.zync.api.ZyncError;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/**
 * Listens for messages from Firebase and acts accordingly
 * as outlined in the Spec doc.
 *
 * @author Mazen Kotb
 */
public class ZyncMessagingService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // todo verify integrity

        if (remoteMessage.getData().size() != 2) {
            return;
        }

        ZyncApplication application = ((ZyncApplication) getApplication());

        if (!application.getPreferences().getBoolean("sync_down", true)) {
            return;
        }

        if (!remoteMessage.getData().containsKey("type") ||
                !remoteMessage.getData().containsKey("size")) {
            return;
        }

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
        long maxSize = application.getMaxSize();

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
