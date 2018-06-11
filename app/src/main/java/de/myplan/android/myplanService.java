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
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

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
                    return false;
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
                        Elements tt_title = doc.select("div.mon_title");
                        builder.append(String.format("<br><h3>%s</h3>", tt_title.text()));
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
                                jcache.append(affected_class);
                                counter++;
                            }
                        }
                        String date_str = tt_title.text().replaceAll("[^0-9.]", "");
                        Date date_obj = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).parse(date_str);
                        String date = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(date_obj);
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


