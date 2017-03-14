package co.zync.zync.firebase;

import co.zync.zync.ZyncApplication;
import co.zync.zync.ZyncClipboardService;
import co.zync.zync.api.ZyncAPI;
import co.zync.zync.api.ZyncClipData;
import co.zync.zync.api.ZyncClipType;
import co.zync.zync.api.ZyncError;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

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

        if (clipType != ZyncClipType.TEXT) {
            return;
        }

        // todo filter by size

        application.getApi().getClipboard(
                application.getPreferences().getString("encryption_pass", null),
                new ZyncAPI.ZyncCallback<ZyncClipData>() {
                    @Override
                    public void success(ZyncClipData value) {
                        if (value == null || value.data() == null) {
                            return;
                        }

                        ZyncClipboardService.getInstance().writeToClip(value.data(), false);
                    }

                    @Override
                    public void handleError(ZyncError error) {
                        // ignored
                    }
                });
    }
}
