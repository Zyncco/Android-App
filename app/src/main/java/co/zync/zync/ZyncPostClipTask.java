package co.zync.zync;

import android.os.AsyncTask;
import co.zync.zync.api.ZyncAPI;
import co.zync.zync.api.ZyncClipData;
import co.zync.zync.api.ZyncClipType;
import co.zync.zync.api.ZyncError;
import org.json.JSONException;

/**
 * @author Mazen Kotb
 */
public class ZyncPostClipTask extends AsyncTask<Void, Void, Void> {
    private final ZyncApplication app;
    private final byte[] data;
    private final ZyncClipType type;
    private ZyncAPI.ZyncResponseListener listener = new ZyncAPI.ZyncResponseListener() {
        @Override
        public void success() {
            System.out.println("Sent request!");
        }

        @Override
        public void handleError(ZyncError error) {
            System.out.println("error");
        }
    };

    public ZyncPostClipTask(ZyncApplication app, byte[] data, ZyncClipType type) {
        this.app = app;
        this.data = data;
        this.type = type;
    }

    public ZyncPostClipTask(ZyncApplication app, byte[] data, ZyncClipType type, ZyncAPI.ZyncResponseListener listener) {
        this.app = app;
        this.data = data;
        this.type = type;
        this.listener = listener;
    }

    @Override
    protected Void doInBackground(Void... params) {
        // act on data and push to servers async
        try {
            ZyncAPI.clipboard(
                    app.httpRequestQueue(),
                    new ZyncClipData(null, type, data),
                    "no_token",
                    listener
            );
        } catch (JSONException ignored) {
        }
        return null;
    }
}
