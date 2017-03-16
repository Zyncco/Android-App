package co.zync.zync.api;

import co.zync.zync.api.generic.ZyncGenericAPIListener;
import co.zync.zync.api.generic.ZyncNullTransformer;
import co.zync.zync.api.generic.ZyncTransformer;
import co.zync.zync.api.generic.ZyncTransformerCallback;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import org.json.JSONException;
import org.json.JSONObject;

public class ZyncAPI {
    public static final String BASE = "https://zync-123456.appspot.com/api/v";
    public static final int VERSION = 0;
    private final RequestQueue queue;
    private final String token;

    public ZyncAPI(RequestQueue queue, String token) {
        this.queue = queue;
        this.token = token;
    }

    public static void signup(final RequestQueue queue, String idToken, String provider, final ZyncCallback<ZyncAPI> callback) {
        ZyncGenericAPIListener listener = new ZyncGenericAPIListener(new ZyncCallback<JSONObject>() {
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
        JsonObjectRequest request = new JsonObjectRequest(BASE + VERSION + "/user/callback?token=" + idToken + "&provider=" + provider, null, listener, listener);

        queue.add(request);
    }

    public static ZyncAPI login(final RequestQueue queue, String zyncToken) {
        return new ZyncAPI(queue, zyncToken);
    }

    /*
     * Sends a request using the Volley library to post a postClipboard update
     */
    public void postClipboard(ZyncClipData clipData,
                              final ZyncCallback<Void> responseListener) throws JSONException {
        JSONObject body = new JSONObject().put("data", clipData.toJson());
        ZyncAuthenticatedRequest request = new ZyncAuthenticatedRequest(
                Request.Method.POST,
                "clipboard",
                body,
                token,
                new ZyncGenericAPIListener(responseListener, new ZyncNullTransformer<Void>())
        );
        queue.add(request);
    }

    /*
     * Sends a request to get the clipboard from the servers
     * This method handles the decryption, decompression, and hash verification of the data.
     * If any of these processes fail, null will be returned to the callback.
     */
    public void getClipboard(final String encryptionKey, final ZyncCallback<ZyncClipData> callback) {
        ZyncAuthenticatedRequest request = new ZyncAuthenticatedRequest(
                Request.Method.GET,
                "clipboard",
                null,
                token,
                new ZyncGenericAPIListener(callback, new ZyncTransformer<ZyncClipData>() {
                    @Override
                    public ZyncClipData transform(JSONObject obj) {
                        try {
                            return new ZyncClipData(encryptionKey, obj);
                        } catch (Exception ex) {
                            return null;
                        }
                    }
                })
        );
        queue.add(request);
    }

    public String getToken() {
        return token;
    }

    public interface ZyncCallback<T> {
        void success(T value);
        void handleError(ZyncError error);
    }
}
