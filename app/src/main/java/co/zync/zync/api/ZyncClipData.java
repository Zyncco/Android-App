package co.zync.zync.api;

import android.util.Base64;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.zip.Deflater;

public class ZyncClipData {
    private final long timestamp;
    private final String hash;
    private final boolean encrypted;
    private final ZyncClipType type;
    private String data; // encoded in base 64

    public ZyncClipData(String encryptionKey,
                        ZyncClipType type, byte[] data) {
        this.timestamp = System.currentTimeMillis();
        this.hash = hash(data);
        this.encrypted = encryptionKey != null;
        this.type = type;

        if (encrypted) {
            data = encrypt(data, encryptionKey);
        }

        data = compress(data);
        this.data = Base64.encodeToString(data, Base64.DEFAULT);
    }

    private static byte[] compress(byte[] data) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Deflater deflater = new Deflater();
        byte[] buff = new byte[256];
        int lastLength = 256;

        deflater.setInput(data);
        deflater.finish();

        while (lastLength == 256) {
            lastLength = deflater.deflate(buff);

            try {
                bos.write(buff);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

        deflater.end();
        return bos.toByteArray();
    }

    private static byte[] encrypt(byte[] data, String encryptionKey) {
        // TODO decide on encryption method and implement from there
        return data;
    }

    // hash provided data into SHA-256 and output the hex representation
    // returns "unsupported" if a SHA-256 implementation is not on the device
    private static String hash(byte[] data) {
        MessageDigest md;

        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            return "unsupported";
        }

        data = md.digest(data);
        StringBuilder sb = new StringBuilder();

        for (byte b : data) {
            sb.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
        }

        return sb.toString();
    }

    public JSONObject toJson() {
        try {
            JSONObject object = new JSONObject();

            object.put("timestamp", timestamp);
            object.put("hash", hash);
            object.put("encrypted", encrypted);
            object.put("type", type.name().toLowerCase(Locale.US));
            object.put("data", data);

            return object;
        } catch (JSONException ignored) {
            return new JSONObject();
        }
    }
}
