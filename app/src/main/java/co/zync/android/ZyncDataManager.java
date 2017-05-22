package co.zync.android;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import co.zync.android.api.ZyncClipData;
import co.zync.android.api.ZyncClipType;
import co.zync.android.api.ZyncError;
import co.zync.android.api.callback.NullZyncCallback;
import co.zync.android.api.callback.ZyncCallback;
import co.zync.android.utils.ZyncCrypto;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import java.io.*;

/**
 * Manager class for large data stored in the clipboard
 * such as images or videos
 *
 * @author Mazen Kotb
 */
public class ZyncDataManager {
    private File clipboardDir;
    private ZyncApplication app;

    ZyncDataManager(ZyncApplication app) {
        this.app = app;
        this.clipboardDir = new File(app.getFilesDir(), "clips");

        if (!clipboardDir.exists()) {
            clipboardDir.mkdirs();
        }
    }

    public File load(ZyncClipData data) {
        return load(data, false, null);
    }

    public File load(ZyncClipData data, boolean dl) {
        return load(data, dl, null);
    }

    /*
     * blocking method used to load the data for specific clip data
     *
     * if dl=true and file does not exist, this will call ZyncAPI#downloadLarge
     * and block the thread with the download
     *
     * if dl=false and file does not exist, this method will return null instantly
     *
     * if the file exists, it will be returned and data can be loaded from there
     */
    public File load(ZyncClipData data, boolean dl, ZyncCallback<Void> handler) {
        File file = fileFor(data, false);

        if (!file.exists() && dl) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();

                Cipher cipher = ZyncCrypto.getCipher(
                        Cipher.DECRYPT_MODE,
                        app.getConfig().getEncryptionPass(),
                        data.salt(),
                        data.iv()
                );

                app.getApi().downloadLarge(
                        new CipherOutputStream(new FileOutputStream(file), cipher),
                        data,
                        handler == null ? new NullZyncCallback<Void>() : handler,
                        true
                );
            } catch (Exception e) {
                if (handler != null) {
                    handler.handleError(new ZyncError(-8,
                            "Unexpected error when downloading file: " +
                                    e.getClass().getSimpleName() + ": " + e.getMessage()));
                }
            }

            return file;
        } else if (!file.exists()) {
            return null;
        }

        return file;
    }

    public File fileFor(ZyncClipData data, boolean create) {
        return fileFor(data.timestamp(), create);
    }

    public File fileFor(long timestamp, boolean create) {
        File file = new File(clipboardDir, timestamp + ".zclip");

        if (create && !file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException ex) {
                return null;
            }
        }

        return file;
    }

    public CipherInputStream cryptoStreamFor(ZyncClipData clip) {
        return cryptoStreamFor(clip.timestamp(), clip.salt(), clip.iv());
    }

    public CipherInputStream cryptoStreamFor(File file, byte[] salt, byte[] iv) {
        Cipher cipher;

        try {
            cipher = ZyncCrypto.getCipher(
                    Cipher.ENCRYPT_MODE,
                    app.getConfig().getEncryptionPass(),
                    salt,
                    iv
            );
            return new CipherInputStream(new FileInputStream(file), cipher);
        } catch (Exception ex) {
            return null;
        }
    }

    public CipherInputStream cryptoStreamFor(long timestamp, byte[] salt, byte[] iv) {
        return cryptoStreamFor(fileFor(timestamp, false), salt, iv);
    }

    public ZyncClipData saveImage(InputStream is) throws Exception {
        return saveImage(BitmapFactory.decodeStream(is));
    }

    public ZyncClipData saveImage(Uri uri) throws Exception {
        return saveImage(MediaStore.Images.Media.getBitmap(app.getContentResolver(), uri));
    }

    // save image to file
    public ZyncClipData saveImage(Bitmap bitmap) throws Exception {
        // preparing clip data
        long timestamp = System.currentTimeMillis();
        byte[] salt = ZyncCrypto.generateSecureSalt();
        byte[] iv = ZyncCrypto.generateSecureIv();
        File clipFile = fileFor(timestamp, true);
        FileOutputStream output = new FileOutputStream(clipFile);

        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output);

        output.flush();
        output.close();

        return new ZyncClipData(timestamp, ZyncClipType.IMAGE, iv, salt, null);
    }

    public void clearData() {
        clipboardDir.delete();
        clipboardDir.mkdirs();
    }
}
