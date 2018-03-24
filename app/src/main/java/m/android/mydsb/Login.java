package m.android.mydsb;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

public class Login extends AppCompatActivity {

    private EditText username;
    private EditText password;
    private Button button_login;
    int attempt_counter = 5;
    private String message;
    boolean logged_in;
    private String api_key;
    String base_url = "https://iphone.dsbcontrol.de/iPhoneService.svc/DSB/";
    String auth_url = "authid/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        if (logged_in) {
            Intent intent = new Intent("m.android.mydsb.User");
            startActivity(intent);
            return;
        }
        login_auth();
    }

    public void login_auth() {
        username = findViewById(R.id.editText_user);
        password = findViewById(R.id.editText_password);
        button_login = findViewById(R.id.button_login);
        button_login.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (username.getText().length() == 0 || password.getText().length() == 0) {
                            return;
                        }
                        request_api_key();
                    }
                }
        );
    }

    public void request_api_key() {
        RequestQueue mRequestQueue;
        StringRequest mStringRequest;

        mRequestQueue = Volley.newRequestQueue(this);
        mStringRequest = new StringRequest(Request.Method.GET, base_url + auth_url + username.getText() + "/" + password.getText(), new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                api_key = response.replaceAll("\"", "");
                logged_in = !api_key.equals("00000000-0000-0000-0000-000000000000");
                if (logged_in) {
                    Toast.makeText(Login.this, "Login successful!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent("m.android.mydsb.User");
                    startActivity(intent);
                    password.setText("");
                    attempt_counter = 5;
                } else {
                    attempt_counter--;
                    if (attempt_counter > 1) {
                        message = attempt_counter + " attempts remaining!";
                    } else if (attempt_counter == 1) {
                        message = attempt_counter + " attempt remaining!";
                    } else {
                        button_login.setEnabled(false);
                        message = "Login temporarily disabled!";
                    }
                    Toast.makeText(Login.this, "Authentication failed!\n" + message, Toast.LENGTH_SHORT).show();
                    password.setText("");

                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(Login.this, error.toString(), Toast.LENGTH_LONG).show();
            }
        });
        mRequestQueue.add(mStringRequest);
    }
}
