package co.zync.zync;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import co.zync.zync.api.ZyncAPI;
import co.zync.zync.api.ZyncClipData;
import co.zync.zync.api.ZyncClipType;
import co.zync.zync.api.ZyncError;
import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ZyncPostImageTask extends AsyncTask<Uri, Void, Void> {
    private final ZyncApplication app;
    private final ContentResolver resolver;

    public ZyncPostImageTask(ZyncApplication app, ContentResolver resolver) {
        this.app = app;
        this.resolver = resolver;
    }

    @Override
    protected Void doInBackground(Uri... params) {
        Bitmap bitmap;

        try {
            bitmap = MediaStore.Images.Media.getBitmap(resolver, params[0]);
        } catch (IOException ex) {
            // somehow notify user that there was an error
            return null;
        }

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();

        // TODO do size-checks

        new ZyncPostClipTask(app, byteArray, ZyncClipType.IMAGE, new ZyncAPI.ZyncResponseListener() {
            @Override
            public void success() {
            }

            @Override
            public void handleError(ZyncError error) {

            }
        }).doInBackground();
        return null;
    }
}
