package co.zync.zync.api;

import co.zync.zync.ZyncApplication;
import co.zync.zync.api.generic.ZyncGenericAPIListener;
import co.zync.zync.api.generic.ZyncNullTransformer;
import co.zync.zync.api.generic.ZyncTransformer;
import co.zync.zync.utils.ZyncCrypto;
import co.zync.zync.utils.ZyncExceptionInfo;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
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
     * Sends a request to post a clipboard update
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

    // if a payload is too large due to it being an image
    // or greater of a certain threshold, this method will be used
    // to download the clip and stream it to file
    // NOTE: Only throws InterruptedException if blocking = true
    public void downloadLarge(final String encryptionPass, final File file,
                              final ZyncClipData data, // only contains metadata
                              final ZyncCallback<File> callback,
                              final boolean blocking) throws InterruptedException {
        // prepare latch to use if blocking
        final CountDownLatch latch = blocking ? new CountDownLatch(1) : null;

        // create file if it doesn't exist
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException ex) {
                callback.handleError(new ZyncError(-7, "Unable to create file " +
                        file.getName() + " for downloading due to IOException: " + ex.getMessage()));
                return;
            }
        }

        Request request = new Request.Builder()
                .url(BASE + VERSION + "/clipboard/download/" + data.timestamp())
                .post(RequestBody.create(OCTET_STREAM_TYPE, file))
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
                InputStream source = response.body().byteStream();
                // prepare cipher to streamline decryption
                Cipher cipher;

                try {
                    cipher = ZyncCrypto.getCipher(Cipher.DECRYPT_MODE, encryptionPass, data.salt(), data.iv());
                } catch (Exception ex) {
                    onFailure(call, ex);
                    return;
                }

                CipherOutputStream os = new CipherOutputStream(new FileOutputStream(file), cipher);
                byte[] buffer = new byte[4096];
                int last = 4096;

                /*
                 * Stream payload to file in
                 * batches of 4096 bytes
                 */
                while (last == 4096) {
                    last = source.read(buffer);
                    os.write(buffer);
                }

                // flush and cleanup
                os.flush();
                os.close();
                source.close();

                // execute callback
                callback.success(file);

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

    // literally does absolutely nothing
    public static class NullZyncCallback<T> implements ZyncCallback<T> {
        @Override
        public void success(T value) {
        }

        @Override
        public void handleError(ZyncError error) {
        }
    }
}
