package co.zync.zync.activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import co.zync.zync.*;
import co.zync.zync.activities.intro.IntroActivity;
import co.zync.zync.api.callback.ZyncCallback;
import co.zync.zync.utils.ZyncPassDialog;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.ResultCodes;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;

import java.util.Arrays;

import co.zync.zync.api.ZyncAPI;
import co.zync.zync.api.ZyncError;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import org.json.JSONException;

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

        if (!preferences.contains("seen_intro") || (BuildConfig.DEBUG && !getIntent().hasExtra("intro_direct"))) {
            startActivity(new Intent(this, IntroActivity.class));
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
        final ProgressDialog dialog = createSignInDialog();

        FirebaseUser mUser = FirebaseAuth.getInstance().getCurrentUser();
        mUser.getToken(true)
                .addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
                    public void onComplete(@NonNull Task<GetTokenResult> task) {
                        if (task.isSuccessful()) {
                            String idToken = task.getResult().getToken();

                            try {
                                getZyncApp().setAuthenticateCallback(new AuthenticateCallback(SignInActivity.this, dialog));
                                ZyncAPI.authenticate(
                                        app.httpClient(),
                                        idToken,
                                        getZyncApp().authenticateCallback()
                                );
                            } catch (JSONException ignored) {
                            }
                        }

                        // todo handle error
                    }
                });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sign_in_button:
                if (getZyncApp().httpClient() != null) {
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setProviders(Arrays.asList(
                                            new AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build()
                                    ))
                                    .setIsSmartLockEnabled(!BuildConfig.DEBUG) // disableClipboardService smart lock if debugging
                                    .setTheme(R.style.AppTheme)
                                    .build(),
                            RC_SIGN_IN);
                } else {
                    AlertDialog.Builder dialog = new AlertDialog.Builder(this);
                    dialog.setTitle(R.string.no_internet);
                    dialog.setMessage(R.string.no_internet_desc);
                    dialog.setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {}
                    });
                    dialog.show();
                }
                break;
        }
    }

    private ProgressDialog createSignInDialog() {
        final ProgressDialog dialog = new ProgressDialog(this, R.style.AppTheme);
        dialog.setIndeterminate(true);
        dialog.setTitle("");
        dialog.setMessage(getString(R.string.signing_in));
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.show();

        return dialog;
    }

    private void signInSuccess() {
        startService(new Intent(this, ZyncClipboardService.class));
        startActivity(new Intent(getApplicationContext(), MainActivity.class));
        getZyncApp().syncDown();
        finish();
    }

    private ZyncApplication getZyncApp() {
        return (ZyncApplication) getApplication();
    }

    public static class AuthenticateCallback implements ZyncCallback<Void> {
        SignInActivity activity;
        ProgressDialog dialog;
        ZyncApplication app;

        AuthenticateCallback(SignInActivity activity, ProgressDialog dialog) {
            this.dialog = dialog;
            this.activity = activity;
            this.app = activity.getZyncApp();
        }

        @Override
        public void success(Void api) {
        }

        @Override
        public void handleError(ZyncError error) {
            System.out.println(error.toString());
            dialog.dismiss();
            // TODO do something
        }

        public void callback(ZyncAPI api) {
            app.setApi(api);
            app.getPreferences().edit().putString("zync_api_token", api.getToken()).apply();

            dialog.dismiss();

            if (!app.getPreferences().contains("encryption_pass")) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        new ZyncPassDialog(activity, app, new ZyncPassDialog.Callback() {
                            @Override
                            public void callback() {
                                activity.signInSuccess();
                            }
                        }).promptForPassword();
                    }
                });
            } else {
                activity.signInSuccess();
            }
        }
    }
}
