package co.zync.zync.activities;

import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import co.zync.zync.R;
import co.zync.zync.ZyncApplication;

public class CreditActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_credit);
        setupActionBar();

        link(R.id.mazen_github, "https://github.com/mkotb");
        link(R.id.mazen_telegram, "https://t.me/mazenk");

        link(R.id.amir_github, "https://github.com/aaomidi");
        link(R.id.brandon_github, "https://github.com/BranicYeti");

        link(R.id.vilsol_github, "https://github.com/Vilsol");
        link(R.id.vilsol_keybase, "https://keybase.io/vilsol");

        link(R.id.mattrick_github, "https://github.com/devmattrick");
        link(R.id.mattrick_keybase, "https://keybase.io/mattrick");
        link(R.id.mattrick_twitter, "https://twitter.com/devmattrick");

        link(R.id.mark_github, "https://github.com/DarkSeraphim");
        link(R.id.mark_linkedin, "https://linkedin.com/in/mark-hendriks-60029391");
    }

    void link(int viewId, final String url) {
        findViewById(viewId).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((ZyncApplication) getApplication()).directToLink(url, R.string.no_webc);
            }
        });
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.credits);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            ((ZyncApplication) getApplication()).openSettings();
            finish();
            System.gc();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
