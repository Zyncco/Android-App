package co.zync.zync;

import android.app.Application;
import android.content.Intent;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

public class ZyncApplication extends Application {
    private GoogleSignInAccount account;
    private RequestQueue httpRequestQueue;

    @Override
    public void onCreate() {
        super.onCreate();
        Intent intent = new Intent(this, ZyncClipboardService.class);

        startService(intent);
        httpRequestQueue = Volley.newRequestQueue(getApplicationContext());
    }

    public RequestQueue httpRequestQueue() {
        return httpRequestQueue;
    }

    public GoogleSignInAccount getAccount() {
        return account;
    }

    public void setAccount(GoogleSignInAccount account) {
        this.account = account;
    }
}
