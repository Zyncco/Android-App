package co.zync.zync.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.NavigationView;
import android.support.v4.content.FileProvider;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import co.zync.zync.*;
import co.zync.zync.api.ZyncAPI;
import co.zync.zync.api.ZyncClipType;
import co.zync.zync.api.ZyncError;
import co.zync.zync.utils.ZyncCircleView;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    private static final Timer TIMER = new Timer();
    public static int REQUEST_IMAGE = 23212;
    private String currentPhotoPath;
    private ZyncCircleView.ColorChangeTask circleColorChangeTask;
    private ZyncCircleView.SizeChangeTask circleSizeChangeTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
         * SHARE START
         *
         * If the share feature was used with Zync,
         * read the data and act accordingly
         */
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if (getZyncApp().getApi() != null) {
                if (type.startsWith("image/")) {
                    Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                    new ZyncPostImageTask(getZyncApp(), getContentResolver())
                            .execute(imageUri);
                } else if ("text/plain".equals(type)) {
                    String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
                    new ZyncPostClipTask(
                            getZyncApp(),
                            sharedText.getBytes(Charset.forName("UTF-8")),
                            ZyncClipType.TEXT
                    ).execute();
                }
            } else {
                startActivity(new Intent(this, SignInActivity.class));
            }

            return;
        }

        /*                            SHARE END                           */

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // remove camera feature if the system does not support it
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            navigationView.removeView(navigationView.findViewById(R.id.feature_x));
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        scheduleCircleTask();
    }

    private void scheduleCircleTask() {
        if (circleColorChangeTask == null) {
            circleColorChangeTask = new ZyncCircleView.ColorChangeTask(
                    (ZyncCircleView) findViewById(R.id.zync_circle),
                    30
            );
            circleSizeChangeTask = new ZyncCircleView.SizeChangeTask(
                    this,
                    (ZyncCircleView) findViewById(R.id.zync_circle),
                    60
            );

            TIMER.scheduleAtFixedRate(circleColorChangeTask, 5, 5);
            TIMER.scheduleAtFixedRate(circleSizeChangeTask, 5, 5);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (circleColorChangeTask != null) {
            circleColorChangeTask.cancel();
            circleSizeChangeTask.cancel();

            circleColorChangeTask = null;
            circleSizeChangeTask = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        scheduleCircleTask();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_settings) {
            getZyncApp().openSettings(this);
        } else if (id == R.id.nav_help) {
            startActivity(new Intent(this, HelpActivity.class));
        } else if (id == R.id.logout) {
            getZyncApp().setApi(null);
            getZyncApp().getPreferences().edit()
                    .remove("encryption_pass")
                    .remove("zync_api_token")
                    .apply();
            stopService(new Intent(this, ZyncClipboardService.class));
            startActivity(new Intent(this, SignInActivity.class));
        } else if (id == R.id.feature_x) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            // ensure there's an app which can take the picture for us
            if (intent.resolveActivity(getPackageManager()) != null) {
                File photoFile = null;

                try {
                    photoFile = createImageFile();
                } catch (IOException ex) {
                    ex.printStackTrace();
                    // unable to create image file. TODO: Complain to user
                }

                if (photoFile != null) {
                    // generate a URI for the app to put the photo in
                    Uri photoURI = FileProvider.getUriForFile(this,
                            "co.zync.zync.fileprovider",
                            photoFile);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);

                    // move the user to the app to take the image
                    startActivityForResult(intent, REQUEST_IMAGE);
                }
            }
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    // creates a temporary image file used when capturing images
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "PNG_" + timeStamp + "_";
        File storageDir = new File(getFilesDir(), "images/");
        storageDir.mkdirs();
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".png",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE && resultCode == RESULT_OK) {
            final ProgressDialog dialog = new ProgressDialog(this);
            dialog.setTitle(R.string.uploading_image);
            dialog.setMessage(getString(R.string.please_wait));
            dialog.setIndeterminate(true);
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dialog.show();

            // post image to zync async
            new ZyncPostImageTask(
                    getZyncApp(),
                    BitmapFactory.decodeFile(currentPhotoPath),
                    new ZyncAPI.ZyncCallback<Void>() {
                        @Override
                        public void success(Void value) {
                            dialog.dismiss();
                            if (getZyncApp().getPreferences().getBoolean("clipboard_change_notification", true)) {
                                getZyncApp().sendNotification(
                                        ZyncApplication.CLIPBOARD_POSTED_ID,
                                        getString(R.string.clipboard_posted_notification),
                                        getString(R.string.clipboard_posted_notification_desc)
                                );
                            }
                        }

                        @Override
                        public void handleError(ZyncError error) {
                            dialog.dismiss();
                            getZyncApp().sendNotification(
                                    ZyncApplication.CLIPBOARD_ERROR_ID,
                                    getString(R.string.clipboard_post_error_notification),
                                    getString(R.string.clipboard_post_error_notification_desc)
                            );
                            Log.e("ZyncClipboardService", "There was an error posting the clipboard: "
                                    + error.code() + " : " + error.message());
                        }
                    }).execute(); // we do not need to give it a URI since we already provided the bitmap
        }
    }

    private ZyncApplication getZyncApp() {
        return (ZyncApplication) getApplication();
    }
}
