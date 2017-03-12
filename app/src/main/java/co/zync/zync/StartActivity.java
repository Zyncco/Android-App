package co.zync.zync;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class StartActivity extends AppCompatActivity {
    private ZyncClipboardHandler clipMan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        clipMan = new ZyncClipboardHandler(this);
    }

    public void onCopyClick(View view) {
        clipMan.writeToClip(
                3,
                ((EditText) findViewById(R.id.editText)).getText().toString(),
                false
        );
    }
}
