package co.zync.zync.activities;

import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import co.zync.zync.R;
import co.zync.zync.ZyncApplication;
import co.zync.zync.ZyncClipboardService;

public class DebugActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);
        setupActionBar();
        final String debugInfo = ((ZyncApplication) getApplication()).debugInfo();

        ((TextView) findViewById(R.id.debug_text_view))
                .setText(debugInfo);

        findViewById(R.id.debug_copy_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ZyncClipboardService.getInstance().writeToClip(debugInfo, false);
                Toast.makeText(
                        DebugActivity.this,
                        R.string.debug_copied,
                        Toast.LENGTH_SHORT
                ).show();
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
            actionBar.setTitle(R.string.debug);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
