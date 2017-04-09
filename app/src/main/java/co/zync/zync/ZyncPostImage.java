package co.zync.zync;

import co.zync.zync.api.*;
import co.zync.zync.api.callback.ZyncCallback;
import co.zync.zync.utils.ZyncExceptionInfo;

import java.io.File;
import java.net.URL;

/**
 * Class which posts image (from file) to cloud clipboard
 *
 * @author Mazen Kotb
 */
public final class ZyncPostImage {
    public static void exec(final ZyncApplication app, final File file,
                            final ZyncClipData data, final ZyncCallback<Void> callback) {
        try {
            // now our data is ready, we can request a URL to stream our file to
            app.getApi().requestUploadUrl(data, new ZyncCallback<URL>() {
                @Override
                public void success(URL url) {
                    // do not upload to any URL except the API
                    // this avoids any type of redirect attack
                    if (!ZyncAPI.API_DOMAIN.equals(url.getHost().replace("www.", ""))) {
                        handleError(new ZyncError(-7, "Provided Upload URL is not the API!"));
                        return;
                    }

                    // finally, upload our file async
                    app.getApi().upload(file, url, new ZyncCallback<Void>() {
                        @Override
                        public void success(Void value) {
                            callback.success(value);
                            app.addToHistory(data);
                        }

                        @Override
                        public void handleError(ZyncError error) {
                            callback.handleError(error);
                        }
                    });
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
