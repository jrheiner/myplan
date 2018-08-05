package de.myplan.android;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.TextView;
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

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class User extends AppCompatActivity {

    private SwipeRefreshLayout mSwipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        switch (getThemeSettings()) {
            case "-1":
                if (savedInstanceState == null) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                }
                break;
            case "0":
                if (savedInstanceState == null) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO);
                }
                break;
            case "1":
                if (savedInstanceState == null) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                }
                break;
            case "2":
                if (savedInstanceState == null) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                }
                break;
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);
        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);
        PreferenceManager.setDefaultValues(this, R.xml.pref_notification, false);
        PreferenceManager.setDefaultValues(this, R.xml.pref_data_sync, false);

        final String[] listItems = getResources().getStringArray(R.array.pref_general_list_titles);
        final String[] listValues = getResources().getStringArray(R.array.pref_general_list_values);
        boolean firstStart = getFirstStart();
        if (firstStart) {
            AlertDialog.Builder mBuilder = new AlertDialog.Builder(User.this);
            mBuilder.setTitle(getString(R.string.user_introduction_title));
            mBuilder.setSingleChoiceItems(listItems, -1, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    setClassSetting(listValues[i]);
                    dialogInterface.dismiss();
                    setFirstStart();
                    recreate();
                }
            });

            AlertDialog mDialog = mBuilder.create();
            mDialog.show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!getThemeSettings().equals(String.valueOf(AppCompatDelegate.getDefaultNightMode()))) {
            switch (getThemeSettings()) {
                case "-1":
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                    this.recreate();
                    break;
                case "0":
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO);
                    this.recreate();
                    break;
                case "1":
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    this.recreate();
                    break;
                case "2":
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    this.recreate();
                    break;
            }
        }
        request_timetableurl(getApiKey());
    }


    @Override
    protected void onStart() {
        super.onStart();
        JobScheduler mJobScheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (getNotificationSetting() && getLoggedIn()) {
            JobInfo.Builder builder = new JobInfo.Builder(1,
                    new ComponentName(getPackageName(),
                            myplanService.class.getName()));
            int sync_freq = Integer.parseInt(getSyncFreq());
            int service_timing = sync_freq * 60000;
            if (service_timing > 0) {
                builder.setPeriodic(service_timing);
            } else {
                return;
            }
            builder.setPersisted(true);

            if (((mJobScheduler != null) ? mJobScheduler.schedule(builder.build()) : 0) == JobScheduler.RESULT_FAILURE) {
                Toast.makeText(this, "Background Service failed to start!", Toast.LENGTH_SHORT).show();
            }
        } else if (!getNotificationSetting() && !getLoggedIn()) {
            assert mJobScheduler != null;
            mJobScheduler.cancelAll();
        }

        ProgressBar progressBar_user = findViewById(R.id.progressBar_user);
        progressBar_user.setVisibility(View.VISIBLE);
        this.setTitle(String.format("%s", getString(R.string.user_header)));
        mSwipeRefreshLayout = findViewById(R.id.user_swipe_refresh_layout);
        mSwipeRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        request_timetableurl(getApiKey());
                    }
                }
        );


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
    public void onBackPressed() {
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
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
                if (mSwipeRefreshLayout != null) mSwipeRefreshLayout.setRefreshing(true);
                request_timetableurl(getApiKey());
                return true;

            case R.id.action_timetable:
                Intent intent_timetable = new Intent(this, UserTimetable.class);
                startActivity(intent_timetable);
                return true;

            case R.id.action_logout:
                Intent intent_login = new Intent(User.this, Login.class);
                intent_login.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent_login);
                resetLoggedIn();
                resetApiKey();
                finish();
                return true;

            default:
                return super.onOptionsItemSelected(item);

        }
    }

    private void request_timetableurl(final String api_key) {
        final ArrayList<String> timetableurls = new ArrayList<>();
        String url = "https://iphone.dsbcontrol.de/iPhoneService.svc/DSB/timetables/" + api_key;

        JsonArrayRequest jsonObjectRequest = new JsonArrayRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONArray>() {

                    @Override
                    public void onResponse(JSONArray response) {
                        try {
                            for (int i = 0; i < response.length(); i++) {
                                JSONObject json_node = (JSONObject) response.get(i);

                                String timetableurl = json_node.getString("timetableurl");
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
                        Toast.makeText(User.this, String.format("%s!", getString(R.string.user_refresh_failed)), Toast.LENGTH_SHORT).show();
                        if (mSwipeRefreshLayout != null) mSwipeRefreshLayout.setRefreshing(false);
                        ProgressBar progressBar_user = findViewById(R.id.progressBar_user);
                        progressBar_user.setVisibility(View.INVISIBLE);
                        String cached_timetable = getWebCacheComplete();
                        final WebView webView_user = findViewById(R.id.webView_user);
                        TextView user_textView_status = findViewById(R.id.user_textView_status);
                        webView_user.loadData(cached_timetable, "text/html; charset=utf-8", "UTF-8");
                        user_textView_status.setText(String.format("%s.\n%s.", getString(R.string.network_not_available), getString(R.string.timetable_not_up_to_date)));
                        user_textView_status.setVisibility(View.VISIBLE);


                    }
                });

        SingletonRequestQueue.getInstance(this).addToRequestQueue(jsonObjectRequest);
    }

    private boolean getFirstStart() {
        SharedPreferences sp = this.getSharedPreferences("first_start", MODE_PRIVATE);
        return sp.getBoolean("first_start", true);
    }

    private void setFirstStart() {
        SharedPreferences sp = getSharedPreferences("first_start", MODE_PRIVATE);
        SharedPreferences.Editor ed = sp.edit();
        ed.putBoolean("first_start", false);
        ed.apply();
    }

    private void setWebCache(String s) {
        SharedPreferences sp = getSharedPreferences("web_cache", MODE_PRIVATE);
        SharedPreferences.Editor ed = sp.edit();
        ed.putString("web_cache", s);
        ed.apply();
    }

    private String getWebCacheComplete() {
        SharedPreferences sp = this.getSharedPreferences("web_cache_complete", MODE_PRIVATE);
        return sp.getString("web_cache_complete", "");
    }

    private void setWebCacheComplete(String s) {
        SharedPreferences sp = getSharedPreferences("web_cache_complete", MODE_PRIVATE);
        SharedPreferences.Editor ed = sp.edit();
        ed.putString("web_cache_complete", s);
        ed.apply();
    }

    private void resetLoggedIn() {
        SharedPreferences sp = getSharedPreferences("logged_in", MODE_PRIVATE);
        SharedPreferences.Editor ed = sp.edit();
        ed.putBoolean("logged_in", false);
        ed.apply();
    }

    private boolean getLoggedIn() {
        SharedPreferences sp = this.getSharedPreferences("logged_in", MODE_PRIVATE);
        return sp.getBoolean("logged_in", false);
    }

    private String getApiKey() {
        SharedPreferences sp = this.getSharedPreferences("api_key", MODE_PRIVATE);
        return sp.getString("api_key", null);
    }

    private String getClassSetting() {
        SharedPreferences sharedPref =
                PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPref.getString("general_list", "0");
    }

    private void setClassSetting(String s) {
        SharedPreferences.Editor ed = PreferenceManager.getDefaultSharedPreferences(this).edit();
        ed.putString("general_list", s);
        ed.apply();
    }

    private String getSyncFreq() {
        SharedPreferences sharedPref =
                PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPref.getString("sync_frequency", "180");
    }

    private Boolean getNotificationSetting() {
        SharedPreferences sharedPref =
                PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPref.getBoolean("notifications_new_message", true);
    }

    private Boolean getTimetableSetting() {
        SharedPreferences sharedPref =
                PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPref.getBoolean("general_timetable_pref", false);
    }

    private String getThemeSettings() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPref.getString("general_theme", "0");
    }

    private void resetApiKey() {
        SharedPreferences sp = getSharedPreferences("api_key", MODE_PRIVATE);
        SharedPreferences.Editor ed = sp.edit();
        ed.putString("api_key", "");
        ed.apply();
    }

    private String getTimetable() {
        SharedPreferences sp = getSharedPreferences("timetable", MODE_PRIVATE);
        return sp.getString("timetable", "{\"day1\":{\"1\":\"0; \",\"2\":\"0; \",\"3\":\"0; \",\"4\":\"0; \",\"5\":\"0; \",\"6\":\"0; \",\"7\":\"0; \",\"8\":\"0; \",\"9\":\"0; \",\"10\":\"0; \",\"11\":\"0; \",\"12\":\"0; \",\"13\":\"0; \"},\"day2\":{\"1\":\"0; \",\"2\":\"0; \",\"3\":\"0; \",\"4\":\"0; \",\"5\":\"0; \",\"6\":\"0; \",\"7\":\"0; \",\"8\":\"0; \",\"9\":\"0; \",\"10\":\"0; \",\"11\":\"0; \",\"12\":\"0; \",\"13\":\"0; \"},\"day3\":{\"1\":\"0; \",\"2\":\"0; \",\"3\":\"0; \",\"4\":\"0; \",\"5\":\"0; \",\"6\":\"0; \",\"7\":\"0; \",\"8\":\"0; \",\"9\":\"0; \",\"10\":\"0; \",\"11\":\"0; \",\"12\":\"0; \",\"13\":\"0; \"},\"day4\":{\"1\":\"0; \",\"2\":\"0; \",\"3\":\"0; \",\"4\":\"0; \",\"5\":\"0; \",\"6\":\"0; \",\"7\":\"0; \",\"8\":\"0; \",\"9\":\"0; \",\"10\":\"0; \",\"11\":\"0; \",\"12\":\"0; \",\"13\":\"0; \"},\"day5\":{\"1\":\"0; \",\"2\":\"0; \",\"3\":\"0; \",\"4\":\"0; \",\"5\":\"0; \",\"6\":\"0; \",\"7\":\"0; \",\"8\":\"0; \",\"9\":\"0; \",\"10\":\"0; \",\"11\":\"0; \",\"12\":\"0; \",\"13\":\"0; \"}}");
    }

    private boolean ttFilter(JSONObject day, String stunde, String lehrer) throws JSONException {
        for (int i = 1; i < 14; i++) {
            if (stunde.contains(String.valueOf(i))) {
                if (day.getString(String.valueOf(i)).split(";")[1].replaceAll(" ", "").toLowerCase().contains(lehrer.toLowerCase())) {
                    return true;
                }
            }
        }
        return false;
    }

    private class JsoupAsyncTask extends AsyncTask<ArrayList<String>, Void, String> {
        final StringBuilder builder = new StringBuilder();
        final JSONObject jwebcache = new JSONObject();

        final String[] class_settings = {"", "5a", "5b", "5c", "5d", "5e",
                "6a", "6b", "6c", "6d", "6e",
                "7a", "7b", "7c", "7d", "7e",
                "8a", "8b", "8c", "8d", "8e",
                "9a", "9b", "9c", "9d", "9e",
                "10", "11"};
        int counter;
        String last_date_title = "";

        @Override
        @SafeVarargs
        protected final String doInBackground(ArrayList<String>... params) {
            for (ArrayList<String> url_list : params) {
                for (String url : url_list) {
                    try {
                        StringBuilder jcache = new StringBuilder();
                        counter = 0;
                        Document doc = Jsoup.connect(url).get();
                        Elements td_list = doc.select("tr.list");
                        Elements html_header = doc.getElementsByTag("head");
                        builder.append(html_header.outerHtml());
                        String tt_title = doc.select("div.mon_title").text();
                        if (tt_title.contains("Seite")) {
                            counter++;
                            if (!last_date_title.equals(tt_title.replaceAll("\\(([A-Z])\\w+ ([0-9]) / ([0-9])\\)", ""))) {
                                builder.append(String.format("<br><h3>%s</h3>", tt_title.replaceAll("\\(([A-Z])\\w+ ([0-9]) / ([0-9])\\)", "")));
                            }
                            last_date_title = tt_title.replaceAll("\\(([A-Z])\\w+ ([0-9]) / ([0-9])\\)", "");
                        } else {
                            builder.append(String.format("<br><h3>%s</h3>", tt_title));
                        }
                        Elements td_info = doc.select("tr.info");
                        for (Element tt_info : td_info) {
                            String info_text = tt_info.text();
                            if (info_text.contains("Unterrichtsfrei")) {
                                builder.append("<table class=\"mon_list\" >\n");
                                builder.append(tt_info.outerHtml().replaceAll("<tr class=\"info\">", "<tr align=\"center\" style=\"background: #5cb85c; font-weight: 700;\">"));
                                builder.append("</table><br>");
                            }
                        }
                        builder.append("<body style=\"background: #fff;\"><table class=\"mon_list\"><tbody>");
                        String last_inline_header = "";
                        String class_setting = getClassSetting();
                        String date_str = tt_title.replaceAll("\\(([A-Z])\\w+ ([0-9]) / ([0-9])\\)", "").replaceAll("[^0-9.]", "");
                        Date date_obj = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).parse(date_str);
                        String date = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(date_obj);
                        for (Element tt_class : td_list) {
                            String affected_class = tt_class.outerHtml();
                            if (affected_class.contains("inline_header")) {
                                last_inline_header = tt_class.text();
                            }
                            if (last_inline_header.contains(class_settings[Integer.parseInt(class_setting)])) {
                                if (getTimetableSetting()) {
                                    Pattern p = Pattern.compile(">(\\d.+|\\d.?-.?\\d+)</td>\\n.+\">(.+)</td>\\n.+\">(?:<b>)?(.+?)(?:</b>)?</td>\\n.+\">(?:<b>)?(.+?)(?:</b>)?</td>\\n.+\">(.+)</td>\\n.+\">(.+)</td>\\n.+\">(.+)</td>");
                                    Matcher m = p.matcher(affected_class);
                                    while (m.find()) {

                                        String stunde = m.group(1);
                                        String lehrer = m.group(2);
                                        /*
                                        String fach = m.group(3);
                                        String vertreter = m.group(4);
                                        String raum = m.group(5);
                                        String art = m.group(6);
                                        String notiz = m.group(7);
                                        */

                                        Calendar c = Calendar.getInstance();
                                        c.setTime(date_obj);
                                        int dayOfWeek = c.get(Calendar.DAY_OF_WEEK);

                                        JSONObject timetable = new JSONObject(getTimetable());

                                        switch (dayOfWeek) {
                                            case Calendar.MONDAY:
                                                JSONObject day1 = timetable.getJSONObject("day1");
                                                if (ttFilter(day1, stunde, lehrer)) {
                                                    builder.append(affected_class);
                                                    jcache.append(affected_class);
                                                    counter++;
                                                }
                                                break;

                                            case Calendar.TUESDAY:
                                                JSONObject day2 = timetable.getJSONObject("day2");
                                                if (ttFilter(day2, stunde, lehrer)) {
                                                    builder.append(affected_class);
                                                    jcache.append(affected_class);
                                                    counter++;
                                                }
                                                break;

                                            case Calendar.WEDNESDAY:
                                                JSONObject day3 = timetable.getJSONObject("day3");
                                                if (ttFilter(day3, stunde, lehrer)) {
                                                    builder.append(affected_class);
                                                    jcache.append(affected_class);
                                                    counter++;
                                                }
                                                break;

                                            case Calendar.THURSDAY:
                                                JSONObject day4 = timetable.getJSONObject("day4");
                                                if (ttFilter(day4, stunde, lehrer)) {
                                                    builder.append(affected_class);
                                                    jcache.append(affected_class);
                                                    counter++;
                                                }
                                                break;

                                            case Calendar.FRIDAY:
                                                JSONObject day5 = timetable.getJSONObject("day4");
                                                if (ttFilter(day5, stunde, lehrer)) {
                                                    builder.append(affected_class);
                                                    jcache.append(affected_class);
                                                    counter++;
                                                }
                                                break;
                                        }
                                    }
                                } else {
                                    builder.append(affected_class);
                                    jcache.append(affected_class);
                                    counter++;
                                }

                            }
                        }
                        jwebcache.put(date, jcache.toString());
                        builder.append("</tbody></table></body>");
                    } catch (JSONException e) {
                        if (mSwipeRefreshLayout != null) mSwipeRefreshLayout.setRefreshing(false);
                        ProgressBar progressBar_user = findViewById(R.id.progressBar_user);
                        progressBar_user.setVisibility(View.INVISIBLE);
                        Toast.makeText(User.this, e.toString(), Toast.LENGTH_SHORT).show();
                    } catch (ParseException | IOException e) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(User.this, String.format("%s!", getString(R.string.user_refresh_failed)), Toast.LENGTH_SHORT).show();
                                if (mSwipeRefreshLayout != null)
                                    mSwipeRefreshLayout.setRefreshing(false);
                                ProgressBar progressBar_user = findViewById(R.id.progressBar_user);
                                progressBar_user.setVisibility(View.INVISIBLE);
                                String cached_timetable = getWebCacheComplete();
                                final WebView webView_user = findViewById(R.id.webView_user);
                                TextView user_textView_status = findViewById(R.id.user_textView_status);
                                webView_user.loadData(cached_timetable, "text/html; charset=utf-8", "UTF-8");
                                user_textView_status.setText(String.format("%s.\n%s.", getString(R.string.network_not_available), getString(R.string.timetable_not_up_to_date)));
                                user_textView_status.setVisibility(View.VISIBLE);
                            }
                        });
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
            TextView user_textView_status = findViewById(R.id.user_textView_status);
            String timetable = builder.toString();
            String timetable_cache = jwebcache.toString();
            timetable = timetable.replaceAll("<th class=\"list\" align=\"center\">Stunde</th>", "");
            timetable = timetable.replaceAll("<th class=\"list\" align=\"center\" width=\"9\">\\(Lehrer\\)</th>", "");
            timetable = timetable.replaceAll("<th class=\"list\" align=\"center\"><b>Fach</b></th>", "");
            timetable = timetable.replaceAll("<th class=\"list\" align=\"center\" width=\"10\"><b>Vertreter</b></th>", "");
            timetable = timetable.replaceAll("<th class=\"list\" align=\"center\" width=\"9\">Raum</th>", "");
            timetable = timetable.replaceAll("<th class=\"list\" align=\"center\">Art</th>", "");
            timetable = timetable.replaceAll("<th class=\"list\" align=\"center\">Vertretungs-Text</th>", "");
            setWebCache(timetable_cache);
            setWebCacheComplete(timetable);
            webView_user.loadData(timetable, "text/html; charset=utf-8", "UTF-8");
            ProgressBar progressBar_user = findViewById(R.id.progressBar_user);
            progressBar_user.setVisibility(View.INVISIBLE);
            user_textView_status.setVisibility(View.INVISIBLE);
            if (mSwipeRefreshLayout != null) mSwipeRefreshLayout.setRefreshing(false);
            Toast.makeText(User.this, String.format("%s!", getString(R.string.user_refresh_success)), Toast.LENGTH_SHORT).show();

        }

    }
}