package co.zync.zync.api;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class PostClipboardRequest extends JsonObjectRequest {
    private String token;

    public PostClipboardRequest(JSONObject body, String token, final ZyncAPI.ZyncResponseListener responseListener) {
        super(Request.Method.POST, ZyncAPI.BASE + ZyncAPI.VERSION + "/clipboard", body, new ResponseListener(responseListener), new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                responseListener.handleError(new ZyncError(-4, "HTTP Error: " + error.getMessage()));
            }
        });
        this.token = token;
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        Map<String,String> headers = new HashMap<>();

        headers.put("X-ZYNC-TOKEN", token);

        return headers;
    }

    protected static class ResponseListener implements Response.Listener<JSONObject> {
        private final ZyncAPI.ZyncResponseListener responseListener;

        ResponseListener(ZyncAPI.ZyncResponseListener responseListener) {
            this.responseListener = responseListener;
        }

        @Override
        public void onResponse(JSONObject node) {
            try {
                if (node == null || !node.has("success")) {
                    responseListener.handleError(new ZyncError(-1, "Server failed to produce a valid response"));
                }

                if (node.getBoolean("success")) {
                    responseListener.success();
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
    }
}
