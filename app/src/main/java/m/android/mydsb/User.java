package m.android.mydsb;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;

import org.json.JSONArray;

public class User extends AppCompatActivity {

    private ProgressBar progressBar_user;


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
                request_timetableurl(getApi_key());
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

    public void request_timetableurl(String api_key) {
        progressBar_user = findViewById(R.id.progressBar_user);
        progressBar_user.setVisibility(View.VISIBLE);
        String url = "https://iphone.dsbcontrol.de/iPhoneService.svc/DSB/timetables/" + api_key;

        JsonArrayRequest jsonObjectRequest = new JsonArrayRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONArray>() {

                    @Override
                    public void onResponse(JSONArray response) {
                        Toast.makeText(User.this, "Response: " + response.toString(), Toast.LENGTH_SHORT).show();
                        progressBar_user.setVisibility(View.INVISIBLE);
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO: Handle error

                    }
                });

        SingletonRequestQueue.getInstance(this).addToRequestQueue(jsonObjectRequest);
    }

    public void setLogged_in(boolean logged_in) {
        SharedPreferences sp3 = getSharedPreferences("logged_in", MODE_PRIVATE);
        SharedPreferences.Editor ed3 = sp3.edit();
        ed3.putBoolean("logged_in", logged_in);
        ed3.apply();
    }

    public String getApi_key() {
        SharedPreferences sp2 = this.getSharedPreferences("api_key", MODE_PRIVATE);
        return sp2.getString("api_key", null);
    }
}
