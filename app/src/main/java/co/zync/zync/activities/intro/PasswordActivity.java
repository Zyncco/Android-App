package co.zync.zync.activities.intro;

import android.content.Intent;
import android.preference.PreferenceActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import co.zync.zync.R;
import co.zync.zync.ZyncApplication;
import co.zync.zync.activities.SettingsActivity;

public class PasswordActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password);

        final EditText passEntry = (EditText) findViewById(R.id.encryption_pass);
        passEntry.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                    ((ZyncApplication) getApplication()).getPreferences().edit()
                            .putString("encryption_pass", passEntry.getText().toString())
                            .putBoolean("encryption_enabled", true)
                            .apply();
                    // move to next screen
                    ((ZyncApplication) getApplication()).openSettings(PasswordActivity.this);
                    return true;
                }
                return false;
            }
        });
    }
}
