package co.zync.android.api;

import co.zync.android.ZyncApplication;
import co.zync.android.api.callback.ProgressCallback;
import co.zync.android.api.callback.ZyncCallback;
import co.zync.android.api.generic.ZyncGenericAPIListener;
import co.zync.android.api.generic.NullZyncTransformer;
import co.zync.android.api.generic.ZyncTransformer;
import co.zync.android.utils.Provider;
import co.zync.android.utils.ZyncExceptionInfo;
import com.google.firebase.iid.FirebaseInstanceId;
import okhttp3.*;
import okio.BufferedSink;
import okio.Okio;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.AEADBadTagException;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

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

    public static void authenticate(final OkHttpClient client, String idToken, final ZyncCallback<Void> callback) throws JSONException {
        ZyncGenericAPIListener listener = new ZyncGenericAPIListener(new ZyncCallback<JSONObject>() {
            @Override
            public void success(JSONObject node) {
            }

            @Override
            public void handleError(ZyncError error) {
                callback.handleError(error);
                ZyncApplication.LOGGED_EXCEPTIONS.add(new ZyncExceptionInfo(new ZyncAPIException(error), "authenticate"));
            }
        });

        JSONObject data = new JSONObject()
                .put("device-id", FirebaseInstanceId.getInstance().getToken())
                .put("firebase-token", idToken);
        Request request = new Request.Builder()
                .url(BASE + VERSION + "/user/authenticate")
                .post(RequestBody.create(
                        JSON_MEDIA_TYPE,
                        new JSONObject().put("data", data).toString()
                ))
                .addHeader("User-Agent", System.getProperty("http.agent"))
                .build();
        client.newCall(request).enqueue(listener);
    }

    public static void validateDevice(final OkHttpClient client, final String token, String randomToken, final ZyncCallback<ZyncAPI> callback) throws JSONException {
        JSONObject data = new JSONObject()
                .put("device-id", FirebaseInstanceId.getInstance().getToken())
                .put("random-token", randomToken);
        Request request = new Request.Builder()
                .url(BASE + VERSION + "/device/validate")
                .post(RequestBody.create(
                        JSON_MEDIA_TYPE,
                        new JSONObject().put("data", data).toString()
                ))
                .addHeader("X-ZYNC-TOKEN", token)
                .addHeader("User-Agent", System.getProperty("http.agent"))
                .build();

        client.newCall(request).enqueue(new ZyncGenericAPIListener(callback, new ZyncTransformer<ZyncAPI>() {
            @Override
            public ZyncAPI transform(JSONObject obj) throws Exception {
                return new ZyncAPI(client, token);
            }
        }));
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
     * Sends a request to post a clipboard update
     */
    public void postClipboard(String encryptionPass, ZyncClipData clipData,
                              final ZyncCallback<Void> responseListener) throws JSONException {
        JSONObject body = new JSONObject().put("data", clipData.toJson(encryptionPass));
        executeAuthenticatedRequest(
                "POST",
                "clipboard",
                body,
                responseListener,
                new NullZyncTransformer<Void>()
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
     * This method handles the decryption, decompression, and hashCrc verification of the data.
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

                                    ex.printStackTrace();
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

    // if a payload is too large due to it being an image
    // or greater of a certain threshold, this method will be used
    // to download the clip and stream it to file
    // NOTE: Only throws InterruptedException if blocking = true
    public void downloadLarge(final OutputStream os,
                              final ZyncClipData data, // only contains metadata
                              final ZyncCallback<Void> callback,
                              final boolean blocking) throws InterruptedException {
        // prepare latch to use if blocking
        final CountDownLatch latch = blocking ? new CountDownLatch(1) : null;

        Request request = new Request.Builder()
                .url(BASE + VERSION + "/clipboard/" + data.timestamp() + "/raw")
                .addHeader("X-ZYNC-TOKEN", token)
                .addHeader("User-Agent", System.getProperty("http.agent"))
                .build();

        // perform request async regardless of blocking
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                onFailure(call, e);
            }

            private void onFailure(Call call, Exception e) {
                callback.handleError(new ZyncError(-4, "HTTP Error: " +
                        (e.getCause() != null ? e.getCause().getClass().getSimpleName() : "null") + ":" + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                // create stream from http response
                BufferedSink sink = Okio.buffer(Okio.sink(os));
                sink.writeAll(response.body().source());
                sink.close();

                // execute callback
                callback.success(null);

                // count down latch if calling thread is waiting
                if (blocking) {
                    latch.countDown();
                }
            }
        });

        // if we're blocking, await
        if (blocking) {
            latch.await();
        }
    }

    // request a URL to upload our encrypted large file
    public void requestUploadUrl(ZyncClipData data, final ZyncCallback<String> callback) throws JSONException {
        executeAuthenticatedRequest(
                "POST",
                "clipboard/upload",
                // for large files, 'data' must be null so no encryption key provided
                new JSONObject().put("data", data.toJson(null)),
                callback,
                new ZyncTransformer<String>() {
                    @Override
                    public String transform(JSONObject obj) throws Exception {
                        return obj.getJSONObject("data").getString("token");
                    }
                }
        );
    }

    public void upload(final Provider<InputStream> streamProvider, String token,
                       final ZyncCallback<Void> callback, final ProgressCallback progressCallback) {
        Request request = new Request.Builder()
                .url(BASE + VERSION + "/clipboard/upload/" + token)
                .post(new RequestBody() {
                    @Override
                    public MediaType contentType() {
                        return OCTET_STREAM_TYPE;
                    }

                    @Override
                    public void writeTo(BufferedSink sink) throws IOException {
                        InputStream is = streamProvider.get();
                        byte[] buff = new byte[16248];
                        int length = buff.length;
                        int totalRead = 0;

                        while (length != -1) {
                            length = is.read(buff);

                            if (length == -1) {
                                break;
                            }

                            totalRead += length;
                            sink.write(buff, 0, length);

                            if (progressCallback != null) {
                                progressCallback.callback(totalRead);
                            }
                        }

                        is.close();
                    }
                })
                .addHeader("X-ZYNC-TOKEN", this.token)
                .addHeader("User-Agent", System.getProperty("http.agent"))
                .build();

        client.newCall(request).enqueue(new ZyncGenericAPIListener(callback, new NullZyncTransformer<Void>()));
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

}
