package co.zync.android.utils;

import android.os.AsyncTask;

/**
 * @author Mazen Kotb
 */
public class GenericAsyncTask extends AsyncTask<Void, Void, Void> {
    private final Runnable runnable;

    public GenericAsyncTask(Runnable runnable) {
        this.runnable = runnable;
    }

    @Override
    protected Void doInBackground(Void... params) {
        runnable.run();
        return null;
    }
}
