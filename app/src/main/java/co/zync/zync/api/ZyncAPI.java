package co.zync.zync.api;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import org.json.JSONException;
import org.json.JSONObject;

public class ZyncAPI {
    public static final String BASE = "https://zync-123456.appspot.com/api/v";
    public static final int VERSION = 0;
    private final RequestQueue queue;
    private final String token;

    private ZyncAPI(RequestQueue queue, String token) {
        this.queue = queue;
        this.token = token;
    }

    public static void signup(final RequestQueue queue, String idToken, final SignupCallback callback) {
        ZyncGenericAPIListener listener = new ZyncGenericAPIListener(new ZyncGenericAPIListener.ListenerCallback() {
            @Override
            public void success(JSONObject node) {
                try {
                    callback.success(new ZyncAPI(queue, node.getJSONObject("data").getString("zync_token")));
                } catch (JSONException ignored) {
                }
            }

            @Override
            public void handleError(ZyncError error) {
                callback.handleError(error);
            }
        });
        JsonObjectRequest request = new JsonObjectRequest(BASE + VERSION + "/callback?token=" + idToken, null, listener, listener);

        queue.add(request);
    }

    /*
     * Sends a request using the Volley library to post a clipboard update
     */
    public void clipboard(ZyncClipData clipData,
                                 final ZyncResponseListener responseListener) throws JSONException {
        JSONObject body = new JSONObject().put("data", clipData.toJson());
        ZyncAuthenticatedRequest request = new ZyncAuthenticatedRequest(
                "clipboard",
                body,
                token,
                new ZyncGenericAPIListener(new ZyncGenericAPIListener.GenericListenerCallback(responseListener))
        );
        queue.add(request);
    }

    public interface SignupCallback {
        void success(ZyncAPI api);
        void handleError(ZyncError error);
    }

    public interface ZyncResponseListener {
        void success();
        void handleError(ZyncError error);
    }
}
