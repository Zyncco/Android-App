package co.zync.zync.api;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ZyncAuthenticatedRequest extends JsonObjectRequest {
    private String token;

    public ZyncAuthenticatedRequest(String method, JSONObject body, String token, final ZyncGenericAPIListener listener) {
        super(Request.Method.POST, ZyncAPI.BASE + ZyncAPI.VERSION + "/" + method, body, listener, listener);
        this.token = token;
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        Map<String,String> headers = new HashMap<>();

        headers.put("X-ZYNC-TOKEN", token);

        return headers;
    }
}
