package m.android.mydsb;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

public class Login extends AppCompatActivity {

    private EditText username;
    private EditText password;
    private Button button_login;
    private int attempt_counter = 5;
    private String message;
    private boolean logged_in;
    private String api_key;
    private ProgressBar progressBar_login;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        this.setTitle("");
        if (getNotLogged_in()) {
            login_auth();
        } else {
            Intent intent = new Intent(Login.this, User.class);
            startActivity(intent);
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        if (getNotLogged_in()) {
            button_login.setEnabled(true);
            login_auth();
        } else {
            Intent intent = new Intent(Login.this, User.class);
            startActivity(intent);
        }
    }

    private void login_auth() {
        username = findViewById(R.id.editText_user);
        password = findViewById(R.id.editText_password);
        button_login = findViewById(R.id.button_login);
        progressBar_login = findViewById(R.id.progressBar_login);
        button_login.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        button_login.setEnabled(false);
                        if (username.getText().length() == 0 || password.getText().length() == 0) {
                            button_login.setEnabled(true);
                            return;
                        }
                        progressBar_login.setVisibility(View.VISIBLE);
                        request_api_key();

                    }
                }
        );
    }

    private void request_api_key() {
        StringRequest mStringRequest;
        progressBar_login = findViewById(R.id.progressBar_login);
        String base_url = "https://iphone.dsbcontrol.de/iPhoneService.svc/DSB/";
        String auth_url = "authid/";
        mStringRequest = new StringRequest(Request.Method.GET, base_url + auth_url + username.getText() + "/" + password.getText(), new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                api_key = response.replaceAll("\"", "");
                logged_in = !api_key.equals("00000000-0000-0000-0000-000000000000");
                if (logged_in) {
                    Toast.makeText(Login.this, String.format("%s!", getString(R.string.login_login_success)), Toast.LENGTH_SHORT).show();
                    setLogged_in();
                    setApi_key(api_key);
                    Intent intent = new Intent("m.android.mydsb.User");
                    startActivity(intent);
                    finish();
                    progressBar_login.setVisibility(View.INVISIBLE);
                    password.setText("");
                    attempt_counter = 5;
                } else {
                    attempt_counter--;
                    if (attempt_counter > 1) {
                        message = String.format("%s %s!", attempt_counter, getString(R.string.login_attempts));
                        button_login.setEnabled(true);
                    } else if (attempt_counter == 1) {
                        message = String.format("%s %s!", attempt_counter, getString(R.string.login_attempt));
                        button_login.setEnabled(true);
                    } else {
                        button_login.setEnabled(false);
                        message = getString(R.string.login_login_disabled);
                    }
                    progressBar_login.setVisibility(View.INVISIBLE);
                    Toast.makeText(Login.this, String.format("%s!\n%s", getString(R.string.login_login_failed), message), Toast.LENGTH_SHORT).show();
                    password.setText("");

                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(Login.this, error.toString(), Toast.LENGTH_LONG).show();
                progressBar_login.setVisibility(View.INVISIBLE);
                button_login.setEnabled(true);
            }
        });
        SingletonRequestQueue.getInstance(this).addToRequestQueue(mStringRequest);
    }

    private void setApi_key(String api_key) {
        SharedPreferences sp = getSharedPreferences("api_key", MODE_PRIVATE);
        SharedPreferences.Editor ed = sp.edit();
        ed.putString("api_key", api_key);
        ed.apply();
    }

    private void setLogged_in() {
        SharedPreferences sp = getSharedPreferences("logged_in", MODE_PRIVATE);
        SharedPreferences.Editor ed = sp.edit();
        ed.putBoolean("logged_in", true);
        ed.apply();
    }

    private boolean getNotLogged_in() {
        SharedPreferences sp = this.getSharedPreferences("logged_in", MODE_PRIVATE);
        return !sp.getBoolean("logged_in", false);
    }
}
