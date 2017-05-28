package co.zync.android.api;

import android.util.Base64;
import co.zync.android.utils.ZyncCrypto;
import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.AEADBadTagException;
import java.io.*;
import java.nio.charset.Charset;
import java.util.Comparator;
import java.util.Locale;
import java.util.zip.*;

public class ZyncClipData implements Cloneable {
    private final long timestamp;
    private String hash;
    private final byte[] iv;
    private final byte[] salt;
    private final ZyncClipType type;
    // ONLY exists if type=TEXT, otherwise stored in file
    private byte[] data; // encoded in base 64

    public ZyncClipData(ZyncClipType type, byte[] data) throws Exception {
        this.timestamp = System.currentTimeMillis();
        this.type = type;
        this.iv = ZyncCrypto.generateSecureIv();
        this.salt = ZyncCrypto.generateSecureSalt();

        if (data != null) {
            this.data = data;
            this.hash = hashCrc(data);
        }
    }

    // only for large files
    public ZyncClipData(long timestamp, ZyncClipType type,
                        byte[] iv, byte[] salt,
                        InputStream is) throws IOException {
        this.timestamp = timestamp;
        this.type = type;
        this.iv = iv;
        this.salt = salt;

        if (is != null) {
            this.hash = hashCrc(is);
        }
    }

    public ZyncClipData(String encryptionKey, JSONObject obj) throws Exception {
        this.timestamp = obj.getLong("timestamp");
        JSONObject encryption = obj.getJSONObject("encryption");
        this.iv = Base64.decode(encryption.getString("iv"), Base64.NO_WRAP);
        this.salt = Base64.decode(encryption.getString("salt"), Base64.NO_WRAP);
        this.type = ZyncClipType.valueOf(obj.getString("payload-type").toUpperCase(Locale.US));

        if (obj.has("payload")) {
            this.data = Base64.decode(obj.getString("payload"), Base64.NO_WRAP);

            try {
                this.data = ZyncCrypto.decrypt(data, encryptionKey, salt, iv);
                JSONObject payload = new JSONObject(new String(data));

                this.data = Base64.decode(payload.getString("data"), Base64.NO_WRAP);
                this.data = decompress(data);
                this.hash = payload.getString("hash");
            } catch (DataFormatException | AEADBadTagException ex) {
                this.data = null;
            }
        }
    }

    private static byte[] compress(byte[] data) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        GZIPOutputStream gzip = new GZIPOutputStream(bos);

        gzip.write(data);
        gzip.close();
        gzip.flush();

        return bos.toByteArray();
    }

    private static byte[] decompress(byte[] data) throws DataFormatException, IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        GZIPInputStream input = new GZIPInputStream(new ByteArrayInputStream(data));
        byte[] buff = new byte[256];
        int lastLength = 256;

        while (lastLength == 256) {
            lastLength = input.read(buff);
            bos.write(buff, 0, lastLength);
        }

        input.close();
        return bos.toByteArray();
    }

    // hashCrc provided data into CRC32 and output the hex representation
    public static String hashCrc(byte[] data) {
        CRC32 crc = new CRC32();
        crc.update(data);
        return Long.toHexString(crc.getValue());
    }

    public static String hashCrc(InputStream is) throws IOException {
        // generate hashCrc efficiently
        // (load data and update hashCrc in 8192 blocks)
        CRC32 crc = new CRC32();
        byte[] buff = new byte[8192];
        int last;

        while (true) {
            last = is.read(buff);

            if (last == -1) {
                break;
            }

            crc.update(buff, 0, last);
        }

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

    public String hash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public JSONObject toJson(String encryptionKey) {
        try {
            JSONObject object = new JSONObject();

            object.put("timestamp", timestamp);
            object.put("encryption", new JSONObject()
                    .put("type", "AES256-GCM-NOPADDING")
                    .put("iv", Base64.encodeToString(iv, Base64.NO_WRAP))
                    .put("salt", Base64.encodeToString(salt, Base64.NO_WRAP)));
            object.put("payload-type", type.name());

            if (data != null) {
                JSONObject payload = new JSONObject();

                payload.put("data", Base64.encode(compress(data), Base64.NO_WRAP));
                payload.put("hash", hash);

                object.put("payload", ZyncCrypto.encrypt(payload.toString().getBytes(), encryptionKey, salt, iv));
            }

            return object;
        } catch (Exception ignored) {
            return new JSONObject();
        }
    }

    @Override
    public ZyncClipData clone() {
        try {
            return (ZyncClipData) super.clone();
        } catch (CloneNotSupportedException ignored) {
            return null;
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
