package co.zync.zync;

import android.app.Application;

public class ZyncApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        new ZyncClipboardHandler(this);
    }
}
