package de.myplan.android.ui;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.google.android.material.snackbar.Snackbar;

import de.myplan.android.R;
import de.myplan.android.util.Preferences;
import de.myplan.android.util.SingletonRequestQueue;

public class LoginActivity extends AppCompatActivity {

    private Preferences preferences;
    private EditText username;
    private EditText password;
    private Button button_login;
    private boolean logged_in;
    private String api_key;
    private ProgressBar progressBar_login;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        preferences = new Preferences(this);
        if (savedInstanceState == null) {
            AppCompatDelegate.setDefaultNightMode(preferences.getTheme());
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        this.setTitle("");
        if (preferences.getApiKey() == null) {
            login_auth();
        } else {
            Intent intent = new Intent(LoginActivity.this, UserActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        if (preferences.getApiKey() == null) {
            if (button_login != null) {
                button_login.setEnabled(true);
            }
            login_auth();
        } else {
            Intent intent = new Intent(LoginActivity.this, UserActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            finish();
            startActivity(intent);
        }
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    public void hintOnClick(View v) {
        AlertDialog.Builder mBuilder = new AlertDialog.Builder(LoginActivity.this);
        mBuilder.setTitle(getString(R.string.login_user_hint))
                .setMessage(getString(R.string.login_user_hint_popup))
                .setPositiveButton("OK", (dialog, id) -> dialog.dismiss());
        AlertDialog mDialog = mBuilder.create();
        mDialog.show();
    }

    private void login_auth() {
        username = findViewById(R.id.editText_user);
        password = findViewById(R.id.editText_password);
        button_login = findViewById(R.id.button_login);
        progressBar_login = findViewById(R.id.progressBar_login);
        button_login.setOnClickListener(
                view -> {
                    button_login.setEnabled(false);
                    if (username.getText().length() == 0 || password.getText().length() == 0) {
                        button_login.setEnabled(true);
                        return;
                    }
                    progressBar_login.setVisibility(View.VISIBLE);
                    request_api_key();

                }
        );
    }

    private void request_api_key() {
        StringRequest mStringRequest;
        progressBar_login = findViewById(R.id.progressBar_login);
        String base_url = "https://iphone.dsbcontrol.de/iPhoneService.svc/DSB/";
        String auth_url = "authid/";
        mStringRequest = new StringRequest(Request.Method.GET, base_url + auth_url + username.getText() + "/" + password.getText(), response -> {
            api_key = response.substring(1, 37); // remove parentheses
            logged_in = !api_key.equals("00000000-0000-0000-0000-000000000000");
            if (logged_in) {
                Toast.makeText(LoginActivity.this, String.format("%s!", getString(R.string.login_login_success)), Toast.LENGTH_SHORT).show();
                preferences.setApiKey(api_key);
                Intent intent = new Intent(LoginActivity.this, UserActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
                progressBar_login.setVisibility(View.INVISIBLE);
                password.setText("");
            } else {
                progressBar_login.setVisibility(View.INVISIBLE);
                final Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), getString(R.string.login_login_failed), Snackbar.LENGTH_INDEFINITE);
                View snackView = snackbar.getView();
                int snackbarTextId = com.google.android.material.R.id.snackbar_text;
                TextView textView = snackView.findViewById(snackbarTextId);
                textView.setTextColor(Color.RED);
                textView.setTypeface(textView.getTypeface(), Typeface.BOLD);
                snackbar.setActionTextColor(Color.RED);
                snackbar.setAction("OK", v -> snackbar.dismiss());
                snackbar.show();
                password.setText("");
            }
        }, error -> {
            final Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), error.toString(), Snackbar.LENGTH_INDEFINITE);
            View snackView = snackbar.getView();
            int snackbarTextId = com.google.android.material.R.id.snackbar_text;
            TextView textView = snackView.findViewById(snackbarTextId);
            textView.setTextColor(Color.RED);
            textView.setTypeface(textView.getTypeface(), Typeface.BOLD);
            snackbar.setActionTextColor(Color.RED);
            snackbar.setAction("OK", v -> snackbar.dismiss());
            snackbar.show();
            progressBar_login.setVisibility(View.INVISIBLE);
            button_login.setEnabled(true);
        });
        SingletonRequestQueue.getInstance(this).addToRequestQueue(mStringRequest);
    }

}
