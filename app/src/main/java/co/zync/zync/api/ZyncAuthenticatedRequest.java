package co.zync.zync.api;

import co.zync.zync.api.generic.ZyncGenericAPIListener;
import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/**
 * An authenticated request from Zync's servers.
 * Due to the nature of Volley's design, this class
 * had to be created to insert the token header
 *
 * @author Mazen Kotb
 */
public class ZyncAuthenticatedRequest extends JsonObjectRequest {
    private String token;

    public ZyncAuthenticatedRequest(int httpMethod, String method, JSONObject body, String token, final ZyncGenericAPIListener listener) {
        super(httpMethod, ZyncAPI.BASE + ZyncAPI.VERSION + "/" + method, body, listener, listener);
        this.token = token;
    }

    @Override
    protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
        try {
            String jsonString = new String(response.data,
                    HttpHeaderParser.parseCharset(response.headers));
            return Response.success(new JSONObject(jsonString),
                    HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException | JSONException e) {
            return Response.error(new ParseError(e));
        }
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        Map<String,String> headers = new HashMap<>();

        headers.put("X-ZYNC-TOKEN", token);

        return headers;
    }
}
