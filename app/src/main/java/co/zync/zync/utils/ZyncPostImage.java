package co.zync.zync.utils;

import android.app.NotificationManager;
import android.content.Context;
import android.support.v4.app.NotificationCompat;
import co.zync.zync.R;
import co.zync.zync.ZyncApplication;
import co.zync.zync.api.*;
import co.zync.zync.api.callback.ProgressCallback;
import co.zync.zync.api.callback.ZyncCallback;

import java.io.*;

/**
 * Class which posts image (from file) to cloud clipboard
 *
 * @author Mazen Kotb
 */
public final class ZyncPostImage {
    public static void exec(final ZyncApplication app, final ZyncClipData data,
                            final ZyncCallback<Void> callback) {
        try {
            final NotificationManager notifManager = (NotificationManager) app.getSystemService(Context.NOTIFICATION_SERVICE);
            final int fileSize = (int) app.getDataManager().fileFor(data, false).length();
            final int size = fileSize + (16 - (fileSize % 16));
            final int notificationId = ZyncApplication.CLIPBOARD_PROGRESS_ID;
            final NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(app.getApplicationContext())
                    .setContentTitle(app.getString(R.string.uploading_image))
                    .setContentText("0%")
                    .setProgress(size, 0, true)
                    .setSmallIcon(R.drawable.notification_icon);

            notifManager.notify(notificationId, notifBuilder.build());

            if (data.hash() == null) {
                data.setHash(ZyncClipData.hashCrc(app.getDataManager().cryptoStreamFor(data)));
                app.getConfig().update(data);
            }

            // now our data is ready, we can request a URL to stream our file to
            app.getApi().requestUploadUrl(data, new ZyncCallback<String>() {
                @Override
                public void success(String token) {
                    // finally, upload our file async
                    app.getApi().upload(
                            new Provider<InputStream>() {
                                @Override
                                public InputStream get() {
                                    return app.getDataManager().cryptoStreamFor(data);
                                }
                            },
                            token,
                            callback,
                            new ProgressCallback() {
                                @Override
                                public void callback(int bytesRead) {
                                    double percent = ((double) bytesRead / (double) size) * 100d;

                                    notifBuilder.setProgress(size, bytesRead, false);
                                    notifBuilder.setContentText(String.valueOf(Math.floor(percent))
                                            .replace(".0", "") + "%");

                                    if (bytesRead >= size) {
                                        notifBuilder.setContentTitle(app.getString(R.string.upload_complete));
                                        notifBuilder.setContentText(null);
                                    }

                                    notifManager.notify(notificationId, notifBuilder.build());

                                    if (bytesRead >= size) {
                                        notifManager.cancel(notificationId);
                                    }
                                }
                            }
                    );
                }

                @Override
                public void handleError(ZyncError error) {
                    callback.handleError(error);
                }
            });
        } catch (Exception ex) {
            callback.handleError(new ZyncError(
                    -5,
                    "Exception=" + ex.getClass().getSimpleName() + ": " + ex.getMessage()
            ));
            ZyncApplication.LOGGED_EXCEPTIONS.add(new ZyncExceptionInfo(ex, "post image clip using PostImageTask"));
        }
    }
}
