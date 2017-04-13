package co.zync.zync.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.net.Uri;
import android.os.AsyncTask;
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
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import co.zync.zync.*;
import co.zync.zync.api.ZyncClipData;
import co.zync.zync.api.ZyncClipType;
import co.zync.zync.api.ZyncError;
import co.zync.zync.api.callback.ZyncCallback;
import co.zync.zync.services.ZyncClipboardService;
import co.zync.zync.listeners.RequestStatusListener;
import co.zync.zync.utils.ZyncCircleView;
import co.zync.zync.utils.ZyncExceptionInfo;
import co.zync.zync.utils.ZyncPostImage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    private static final Timer TIMER = new Timer();
    public static int REQUEST_IMAGE = 23212;
    private File currentFile;
    private ZyncCircleView.ColorChangeTask circleColorChangeTask;
    private ZyncCircleView.SizeChangeTask circleBreathingTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkIntent();

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
            navigationView.removeView(navigationView.findViewById(R.id.camera));
        }

        // circle toggle
        findViewById(R.id.zync_circle).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ZyncConfiguration preferences = getZyncApp().getConfig();
                preferences.setZyncOn(!preferences.zyncOn());

                updateCircleColor();
            }
        });

        // debug menu
        if (BuildConfig.DEBUG) {
            findViewById(R.id.zync_circle).setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    startActivity(new Intent(MainActivity.this, DebugActivity.class));
                    return true;
                }
            });
        }

        ViewGroup.LayoutParams params = findViewById(R.id.main_logo).getLayoutParams();
        int imageSize = (int) (Resources.getSystem().getDisplayMetrics().widthPixels * 0.75);

        params.height = imageSize;
        params.width = imageSize;

        findViewById(R.id.main_logo).requestLayout();
    }

    @Override
    protected void onStart() {
        super.onStart();
        scheduleCircleTask();
    }

    private void scheduleCircleTask() {
        if (circleColorChangeTask == null) {
            circleColorChangeTask = new ZyncCircleView.ColorChangeTask(
                    this,
                    (ZyncCircleView) findViewById(R.id.zync_circle),
                    30
            );

            TIMER.scheduleAtFixedRate(circleColorChangeTask, 50, 50);
        }

        scheduleCircleSizeTask();

        getZyncApp().setRequestStatusListener(new RequestStatusListener() {
            @Override
            public void onStatusChange(boolean value) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateCircleColor();
                    }
                });
            }
        });
    }

    private void scheduleCircleSizeTask() {
        if (circleBreathingTask == null) {
            circleBreathingTask = new ZyncCircleView.SizeChangeTask(
                    this,
                    (ZyncCircleView) findViewById(R.id.zync_circle),
                    120
            );

            TIMER.scheduleAtFixedRate(circleBreathingTask, 75, 75);
        }
    }

    private void updateCircleColor() {
        ZyncCircleView circle = (ZyncCircleView) findViewById(R.id.zync_circle);

        if (!getZyncApp().getConfig().zyncOn()) {
            // make circle gray start
            ColorMatrix matrix = new ColorMatrix();
            matrix.setSaturation(0);

            ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
            ((ImageView) findViewById(R.id.main_logo)).setColorFilter(filter);
            // make circle gray end

            // update color of "border"
            circle.setColor(ZyncCircleView.GRAY_OFF);
            getZyncApp().disableClipboardService();

            // stop breathing of circle
            if (circleBreathingTask != null) {
                circleBreathingTask.cancel();
                circleBreathingTask = null;
            }

            return;
        } else {
            circle.setColor(ZyncCircleView.GREEN_OK);

            ((ImageView) findViewById(R.id.main_logo)).setColorFilter(null);
            getZyncApp().enableClipboardService();
            scheduleCircleSizeTask();
        }

        if (!getZyncApp().lastRequestStatus()) {
            circle.setColor(ZyncCircleView.RED_ERROR);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (circleColorChangeTask != null) {
            circleColorChangeTask.cancel();
            circleColorChangeTask = null;
        }

        if (circleBreathingTask != null) {
            circleBreathingTask.cancel();
            circleBreathingTask = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        scheduleCircleTask();
        updateCircleColor();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        }
    }

    // creates a temporary image file used when capturing images
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "PNG_" + timeStamp + "_";
        File storageDir = new File(getFilesDir(), "images/");
        storageDir.mkdirs();
        return File.createTempFile(
                imageFileName,  /* prefix */
                ".png",         /* suffix */
                storageDir      /* directory */
        );
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_settings) {
            getZyncApp().openSettings();
        } else if (id == R.id.history) {
            startActivity(new Intent(this, HistoryActivity.class));
        } else if (id == R.id.nav_help) {
            startActivity(new Intent(this, HelpActivity.class));
        } else if (id == R.id.logout) {
            getZyncApp().setApi(null);
            getZyncApp().getConfig().clear();
            stopService(new Intent(this, ZyncClipboardService.class));
            startActivity(new Intent(this, SignInActivity.class));
        } else if (id == R.id.camera) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            // ensure there's an app which can take the picture for us
            if (intent.resolveActivity(getPackageManager()) != null) {
                File photoFile = null;

                try {
                    photoFile = currentFile = createImageFile();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    ZyncApplication.LOGGED_EXCEPTIONS.add(new ZyncExceptionInfo(ex, "create image file for camera feature"));
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

    private ProgressDialog createUploadingDialog() {
        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setTitle(R.string.uploading_image);
        dialog.setMessage(getString(R.string.please_wait));
        dialog.setIndeterminate(true);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.show();

        return dialog;
    }

    @Override
    // method called back when a picture is taken through the cam feature
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE && resultCode == RESULT_OK) {
            final ProgressDialog dialog = createUploadingDialog();

            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    executeCameraCallback(dialog);
                    return null;
                }
            }.execute();
        }
    }

    private void executeCameraCallback(final ProgressDialog dialog) {
        try {
            // compress, encrypt, hash, and create clip data
            ZyncClipData clipData = getZyncApp().getDataManager().saveImage(new FileInputStream(currentFile));
            File clipFile = getZyncApp().getDataManager().fileFor(clipData, false);

            currentFile.delete(); // remove unencrypted file
            currentFile = null; // reset current file

            // post image to zync async
            ZyncPostImage.exec(
                    getZyncApp(),
                    clipFile, // temporary
                    clipData,
                    new ZyncCallback<Void>() {
                        @Override
                        public void success(Void value) {
                            dialog.dismiss();
                            getZyncApp().sendClipPostedNotification();
                        }

                        @Override
                        public void handleError(ZyncError error) {
                            dialog.dismiss();
                            getZyncApp().sendClipErrorNotification();
                            Log.e("ZyncClipboardService", "There was an error posting the clipboard: "
                                    + error.code() + " : " + error.message());
                        }
                    }
            );

            getZyncApp().getConfig().addToHistory(clipData);
        } catch (Exception ignored) {
            // ex here would be due to an encryption error from ZyncClipData
            // so it's not quite possible as we didn't provide any data
        }
    }

    private boolean checkIntent() {
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if (getZyncApp().getApi() != null) {
                final ProgressDialog uploadingDialog = createUploadingDialog();
                ZyncCallback<Void> callback = new ZyncCallback<Void>() {
                    @Override
                    public void success(Void value) {
                        uploadingDialog.dismiss();
                        getZyncApp().sendClipPostedNotification();
                    }

                    @Override
                    public void handleError(ZyncError error) {
                        uploadingDialog.dismiss();
                        getZyncApp().sendClipErrorNotification();
                    }
                };

                if (type.startsWith("image/")) {
                    try {
                        Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                        ZyncClipData clip = getZyncApp().getDataManager().saveImage(imageUri);

                        ZyncPostImage.exec(
                                getZyncApp(),
                                getZyncApp().getDataManager().fileFor(clip, false),
                                clip,
                                callback
                        );

                        getZyncApp().getConfig().addToHistory(clip);
                    } catch (IOException ex) {
                        uploadingDialog.dismiss();
                        getZyncApp().sendNotification(
                                ZyncApplication.CLIPBOARD_ERROR_ID,
                                getString(R.string.clipboard_post_error_notification),
                                getString(R.string.clipboard_post_error_notification_desc)
                        );
                    } catch (Exception ignored) {
                    }
                } else if ("text/plain".equals(type)) {
                    String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);

                    try {
                        getZyncApp().getApi().postClipboard(new ZyncClipData(
                                getZyncApp().getConfig().getEncryptionPass(),
                                ZyncClipType.TEXT,
                                sharedText.getBytes(Charset.forName("UTF-8"))
                        ), callback);
                        getZyncApp().sendClipPostedNotification();
                    } catch (Exception e) {
                        ZyncApplication.LOGGED_EXCEPTIONS.add(new ZyncExceptionInfo(e, "post clip from share"));
                        getZyncApp().sendClipErrorNotification();
                    }
                }
            } else {
                startActivity(new Intent(this, SignInActivity.class));
            }

            return true;
        }

        return false;
    }

    private ZyncApplication getZyncApp() {
        return (ZyncApplication) getApplication();
    }
}
