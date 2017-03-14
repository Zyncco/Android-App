package co.zync.zync.api;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author Mazen Kotb
 */
public class ZyncGenericAPIListener implements Response.Listener<JSONObject>, Response.ErrorListener {
    private final ListenerCallback responseListener;

    public ZyncGenericAPIListener(ListenerCallback responseListener) {
        this.responseListener = responseListener;
    }

    @Override
    public void onErrorResponse(VolleyError error) {
        responseListener.handleError(new ZyncError(-4, "HTTP Error: " + error.getMessage()));
    }

    @Override
    public void onResponse(JSONObject node) {
        try {
            if (node == null || !node.has("success")) {
                responseListener.handleError(new ZyncError(-1, "Server failed to produce a valid response"));
            }

            if (node.getBoolean("success")) {
                responseListener.success(node);
            } if (!node.has("error")) {
                responseListener.handleError(new ZyncError(-2, "Server stated request was unsuccessful but " +
                        "did not provide error"));
            }

            JSONObject error = node.getJSONObject("error");

            if (!error.has("code") || !error.has("message")) {
                responseListener.handleError(new ZyncError(-3, "Server stated request was unsuccessful but " +
                        "did not provide a proper error"));
            }

            responseListener.handleError(new ZyncError(error.getInt("code"), error.getString("message")));
        } catch (JSONException ignored) {
        }
    }

    public interface ListenerCallback {
        void success(JSONObject node);
        void handleError(ZyncError error);
    }

    public static class GenericListenerCallback implements ListenerCallback {
        private final ZyncAPI.ZyncResponseListener listener;


        public GenericListenerCallback(ZyncAPI.ZyncResponseListener listener) {
            this.listener = listener;
        }

        @Override
        public void success(JSONObject node) {
            listener.success();
        }

        @Override
        public void handleError(ZyncError error) {
            listener.handleError(error);
        }
    }
}
