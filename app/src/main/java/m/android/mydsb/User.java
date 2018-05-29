package m.android.mydsb;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class User extends AppCompatActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.user_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                // open Settings
                Toast.makeText(User.this, "settings selected", Toast.LENGTH_SHORT).show();
                return true;

            case R.id.action_refresh:
                // Refresh
                Toast.makeText(User.this, "refresh selected", Toast.LENGTH_SHORT).show();
                return true;

            case R.id.action_logout:
                // Logout
                setLogged_in(false);
                Intent intent = new Intent(User.this, Login.class);
                startActivity(intent);
                return true;

            default:
                return super.onOptionsItemSelected(item);

        }
    }

    public void setLogged_in(boolean logged_in) {
        SharedPreferences sp3 = getSharedPreferences("logged_in", MODE_PRIVATE);
        SharedPreferences.Editor ed3 = sp3.edit();
        ed3.putBoolean("logged_in", logged_in);
        ed3.apply();
    }
}
