package co.zync.zync.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;

import co.zync.zync.R;
import co.zync.zync.ZyncApplication;

public class ZyncPassDialog {
    private Context context;
    private ZyncApplication app;
    private Callback callback;
    private AlertDialog passwordDialog;

    public ZyncPassDialog(Context context, ZyncApplication app, Callback callback) {
        this.context = context;
        this.app = app;
        this.callback = callback;
    }

    public void promptForPassword() {
        final AlertDialog.Builder passwordDialogBuilder = new AlertDialog.Builder(context);
        final EditText view = new EditText(context);
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
            public void onClick(DialogInterface dialog, int which) {
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

    private boolean handlePassword(EditText view) {
        String enteredPass = view.getText().toString();

        if (enteredPass.length() < 10) {
            view.setError(app.getString(R.string.password_insufficient));
            return false;
        }

        // todo test password entropy (consecutive characters, etc.)

        app.getPreferences().edit()
                .putString("encryption_pass", enteredPass)
                .apply();
        callback.callback();
        return true;
    }

    public interface Callback {
        void callback();
    }
}
