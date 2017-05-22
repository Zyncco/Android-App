package co.zync.android.activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import co.zync.android.*;
import co.zync.android.activities.intro.IntroActivity;
import co.zync.android.api.callback.ZyncCallback;
import co.zync.android.services.ZyncClipboardService;
import co.zync.android.utils.ZyncPassDialog;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;

import co.zync.android.api.ZyncAPI;
import co.zync.android.api.ZyncError;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.*;
import io.fabric.sdk.android.Fabric;
import org.json.JSONException;

/*
 * Activity presented to the user when they are signing in (with google)
 */
public class SignInActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener, View.OnClickListener {
    private static final int RC_SIGN_IN = 64209;
    private GoogleApiClient googleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signin);

        // make sure google play services is available
        GoogleApiAvailability.getInstance().makeGooglePlayServicesAvailable(this);
        Fabric.with(this, new Crashlytics());

        SignInButton signInButton = (SignInButton) findViewById(R.id.sign_in_button);
        signInButton.setSize(SignInButton.SIZE_WIDE);

        findViewById(R.id.sign_in_button).setOnClickListener(this);

        ZyncConfiguration preferences = getZyncApp().getConfig();

        if (!preferences.seenIntro()) {
            startActivity(new Intent(this, IntroActivity.class));
        }

        if (preferences.apiToken() != null) {
            getZyncApp().setApi(new ZyncAPI(getZyncApp().httpClient(), preferences.apiToken()));
            signInSuccess();
        }

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken("958433754089-lu2d68r11jasp08ihgm5mc079avv21af.apps.googleusercontent.com")
                .build();

        googleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();
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
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);

            // Successfully signed in
            if (resultCode == RESULT_OK && result != null && result.isSuccess()) {
                System.out.println("yay signed in for " + result.getSignInAccount().getEmail());
                AuthCredential credential = GoogleAuthProvider.getCredential(
                        result.getSignInAccount().getIdToken(),
                        null
                );

                FirebaseAuth.getInstance().signInWithCredential(credential).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            handleSignIn(task.getResult().getUser());
                        } else {
                            getZyncApp().handleErrorGeneric(SignInActivity.this, new ZyncError(-12,
                                    "Couldn't log into firebase !"), R.string.log_in);
                        }
                    }
                });
            } else {
                String extension = result == null ? "" : "due to error " + result.getStatus().getStatusCode()
                        + ":" + result.getStatus().getStatusMessage();
                getZyncApp().handleErrorGeneric(this, new ZyncError(-11,
                        "Couldn't log into firebase " + extension), R.string.log_in);
                System.out.println("that's sad... now what");
            }
        }
    }

    private void handleSignIn(FirebaseUser user) {
        // Signed in successfully, show authenticated UI.
        final ZyncApplication app = getZyncApp();
        final ProgressDialog dialog = createSignInDialog();

        Crashlytics.setUserEmail(user.getEmail());
        Crashlytics.setUserName(user.getDisplayName());

        user.getToken(true)
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
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        getZyncApp().handleErrorGeneric(SignInActivity.this, new ZyncError(
                                -10, e.getClass().getSimpleName() + ": " + e.getMessage()
                        ), R.string.firebase_token_error);
                    }
                });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sign_in_button:
                if (getZyncApp().httpClient() != null) {
                    Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient);
                    startActivityForResult(signInIntent, RC_SIGN_IN);
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
            app.getConfig().setApiToken(api.getToken());

            dialog.dismiss();

            if ("default".equals(app.getConfig().getEncryptionPass())) {
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
