package co.zync.zync.activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import co.zync.zync.R;
import co.zync.zync.ZyncApplication;
import co.zync.zync.api.ZyncAPI;
import co.zync.zync.api.ZyncClipData;
import co.zync.zync.api.ZyncError;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

public class HistoryActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onStart() {
        super.onStart();
        setContentView(R.layout.activity_history);

        final ProgressDialog dialog = new ProgressDialog(this);

        dialog.setIndeterminate(true);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setTitle(R.string.loading_history);
        dialog.setMessage(getString(R.string.please_wait));

        getZyncApp().getApi().getHistory(getZyncApp().getEncryptionPass(), new ZyncAPI.ZyncCallback<List<ZyncClipData>>() {
            @Override
            public void success(List<ZyncClipData> value) {
                dialog.dismiss();
                getZyncApp().setLastRequestStatus(true);
                // todo do stuff and check values, add to menu
            }

            @Override
            public void handleError(ZyncError error) {
                dialog.dismiss();
                getZyncApp().setLastRequestStatus(false);
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(HistoryActivity.this);

                alertDialog.setTitle(R.string.unable_fetch_history);
                alertDialog.setMessage(getString(R.string.unable_fetch_history_msg, error.code(), error.message()));
                alertDialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {}
                });
                alertDialog.show();

                loadHistoryFromFile();
            }
        });
    }



    private ZyncApplication getZyncApp() {
        return (ZyncApplication) getApplication();
    }

    private void loadHistoryFromFile() {
        Set<String> historyStr = getZyncApp().getPreferences()
                .getStringSet("zync_history", new HashSet<String>());
        final List<ZyncClipData> history = new ArrayList<>(historyStr.size());

        for (String json : historyStr) {
            try {
                history.add(new ZyncClipData(getZyncApp().getEncryptionPass(), new JSONObject(json)));
            } catch (JSONException ignored) {
            }
        }

        Collections.sort(history, new ZyncClipData.TimeComparator());

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setHistory(history);
            }
        });
    }

    private void setHistory(List<ZyncClipData> history) {
        for (int i = 0; i < history.size(); i++) {
            ZyncClipData data = history.get(i);

            // prepare the layout this entry will fall under
            // (allows us to set an onClick listener for the whole region the entry covers)
            RelativeLayout layoutForClip = new RelativeLayout(this);

            layoutForClip.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
            layoutForClip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // TODO update their clipboard with new thing
                    //    - update history with new entries, redrawing the activity
                    //    - notify them of the change
                }
            });

            // prepare the title
            TextView title = new TextView(this);
            // create a string matching to their locale
            Date date = new Date(data.timestamp());
            String stampString = DateFormat.getDateFormat(this).format(date) +
                    " " + DateFormat.getTimeFormat(this).format(date);

            // TODO make the timestamp an info button that is shown as a toast
            // set title, layout params, and appearance of title
            title.setText(getString(R.string.history_entry_title, stampString));
            setLayout(title, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 17);
            setTextAppearance(title, android.R.style.TextAppearance_Material_Headline);
            title.setTextSize(20);

            layoutForClip.addView(title);

            // prepare little "click to copy"
            TextView subTitle = new TextView(this);

            subTitle.setText(R.string.click_to_copy);
            setLayout(subTitle, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, -1);
            setTextAppearance(subTitle, android.R.style.TextAppearance_Material_Small);
            subTitle.setTextSize(13);

            layoutForClip.addView(subTitle);

            TextView description = new TextView(this);
            // prepare description
            switch (data.type()) {
                case TEXT:
                    byte[] rawData = data.data();
                    String text = rawData == null ?
                            getString(R.string.history_encryption_error) :
                            new String(data.data());

                    if (text.length() > 50) { // todo check if this is too long
                        text = text.substring(0, 46) + "\u2026";
                    }

                    description.setText(text);
                    break;

                case IMAGE:
                    description.setText(R.string.history_image_desc);

            }

            // set details for description and add to layout
            setLayout(description, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 10);
            setTextAppearance(description, android.R.style.TextAppearance_Material_Body1);
            description.setTextSize(13);
            layoutForClip.addView(description);

            // prepare separator
            ImageView separator = new ImageView(this);

            setLayout(separator, LayoutParams.MATCH_PARENT, 1, 14);
            separator.setContentDescription(getString(R.string.separator));
            separator.setColorFilter(new PorterDuffColorFilter(Color.rgb(211, 211, 211), PorterDuff.Mode.SRC_ATOP));

            // add separator
            layoutForClip.addView(separator);

            // add layout for this entry to the main linear layout
            ((LinearLayout) findViewById(R.id.history_layout)).addView(layoutForClip);
        }
    }

    private void setTextAppearance(TextView view, int resId) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            view.setTextAppearance(this, resId);
        } else {
            view.setTextAppearance(resId);
        }
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
            params.topMargin = marginTop;
        }

        view.setLayoutParams(params);
    }

    private int convertDpToPixel(float dp) {
        Resources resources = getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return (int) Math.floor(dp * (metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT));
    }
}
