package co.zync.android.services;

import android.app.Notification;
import android.app.Service;
import android.content.*;
import android.os.Build;
import android.os.IBinder;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import co.zync.android.R;
import co.zync.android.ZyncApplication;
import co.zync.android.api.*;
import co.zync.android.api.callback.ZyncCallback;
import co.zync.android.utils.ZyncExceptionInfo;
import org.json.JSONException;

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
        instance = this;
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

        if (app.getConfig().persistentNotification()) {
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

            if (!app.getConfig().syncUp()) {
                return;
            }

            long maxSize = app.getConfig().getMaxSize();

            if (maxSize != 0 && data.length > maxSize) {
                return;
            }

            if (data.length != 0) {
                ZyncClipData clipData;

                try {
                    clipData = new ZyncClipData(ZyncClipType.TEXT, data);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    ZyncApplication.LOGGED_EXCEPTIONS.add(new ZyncExceptionInfo(ex, "create ClipData from copy"));
                    app.setLastRequestStatus(false);
                    return;
                }

                try {
                    app.getApi().postClipboard(app.getConfig().getEncryptionPass(), clipData, new ZyncCallback<Void>() {
                        @Override
                        public void success(Void value) {
                            app.sendClipPostedNotification();
                            app.setLastRequestStatus(true);
                        }

                        @Override
                        public void handleError(ZyncError error) {
                            app.sendClipErrorNotification();
                            app.setLastRequestStatus(false);
                            ZyncApplication.LOGGED_EXCEPTIONS.add(new ZyncExceptionInfo(new ZyncAPIException(error), "post clipboard"));
                            Log.e("ZyncClipboardService", "There was an error posting the clipboard: "
                                    + error.code() + " : " + error.message());
                        }
                    });
                } catch (JSONException ignored) {
                }

                app.getConfig().addToHistory(clipData);
            }
        }
    }
}
