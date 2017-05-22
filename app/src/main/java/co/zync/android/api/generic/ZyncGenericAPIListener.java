package co.zync.android.api.generic;

import co.zync.android.api.ZyncError;
import co.zync.android.api.callback.ZyncCallback;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * @author Mazen Kotb
 */
public class ZyncGenericAPIListener implements Callback {
    private final ZyncCallback<JSONObject> responseListener;

    public ZyncGenericAPIListener(ZyncCallback<JSONObject> responseListener) {
        this.responseListener = responseListener;
    }

    public <T> ZyncGenericAPIListener(ZyncCallback<T> callback, ZyncTransformer<T> transformer) {
        this.responseListener = new ZyncTransformerCallback<>(callback, transformer);
    }

    @Override
    public void onFailure(Call call, IOException e) {
        responseListener.handleError(new ZyncError(-4, "HTTP Error: " +
                (e.getCause() != null ? e.getCause().getClass().getSimpleName() : "null") + ":" + e.getMessage()));
    }

    @Override
    public void onResponse(Call call, Response response) throws IOException {
        String originalString = response.body().string();
        JSONObject node;

        try {
            node = new JSONObject(originalString);
        } catch (JSONException ex) {
            responseListener.handleError(new ZyncError(-5, "Server returned body which is not JSON! Body: " + originalString));
            return;
        }

        try {
            // ensure there is the one required field
            if (!node.has("success")) {
                responseListener.handleError(new ZyncError(-1, "Server failed to produce a valid response"));
            }

            if (node.getBoolean("success")) {
                responseListener.success(node); // if server says successful, execute callback
                return;
            } else if (!node.has("error")) { // if it's unsuccessful there *must* be error
                responseListener.handleError(new ZyncError(-2, "Server stated request was unsuccessful but " +
                        "did not provide error"));
                return;
            }

            JSONObject error = node.getJSONObject("error");

            // check required fields
            if (!error.has("code") || !error.has("message")) {
                responseListener.handleError(new ZyncError(-3, "Server stated request was unsuccessful but " +
                        "did not provide a proper error"));
                return;
            }

            // handle error sent by server
            responseListener.handleError(new ZyncError(error.getInt("code"), error.getString("message")));
        } catch (JSONException ignored) {
        }
    }
}
