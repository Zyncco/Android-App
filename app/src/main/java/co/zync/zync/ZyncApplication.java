package co.zync.zync;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceActivity;
import co.zync.zync.activities.SettingsActivity;
import co.zync.zync.api.ZyncAPI;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

public class ZyncApplication extends Application {
    private GoogleSignInAccount account;
    private RequestQueue httpRequestQueue;
    private ZyncAPI api;

    @Override
    public void onCreate() {
        super.onCreate();
        Intent intent = new Intent(this, ZyncClipboardService.class);

        startService(intent);
        httpRequestQueue = Volley.newRequestQueue(getApplicationContext());
    }

    public void openSettings(Context context) {
        Intent settingsIntent = new Intent(context, SettingsActivity.class);

        settingsIntent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, SettingsActivity.GeneralPreferenceFragment.class.getName());
        settingsIntent.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true);
        startActivity(settingsIntent);
    }

    public RequestQueue httpRequestQueue() {
        return httpRequestQueue;
    }

    public GoogleSignInAccount getAccount() {
        return account;
    }

    public ZyncAPI getApi() {
        return api;
    }

    public SharedPreferences getPreferences() {
        return getSharedPreferences(SettingsActivity.PREFERENCES_NAME, 0);
    }

    public void setApi(ZyncAPI api) {
        this.api = api;
    }

    public void setAccount(GoogleSignInAccount account) {
        this.account = account;
    }
}
