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
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.gson.GsonBuilder;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

import de.myplan.android.events.DsbTimetableEvent;
import de.myplan.android.events.NetworkErrorEvent;
import de.myplan.android.model.DsbTimetable;
import de.myplan.android.ui.UserActivity;
import de.myplan.android.util.Constants;
import de.myplan.android.util.GsonRequest;
import de.myplan.android.util.Preferences;
import de.myplan.android.util.SingletonRequestQueue;

import static androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC;


public class MyplanService extends JobService {

    private Preferences preferences;
    private Handler jobHandler;

    @Override
    public void onCreate() {
        preferences = new Preferences(this);
        jobHandler = new Handler(msg -> {

            if (getNotificationSetting() && preferences.getApiKey() != null) {
                requestTimetableUrl();
            }

            jobFinished((JobParameters) msg.obj, false);
            return true;
        });
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        jobHandler.sendMessage(Message.obtain(jobHandler, 1, params));
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        jobHandler.removeMessages(1);
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
            m_class_setting = String.format("Klasse %s", Constants.classSettings[class_setting]);
        }
        if (class_setting > 25) {
            m_class_setting = String.format("den %s. Jahrgang", Constants.classSettings[class_setting]);
        }

        Intent intent = new Intent(this, UserActivity.class);
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

        int notificationId = 1;
        if (getNotificationSetting()) {
            notificationManager.notify(notificationId, mBuilder.build());
        }
    }

    private void requestTimetableUrl() {
        final String apiKey = preferences.getApiKey();
        final String url = "https://iphone.dsbcontrol.de/iPhoneService.svc/DSB/timetables/" + apiKey;

        GsonRequest<DsbTimetable[]> request = new GsonRequest<>(url,
                new GsonBuilder().setDateFormat("dd.MM.yyyy HH:mm").create(),
                DsbTimetable[].class,
                response -> {
                    Date lastUpdate = getLatest(response);
                    boolean changed = preferences.getLastUpdate().before(lastUpdate);
                    EventBus.getDefault().post(new DsbTimetableEvent(response, changed, lastUpdate));
                    if (!changed)
                        return;
                    preferences.setLastUpdate(lastUpdate);
                    String[] timetableUrls = new String[response.length];
                    for (int i = 0; i < response.length; i++) {
                        timetableUrls[i] = response[i].url;
                    }
                    new MyplanService.JsoupAsyncTask().execute(timetableUrls);
                },
                error -> EventBus.getDefault().post(new NetworkErrorEvent(error)));

        SingletonRequestQueue.getInstance(this).addToRequestQueue(request);
    }

    private Date getLatest(DsbTimetable[] timetables) {
        Date latest = new Date(0);
        for (DsbTimetable timetable : timetables) {
            if (timetable.date.after(latest))
                latest = timetable.date;
        }
        return latest;
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


    private class JsoupAsyncTask extends AsyncTask<String, Void, String> {
        final StringBuilder builder = new StringBuilder();
        final JSONObject jwebcache = new JSONObject();
        int counter;
        String last_date_title = "";

        @Override
        protected final String doInBackground(String... params) {
            for (String url : params) {
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
                        if (last_inline_header.contains(Constants.classSettings[Integer.parseInt(class_setting)])) {

                            builder.append(affected_class);
                            jcache.append(affected_class);
                            counter++;
                        }
                    }
                    jwebcache.put(date, jcache.toString());
                    builder.append("</tbody></table></body>");
                } catch (java.io.IOException e) {
                    Log.e("MyplanService", e.toString());
                } catch (JSONException | ParseException e) {
                    e.printStackTrace();
                }
                if (counter == 0) {
                    builder.append("<table class=\"mon_list\"><tbody>");
                    builder.append("<tr class=\"list\" style=\"background: #ff975b;\"><td class=\"list\" align=\"center\" style=\"font-weight: 700;\">keine Vertretungen</td></tr>");
                    builder.append("</tbody></table>");
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
