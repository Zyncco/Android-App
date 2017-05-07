package co.zync.zync;

import android.content.SharedPreferences;
import co.zync.zync.activities.SettingsActivity;
import co.zync.zync.api.ZyncClipData;
import co.zync.zync.utils.ZyncCrypto;
import co.zync.zync.utils.ZyncExceptionInfo;
import org.json.JSONObject;

import javax.crypto.AEADBadTagException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * @author Mazen Kotb
 */
public class ZyncConfiguration {
    private ZyncApplication app;

    ZyncConfiguration(ZyncApplication app) {
        this.app = app;
    }

    public SharedPreferences getPreferences() {
        return app.getSharedPreferences(SettingsActivity.PREFERENCES_NAME, 0);
    }

    // Set the history that is stored on file
    public void setHistory(List<ZyncClipData> history) {
        Set<String> historyText = new HashSet<>(history.size());

        for (ZyncClipData d : history) {
            ZyncClipData data = d.clone();

            if (data.data() != null) {
                data.encrypt(getEncryptionPass());
            }

            historyText.add(data.toJson().toString());
        }

        getPreferences().edit().putStringSet("zync_history", historyText).apply();
    }

    // adds an item to history and writes changes to file
    public void addToHistory(ZyncClipData d) {
        ZyncClipData data = d.clone();
        List<String> history = new ArrayList<>(getPreferences().getStringSet("zync_history", new HashSet<String>()));

        if (history.size() == 10) {
            history.remove(9);
        }

        if (data.data() != null) {
            data.encrypt(getEncryptionPass());
        }

        history.add(data.toJson().toString());
        getPreferences().edit().putStringSet("zync_history", new HashSet<>(history))
                .apply();
    }

    public void update(ZyncClipData data) {
        Set<String> historyText = new HashSet<>();

        for (ZyncClipData clip : getHistory()) {
            if (clip.timestamp() == data.timestamp()) {
                clip = data;
            }

            if (clip.data() != null) {
                clip.encrypt(getEncryptionPass());
            }

            historyText.add(clip.toJson().toString());
        }

        getPreferences().edit().putStringSet("zync_history", historyText)
                .apply();
    }

    public List<ZyncClipData> getHistory() {
        Set<String> historyStr = getPreferences()
                .getStringSet("zync_history", new HashSet<String>());
        final List<ZyncClipData> history = new ArrayList<>(historyStr.size());

        for (String json : historyStr) {
            try {
                history.add(new ZyncClipData(getEncryptionPass(), new JSONObject(json)));
            } catch (Exception e) {
                e.printStackTrace();

                if (!(e instanceof AEADBadTagException)) {
                    ZyncApplication.LOGGED_EXCEPTIONS.add(new ZyncExceptionInfo(e, "decoding history from file"));
                }
            }
        }

        Collections.sort(history, new ZyncClipData.TimeComparator());
        return history;
    }

    public void clear() {
        getPreferences().edit()
                .remove("zync_api_token")
                .remove("zync_history")
                .apply();
    }

    public String getEncryptionPass() {
        return getPreferences().getString("encryption_pass", "default");
    }

    public void setEncryptionPass(String password) {
        String hash = ZyncCrypto.hashSha(password.getBytes(StandardCharsets.UTF_8));

        if ("unsupported".equals(hash)) {
            hash = password; // fallback on plaintext storage on device if it doesn't support SHA-256
        }

        getPreferences().edit().putString("encryption_pass", hash).apply();
    }

    // max size of a payload allowed based on settings in bytes
    public long getMaxSize() {
        return getPreferences().getInt("max_size", 10) * 1000000;
    }

    public boolean useOnData() {
        return getPreferences().getBoolean("use_on_data", true);
    }

    public boolean sendClipboardOnChange() {
        return getPreferences().getBoolean("clipboard_change_notification", true);
    }

    public boolean persistentNotification() {
        return getPreferences().getBoolean("enable_persistent_notification", true);
    }

    public boolean syncDown() {
        return getPreferences().getBoolean("sync_down", true);
    }

    public boolean syncUp() {
        return getPreferences().getBoolean("sync_up", true);
    }

    public boolean zyncOn() {
        return getPreferences().getBoolean("zync_on", true);
    }

    public void setZyncOn(boolean v) {
        getPreferences().edit().putBoolean("zync_on", v).apply();
    }

    public boolean seenIntro() {
        return getPreferences().getBoolean("seen_intro", false);
    }

    public void setSeenIntro(boolean v) {
        getPreferences().edit().putBoolean("seen_intro", v).apply();
    }

    public String apiToken() {
        return getPreferences().getString("zync_api_token", null);
    }

    public void setApiToken(String token) {
        getPreferences().edit().putString("zync_api_token", token).apply();
    }
}
