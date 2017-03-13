package co.zync.zync;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.widget.TextView;

import java.nio.charset.Charset;

/**
 * @author Mazen Kotb
 */
public class ZyncClipboardHandler {
    private static ZyncClipboardHandler instance = null;
    private final ClipboardManager clipMan;

    public ZyncClipboardHandler(ZyncApplication app) {
        this.clipMan = (ClipboardManager) app.getSystemService(Context.CLIPBOARD_SERVICE);
        clipMan.addPrimaryClipChangedListener(new ZyncClipboardListener());
        instance = null;
    }

    public static ZyncClipboardHandler getInstance() {
        return instance;
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

    public class ZyncClipboardListener implements ClipboardManager.OnPrimaryClipChangedListener {
        @Override
        public void onPrimaryClipChanged() {
            byte[] data = getRawData();
            // act on data and push to servers async
        }
    }
}
