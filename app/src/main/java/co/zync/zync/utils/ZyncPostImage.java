package co.zync.zync.utils;

import co.zync.zync.ZyncApplication;
import co.zync.zync.api.*;
import co.zync.zync.api.callback.ZyncCallback;

import java.io.*;

/**
 * Class which posts image (from file) to cloud clipboard
 *
 * @author Mazen Kotb
 */
public final class ZyncPostImage {
    public static void exec(final ZyncApplication app, final File file,
                            final ZyncClipData data, final ZyncCallback<Void> callback) {
        try {
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
                            new ZyncCallback<Void>() {
                                @Override
                                public void success(Void value) {
                                    callback.success(value);
                                }

                                @Override
                                public void handleError(ZyncError error) {
                                    callback.handleError(error);
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
