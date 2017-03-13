package co.zync.zync;

import android.app.Application;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

public class ZyncApplication extends Application {
    private RequestQueue httpRequestQueue;

    @Override
    public void onCreate() {
        super.onCreate();
        new ZyncClipboardHandler(this);
        httpRequestQueue = Volley.newRequestQueue(getApplicationContext());
    }

    public RequestQueue httpRequestQueue() {
        return httpRequestQueue;
    }
}
