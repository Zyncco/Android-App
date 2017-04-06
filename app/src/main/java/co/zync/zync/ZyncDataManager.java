package co.zync.zync;

import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import co.zync.zync.api.ZyncAPI;
import co.zync.zync.api.ZyncClipData;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

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
    public File load(ZyncClipData data, boolean dl, ZyncAPI.ZyncCallback<File> handler) {
        File file = fileFor(data, false);

        if (!file.exists() && dl) {
            try {
                app.getApi().downloadLarge(
                        app.getEncryptionPass(),
                        file,
                        data,
                        handler == null ? new ZyncAPI.NullZyncCallback<File>() : handler,
                        true
                );
            } catch (InterruptedException ignored) {
            }

            return file;
        } else if (!file.exists()) {
            return null;
        }

        return file;
    }

    public File fileFor(ZyncClipData data, boolean create) {
        File file = new File(clipboardDir, data.timestamp() + ".zclip");

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

    // save image to file
    public void saveImage(ZyncClipData data, Uri uri) throws IOException {
        Bitmap bitmap = MediaStore.Images.Media.getBitmap(app.getContentResolver(), uri);
        FileOutputStream fos = new FileOutputStream(fileFor(data, true));

        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);

        fos.flush();
        fos.close();
    }
}
