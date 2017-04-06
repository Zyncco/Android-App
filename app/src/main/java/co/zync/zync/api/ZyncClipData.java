package co.zync.zync.api;

import android.util.Base64;
import co.zync.zync.utils.ZyncCrypto;
import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.AEADBadTagException;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Comparator;
import java.util.Locale;
import java.util.zip.CRC32;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class ZyncClipData {
    private final long timestamp;
    private String hash;
    private final byte[] iv;
    private final byte[] salt;
    private final ZyncClipType type;
    // ONLY exists if type=TEXT, otherwise stored in file
    private byte[] data; // encoded in base 64

    public ZyncClipData(String encryptionKey,
                        ZyncClipType type, byte[] data) throws Exception {
        this.timestamp = System.currentTimeMillis();
        this.type = type;
        this.iv = ZyncCrypto.generateSecureIv();
        this.salt = ZyncCrypto.generateSecureSalt();

        if (data != null) {
            data = compress(data);
            data = ZyncCrypto.encrypt(data, encryptionKey, salt, iv);
            this.hash = hash(data);
            this.data = Base64.encodeToString(data, Base64.NO_WRAP).getBytes(Charset.forName("UTF-8"));
        }
    }

    public ZyncClipData(String encryptionKey, JSONObject obj) throws Exception {
        this.timestamp = obj.getLong("timestamp");
        this.hash = obj.getJSONObject("hash").getString("crc32");
        JSONObject encryption = obj.getJSONObject("encryption");
        this.iv = Base64.decode(encryption.getString("iv"), Base64.NO_WRAP);
        this.salt = Base64.decode(encryption.getString("salt"), Base64.NO_WRAP);
        this.type = ZyncClipType.valueOf(obj.getString("payload-type").toUpperCase(Locale.US));

        if (obj.has("payload")) {
            this.data = Base64.decode(obj.getString("payload"), Base64.NO_WRAP);

            try {
                this.data = ZyncCrypto.decrypt(data, encryptionKey, salt, iv);
                this.data = decompress(data);
            } catch (DataFormatException | AEADBadTagException ex) {
                this.data = null;
            }
        }
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

    public byte[] iv() {
        return iv;
    }

    public byte[] salt() {
        return salt;
    }

    public long timestamp() {
        return timestamp;
    }

    public byte[] data() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public ZyncClipType type() {
        return type;
    }

    public JSONObject toJson() {
        try {
            JSONObject object = new JSONObject();

            object.put("timestamp", timestamp);
            object.put("encryption", new JSONObject()
                    .put("type", "AES256-GCM-NOPADDING")
                    .put("iv", Base64.encodeToString(iv, Base64.NO_WRAP))
                    .put("salt", Base64.encodeToString(salt, Base64.NO_WRAP)));
            object.put("payload-type", type.name());

            if (data != null) {
                object.put("hash", new JSONObject().put("crc32", hash));
                object.put("payload", new String(data, "UTF-8"));
            }

            return object;
        } catch (JSONException | UnsupportedEncodingException ignored) {
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
