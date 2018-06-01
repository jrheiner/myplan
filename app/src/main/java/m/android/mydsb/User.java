package m.android.mydsb;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
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

    SwipeRefreshLayout mSwipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);
        ProgressBar progressBar_user = findViewById(R.id.progressBar_user);
        progressBar_user.setVisibility(View.VISIBLE);
        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);
        PreferenceManager.setDefaultValues(this, R.xml.pref_notification, false);
        PreferenceManager.setDefaultValues(this, R.xml.pref_data_sync, false);
        this.setTitle(String.format("%s", getString(R.string.user_header)));
        request_timetableurl(getApi_key());
        mSwipeRefreshLayout = findViewById(R.id.user_swipe_refresh_layout);
        mSwipeRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        request_timetableurl(getApi_key());
                    }
                }
        );
    }

    @Override
    public void onResume() {
        super.onResume();
        request_timetableurl(getApi_key());
    }

    @Override
    protected void onStart() {
        super.onStart();
        JobScheduler mJobScheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (getNotificationSetting() && getLogged_in()) {
            JobInfo.Builder builder = new JobInfo.Builder(1,
                    new ComponentName(getPackageName(),
                            mydsbService.class.getName()));
            int sync_freq = Integer.parseInt(getSyncFreq());
            int service_timing = sync_freq * 60000;
            Log.i("mydsb", String.valueOf(sync_freq));

            if (service_timing > 0) {
                builder.setPeriodic(service_timing);
            } else {
                return;
            }
            builder.setPersisted(true);
            if (mJobScheduler.schedule(builder.build()) == JobScheduler.RESULT_FAILURE) {
                Log.i("mydsb.USER", "JobService failure");
                Toast.makeText(this, "Background Service failed to start!", Toast.LENGTH_SHORT).show();
            } else {
                Log.i("mydsb.USER", "JobService success");
            }
        } else {
            mJobScheduler.cancelAll();
        }

        final WebView webView_user = findViewById(R.id.webView_user);
        mSwipeRefreshLayout = findViewById(R.id.user_swipe_refresh_layout);
        mSwipeRefreshLayout.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                if (webView_user.getScrollY() == 0)
                    mSwipeRefreshLayout.setEnabled(true);
                else
                    mSwipeRefreshLayout.setEnabled(false);

            }
        });
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
                mSwipeRefreshLayout.setRefreshing(true);
                request_timetableurl(getApi_key());
                return true;

            case R.id.action_logout:
                // Logout
                Intent intent_login = new Intent(User.this, Login.class);
                startActivity(intent_login);
                resetLogged_in();
                resetApi_key();
                finish();
                return true;

            default:
                return super.onOptionsItemSelected(item);

        }
    }


    public void request_timetableurl(final String api_key) {
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

    public void setWebCache(String s) {
        SharedPreferences sp = getSharedPreferences("web_cache", MODE_PRIVATE);
        SharedPreferences.Editor ed1 = sp.edit();
        ed1.putString("web_cache", s);
        ed1.apply();
    }

    public void resetLogged_in() {
        SharedPreferences sp3 = getSharedPreferences("logged_in", MODE_PRIVATE);
        SharedPreferences.Editor ed3 = sp3.edit();
        ed3.putBoolean("logged_in", false);
        ed3.apply();
    }

    public boolean getLogged_in() {
        SharedPreferences sp2 = this.getSharedPreferences("logged_in", MODE_PRIVATE);
        return sp2.getBoolean("logged_in", false);
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

    public String getSyncFreq() {
        SharedPreferences sharedPref =
                PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPref.getString("sync_frequency", "180");
    }

    public Boolean getNotificationSetting() {
        SharedPreferences sharedPref =
                PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPref.getBoolean("notifications_new_message", true);
    }

    public void resetApi_key() {
        SharedPreferences sp1 = getSharedPreferences("api_key", MODE_PRIVATE);
        SharedPreferences.Editor ed1 = sp1.edit();
        ed1.putString("api_key", "");
        ed1.apply();
    }

    public String getWebcache() {
        SharedPreferences sp = this.getSharedPreferences("web_cache", MODE_PRIVATE);
        return sp.getString("web_cache", "");
    }

    private class JsoupAsyncTask extends AsyncTask<ArrayList<String>, Void, String> {
        final StringBuilder builder = new StringBuilder();
        String class_settings[] = {"", "5a", "5b", "5c", "5d", "5e",
                "6a", "6b", "6c", "6d", "6e",
                "7a", "7b", "7c", "7d", "7e",
                "8a", "8b", "8c", "8d", "8e",
                "9a", "9b", "9c", "9d", "9e",
                "10", "11"};
        int counter = 0;

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
                        counter = 0; //#6f6f6f
                        Document doc = Jsoup.connect(url).get();
                        Elements td_list = doc.select("tr.list");
                        Elements html_header = doc.getElementsByTag("head");
                        builder.append(html_header.outerHtml());
                        Elements tt_title = doc.select("div.mon_title");
                        builder.append(String.format("<br><h3>%s</h3>", tt_title.text()));
                        //custom webview background color
                        builder.append("<body style=\"background: #fff;\"><table class=\"mon_list\"><tbody>");
                        String last_inline_header = "";
                        String class_setting = getClassSetting();
                        for (Element tt_class : td_list) {
                            String affected_class = tt_class.outerHtml();
                            if (affected_class.contains("inline_header")) {
                                last_inline_header = tt_class.text();
                            }
                            if (last_inline_header.contains(class_settings[Integer.parseInt(class_setting)])) {
                                builder.append(affected_class);
                                counter++;
                            }
                        }
                        builder.append("</tbody></table></body>");
                    } catch (java.io.IOException e) {
                        Log.e("mydsb.User", e.toString());
                        Toast.makeText(User.this, e.toString(), Toast.LENGTH_SHORT).show();
                    }
                    if (counter == 0) {
                        builder.append("<table class=\"mon_list\"><tbody>");
                        builder.append("<tr class=\"list\" style=\"background: #ff975b;\"><td class=\"list\" align=\"center\" style=\"font-weight: 700;\">keine Vertretungen</td></tr>");
                        builder.append("</tbody></table>");
                    }
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            final WebView webView_user = findViewById(R.id.webView_user);
            String timetable = builder.toString();
            timetable = timetable.replaceAll("<th class=\"list\" align=\"center\">Stunde</th>", "");
            timetable = timetable.replaceAll("<th class=\"list\" align=\"center\" width=\"9\">\\(Lehrer\\)</th>", "");
            timetable = timetable.replaceAll("<th class=\"list\" align=\"center\"><b>Fach</b></th>", "");
            timetable = timetable.replaceAll("<th class=\"list\" align=\"center\" width=\"10\"><b>Vertreter</b></th>", "");
            timetable = timetable.replaceAll("<th class=\"list\" align=\"center\" width=\"9\">Raum</th>", "");
            timetable = timetable.replaceAll("<th class=\"list\" align=\"center\">Art</th>", "");
            timetable = timetable.replaceAll("<th class=\"list\" align=\"center\">Vertretungs-Text</th>", "");
            setWebCache(timetable);
            webView_user.loadData(timetable, "text/html; charset=utf-8", "UTF-8");
            ProgressBar progressBar_user = findViewById(R.id.progressBar_user);
            progressBar_user.setVisibility(View.INVISIBLE);
            mSwipeRefreshLayout.setRefreshing(false);
            Toast.makeText(User.this, String.format("%s!", getString(R.string.user_refresh_success)), Toast.LENGTH_SHORT).show();

        }

    }
}
