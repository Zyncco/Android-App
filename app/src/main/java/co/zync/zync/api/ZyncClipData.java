package co.zync.zync.api;

import android.util.Base64;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.Comparator;
import java.util.Locale;
import java.util.zip.CRC32;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class ZyncClipData {
    private final long timestamp;
    private final String hash;
    private final ZyncClipType type;
    private byte[] data; // encoded in base 64

    public ZyncClipData(String encryptionKey,
                        ZyncClipType type, byte[] data) {
        this.timestamp = System.currentTimeMillis();
        this.hash = hash(data);
        this.type = type;

        data = encrypt(data, encryptionKey);
        data = compress(data);
        this.data = Base64.encodeToString(data, Base64.DEFAULT).getBytes(Charset.forName("UTF-8"));
    }

    public ZyncClipData(String encryptionKey, JSONObject obj) throws JSONException {
        this.timestamp = obj.getLong("timestamp");
        this.hash = obj.getString("hash");
        this.type = ZyncClipType.valueOf(obj.getString("type").toUpperCase(Locale.US));
        this.data = Base64.decode(obj.getString("data"), Base64.DEFAULT);

        try {
            this.data = decompress(data);
        } catch (DataFormatException ex) {
            this.data = null;
        }
        // todo decrypt and decompress
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
            bos.write(buff, 0, lastLength);
        }

        deflater.end();
        return bos.toByteArray();
    }

    private static byte[] decompress(byte[] data) throws DataFormatException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Inflater inflate = new Inflater();
        byte[] buff = new byte[256];
        int lastLength = 256;

        inflate.setInput(data);

        while (lastLength == 256) {
            lastLength = inflate.inflate(buff);
            bos.write(buff, 0, lastLength);
        }

        inflate.end();
        return bos.toByteArray();
    }

    private static byte[] encrypt(byte[] data, String encryptionKey) {
        // TODO decide on encryption method and implement from there
        return data;
    }

    // hash provided data into CRC32 and output the hex representation
    private static String hash(byte[] data) {
        CRC32 crc = new CRC32();
        crc.update(data);
        return Long.toHexString(crc.getValue());
    }

    public long timestamp() {
        return timestamp;
    }

    public byte[] data() {
        return data;
    }

    public ZyncClipType type() {
        return type;
    }

    public JSONObject toJson() {
        try {
            JSONObject object = new JSONObject();

            object.put("timestamp", timestamp);
            object.put("hash", hash);
            object.put("type", type.name().toLowerCase(Locale.US));
            object.put("data", new String(data));

            return object;
        } catch (JSONException ignored) {
            return new JSONObject();
        }
    }

    // sorts latest -> oldest
    public static class TimeComparator implements Comparator<ZyncClipData> {
        @Override
        public int compare(ZyncClipData o1, ZyncClipData o2) {
            return (int) (o2.timestamp() - o1.timestamp());
        }
    }
}
