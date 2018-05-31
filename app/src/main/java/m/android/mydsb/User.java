package m.android.mydsb;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;

public class User extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);
        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);
        PreferenceManager.setDefaultValues(this, R.xml.pref_notification, false);
        PreferenceManager.setDefaultValues(this, R.xml.pref_data_sync, false);
        this.setTitle(String.format("%s", getString(R.string.user_header)));
        request_timetableurl(getApi_key());
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
                Intent intent_settings = new Intent(this, Settings.class);
                startActivity(intent_settings);
                return true;

            case R.id.action_refresh:
                // Refresh
                request_timetableurl(getApi_key());
                return true;

            case R.id.action_logout:
                // Logout
                setLogged_in(false);
                Intent intent_login = new Intent(User.this, Login.class);
                startActivity(intent_login);
                resetApi_key();
                finish();
                return true;

            default:
                return super.onOptionsItemSelected(item);

        }
    }

    public void request_timetableurl(final String api_key) {
        ProgressBar progressBar_user = findViewById(R.id.progressBar_user);
        progressBar_user.setVisibility(View.VISIBLE);
        final ArrayList<String> timetableurls = new ArrayList<String>();
        String url = "https://iphone.dsbcontrol.de/iPhoneService.svc/DSB/timetables/" + api_key;

        JsonArrayRequest jsonObjectRequest = new JsonArrayRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONArray>() {

                    @Override
                    public void onResponse(JSONArray response) {
                        try {

                            for (int i = 0; i < response.length(); i++) {
                                JSONObject json_node = (JSONObject) response.get(i);

                                String timetableurl = json_node.getString("timetableurl");
                                Log.i("mydsb.User", timetableurl);
                                timetableurls.add(timetableurl);
                            }
                            new JsoupAsyncTask().execute(timetableurls);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO: Handle error

                    }
                });

        SingletonRequestQueue.getInstance(this).addToRequestQueue(jsonObjectRequest);
    }

    private class JsoupAsyncTask extends AsyncTask<ArrayList<String>, Void, String> {
        final StringBuilder builder = new StringBuilder();
        String class_settings[] = {"", "5a", "5b", "5c", "5d", "6a", "6b", "6c", "6d", "7a", "7b", "7c", "7d",
                "8a", "8b", "8c", "8d", "9a", "9b", "9c", "9d", "10", "11"};

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        @SafeVarargs
        protected final String doInBackground(ArrayList<String>... params) {
            for (ArrayList<String> url_list : params) {
                for (String url : url_list) {
                    try {
                        Document doc = Jsoup.connect(url).get();
                        Elements td_list = doc.select("tr.list");
                        Elements html_header = doc.getElementsByTag("head");
                        builder.append(html_header.outerHtml());
                        Elements tt_title = doc.select("div.mon_title");
                        builder.append(String.format("<br><b>%s</b>", tt_title.text()));
                        builder.append("<table class=\"mon_list\"><tbody>");
                        String last_inline_header = "";
                        String class_setting = getClassSetting();
                        for (Element tt_class : td_list) {
                            String affected_class = tt_class.outerHtml();
                            if (affected_class.contains("inline_header")) {
                                last_inline_header = tt_class.text();
                            }
                            if (last_inline_header.contains(class_settings[Integer.parseInt(class_setting)])) {
                                builder.append(affected_class);
                            }
                        }
                        builder.append("</tbody></table>");
                    } catch (java.io.IOException e) {
                        Log.e("mydsb.User", e.toString());
                    }
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            final WebView webView_user = findViewById(R.id.webView_user);
            Log.i("load_timetable", builder.toString());
            webView_user.loadData(builder.toString(), "text/html; charset=utf-8", "UTF-8");
            ProgressBar progressBar_user = findViewById(R.id.progressBar_user);
            progressBar_user.setVisibility(View.INVISIBLE);
            Toast.makeText(User.this, "Aktualisierung erfolgreich!", Toast.LENGTH_SHORT).show();

        }

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

    public String getClassSetting() {
        SharedPreferences sharedPref =
                PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPref.getString("general_list", "0");
    }

    public void resetApi_key() {
        SharedPreferences sp1 = getSharedPreferences("api_key", MODE_PRIVATE);
        SharedPreferences.Editor ed1 = sp1.edit();
        ed1.putString("api_key", "");
        ed1.apply();
    }
}
