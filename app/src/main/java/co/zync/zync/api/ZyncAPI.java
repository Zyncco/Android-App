package co.zync.zync.api;

import com.android.volley.RequestQueue;
import org.json.JSONException;
import org.json.JSONObject;

public class ZyncAPI {
    public static final String BASE = "https://api.zync.co/api/v";
    public static final int VERSION = 0;

    /*
     * Sends a request using the Volley library to post a clipboard update
     */
    public static void clipboard(RequestQueue queue, ZyncClipData clipData,
                                 String token, final ZyncResponseListener responseListener) throws JSONException {
        JSONObject body = new JSONObject().put("data", clipData.toJson());
        PostClipboardRequest request = new PostClipboardRequest(body, token, responseListener);
        queue.add(request);
    }

    public interface ZyncResponseListener {
        void success();
        void handleError(ZyncError error);
    }
}
