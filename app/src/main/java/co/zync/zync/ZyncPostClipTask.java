package co.zync.zync;

import android.os.AsyncTask;
import co.zync.zync.api.ZyncAPI;
import co.zync.zync.api.ZyncClipData;
import co.zync.zync.api.ZyncClipType;
import co.zync.zync.api.ZyncError;

/**
 * Task which when given the appropriate variables
 * posts provided content to cloud clipboard
 *
 * @author Mazen Kotb
 */
public class ZyncPostClipTask extends AsyncTask<Void, Void, Void> {
    private final ZyncApplication app;
    private final ZyncClipData data;
    private ZyncAPI.ZyncCallback<Void> listener = new ZyncAPI.ZyncCallback<Void>() {
        @Override
        public void success(Void v) {
            System.out.println("Sent request!");
        }

        @Override
        public void handleError(ZyncError error) {
            System.out.println("error");
        }
    };

    public ZyncPostClipTask(ZyncApplication app, byte[] data, ZyncClipType type) throws Exception {
        this.app = app;
        this.data = new ZyncClipData(app.getEncryptionPass(), type, data);
    }

    public ZyncPostClipTask(ZyncApplication app, byte[] data, ZyncClipType type, ZyncAPI.ZyncCallback<Void> listener) throws Exception {
        this.app = app;
        this.data = new ZyncClipData(app.getEncryptionPass(), type, data);
        this.listener = listener;
    }

    public ZyncPostClipTask(ZyncApplication app, ZyncClipData data) {
        this.app = app;
        this.data = data;
    }

    public ZyncPostClipTask(ZyncApplication app, ZyncClipData data, ZyncAPI.ZyncCallback<Void> listener) {
        this.app = app;
        this.data = data;
        this.listener = listener;
    }

    @Override
    protected Void doInBackground(Void... params) {
        // act on data and push to servers async
        if (app.getApi() == null) {
            return null; // app is not running yet
        }

        try {
            app.getApi().postClipboard(
                    data,
                    listener
            );
        } catch (Exception ignored) {
        }

        return null;
    }

    public interface RequestStatusListener {
        void onStatusChange(boolean value);
    }
}
