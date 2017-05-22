package co.zync.android.activities;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.support.v4.content.FileProvider;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;
import co.zync.android.R;
import co.zync.android.ZyncApplication;
import co.zync.android.utils.ZyncExceptionInfo;
import co.zync.android.utils.ZyncPassDialog;

import java.io.IOException;
import java.util.List;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 */
public class SettingsActivity extends AppCompatPreferenceActivity {
    public static final String PREFERENCES_NAME = "ZyncPrefs";

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.zync_settings);
        }
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            if (!super.onMenuItemSelected(featureId, item)) {
                startActivity(new Intent(this, MainActivity.class));
            }
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return GeneralPreferenceFragment.class.getName().equals(fragmentName);
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            getPreferenceManager().setSharedPreferencesName(PREFERENCES_NAME);
            getPreferenceManager().setSharedPreferencesMode(Context.MODE_PRIVATE);
            addPreferencesFromResource(R.xml.pref_general);
            findPreference("encryption_pass").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    new ZyncPassDialog(
                            getActivity(),
                            (ZyncApplication) getActivity().getApplication(),
                            new ZyncPassDialog.Callback() {
                                @Override
                                public void callback() {
                                }
                            }).promptForPassword();
                    return true;
                }
            });
            //setHasOptionsMenu(true);

            findPreference("feedback").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);

                    emailIntent.setType("*/*");
                    emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] {"feedback@zync.co"});
                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Feedback on Zync for Android");
                    emailIntent.putExtra(Intent.EXTRA_TEXT, "Enter your feedback here! The attached file is information about your session " +
                            "that will allow us to fix any bugs that you encountered and reporting about. If you're just leaving a message " +
                            "and not reporting an issue, feel free to remove the attachment.");

                    try {
                        emailIntent.putExtra(
                                Intent.EXTRA_STREAM,
                                FileProvider.getUriForFile(
                                        getActivity().getApplicationContext(),
                                        "co.zync.zync.fileprovider",
                                        ((ZyncApplication) getActivity().getApplication()).createInfoFile()
                                )
                        );
                    } catch (IOException ex) {
                        ZyncApplication.LOGGED_EXCEPTIONS.add(new ZyncExceptionInfo(ex, "creating debug file"));
                        ex.printStackTrace();
                    }

                    if (emailIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                        startActivity(emailIntent);
                    } else {
                        ((ZyncApplication) getActivity().getApplication()).directToLink("https://github.com/Zyncco/Android-App/issues/new", R.string.no_emailc_or_web);
                    }
                    return true;
                }
            });

            findPreference("github").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    ((ZyncApplication) getActivity().getApplication()).directToLink("https://github.com/Zyncco/Android-App/", R.string.no_webc);
                    return true;
                }
            });

            findPreference("credits").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    startActivity(new Intent(getActivity(), CreditActivity.class));
                    return true;
                }
            });
        }
    }
}
