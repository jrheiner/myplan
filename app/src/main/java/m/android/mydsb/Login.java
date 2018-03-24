package m.android.mydsb;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class Login extends AppCompatActivity {

    private EditText username;
    private EditText password;
    private Button button_login;
    int attempt_counter = 5;
    private String message;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
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
                        if (username.getText().toString().equals("123") && password.getText().toString().equals("test")) {
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
                }
        );
    }
}
