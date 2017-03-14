package co.zync.zync.api;

import co.zync.zync.api.generic.ZyncGenericAPIListener;
import com.android.volley.AuthFailureError;
import com.android.volley.toolbox.JsonObjectRequest;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ZyncAuthenticatedRequest extends JsonObjectRequest {
    private String token;

    public ZyncAuthenticatedRequest(int httpMethod, String method, JSONObject body, String token, final ZyncGenericAPIListener listener) {
        super(httpMethod, ZyncAPI.BASE + ZyncAPI.VERSION + "/" + method, body, listener, listener);
        this.token = token;
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        Map<String,String> headers = new HashMap<>();

        headers.put("X-ZYNC-TOKEN", token);

        return headers;
    }
}
