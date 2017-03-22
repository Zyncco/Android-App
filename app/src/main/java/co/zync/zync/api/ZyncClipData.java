package co.zync.zync.api;

import android.util.Base64;
import co.zync.zync.utils.ZyncCrypto;
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
    private final byte[] iv;
    private final byte[] salt;
    private final ZyncClipType type;
    private byte[] data; // encoded in base 64

    public ZyncClipData(String encryptionKey,
                        ZyncClipType type, byte[] data) throws Exception {
        this.timestamp = System.currentTimeMillis();
        this.type = type;
        this.iv = ZyncCrypto.generateSecureIv();
        this.salt = ZyncCrypto.generateSecureSalt();

        data = ZyncCrypto.encrypt(data, encryptionKey, salt, iv);
        this.hash = hash(data);
        data = compress(data);
        this.data = Base64.encodeToString(data, Base64.DEFAULT).getBytes(Charset.forName("UTF-8"));
    }

    public ZyncClipData(String encryptionKey, JSONObject obj) throws Exception {
        this.timestamp = obj.getLong("timestamp");
        this.hash = obj.getJSONObject("hash").getString("crc32");
        JSONObject encryption = obj.getJSONObject("encryption");
        this.iv = Base64.decode(encryption.getString("iv"), Base64.DEFAULT);
        this.salt = Base64.decode(encryption.getString("salt"), Base64.DEFAULT);
        this.type = ZyncClipType.valueOf(obj.getString("paylod-type").toUpperCase(Locale.US));
        this.data = Base64.decode(obj.getString("payload"), Base64.DEFAULT);

        try {
            this.data = decompress(data);
        } catch (DataFormatException ex) {
            this.data = null;
        }

        this.data = ZyncCrypto.decrypt(data, encryptionKey, salt, iv);
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
            object.put("hash", new JSONObject().put("crc32", hash));
            object.put("encryption", new JSONObject()
                    .put("type", "aes256-gcm-nopadding")
                    .put("iv", Base64.encode(iv, Base64.DEFAULT))
                    .put("salt", Base64.encode(salt, Base64.DEFAULT)));
            object.put("payload-type", type.name());
            object.put("payload", new String(data));

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
