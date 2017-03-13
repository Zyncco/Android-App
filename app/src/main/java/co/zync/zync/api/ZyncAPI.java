package co.zync.zync.api;

import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONException;
import org.json.JSONObject;

public class ZyncAPI {
    public static final String BASE = "https://api.zync.co/api/v";
    public static final int VERSION = 0;

    public static ZyncError clipboard(ZyncClipData clipData, String token)
            throws UnirestException, JSONException {
        JSONObject body = new JSONObject().put("data", clipData.toJson());

        JSONObject node = Unirest.post(BASE + VERSION + "/clipboard")
                .header("X-ZYNC-TOKEN", token)
                .body(body.toString()).asJson().getBody().getObject();

        if (node == null || !node.has("success")) {
            return new ZyncError(-1, "Server failed to produce a valid response");
        }

        if (node.getBoolean("success")) {
            return null; // request was successful
        } else if (!node.has("error")) {
            return new ZyncError(-2, "Server stated request was unsuccessful but " +
                    "did not provide error");
        }

        JSONObject error = node.getJSONObject("error");

        if (!node.has("code") || !node.has("message")) {
            return new ZyncError(-3, "Server stated request was unsuccessful but " +
                    "did not provide a proper error");
        }

        return new ZyncError(node.getInt("code"), node.getString("message"));
    }
}
