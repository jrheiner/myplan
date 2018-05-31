package m.android.mydsb;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
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
                // open Settings
                Toast.makeText(User.this, "settings selected", Toast.LENGTH_SHORT).show();
                return true;

            case R.id.action_refresh:
                // Refresh
                // Toast.makeText(User.this, "refresh selected", Toast.LENGTH_SHORT).show();
                request_timetableurl(getApi_key());

                return true;

            case R.id.action_logout:
                // Logout
                setLogged_in(false);
                Intent intent = new Intent(User.this, Login.class);
                startActivity(intent);
                setApi_key("");
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
                        //Toast.makeText(User.this, "Response: " + response.toString(), Toast.LENGTH_SHORT).show();

                        try {

                            for (int i = 0; i < response.length(); i++) {
                                JSONObject json_node = (JSONObject) response.get(i);

                                String timetableurl = json_node.getString("timetableurl");
                                Log.i("mydsb.User", timetableurl);
                                timetableurls.add(timetableurl);
                            }
                            new JsoupAsyncTask().execute(timetableurls);
                            /*
                            //Only show newest page for now

                            JSONObject json_node = (JSONObject) response.get(0);

                            String timetableurl = json_node.getString("timetableurl");
                            //webView_user.loadUrl(timetableurl);
                            parse_html(timetableurl);
                            */

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
/*
    private void parse_html(final String timetableurl) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                //final StringBuilder builder = new StringBuilder();
                //final WebView webView_user = findViewById(R.id.webView_user);


                try {
                    Document doc = Jsoup.connect(timetableurl).get();
                    Elements td_list = doc.select("tr.list");
                    Elements html_header = doc.getElementsByTag("head");
                    //Log.i("mydsb.User", html_header.outerHtml());
                    builder.append(html_header.outerHtml());
                    Elements tt_title = doc.select("div.mon_title");
                    builder.append(String.format("<b>%s</b>", tt_title.text()));
                    builder.append("<table class=\"mon_list\"><tbody>");
                    String last_inline_header = "";
                    for (Element tt_class : td_list) {
                        String affected_class = tt_class.outerHtml();
                        //Log.i("mydsb.User", affected_class);
                        if  (affected_class.contains("inline_header")) {
                            last_inline_header =  tt_class.text();
                        }
                        if (last_inline_header.contains("11")) {
                            builder.append(affected_class);
                        }
                    }
                    builder.append("</tbody></table>");
                } catch (java.io.IOException e) {
                    Log.e("mydsb.User", e.toString());
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //set webview here
                        Log.i("mydsb.User", builder.toString());
                        //webView_user.loadData(builder.toString(), "text/html; charset=utf-8", "UTF-8");
                    }
                });
            }
        }).start();
    }
*/

    private class JsoupAsyncTask extends AsyncTask<ArrayList<String>, Void, String> {
        final StringBuilder builder = new StringBuilder();

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
                        for (Element tt_class : td_list) {
                            String affected_class = tt_class.outerHtml();
                            if (affected_class.contains("inline_header")) {
                                last_inline_header = tt_class.text();
                            }
                            if (last_inline_header.contains("11") || last_inline_header.contains("7c")) {
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
            //Log.i("load_timetable", builder.toString());
            Log.i("load_timetable", "LOAD TIMETABLE HERE");
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

    public void setApi_key(String api_key) {
        SharedPreferences sp1 = getSharedPreferences("api_key", MODE_PRIVATE);
        SharedPreferences.Editor ed1 = sp1.edit();
        ed1.putString("api_key", api_key);
        ed1.apply();
    }
}
