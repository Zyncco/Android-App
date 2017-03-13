package co.zync.zync;

import android.app.Application;
import android.content.Intent;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

public class ZyncApplication extends Application {
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
}
