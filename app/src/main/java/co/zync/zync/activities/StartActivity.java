package co.zync.zync.activities;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import co.zync.zync.R;
import co.zync.zync.ZyncApplication;
import co.zync.zync.ZyncPostClipTask;
import co.zync.zync.ZyncPostImageTask;
import co.zync.zync.api.ZyncClipType;

import java.nio.charset.Charset;

public class StartActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if (type.startsWith("image/")) {
                Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                new ZyncPostImageTask((ZyncApplication) getApplication(), getContentResolver())
                        .execute(imageUri);
            } else if ("text/plain".equals(type)) {
                String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
                new ZyncPostClipTask(
                        (ZyncApplication) getApplication(),
                        sharedText.getBytes(Charset.forName("UTF-8")),
                        ZyncClipType.TEXT
                ).execute();
            }
        }
    }
}
