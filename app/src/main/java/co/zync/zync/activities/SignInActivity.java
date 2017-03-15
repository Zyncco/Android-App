package co.zync.zync.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import co.zync.zync.*;
import co.zync.zync.activities.intro.IntroActivity;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.ResultCodes;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;

import java.util.Arrays;

import co.zync.zync.activities.intro.PasswordActivity;
import co.zync.zync.api.ZyncAPI;
import co.zync.zync.api.ZyncError;

/*
 * Activity presented to the user when they are signing in (with google)
 */
public class SignInActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener, View.OnClickListener {
    private static final int RC_SIGN_IN = 64209;

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
                new ZyncAPI.ZyncCallback<ZyncAPI>() {
                    @Override
                    public void success(ZyncAPI api) {
                        app.setApi(api);
                        app.getPreferences().edit().putString("zync_api_token", api.getToken()).apply();


                        if (!app.getPreferences().contains("encryption_enabled") || BuildConfig.DEBUG) {
                            startActivity(new Intent(SignInActivity.this, PasswordActivity.class));
                        } else {
                            //app.openSettings(SignInActivity.this);
                            startActivity(new Intent(SignInActivity.this, MainActivity.class));
                            app.syncDown();
                        }

                        dialog.dismiss();
                    }

                    @Override
                    public void handleError(ZyncError error) {
                        System.out.println(error.toString());
                        dialog.dismiss();
                        // TODO do something
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
                                .setProviders(Arrays.asList(new AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build()))
                                .setTheme(R.style.AppTheme)
                                .build(),
                        RC_SIGN_IN);
                break;
        }
    }

    private ZyncApplication getZyncApp() {
        return (ZyncApplication) getApplication();
    }
}
