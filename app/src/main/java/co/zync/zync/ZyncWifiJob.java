package co.zync.zync;

import android.app.job.JobParameters;
import android.app.job.JobService;

public class ZyncWifiJob extends JobService {
    @Override
    public boolean onStartJob(JobParameters params) {
        ZyncApplication app = (ZyncApplication) getApplication();

        if (app.isWifiConnected() && app.httpRequestQueue() == null) {
            if (!app.getPreferences().getBoolean("use_on_data", true)) {
                app.setupNetwork();
            }
        } else if (app.httpRequestQueue() != null) {
            app.removeNetworkUsages();
        }

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }
}
