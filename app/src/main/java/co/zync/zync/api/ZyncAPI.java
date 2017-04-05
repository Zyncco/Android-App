package co.zync.zync.api;

import co.zync.zync.ZyncApplication;
import co.zync.zync.api.generic.ZyncGenericAPIListener;
import co.zync.zync.api.generic.ZyncNullTransformer;
import co.zync.zync.api.generic.ZyncTransformer;
import co.zync.zync.utils.ZyncExceptionInfo;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.AEADBadTagException;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ZyncAPI {
    private static final MediaType OCTET_STREAM_TYPE = MediaType.parse("application/octet-stream");
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json");
    public static final String API_DOMAIN = "beta-api.zync.co"; // used for verification purposes
    public static final String BASE = "https://beta-api.zync.co/v";
    public static final int VERSION = 0;
    private OkHttpClient client;
    private final String token;

    public ZyncAPI(OkHttpClient client, String token) {
        this.client = client;
        this.token = token;
    }

    public static void authenticate(final OkHttpClient client, String idToken, final ZyncCallback<ZyncAPI> callback) {
        ZyncGenericAPIListener listener = new ZyncGenericAPIListener(new ZyncCallback<JSONObject>() {
            @Override
            public void success(JSONObject node) {
                try {
                    callback.success(new ZyncAPI(client, node.getJSONObject("data").getString("zync_token")));
                } catch (JSONException ignored) {
                }
            }

            @Override
            public void handleError(ZyncError error) {
                callback.handleError(error);
                ZyncApplication.LOGGED_EXCEPTIONS.add(new ZyncExceptionInfo(new ZyncAPIException(error), "authenticate"));
            }
        });
        Request request = new Request.Builder()
                .url(BASE + VERSION + "/user/authenticate?token=" + idToken)
                .addHeader("User-Agent", System.getProperty("http.agent"))
                .build();
        client.newCall(request).enqueue(listener);
    }

    public <T> void executeAuthenticatedRequest(String httpMethod, String method, JSONObject body,
                                                          ZyncCallback<T> callback,
                                                          ZyncTransformer<T> transformer) {
        Request request = new Request.Builder()
                .url(BASE + VERSION + "/" + method)
                .method(httpMethod, (body == null) ? null : RequestBody.create(JSON_MEDIA_TYPE, body.toString()))
                .addHeader("X-ZYNC-TOKEN", token)
                .addHeader("User-Agent", System.getProperty("http.agent"))
                .build();

        client.newCall(request).enqueue(new ZyncGenericAPIListener(callback, transformer));
    }

    /*
     * Sends a request using the Volley library to post a postClipboard update
     */
    public void postClipboard(ZyncClipData clipData,
                              final ZyncCallback<Void> responseListener) throws JSONException {
        JSONObject body = new JSONObject().put("data", clipData.toJson());
        executeAuthenticatedRequest(
                "POST",
                "clipboard",
                body,
                responseListener,
                new ZyncNullTransformer<Void>()
        );
    }

    public void getClipboard(final String encryptionKey, final List<Long> timestamps,
                             final ZyncCallback<List<ZyncClipData>> callback) {
        StringBuilder builder = new StringBuilder();

        for (Long timestamp : timestamps) {
            builder.append(timestamp.longValue()).append(",");
        }

        builder.setLength(builder.length() - 1);

        executeAuthenticatedRequest(
                "GET",
                "clipboard/" + builder.toString(),
                null,
                callback,
                new ZyncTransformer<List<ZyncClipData>>() {
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
                }
        );
    }

    /*
     * Sends a request to get the clipboard from the servers
     * This method handles the decryption, decompression, and hash verification of the data.
     * If any of these processes fail, null will be returned to the callback.
     */
    public void getClipboard(final String encryptionKey, final ZyncCallback<ZyncClipData> callback) {
        executeAuthenticatedRequest(
                "GET",
                "clipboard",
                null,
                callback,
                new ZyncTransformer<ZyncClipData>() {
                    @Override
                    public ZyncClipData transform(JSONObject obj) throws Exception {
                        return new ZyncClipData(encryptionKey, obj.getJSONObject("data"));
                    }
                }
        );
    }

    public void getHistory(final String encryptionKey, final ZyncCallback<List<ZyncClipData>> callback) {
        executeAuthenticatedRequest(
                "GET",
                "clipboard/history",
                null,
                callback,
                new ZyncTransformer<List<ZyncClipData>>() {
                    @Override
                    public List<ZyncClipData> transform(JSONObject obj) {
                        try {
                            JSONArray array = obj.getJSONObject("data").getJSONArray("history");
                            List<ZyncClipData> history = new ArrayList<>(array.length());

                            for (int i = 0; i < array.length(); i++) {
                                try {
                                    history.add(new ZyncClipData(encryptionKey, array.getJSONObject(i)));
                                } catch (Exception ex) {
                                    if (!(ex instanceof AEADBadTagException)) {
                                        ZyncApplication.LOGGED_EXCEPTIONS.add(new ZyncExceptionInfo(ex, "Decode clip entry from server"));
                                    }
                                }
                            }

                            Collections.sort(history, new ZyncClipData.TimeComparator()); // sort by time
                            return history;
                        } catch (JSONException ex) {
                            return null;
                        }
                    }
                }
        );
    }

    // request a URL to upload our encrypted large file
    public void requestUploadUrl(ZyncClipData data, final ZyncCallback<URL> callback) {
        executeAuthenticatedRequest(
                "GET",
                "requestUpload", // todo verify with vilsol
                data.toJson(),
                callback,
                new ZyncTransformer<URL>() {
                    @Override
                    public URL transform(JSONObject obj) throws Exception {
                        return new URL(obj.getJSONObject("data").getString("url"));
                    }
                }
        );
    }

    public void upload(File file, URL url, final ZyncCallback<Void> callback) {
        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(OCTET_STREAM_TYPE, file))
                .addHeader("X-ZYNC-TOKEN", token)
                .addHeader("User-Agent", System.getProperty("http.agent"))
                .build();

        client.newCall(request).enqueue(new ZyncGenericAPIListener(callback, new ZyncNullTransformer<Void>()));
    }

    public String getToken() {
        return token;
    }

    public OkHttpClient client() {
        return client;
    }

    public void setClient(OkHttpClient client) {
        this.client = client;
    }

    public interface ZyncCallback<T> {
        void success(T value);
        void handleError(ZyncError error);
    }
}
