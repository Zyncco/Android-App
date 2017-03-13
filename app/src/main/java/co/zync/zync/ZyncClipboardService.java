package co.zync.zync;

import android.app.Service;
import android.content.*;
import android.os.AsyncTask;
import android.os.IBinder;
import co.zync.zync.api.ZyncAPI;
import co.zync.zync.api.ZyncClipData;
import co.zync.zync.api.ZyncClipType;
import co.zync.zync.api.ZyncError;
import org.json.JSONException;

import java.nio.charset.Charset;

/**
 * @author Mazen Kotb
 */
public class ZyncClipboardService extends Service {
    private static ZyncClipboardService instance = null;
    private ZyncApplication app;
    private ClipboardManager clipMan;

    public ZyncClipboardService() {
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
        clipMan.addPrimaryClipChangedListener(new ZyncClipboardListener());
    }

    public byte[] getRawData() {
        ClipData data = clipMan.getPrimaryClip();
        ClipData.Item item = data.getItemAt(0);

        switch (data.getDescription().getMimeType(0)) {
            case ClipDescription.MIMETYPE_TEXT_HTML:
            case ClipDescription.MIMETYPE_TEXT_PLAIN:
                return item.getText().toString().getBytes(Charset.forName("UTF-8"));

            case ClipDescription.MIMETYPE_TEXT_URILIST:
                // ????
                return new byte[256];

            default:
                return new byte[0];
        }
    }

    public void writeToClip(int id, String data, boolean html) {
        clipMan.setPrimaryClip(new ClipData(
                "zync_paste_" + id,
                new String[] {html ? ClipDescription.MIMETYPE_TEXT_HTML : ClipDescription.MIMETYPE_TEXT_PLAIN},
                new ClipData.Item(data)
        ));
    }


    public void writeImageToClip(int id, byte[] data) {
        // TODO figure out how this works
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public class ZyncClipboardListener implements ClipboardManager.OnPrimaryClipChangedListener {
        @Override
        public void onPrimaryClipChanged() {
            new ZyncPostClipTask(app, getRawData()).execute();
        }
    }

    public static class ZyncPostClipTask extends AsyncTask<Void, Void, Void> {
        private final ZyncApplication app;
        private final byte[] data;

        public ZyncPostClipTask(ZyncApplication app, byte[] data) {
            this.app = app;
            this.data = data;
        }

        @Override
        protected Void doInBackground(Void... params) {
            // act on data and push to servers async
            try {
                ZyncAPI.clipboard(
                        app.httpRequestQueue(),
                        new ZyncClipData(null, ZyncClipType.TEXT, data),
                        "no_token",
                        new ZyncAPI.ZyncResponseListener() {
                            @Override
                            public void success() {
                                System.out.println("Sent request!");
                            }

                            @Override
                            public void handleError(ZyncError error) {
                                System.out.println("error");
                            }
                        }
                );
            } catch (JSONException ignored) {
            }
            return null;
        }
    }
}
