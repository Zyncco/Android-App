package co.zync.zync;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import co.zync.zync.api.*;
import co.zync.zync.utils.ZyncCrypto;
import co.zync.zync.utils.ZyncExceptionInfo;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;

/**
 * Task which compresses (PNG) and posts image to cloud clipboard
 *
 * @author Mazen Kotb
 */
public class ZyncPostImageTask extends AsyncTask<Uri, Void, Void> {
    private final ZyncApplication app;
    private final File file;
    private final ZyncClipData data;
    private ZyncAPI.ZyncCallback<Void> callback = new ZyncAPI.ZyncCallback<Void>() {
        @Override
        public void success(Void v) {
        }

        @Override
        public void handleError(ZyncError error) {

        }
    };

    public ZyncPostImageTask(ZyncApplication app, File file, ZyncClipData data) {
        this.app = app;
        this.file = file;
        this.data = data;
    }

    public ZyncPostImageTask(ZyncApplication app, File file, ZyncClipData data, ZyncAPI.ZyncCallback<Void> callback) {
        this.app = app;
        this.file = file;
        this.data = data;
        this.callback = callback;
    }

    @Override
    protected Void doInBackground(Uri... params) {
        try {
            // converting to PNG creates a universal file format
            // and does ZLIB compression for us, great!
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(app.getContentResolver(), params[0]);
            Cipher cipher = ZyncCrypto.getCipher(Cipher.ENCRYPT_MODE, app.getEncryptionPass(), data.salt(), data.iv());
            // use this output stream to encrypt the data of the image to file
            // saves another round of I/O
            CipherOutputStream output = new CipherOutputStream(new FileOutputStream(file), cipher);

            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output);

            output.flush();
            output.close();

            // now our data is ready, we can request a URL to stream our file to
            app.getApi().requestUploadUrl(data, new ZyncAPI.ZyncCallback<URL>() {
                @Override
                public void success(URL url) {
                    // do not upload to any URL except the API
                    // this avoids any type of redirect attack
                    if (!ZyncAPI.API_DOMAIN.equals(url.getHost().replace("www.", ""))) {
                        handleError(new ZyncError(-7, "Provided Upload URL is not the API!"));
                        return;
                    }

                    // finally, upload our file async
                    app.getApi().upload(file, url, new ZyncAPI.ZyncCallback<Void>() {
                        @Override
                        public void success(Void value) {
                            callback.success(value);
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
        return null;
    }
}
