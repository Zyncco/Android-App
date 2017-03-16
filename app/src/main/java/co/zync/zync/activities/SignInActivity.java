package co.zync.zync.activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;

import android.widget.EditText;
import co.zync.zync.*;
import co.zync.zync.activities.intro.IntroActivity;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.ResultCodes;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;

import java.util.Arrays;

import co.zync.zync.api.ZyncAPI;
import co.zync.zync.api.ZyncError;

/*
 * Activity presented to the user when they are signing in (with google)
 */
public class SignInActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener, View.OnClickListener {
    private static final int RC_SIGN_IN = 64209;
    private AlertDialog passwordDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signin);

        // make sure google play services is available
        GoogleApiAvailability.getInstance().makeGooglePlayServicesAvailable(this);
        findViewById(R.id.sign_in_button).setOnClickListener(this);

        SharedPreferences preferences = getZyncApp().getPreferences();

        if (!preferences.contains("seen_intro")) {
            startActivity(new Intent(this, IntroActivity.class));
            return;
        }

        /*
         * If the user has logged in before, set the API variable and continue to settings.
         */
        if (preferences.contains("zync_api_token") && !BuildConfig.DEBUG) {
            getZyncApp().setApi(ZyncAPI.login(
                    getZyncApp().httpRequestQueue(),
                    preferences.getString("zync_api_token", "")
            ));
            getZyncApp().openSettings(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        GoogleApiAvailability.getInstance().makeGooglePlayServicesAvailable(this);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // yell at user to get internet
        // Unrecoverable.
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            // Successfully signed in
            if (resultCode == ResultCodes.OK) {
                System.out.println("yay signed in for " + response.getEmail());
                handleSignIn(response);
            } else {
                System.out.println("that's sad... now what");
            }
        }
    }

    private void handleSignIn(IdpResponse result) {
        // Signed in successfully, show authenticated UI.
        System.out.println(result.getEmail() + " is logged in!");
        final ZyncApplication app = getZyncApp();
        final ProgressDialog dialog = new ProgressDialog(this, R.style.AppTheme);
        dialog.setIndeterminate(true);
        dialog.setTitle("");
        dialog.setMessage(getString(R.string.signing_in));
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.show();

        ZyncAPI.signup(
                app.httpRequestQueue(),
                result.getIdpToken(),
                result.getProviderType(),
                new ZyncAPI.ZyncCallback<ZyncAPI>() {
                    @Override
                    public void success(ZyncAPI api) {
                        app.setApi(api);
                        app.getPreferences().edit().putString("zync_api_token", api.getToken()).apply();


                        if (!app.getPreferences().contains("encryption_enabled") || BuildConfig.DEBUG) {
                            dialog.dismiss();
                            promptForPassword();
                        } else {
                            enterMainActivity();
                        }
                    }

                    @Override
                    public void handleError(ZyncError error) {
                        System.out.println(error.toString());
                        dialog.dismiss();
                        // TODO do something
                        if (BuildConfig.DEBUG) {
                            success(new ZyncAPI(app.httpRequestQueue(), ""));
                        }
                    }
                }
        );
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sign_in_button:
                startActivityForResult(
                        AuthUI.getInstance()
                                .createSignInIntentBuilder()
                                .setProviders(Arrays.asList(
                                        new AuthUI.IdpConfig.Builder(AuthUI.FACEBOOK_PROVIDER).build(),
                                        new AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build()
                                        ))
                                .setTheme(R.style.AppTheme)
                                .build(),
                        RC_SIGN_IN);
                break;
        }
    }

    private void enterMainActivity() {
        startActivity(new Intent(SignInActivity.this, MainActivity.class));
        getZyncApp().syncDown();
    }

    private boolean handlePassword(EditText view) {
        String enteredPass = view.getText().toString();

        if (enteredPass.length() <= 10) {
            view.setError(getString(R.string.password_insufficient));
            return false;
        }

        // todo test password entropy (consecutive characters, etc.)

        ZyncApplication app = (ZyncApplication) getApplication();
        app.getPreferences().edit()
                .putString("encryption_pass", enteredPass)
                .putBoolean("encryption_enabled", true)
                .apply();
        // move to next screen
        startActivity(new Intent(this, MainActivity.class));
        app.syncDown();
        return true;
    }

    private void dontUseEncryption() {
        ZyncApplication app = (ZyncApplication) getApplication();
        app.getPreferences().edit()
                .putString("encryption_pass", "")
                .putBoolean("encryption_enabled", false)
                .apply();
        // move to next screen
        startActivity(new Intent(this, MainActivity.class));
        app.syncDown();
    }

    private void promptForPassword() {
        final AlertDialog.Builder passwordDialogBuilder = new AlertDialog.Builder(this);
        final EditText view = new EditText(this);
        view.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        view.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                return event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER && handlePassword(view);
            }
        });

        passwordDialogBuilder.setView(view);
        passwordDialogBuilder.setTitle(R.string.encryption_password_title);
        passwordDialogBuilder.setMessage(R.string.encryption_pass_sum);
        passwordDialogBuilder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {}
        });
        passwordDialogBuilder.setNegativeButton(R.string.disable_encryption, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                passwordDialog.dismiss();
                passwordDialog = null;
                showEncryptionWarning();
            }
        });

        passwordDialog = passwordDialogBuilder.show();

        passwordDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (handlePassword(view)) {
                    passwordDialog.dismiss();
                }
            }
        });
    }

    private void showEncryptionWarning() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle(R.string.encryption_warning_title);
        alert.setMessage(R.string.encryption_warning_message);
        alert.setNegativeButton(R.string.encryption_warning_yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dontUseEncryption();
            }
        });
        alert.setPositiveButton(R.string.encryption_warning_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                promptForPassword();
            }
        });

        alert.show();
    }

    private ZyncApplication getZyncApp() {
        return (ZyncApplication) getApplication();
    }
}
