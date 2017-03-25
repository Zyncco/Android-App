package co.zync.zync;

import android.app.Notification;
import android.app.Service;
import android.content.*;
import android.os.Build;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import co.zync.zync.api.ZyncAPI;
import co.zync.zync.api.ZyncClipData;
import co.zync.zync.api.ZyncClipType;
import co.zync.zync.api.ZyncError;
import co.zync.zync.utils.ZyncExceptionInfo;

import javax.crypto.AEADBadTagException;
import java.nio.charset.Charset;

/**
 * @author Mazen Kotb
 */
public class ZyncClipboardService extends Service {
    private static ZyncClipboardService instance = null;
    private ZyncApplication app;
    private ClipboardManager clipMan;
    private ZyncClipboardListener clipboardListener;

    public ZyncClipboardService() {
    }

    public static void nullify() {
        instance = null;
    }

    public static ZyncClipboardService getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.clipMan = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        this.app = (ZyncApplication) getApplication();
        instance = this;
        clipboardListener = new ZyncClipboardListener();
        clipMan.addPrimaryClipChangedListener(clipboardListener);

        if (app.getPreferences().getBoolean("enable_persistent_notification", true)) {
            becomePersistent();
        }
    }

    @Override
    public void onDestroy() {
        clipMan.removePrimaryClipChangedListener(clipboardListener);
    }

    public void becomePersistent() {
        // request that this service operates on the foreground and doesn't get killed
        // to do this, we must have a persistent notification
        startForeground(ZyncApplication.PERSISTENT_NOTIFICATION_ID, new Notification.Builder(this)
                .setOngoing(true)
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle(getString(R.string.zync_persistentnotif_title))
                .setContentText(getString(R.string.zync_persistentnotif_descr))
                .setPriority(Notification.PRIORITY_MIN)
                .build());
    }

    public void removeNotification() {
        stopForeground(true);
    }

    public byte[] getRawData() {
        ClipData data = clipMan.getPrimaryClip();
        ClipData.Item item = data.getItemAt(0);

        switch (data.getDescription().getMimeType(0)) {
            case ClipDescription.MIMETYPE_TEXT_HTML:
                if (item.getHtmlText() == null) {
                    return null;
                }

                return fromHtml(item.getHtmlText()).toString().getBytes(Charset.forName("UTF-8"));

            case ClipDescription.MIMETYPE_TEXT_PLAIN:
                if (item.getText() == null) {
                    return null;
                }

                return item.getText().toString().getBytes(Charset.forName("UTF-8"));

            default:
                return new byte[0];
        }
    }

    private Spanned fromHtml(String text) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return Html.fromHtml(text);
        } else {
            return Html.fromHtml(text, Html.FROM_HTML_MODE_COMPACT);
        }
    }

    public void writeToClip(String data, boolean html) {
        clipMan.setPrimaryClip(new ClipData(
                "zync_paste",
                new String[] {html ? ClipDescription.MIMETYPE_TEXT_HTML : ClipDescription.MIMETYPE_TEXT_PLAIN},
                new ClipData.Item(data)
        ));
    }


    public void writeImageToClip(byte[] data) {
        // TODO figure out how this works
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /*
     * Listens for changes in the clipboard of the system
     * and posts them to the server async (if it's enabled)
     */
    public class ZyncClipboardListener implements ClipboardManager.OnPrimaryClipChangedListener {
        @Override
        public void onPrimaryClipChanged() {
            byte[] data = getRawData();

            if (data == null) {
                return;
            }

            if ("zync_paste".equals(clipMan.getPrimaryClipDescription().getLabel())) {
                return;
            }

            if (app.getApi() == null) {
                return; // they haven't logged in yet
            }

            if (!app.getPreferences().getBoolean("sync_up", true)) {
                return;
            }

            long maxSize = app.getMaxSize();

            if (maxSize != 0 && data.length > maxSize) {
                return;
            }

            if (data.length != 0) {
                ZyncClipData clipData;

                try {
                    clipData = new ZyncClipData(app.getEncryptionPass(), ZyncClipType.TEXT, data);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    ZyncApplication.LOGGED_EXCEPTIONS.add(new ZyncExceptionInfo(ex, "create ClipData from copy"));
                    app.setLastRequestStatus(false);
                    return;
                }

                new ZyncPostClipTask(app, clipData, new ZyncAPI.ZyncCallback<Void>() {
                    @Override
                    public void success(Void value) {
                        if (app.getPreferences().getBoolean("clipboard_change_notification", true)) {
                            app.sendNotification(
                                    ZyncApplication.CLIPBOARD_POSTED_ID,
                                    getString(R.string.clipboard_posted_notification),
                                    getString(R.string.clipboard_posted_notification_desc)
                            );
                        }

                        System.out.println("posted (" + hashCode() + ")");

                        app.setLastRequestStatus(true);
                    }

                    @Override
                    public void handleError(ZyncError error) {
                        app.sendNotification(
                                ZyncApplication.CLIPBOARD_ERROR_ID,
                                getString(R.string.clipboard_post_error_notification),
                                getString(R.string.clipboard_post_error_notification_desc)
                        );
                        app.setLastRequestStatus(false);
                        Log.e("ZyncClipboardService", "There was an error posting the clipboard: "
                                + error.code() + " : " + error.message());
                    }
                }).execute();

                app.addToHistory(clipData);
            }
        }
    }
}
