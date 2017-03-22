package co.zync.zync.api;

import co.zync.zync.api.generic.ZyncGenericAPIListener;
import co.zync.zync.api.generic.ZyncNullTransformer;
import co.zync.zync.api.generic.ZyncTransformer;
import co.zync.zync.api.generic.ZyncTransformerCallback;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.AEADBadTagException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ZyncAPI {
    public static final String BASE = "https://api.zync.co/v";
    public static final int VERSION = 0;
    private RequestQueue queue;
    private final String token;

    public ZyncAPI(RequestQueue queue, String token) {
        this.queue = queue;
        this.token = token;
    }

    public static void signup(final RequestQueue queue, String idToken, final ZyncCallback<ZyncAPI> callback) {
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
        JsonObjectRequest request = new JsonObjectRequest(BASE + VERSION + "/user/authenticate?token=" + idToken, null, listener, listener);

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

    public void getClipboard(final String encryptionKey, final List<Long> timestamps,
                             final ZyncCallback<List<ZyncClipData>> callback) {
        StringBuilder builder = new StringBuilder();

        for (Long timestamp : timestamps) {
            builder.append(timestamp.longValue()).append(",");
        }

        builder.setLength(builder.length() - 1);

        System.out.println("builder: " + builder.toString());

        ZyncAuthenticatedRequest request = new ZyncAuthenticatedRequest(
                Request.Method.GET,
                "clipboard/" + builder.toString(),
                null,
                token,
                new ZyncGenericAPIListener(callback, new ZyncTransformer<List<ZyncClipData>>() {
                    @Override
                    public List<ZyncClipData> transform(JSONObject obj) throws Exception {
                        if (timestamps.size() == 1) {
                            try {
                                return Collections.singletonList(new ZyncClipData(encryptionKey, obj.getJSONObject("data")));
                            } catch (AEADBadTagException ignored) {
                                return Collections.emptyList();
                            }
                        } else {
                            JSONArray clips = obj.getJSONObject("data").getJSONArray("clipboards");
                            List<ZyncClipData> clipboards = new ArrayList<>(clips.length());

                            for (int i = 0; i < clips.length(); i++) {
                                try {
                                    clipboards.add(new ZyncClipData(encryptionKey, clips.getJSONObject(i)));
                                } catch (AEADBadTagException ignored) {
                                    // pass is wrong / encryption error, ignore
                                }
                            }

                            return clipboards;
                        }
                    }
                })
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
                    public ZyncClipData transform(JSONObject obj) throws Exception {
                        return new ZyncClipData(encryptionKey, obj.getJSONObject("data"));
                    }
                })
        );
        queue.add(request);
    }

    public void getHistory(final String encryptionKey, final ZyncCallback<List<ZyncClipData>> callback) {
        ZyncAuthenticatedRequest request = new ZyncAuthenticatedRequest(
                Request.Method.GET,
                "clipboard/history",
                null,
                token,
                new ZyncGenericAPIListener(callback, new ZyncTransformer<List<ZyncClipData>>() {
                    @Override
                    public List<ZyncClipData> transform(JSONObject obj) {
                        try {
                            JSONArray array = obj.getJSONObject("data").getJSONArray("history");
                            List<ZyncClipData> history = new ArrayList<>(array.length());

                            for (int i = 0; i < array.length(); i++) {
                                history.add(new ZyncClipData(encryptionKey, array.getJSONObject(i)));
                            }

                            Collections.sort(history, new ZyncClipData.TimeComparator()); // sort by time
                            return history;
                        } catch (Exception ex) {
                            ex.printStackTrace();
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

    public RequestQueue queue() {
        return queue;
    }

    public void setQueue(RequestQueue queue) {
        this.queue = queue;
    }

    public interface ZyncCallback<T> {
        void success(T value);
        void handleError(ZyncError error);
    }
}
