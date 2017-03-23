package co.zync.zync.activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.DisplayMetrics;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.*;
import co.zync.zync.R;
import co.zync.zync.ZyncApplication;
import co.zync.zync.ZyncClipboardService;
import co.zync.zync.api.ZyncAPI;
import co.zync.zync.api.ZyncClipData;
import co.zync.zync.api.ZyncClipType;
import co.zync.zync.api.ZyncError;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class HistoryActivity extends AppCompatActivity {
    private int baseId = ThreadLocalRandom.current().nextInt(32189, 98432);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onStart() {
        super.onStart();
        setContentView(R.layout.activity_history);
        setupActionBar();

        final ProgressDialog dialog = new ProgressDialog(this);

        dialog.setIndeterminate(true);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setTitle(R.string.loading_history);
        dialog.setMessage(getString(R.string.please_wait));

        dialog.show();

        final ZyncApplication app = getZyncApp();

        app.getApi().getHistory(app.getEncryptionPass(), new ZyncAPI.ZyncCallback<List<ZyncClipData>>() {
            @Override
            public void success(final List<ZyncClipData> history) {
                app.setLastRequestStatus(history != null);

                if (history != null) {
                    /*
                     * Load history from file and compare with server.
                     *
                     * If the client has the data for the clipboard entry locally, load from there.
                     * Otherwise, add the timestamp to a list that we will request the payload for.
                     */
                    List<ZyncClipData> localHistory = historyFromFile();
                    List<Long> missingTimestamps = new ArrayList<>();

                    for (ZyncClipData historyEntry : history) {
                        ZyncClipData local = app.clipFromTimestamp(historyEntry.timestamp(), localHistory);

                        if (local != null && local.data() != null) {
                            historyEntry.setData(local.data());
                        } else {
                            missingTimestamps.add(historyEntry.timestamp());
                        }
                    }

                    /*
                     * If we are missing data (if we have been offline for some time or some other reason):
                     *
                     * Request the clip data from the server
                     * add the data to our list
                     * Update history in local storage
                     * Display history on Activity
                     *
                     * Otherwise, display local history on activity
                     */
                    if(!missingTimestamps.isEmpty()) {
                        app.getApi().getClipboard(getZyncApp().getEncryptionPass(), missingTimestamps, new ZyncAPI.ZyncCallback<List<ZyncClipData>>() {
                            @Override
                            public void success(List<ZyncClipData> value) {
                                for (ZyncClipData clip : value) {
                                    app.clipFromTimestamp(clip.timestamp(), history).setData(clip.data());
                                }

                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        setHistory(history);
                                        app.setHistory(history);
                                        dialog.dismiss();
                                    }
                                });
                            }

                            @Override
                            public void handleError(ZyncError error) {
                                handleHistoryError(dialog, error);
                            }
                        });
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setHistory(history);
                                app.setHistory(history);
                                dialog.dismiss();
                            }
                        });
                    }
                } else {
                    // if there was an error processing server history, load from file
                    loadHistoryFromFile();
                }
            }

            @Override
            public void handleError(ZyncError error) {
                handleHistoryError(dialog, error);
            }
        });
    }

    private void handleHistoryError(ProgressDialog dialog, ZyncError error) {
        dialog.dismiss();
        getZyncApp().setLastRequestStatus(false);
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(HistoryActivity.this);

        alertDialog.setTitle(R.string.unable_fetch_history);
        alertDialog.setMessage(getString(R.string.unable_fetch_history_msg, error.code(), error.message()));
        alertDialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {}
        });

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                alertDialog.show();
            }
        });

        loadHistoryFromFile();
    }

    /*       ACTION BAR START         */

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.history);
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

    /*       ACTION BAR END         */

    private ZyncApplication getZyncApp() {
        return (ZyncApplication) getApplication();
    }

    private void loadHistoryFromFile() {
        final List<ZyncClipData> history = historyFromFile();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setHistory(history);
            }
        });
    }

    private List<ZyncClipData> historyFromFile() {
        Set<String> historyStr = getZyncApp().getPreferences()
                .getStringSet("zync_history", new HashSet<String>());
        final List<ZyncClipData> history = new ArrayList<>(historyStr.size());

        for (String json : historyStr) {
            try {
                history.add(new ZyncClipData(getZyncApp().getEncryptionPass(), new JSONObject(json)));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Collections.sort(history, new ZyncClipData.TimeComparator());
        return history;
    }

    // set history that is being displayed on the activity
    // this will not override any pre-existing entries
    private void setHistory(List<ZyncClipData> history) {
        int nextId = baseId;

        for (int i = 0; i < history.size(); i++) {
            final ZyncClipData data = history.get(i);

            if (data.data() == null) {
                System.out.println(data.timestamp() + " is ignored due to null data");
                continue;
            }

            // (allows us to set an onClick listener for the whole region the entry covers)
            RelativeLayout layoutForClip = new RelativeLayout(this);
            layoutForClip.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

            // prepare the title
            TextView title = new TextView(this);

            // set title, layout params, and appearance of title
            title.setId(nextId++);
            title.setText(getString(R.string.history_entry_title, getString(data.type().presentableName())));
            setLayout(title, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 17);
            setTextAppearance(title, android.R.style.TextAppearance_Material_Headline);
            title.setTextSize(20);

            layoutForClip.addView(title);

            // prepare buttons
            layoutForClip.addView(createShareButton(
                    data,
                    title,
                    nextId++
            ));

            if (data.type() == ZyncClipType.TEXT) {
                layoutForClip.addView(createCopyButton(
                        data,
                        title,
                        nextId++
                ));
            }

            // prepare little "click to copy"
            TextView subTitle = new TextView(this);

            subTitle.setId(nextId++);
            subTitle.setText(DateUtils.getRelativeTimeSpanString(
                    data.timestamp(),
                    System.currentTimeMillis(),
                    DateUtils.SECOND_IN_MILLIS,
                    DateUtils.FORMAT_NUMERIC_DATE
            ));
            setLayout(subTitle, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, -1);
            setLayoutBelow(subTitle, title);
            setTextAppearance(subTitle, android.R.style.TextAppearance_Material_Small);
            subTitle.setTextSize(12);

            layoutForClip.addView(subTitle);

            TextView description = new TextView(this);
            // prepare description
            switch (data.type()) {
                case TEXT:
                    byte[] rawData = data.data();
                    String text = rawData == null ?
                            getString(R.string.history_encryption_error) :
                            new String(data.data());

                    if (text.length() > 50) {
                        text = text.substring(0, 46) + "\u2026";
                    }

                    description.setText(text);
                    break;

                case IMAGE:
                    // TODO give image info (or possibly preview image?)
                    description.setText(R.string.history_image_desc);

            }

            // set details for description and add to layout
            description.setId(nextId++);
            setLayout(description, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 10);
            setLayoutBelow(description, subTitle);
            setTextAppearance(description, android.R.style.TextAppearance_Material_Body1);
            description.setTextSize(13);
            layoutForClip.addView(description);

            // prepare separator
            ImageView separator = new ImageView(this);

            separator.setId(nextId++);
            setLayout(separator, LayoutParams.MATCH_PARENT, 1, 14);
            setLayoutBelow(separator, description);
            separator.setContentDescription(getString(R.string.separator));
            separator.setImageDrawable(new ColorDrawable(Color.rgb(211, 211, 211)));

            // add separator
            layoutForClip.addView(separator);

            // add layout for this entry to the main linear layout
            ((LinearLayout) findViewById(R.id.history_layout)).addView(layoutForClip);
        }
    }

    private ImageView createCopyButton(final ZyncClipData data, TextView title, int id) {
        final ImageView view = new ImageView(this);

        view.setId(id);
        setLayout(view, 30f, 30f, 3);
        view.setImageDrawable(getDrawable(R.drawable.ic_content_copy_black));
        view.setContentDescription(getString(R.string.history_copy_button));

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(view.getLayoutParams());

        params.addRule(RelativeLayout.BELOW, title.getId());
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        params.rightMargin = convertDpToPixel(50);

        view.setLayoutParams(params);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // update clipboard and display snackbar
                ZyncClipboardService.getInstance().writeToClip(new String(data.data()), false);
                Snackbar.make(
                        v,
                        R.string.history_clip_updated_msg,
                        Snackbar.LENGTH_LONG
                ).show();
            }
        });

        return view;
    }

    private ImageView createShareButton(final ZyncClipData data, TextView title, int id) {
        final ImageView view = new ImageView(this);

        view.setId(id);
        setLayout(view, 30f, 30f, 3);
        view.setImageDrawable(getDrawable(R.drawable.ic_share_black));

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(view.getLayoutParams());

        params.addRule(RelativeLayout.BELOW, title.getId());
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);

        view.setLayoutParams(params);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (data.type()) {
                    case TEXT:
                        Intent sendIntent = new Intent();
                        sendIntent.setAction(Intent.ACTION_SEND);
                        sendIntent.putExtra(Intent.EXTRA_TEXT, new String(data.data()));
                        sendIntent.setType("text/plain");
                        startActivity(sendIntent);
                        break;

                    case IMAGE:
                        // TODO
                }
            }
        });

        return view;
    }

    private void setTextAppearance(TextView view, int resId) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            view.setTextAppearance(this, resId);
        } else {
            view.setTextAppearance(resId);
        }
    }

    private void setLayoutBelow(View view, View below) {
        RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(view.getLayoutParams());

        if (view.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            p.topMargin = ((ViewGroup.MarginLayoutParams) view.getLayoutParams()).topMargin;
        }

        p.addRule(RelativeLayout.BELOW, below.getId());

        view.setLayoutParams(p);
    }

    private void setLayout(View view, float width, float height, int marginTop) {
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        int heightPx = height < 0 ? (int) height : convertDpToPixel(height);
        int widthPx = width < 0 ? (int) width : convertDpToPixel(width);

        if (params == null) {
            params = new ViewGroup.MarginLayoutParams(widthPx, heightPx);
        }

        params.height = heightPx;
        params.width = widthPx;

        if (marginTop != -1) {
            params.topMargin = convertDpToPixel(marginTop);
        }

        view.setLayoutParams(params);
    }

    private int convertDpToPixel(float dp) {
        Resources resources = getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return (int) Math.floor(dp * (metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT));
    }
}
