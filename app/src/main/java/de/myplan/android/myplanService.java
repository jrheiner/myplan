package de.myplan.android;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.support.v4.app.NotificationCompat.VISIBILITY_PUBLIC;


public class myplanService extends JobService {

    private final String[] class_settings = {"", "5a", "5b", "5c", "5d", "5e",
            "6a", "6b", "6c", "6d", "6e",
            "7a", "7b", "7c", "7d", "7e",
            "8a", "8b", "8c", "8d", "8e",
            "9a", "9b", "9c", "9d", "9e",
            "10", "11"};
    private final Handler mJobHandler = new Handler(new Handler.Callback() {

        @Override
        public boolean handleMessage(Message msg) {

            if (getNotificationSetting() && getLoggedIn()) {
                request_timetableurl();
            }

            jobFinished((JobParameters) msg.obj, false);
            return true;
        }

    });

    @Override
    public boolean onStartJob(JobParameters params) {
        mJobHandler.sendMessage(Message.obtain(mJobHandler, 1, params));
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        mJobHandler.removeMessages(1);
        return false;
    }

    private void createNotificationChannel() {
        String CHANNEL_ID = "1";
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "myplan";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            assert notificationManager != null;
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void create_notification() {
        String m_class_setting = "deine ausgewählte Stufe";
        int class_setting = Integer.parseInt(getClassSetting());
        if (class_setting == 0) {
            m_class_setting = "alle Klassenstufen";
        }
        if (class_setting > 0 && class_setting < 26) {
            m_class_setting = String.format("Klasse %s", class_settings[class_setting]);
        }
        if (class_setting > 25) {
            m_class_setting = String.format("den %s. Jahrgang", class_settings[class_setting]);
        }

        Intent intent = new Intent(this, User.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        String CHANNEL_ID = "1";

        this.createNotificationChannel();

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_playlist_add_check_black_24dp)
                .setLargeIcon(BitmapFactory.decodeResource(this.getResources(),
                        R.mipmap.ic_launcher))
                .setContentTitle("Vertretungsplan aktualisiert!")
                .setContentText(String.format("neue Vertretungen für %s", m_class_setting))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setVisibility(VISIBILITY_PUBLIC)
                .setChannelId(CHANNEL_ID);

        if (getNotificationSetting() && getVibrationSetting()) {
            mBuilder.setVibrate(new long[]{0, 250});
        }

        if (getNotificationSetting() && getLEDSetting()) {
            mBuilder.setLights(Color.BLUE, 2000, 5000);
        }

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        int notificationId = 123;
        if (getNotificationSetting()) {
            notificationManager.notify(notificationId, mBuilder.build());
        }
    }

    private void request_timetableurl() {
        final String api_key = getApiKey();
        final ArrayList<String> timetableurls = new ArrayList<>();
        String url = "https://iphone.dsbcontrol.de/iPhoneService.svc/DSB/timetables/" + api_key;

        JsonArrayRequest jsonObjectRequest = new JsonArrayRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONArray>() {

                    @Override
                    public void onResponse(JSONArray response) {
                        try {
                            String last_update = getLastUpdate();
                            JSONObject current_request = (JSONObject) response.get(0);
                            String timetabledate = current_request.getString("timetabledate");
                            if (last_update.equals(timetabledate)) {
                                return;
                            } else {
                                setLastUpdate(timetabledate);
                            }
                            for (int i = 0; i < response.length(); i++) {
                                JSONObject json_node = (JSONObject) response.get(i);

                                String timetableurl = json_node.getString("timetableurl");
                                timetableurls.add(timetableurl);
                            }
                            new myplanService.JsoupAsyncTask().execute(timetableurls);


                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                    }
                });

        SingletonRequestQueue.getInstance(this).addToRequestQueue(jsonObjectRequest);
    }

    private String getApiKey() {
        SharedPreferences sp2 = this.getSharedPreferences("api_key", MODE_PRIVATE);
        return sp2.getString("api_key", null);
    }

    private Boolean getNotificationSetting() {
        SharedPreferences sharedPref =
                PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPref.getBoolean("notifications_new_message", true);
    }

    private Boolean getVibrationSetting() {
        SharedPreferences sharedPref =
                PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPref.getBoolean("notifications_new_message_vibrate", true);
    }

    private Boolean getLEDSetting() {
        SharedPreferences sharedPref =
                PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPref.getBoolean("notifications_new_message_led", true);
    }

    private void setWebCache(String s) {
        SharedPreferences sp = getSharedPreferences("web_cache", MODE_PRIVATE);
        SharedPreferences.Editor ed1 = sp.edit();
        ed1.putString("web_cache", s);
        ed1.apply();
    }

    private String getWebcache() {
        SharedPreferences sp = this.getSharedPreferences("web_cache", MODE_PRIVATE);
        return sp.getString("web_cache", "");
    }

    private void setWebCacheComplete(String s) {
        SharedPreferences sp = getSharedPreferences("web_cache_complete", MODE_PRIVATE);
        SharedPreferences.Editor ed = sp.edit();
        ed.putString("web_cache_complete", s);
        ed.apply();
    }

    private String getClassSetting() {
        SharedPreferences sharedPref =
                PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPref.getString("general_list", "0");
    }

    private boolean getLoggedIn() {
        SharedPreferences sp2 = this.getSharedPreferences("logged_in", MODE_PRIVATE);
        return sp2.getBoolean("logged_in", false);
    }

    private Boolean getTimetableSetting() {
        SharedPreferences sharedPref =
                PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPref.getBoolean("general_timetable_pref", false);
    }

    private String getTimetable() {
        SharedPreferences sp = getSharedPreferences("timetable", MODE_PRIVATE);
        return sp.getString("timetable", "{\"day1\":{\"1\":\"0; \",\"2\":\"0; \",\"3\":\"0; \",\"4\":\"0; \",\"5\":\"0; \",\"6\":\"0; \",\"7\":\"0; \",\"8\":\"0; \",\"9\":\"0; \",\"10\":\"0; \",\"11\":\"0; \",\"12\":\"0; \",\"13\":\"0; \"},\"day2\":{\"1\":\"0; \",\"2\":\"0; \",\"3\":\"0; \",\"4\":\"0; \",\"5\":\"0; \",\"6\":\"0; \",\"7\":\"0; \",\"8\":\"0; \",\"9\":\"0; \",\"10\":\"0; \",\"11\":\"0; \",\"12\":\"0; \",\"13\":\"0; \"},\"day3\":{\"1\":\"0; \",\"2\":\"0; \",\"3\":\"0; \",\"4\":\"0; \",\"5\":\"0; \",\"6\":\"0; \",\"7\":\"0; \",\"8\":\"0; \",\"9\":\"0; \",\"10\":\"0; \",\"11\":\"0; \",\"12\":\"0; \",\"13\":\"0; \"},\"day4\":{\"1\":\"0; \",\"2\":\"0; \",\"3\":\"0; \",\"4\":\"0; \",\"5\":\"0; \",\"6\":\"0; \",\"7\":\"0; \",\"8\":\"0; \",\"9\":\"0; \",\"10\":\"0; \",\"11\":\"0; \",\"12\":\"0; \",\"13\":\"0; \"},\"day5\":{\"1\":\"0; \",\"2\":\"0; \",\"3\":\"0; \",\"4\":\"0; \",\"5\":\"0; \",\"6\":\"0; \",\"7\":\"0; \",\"8\":\"0; \",\"9\":\"0; \",\"10\":\"0; \",\"11\":\"0; \",\"12\":\"0; \",\"13\":\"0; \"}}");
    }

    private String getLastUpdate() {
        SharedPreferences sp = this.getSharedPreferences("last_update", MODE_PRIVATE);
        return sp.getString("last_update", "");
    }

    private void setLastUpdate(String s) {
        SharedPreferences sp = getSharedPreferences("last_update", MODE_PRIVATE);
        SharedPreferences.Editor ed = sp.edit();
        ed.putString("last_update", s);
        ed.apply();
    }

    private boolean cacheIsEqual(JSONObject new_table, JSONObject old_table) {
        Iterator<String> ttc_keys = new_table.keys();
        while (ttc_keys.hasNext()) {
            String key = ttc_keys.next();
            try {
                String new_value = new_table.getString(key);
                if (old_table.has(key)) {
                    String old_value = old_table.getString(key);
                    if (!new_value.equals(old_value)) {
                        return true;
                    }
                } else {
                    return true;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private class JsoupAsyncTask extends AsyncTask<ArrayList<String>, Void, String> {
        final StringBuilder builder = new StringBuilder();
        final JSONObject jwebcache = new JSONObject();
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
                                                if (stunde.contains("1")) {
                                                    if (day1.getString("1").split(";")[1].replaceAll(" ", "").toLowerCase().contains(lehrer.toLowerCase())) {
                                                        builder.append(affected_class);
                                                        jcache.append(affected_class);
                                                        counter++;
                                                    }

                                                } else if (stunde.contains("2")) {
                                                    if (day1.getString("2").split(";")[1].replaceAll(" ", "").toLowerCase().contains(lehrer.toLowerCase())) {
                                                        builder.append(affected_class);
                                                        jcache.append(affected_class);
                                                        counter++;
                                                    }

                                                } else if (stunde.contains("3")) {
                                                    if (day1.getString("3").split(";")[1].replaceAll(" ", "").toLowerCase().contains(lehrer.toLowerCase())) {
                                                        builder.append(affected_class);
                                                        jcache.append(affected_class);
                                                        counter++;
                                                    }

                                                } else if (stunde.contains("4")) {
                                                    if (day1.getString("4").split(";")[1].replaceAll(" ", "").toLowerCase().contains(lehrer.toLowerCase())) {
                                                        builder.append(affected_class);
                                                        jcache.append(affected_class);
                                                        counter++;
                                                    }

                                                } else if (stunde.contains("5")) {
                                                    if (day1.getString("5").split(";")[1].replaceAll(" ", "").toLowerCase().contains(lehrer.toLowerCase())) {
                                                        builder.append(affected_class);
                                                        jcache.append(affected_class);
                                                        counter++;
                                                    }

                                                } else if (stunde.contains("6")) {
                                                    if (day1.getString("6").split(";")[1].replaceAll(" ", "").toLowerCase().contains(lehrer.toLowerCase())) {
                                                        builder.append(affected_class);
                                                        jcache.append(affected_class);
                                                        counter++;
                                                    }

                                                } else if (stunde.contains("7")) {
                                                    if (day1.getString("7").split(";")[1].replaceAll(" ", "").toLowerCase().contains(lehrer.toLowerCase())) {
                                                        builder.append(affected_class);
                                                        jcache.append(affected_class);
                                                        counter++;
                                                    }

                                                } else if (stunde.contains("8")) {
                                                    if (day1.getString("8").split(";")[1].replaceAll(" ", "").toLowerCase().contains(lehrer.toLowerCase())) {
                                                        builder.append(affected_class);
                                                        jcache.append(affected_class);
                                                        counter++;
                                                    }

                                                } else if (stunde.contains("9")) {
                                                    if (day1.getString("9").split(";")[1].replaceAll(" ", "").toLowerCase().contains(lehrer.toLowerCase())) {
                                                        builder.append(affected_class);
                                                        jcache.append(affected_class);
                                                        counter++;
                                                    }

                                                } else if (stunde.contains("10")) {
                                                    if (day1.getString("10").split(";")[1].replaceAll(" ", "").toLowerCase().contains(lehrer.toLowerCase())) {
                                                        builder.append(affected_class);
                                                        jcache.append(affected_class);
                                                        counter++;
                                                    }

                                                } else if (stunde.contains("11")) {
                                                    if (day1.getString("11").split(";")[1].replaceAll(" ", "").toLowerCase().contains(lehrer.toLowerCase())) {
                                                        builder.append(affected_class);
                                                        jcache.append(affected_class);
                                                        counter++;
                                                    }

                                                } else if (stunde.contains("12")) {
                                                    if (day1.getString("12").split(";")[1].replaceAll(" ", "").toLowerCase().contains(lehrer.toLowerCase())) {
                                                        builder.append(affected_class);
                                                        jcache.append(affected_class);
                                                        counter++;
                                                    }

                                                } else if (stunde.contains("13")) {
                                                    if (day1.getString("13").split(";")[1].replaceAll(" ", "").toLowerCase().contains(lehrer.toLowerCase())) {
                                                        builder.append(affected_class);
                                                        jcache.append(affected_class);
                                                        counter++;
                                                    }

                                                } else {
                                                    builder.append("<table class=\"mon_list\"><tbody>");
                                                    builder.append("<tr class=\"list\" style=\"background: #ff975b;\"><td class=\"list\" align=\"center\" style=\"font-weight: 700;\">keine Vertretungen</td></tr>");
                                                    builder.append("</tbody></table>");
                                                }
                                                break;

                                            case Calendar.TUESDAY:
                                                JSONObject day2 = timetable.getJSONObject("day2");
                                                if (stunde.contains("1")) {
                                                    if (day2.getString("1").split(";")[1].replaceAll(" ", "").toLowerCase().contains(lehrer.toLowerCase())) {
                                                        builder.append(affected_class);
                                                        jcache.append(affected_class);
                                                        counter++;
                                                    }

                                                } else if (stunde.contains("2")) {
                                                    if (day2.getString("2").split(";")[1].replaceAll(" ", "").toLowerCase().contains(lehrer.toLowerCase())) {
                                                        builder.append(affected_class);
                                                        jcache.append(affected_class);
                                                        counter++;
                                                    }

                                                } else if (stunde.contains("3")) {
                                                    if (day2.getString("3").split(";")[1].replaceAll(" ", "").toLowerCase().contains(lehrer.toLowerCase())) {
                                                        builder.append(affected_class);
                                                        jcache.append(affected_class);
                                                        counter++;
                                                    }

                                                } else if (stunde.contains("4")) {
                                                    if (day2.getString("4").split(";")[1].replaceAll(" ", "").toLowerCase().contains(lehrer.toLowerCase())) {
                                                        builder.append(affected_class);
                                                        jcache.append(affected_class);
                                                        counter++;
                                                    }

                                                } else if (stunde.contains("5")) {
                                                    if (day2.getString("5").split(";")[1].replaceAll(" ", "").toLowerCase().contains(lehrer.toLowerCase())) {
                                                        builder.append(affected_class);
                                                        jcache.append(affected_class);
                                                        counter++;
                                                    }

                                                } else if (stunde.contains("6")) {
                                                    if (day2.getString("6").split(";")[1].replaceAll(" ", "").toLowerCase().contains(lehrer.toLowerCase())) {
                                                        builder.append(affected_class);
                                                        jcache.append(affected_class);
                                                        counter++;
                                                    }

                                                } else if (stunde.contains("7")) {
                                                    if (day2.getString("7").split(";")[1].replaceAll(" ", "").toLowerCase().contains(lehrer.toLowerCase())) {
                                                        builder.append(affected_class);
                                                        jcache.append(affected_class);
                                                        counter++;
                                                    }

                                                } else if (stunde.contains("8")) {
                                                    if (day2.getString("8").split(";")[1].replaceAll(" ", "").toLowerCase().contains(lehrer.toLowerCase())) {
                                                        builder.append(affected_class);
                                                        jcache.append(affected_class);
                                                        counter++;
                                                    }

                                                } else if (stunde.contains("9")) {
                                                    if (day2.getString("9").split(";")[1].replaceAll(" ", "").toLowerCase().contains(lehrer.toLowerCase())) {
                                                        builder.append(affected_class);
                                                        jcache.append(affected_class);
                                                        counter++;
                                                    }

                                                } else if (stunde.contains("10")) {
                                                    if (day2.getString("10").split(";")[1].replaceAll(" ", "").toLowerCase().contains(lehrer.toLowerCase())) {
                                                        builder.append(affected_class);
                                                        jcache.append(affected_class);
                                                        counter++;
                                                    }

                                                } else if (stunde.contains("11")) {
                                                    if (day2.getString("11").split(";")[1].replaceAll(" ", "").toLowerCase().contains(lehrer.toLowerCase())) {
                                                        builder.append(affected_class);
                                                        jcache.append(affected_class);
                                                        counter++;
                                                    }

                                                } else if (stunde.contains("12")) {
                                                    if (day2.getString("12").split(";")[1].replaceAll(" ", "").toLowerCase().contains(lehrer.toLowerCase())) {
                                                        builder.append(affected_class);
                                                        jcache.append(affected_class);
                                                        counter++;
                                                    }

                                                } else if (stunde.contains("13")) {
                                                    if (day2.getString("13").split(";")[1].replaceAll(" ", "").toLowerCase().contains(lehrer.toLowerCase())) {
                                                        builder.append(affected_class);
                                                        jcache.append(affected_class);
                                                        counter++;
                                                    }

                                                } else {
                                                    builder.append("<table class=\"mon_list\"><tbody>");
                                                    builder.append("<tr class=\"list\" style=\"background: #ff975b;\"><td class=\"list\" align=\"center\" style=\"font-weight: 700;\">keine Vertretungen</td></tr>");
                                                    builder.append("</tbody></table>");
                                                }
                                                break;

                                            case Calendar.WEDNESDAY:
                                                JSONObject day3 = timetable.getJSONObject("day3");
                                                if (stunde.contains("1")) {
                                                    if (day3.getString("1").split(";")[1].replaceAll(" ", "").toLowerCase().contains(lehrer.toLowerCase())) {
                                                        builder.append(affected_class);
                                                        jcache.append(affected_class);
                                                        counter++;
                                                    }

                                                } else if (stunde.contains("2")) {
                                                    if (day3.getString("2").split(";")[1].replaceAll(" ", "").toLowerCase().contains(lehrer.toLowerCase())) {
                                                        builder.append(affected_class);
                                                        jcache.append(affected_class);
                                                        counter++;
                                                    }

                                                } else if (stunde.contains("3")) {
                                                    if (day3.getString("3").split(";")[1].replaceAll(" ", "").toLowerCase().contains(lehrer.toLowerCase())) {
                                                        builder.append(affected_class);
                                                        jcache.append(affected_class);
                                                        counter++;
                                                    }

                                                } else if (stunde.contains("4")) {
                                                    if (day3.getString("4").split(";")[1].replaceAll(" ", "").toLowerCase().contains(lehrer.toLowerCase())) {
                                                        builder.append(affected_class);
                                                        jcache.append(affected_class);
                                                        counter++;
                                                    }

                                                } else if (stunde.contains("5")) {
                                                    if (day3.getString("5").split(";")[1].replaceAll(" ", "").toLowerCase().contains(lehrer.toLowerCase())) {
                                                        builder.append(affected_class);
                                                        jcache.append(affected_class);
                                                        counter++;
                                                    }

                                                } else if (stunde.contains("6")) {
                                                    if (day3.getString("6").split(";")[1].replaceAll(" ", "").toLowerCase().contains(lehrer.toLowerCase())) {
                                                        builder.append(affected_class);
                                                        jcache.append(affected_class);
                                                        counter++;
                                                    }

                                                } else if (stunde.contains("7")) {
                                                    if (day3.getString("7").split(";")[1].replaceAll(" ", "").toLowerCase().contains(lehrer.toLowerCase())) {
                                                        builder.append(affected_class);
                                                        jcache.append(affected_class);
                                                        counter++;
                                                    }

                                                } else if (stunde.contains("8")) {
                                                    if (day3.getString("8").split(";")[1].replaceAll(" ", "").toLowerCase().contains(lehrer.toLowerCase())) {
                                                        builder.append(affected_class);
                                                        jcache.append(affected_class);
                                                        counter++;
                                                    }

                                                } else if (stunde.contains("9")) {
                                                    if (day3.getString("9").split(";")[1].replaceAll(" ", "").toLowerCase().contains(lehrer.toLowerCase())) {
                                                        builder.append(affected_class);
                                                        jcache.append(affected_class);
                                                        counter++;
                                                    }

                                                } else if (stunde.contains("10")) {
                                                    if (day3.getString("10").split(";")[1].replaceAll(" ", "").toLowerCase().contains(lehrer.toLowerCase())) {
                                                        builder.append(affected_class);
                                                        jcache.append(affected_class);
                                                        counter++;
                                                    }

                                                } else if (stunde.contains("11")) {
                                                    if (day3.getString("11").split(";")[1].replaceAll(" ", "").toLowerCase().contains(lehrer.toLowerCase())) {
                                                        builder.append(affected_class);
                                                        jcache.append(affected_class);
                                                        counter++;
                                                    }

                                                } else if (stunde.contains("12")) {
                                                    if (day3.getString("12").split(";")[1].replaceAll(" ", "").toLowerCase().contains(lehrer.toLowerCase())) {
                                                        builder.append(affected_class);
                                                        jcache.append(affected_class);
                                                        counter++;
                                                    }

                                                } else if (stunde.contains("13")) {
                                                    if (day3.getString("13").split(";")[1].replaceAll(" ", "").toLowerCase().contains(lehrer.toLowerCase())) {
                                                        builder.append(affected_class);
                                                        jcache.append(affected_class);
                                                        counter++;
                                                    }

                                                } else {
                                                    builder.append("<table class=\"mon_list\"><tbody>");
                                                    builder.append("<tr class=\"list\" style=\"background: #ff975b;\"><td class=\"list\" align=\"center\" style=\"font-weight: 700;\">keine Vertretungen</td></tr>");
                                                    builder.append("</tbody></table>");
                                                }
                                                break;

                                            case Calendar.THURSDAY:
                                                JSONObject day4 = timetable.getJSONObject("day4");
                                                if (stunde.contains("1")) {
                                                    if (day4.getString("1").split(";")[1].replaceAll(" ", "").toLowerCase().contains(lehrer.toLowerCase())) {
                                                        builder.append(affected_class);
                                                        jcache.append(affected_class);
                                                        counter++;
                                                    }

                                                } else if (stunde.contains("2")) {
                                                    if (day4.getString("2").split(";")[1].replaceAll(" ", "").toLowerCase().contains(lehrer.toLowerCase())) {
                                                        builder.append(affected_class);
                                                        jcache.append(affected_class);
                                                        counter++;
                                                    }

                                                } else if (stunde.contains("3")) {
                                                    if (day4.getString("3").split(";")[1].replaceAll(" ", "").toLowerCase().contains(lehrer.toLowerCase())) {
                                                        builder.append(affected_class);
                                                        jcache.append(affected_class);
                                                        counter++;
                                                    }

                                                } else if (stunde.contains("4")) {
                                                    if (day4.getString("4").split(";")[1].replaceAll(" ", "").toLowerCase().contains(lehrer.toLowerCase())) {
                                                        builder.append(affected_class);
                                                        jcache.append(affected_class);
                                                        counter++;
                                                    }

                                                } else if (stunde.contains("5")) {
                                                    if (day4.getString("5").split(";")[1].replaceAll(" ", "").toLowerCase().contains(lehrer.toLowerCase())) {
                                                        builder.append(affected_class);
                                                        jcache.append(affected_class);
                                                        counter++;
                                                    }

                                                } else if (stunde.contains("6")) {
                                                    if (day4.getString("6").split(";")[1].replaceAll(" ", "").toLowerCase().contains(lehrer.toLowerCase())) {
                                                        builder.append(affected_class);
                                                        jcache.append(affected_class);
                                                        counter++;
                                                    }

                                                } else if (stunde.contains("7")) {
                                                    if (day4.getString("7").split(";")[1].replaceAll(" ", "").toLowerCase().contains(lehrer.toLowerCase())) {
                                                        builder.append(affected_class);
                                                        jcache.append(affected_class);
                                                        counter++;
                                                    }

                                                } else if (stunde.contains("8")) {
                                                    if (day4.getString("8").split(";")[1].replaceAll(" ", "").toLowerCase().contains(lehrer.toLowerCase())) {
                                                        builder.append(affected_class);
                                                        jcache.append(affected_class);
                                                        counter++;
                                                    }

                                                } else if (stunde.contains("9")) {
                                                    if (day4.getString("9").split(";")[1].replaceAll(" ", "").toLowerCase().contains(lehrer.toLowerCase())) {
                                                        builder.append(affected_class);
                                                        jcache.append(affected_class);
                                                        counter++;
                                                    }

                                                } else if (stunde.contains("10")) {
                                                    if (day4.getString("10").split(";")[1].replaceAll(" ", "").toLowerCase().contains(lehrer.toLowerCase())) {
                                                        builder.append(affected_class);
                                                        jcache.append(affected_class);
                                                        counter++;
                                                    }

                                                } else if (stunde.contains("11")) {
                                                    if (day4.getString("11").split(";")[1].replaceAll(" ", "").toLowerCase().contains(lehrer.toLowerCase())) {
                                                        builder.append(affected_class);
                                                        jcache.append(affected_class);
                                                        counter++;
                                                    }

                                                } else if (stunde.contains("12")) {
                                                    if (day4.getString("12").split(";")[1].replaceAll(" ", "").toLowerCase().contains(lehrer.toLowerCase())) {
                                                        builder.append(affected_class);
                                                        jcache.append(affected_class);
                                                        counter++;
                                                    }

                                                } else if (stunde.contains("13")) {
                                                    if (day4.getString("13").split(";")[1].replaceAll(" ", "").toLowerCase().contains(lehrer.toLowerCase())) {
                                                        builder.append(affected_class);
                                                        jcache.append(affected_class);
                                                        counter++;
                                                    }

                                                } else {
                                                    builder.append("<table class=\"mon_list\"><tbody>");
                                                    builder.append("<tr class=\"list\" style=\"background: #ff975b;\"><td class=\"list\" align=\"center\" style=\"font-weight: 700;\">keine Vertretungen</td></tr>");
                                                    builder.append("</tbody></table>");
                                                }
                                                break;
                                            case Calendar.FRIDAY:
                                                JSONObject day5 = timetable.getJSONObject("day5");
                                                if (stunde.contains("1")) {
                                                    if (day5.getString("1").split(";")[1].replaceAll(" ", "").toLowerCase().contains(lehrer.toLowerCase())) {
                                                        builder.append(affected_class);
                                                        jcache.append(affected_class);
                                                        counter++;
                                                    }

                                                } else if (stunde.contains("2")) {
                                                    if (day5.getString("2").split(";")[1].replaceAll(" ", "").toLowerCase().contains(lehrer.toLowerCase())) {
                                                        builder.append(affected_class);
                                                        jcache.append(affected_class);
                                                        counter++;
                                                    }

                                                } else if (stunde.contains("3")) {
                                                    if (day5.getString("3").split(";")[1].replaceAll(" ", "").toLowerCase().contains(lehrer.toLowerCase())) {
                                                        builder.append(affected_class);
                                                        jcache.append(affected_class);
                                                        counter++;
                                                    }

                                                } else if (stunde.contains("4")) {
                                                    if (day5.getString("4").split(";")[1].replaceAll(" ", "").toLowerCase().contains(lehrer.toLowerCase())) {
                                                        builder.append(affected_class);
                                                        jcache.append(affected_class);
                                                        counter++;
                                                    }

                                                } else if (stunde.contains("5")) {
                                                    if (day5.getString("5").split(";")[1].replaceAll(" ", "").toLowerCase().contains(lehrer.toLowerCase())) {
                                                        builder.append(affected_class);
                                                        jcache.append(affected_class);
                                                        counter++;
                                                    }

                                                } else if (stunde.contains("6")) {
                                                    if (day5.getString("6").split(";")[1].replaceAll(" ", "").toLowerCase().contains(lehrer.toLowerCase())) {
                                                        builder.append(affected_class);
                                                        jcache.append(affected_class);
                                                        counter++;
                                                    }

                                                } else if (stunde.contains("7")) {
                                                    if (day5.getString("7").split(";")[1].replaceAll(" ", "").toLowerCase().contains(lehrer.toLowerCase())) {
                                                        builder.append(affected_class);
                                                        jcache.append(affected_class);
                                                        counter++;
                                                    }

                                                } else if (stunde.contains("8")) {
                                                    if (day5.getString("8").split(";")[1].replaceAll(" ", "").toLowerCase().contains(lehrer.toLowerCase())) {
                                                        builder.append(affected_class);
                                                        jcache.append(affected_class);
                                                        counter++;
                                                    }

                                                } else if (stunde.contains("9")) {
                                                    if (day5.getString("9").split(";")[1].replaceAll(" ", "").toLowerCase().contains(lehrer.toLowerCase())) {
                                                        builder.append(affected_class);
                                                        jcache.append(affected_class);
                                                        counter++;
                                                    }

                                                } else if (stunde.contains("10")) {
                                                    if (day5.getString("10").split(";")[1].replaceAll(" ", "").toLowerCase().contains(lehrer.toLowerCase())) {
                                                        builder.append(affected_class);
                                                        jcache.append(affected_class);
                                                        counter++;
                                                    }

                                                } else if (stunde.contains("11")) {
                                                    if (day5.getString("11").split(";")[1].replaceAll(" ", "").toLowerCase().contains(lehrer.toLowerCase())) {
                                                        builder.append(affected_class);
                                                        jcache.append(affected_class);
                                                        counter++;
                                                    }

                                                } else if (stunde.contains("12")) {
                                                    if (day5.getString("12").split(";")[1].replaceAll(" ", "").toLowerCase().contains(lehrer.toLowerCase())) {
                                                        builder.append(affected_class);
                                                        jcache.append(affected_class);
                                                        counter++;
                                                    }

                                                } else if (stunde.contains("13")) {
                                                    if (day5.getString("13").split(";")[1].replaceAll(" ", "").toLowerCase().contains(lehrer.toLowerCase())) {
                                                        builder.append(affected_class);
                                                        jcache.append(affected_class);
                                                        counter++;
                                                    }

                                                } else {
                                                    builder.append("<table class=\"mon_list\"><tbody>");
                                                    builder.append("<tr class=\"list\" style=\"background: #ff975b;\"><td class=\"list\" align=\"center\" style=\"font-weight: 700;\">keine Vertretungen</td></tr>");
                                                    builder.append("</tbody></table>");
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
                    } catch (java.io.IOException e) {
                        Log.e("myplanService", e.toString());
                    } catch (JSONException | ParseException e) {
                        e.printStackTrace();
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
            setWebCacheComplete(builder.toString());
            String timetable_cache = jwebcache.toString();
            String old_webcache = getWebcache();
            try {
                JSONObject jtimetable_cache = new JSONObject(timetable_cache);
                JSONObject jold_webcache = new JSONObject(old_webcache);
                if (cacheIsEqual(jtimetable_cache, jold_webcache)) {
                    setWebCache(timetable_cache);
                    create_notification();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

    }
}


