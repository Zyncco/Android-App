package co.zync.zync.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;

import java.nio.charset.Charset;

import co.zync.zync.R;
import co.zync.zync.ZyncApplication;
import co.zync.zync.ZyncPostClipTask;
import co.zync.zync.ZyncPostImageTask;
import co.zync.zync.activities.intro.PasswordActivity;
import co.zync.zync.api.ZyncAPI;
import co.zync.zync.api.ZyncClipType;
import co.zync.zync.api.ZyncError;

/*
 * Activity presented to the user when they are signing in (with google)
 */
public class SignInActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener, View.OnClickListener {
    private static final int RC_SIGN_IN = 64209;
    private GoogleApiClient googleApiClient;

    // Progress of signing into google.
    private ProgressDialog connectionProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signin);

        // make sure google play services is available
        GoogleApiAvailability.getInstance().makeGooglePlayServicesAvailable(this);

        SignInButton signInButton = (SignInButton) findViewById(R.id.sign_in_button);
        signInButton.setSize(SignInButton.SIZE_WIDE);

        findViewById(R.id.sign_in_button).setOnClickListener(this);

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

            return;
        }

        /*                            SHARE END                           */


        SharedPreferences preferences = getZyncApp().getPreferences();

        /*
         * If the user has logged in before, set the API variable and continue to settings.
         */
        if (preferences.contains("zync_api_token")) {
            getZyncApp().setApi(ZyncAPI.login(
                    getZyncApp().httpRequestQueue(),
                    preferences.getString("zync_api_token", "")
            ));
            getZyncApp().openSettings(this);
            return;
        }


        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken("837094062175-1jgrmfclvp8pc88gaa79u6qejve321k6.apps.googleusercontent.com")
                .build();

        googleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        connectionProgressDialog = new ProgressDialog(this);
        // TODO: I have no idea how to get the actual string?
        connectionProgressDialog.setMessage(getString(R.string.signing_in));
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
            handleSignInResult(result);
        }
    }

    private void handleSignInResult(GoogleSignInResult result) {
        if (result.isSuccess()) {
            // Signed in successfully, show authenticated UI.
            GoogleSignInAccount acct = result.getSignInAccount();
            getZyncApp().setAccount(acct);
            System.out.println(acct.getDisplayName() + " is logged in!");

            ZyncAPI.signup(
                    getZyncApp().httpRequestQueue(),
                    acct.getIdToken(),
                    new ZyncAPI.ZyncCallback<ZyncAPI>() {
                        @Override
                        public void success(ZyncAPI api) {
                            getZyncApp().setApi(api);
                            getZyncApp().getPreferences().edit().putString("zync_api_token", api.getToken()).apply();


                            if (!getZyncApp().getPreferences().contains("encryption_enabled")) {
                                startActivity(new Intent(SignInActivity.this, PasswordActivity.class));
                            } else {
                                getZyncApp().openSettings(SignInActivity.this);
                            }
                        }

                        @Override
                        public void handleError(ZyncError error) {
                            System.out.println(error.toString());
                            // TODO do something
                        }
                    }
            );
        } else {
            // they didn't sign in :c
            System.out.println("no sign in :c");
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sign_in_button:
                Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient);
                startActivityForResult(signInIntent, RC_SIGN_IN);
                break;
        }
    }

    private ZyncApplication getZyncApp() {
        return (ZyncApplication) getApplication();
    }
}
