package co.zync.zync;

import android.app.Application;

public class ZyncApplication extends Application {
    private ZyncClipboardHandler clipboardHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        clipboardHandler = new ZyncClipboardHandler(this);
    }
}
