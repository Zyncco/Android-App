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

            default:
                return new byte[0];
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

            if (!app.getPreferences().getBoolean("sync_up", true)) {
                return;
            }

            if (data.length != 0) {
                new ZyncPostClipTask(app, data, ZyncClipType.TEXT).execute();
            }
        }
    }
}
