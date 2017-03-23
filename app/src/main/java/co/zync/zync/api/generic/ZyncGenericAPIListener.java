package co.zync.zync.api.generic;

import co.zync.zync.api.ZyncAPI;
import co.zync.zync.api.ZyncError;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author Mazen Kotb
 */
public class ZyncGenericAPIListener implements Response.Listener<JSONObject>, Response.ErrorListener {
    private final ZyncAPI.ZyncCallback<JSONObject> responseListener;

    public ZyncGenericAPIListener(ZyncAPI.ZyncCallback<JSONObject> responseListener) {
        this.responseListener = responseListener;
    }

    public <T> ZyncGenericAPIListener(ZyncAPI.ZyncCallback<T> callback, ZyncTransformer<T> transformer) {
        this.responseListener = new ZyncTransformerCallback<>(callback, transformer);
    }

    @Override
    public void onErrorResponse(VolleyError error) {
        if (error.networkResponse != null && error.networkResponse.statusCode != 404) {
            try {
                onResponse(new JSONObject(new String(error.networkResponse.data)));
                return;
            } catch (JSONException ignored) {
            }
        }

        responseListener.handleError(new ZyncError(-4, "HTTP Error: " +
                (error.getCause() != null ? error.getCause().getClass().getSimpleName() : "null") + ":" + error.getMessage()));
    }

    @Override
    public void onResponse(JSONObject node) {
        try {
            if (node == null || !node.has("success")) {
                responseListener.handleError(new ZyncError(-1, "Server failed to produce a valid response"));
            }

            if (node.getBoolean("success")) {
                responseListener.success(node);
                return;
            } else if (!node.has("error")) {
                responseListener.handleError(new ZyncError(-2, "Server stated request was unsuccessful but " +
                        "did not provide error"));
                return;
            }

            JSONObject error = node.getJSONObject("error");

            if (!error.has("code") || !error.has("message")) {
                responseListener.handleError(new ZyncError(-3, "Server stated request was unsuccessful but " +
                        "did not provide a proper error"));
                return;
            }

            responseListener.handleError(new ZyncError(error.getInt("code"), error.getString("message")));
        } catch (JSONException ignored) {
        }
    }
}
