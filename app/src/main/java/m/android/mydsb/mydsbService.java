package m.android.mydsb;

import android.app.PendingIntent;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
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

import java.util.ArrayList;

import static android.support.v4.app.NotificationCompat.VISIBILITY_PUBLIC;


public class mydsbService extends JobService {

    String class_settings[] = {"", "5a", "5b", "5c", "5d", "5e",
            "6a", "6b", "6c", "6d", "6e",
            "7a", "7b", "7c", "7d", "7e",
            "8a", "8b", "8c", "8d", "8e",
            "9a", "9b", "9c", "9d", "9e",
            "10", "11"};

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

    private Handler mJobHandler = new Handler(new Handler.Callback() {

        @Override
        public boolean handleMessage(Message msg) {

            if (getNotificationSetting() && getLogged_in()) {
                request_timetableurl();
            }

            jobFinished((JobParameters) msg.obj, false);
            return true;
        }

    });

    public void create_notification() {
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

        // Create an explicit intent for an Activity in your app
        Intent intent = new Intent(this, User.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        String CHANNEL_ID = "1";

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_playlist_add_check_black_24dp)
                .setLargeIcon(BitmapFactory.decodeResource(this.getResources(),
                        R.mipmap.ic_launcher))
                .setContentTitle("Vertretungsplan aktualisiert!")
                .setContentText(String.format("neue Vertretungen für %s", m_class_setting))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setVisibility(VISIBILITY_PUBLIC);

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

    public void request_timetableurl() {
        final String api_key = getApi_key();
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
                                Log.i("mydsbService", timetableurl);
                                timetableurls.add(timetableurl);
                            }
                            new mydsbService.JsoupAsyncTask().execute(timetableurls);


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

    public String getApi_key() {
        SharedPreferences sp2 = this.getSharedPreferences("api_key", MODE_PRIVATE);
        return sp2.getString("api_key", null);
    }

    public Boolean getNotificationSetting() {
        SharedPreferences sharedPref =
                PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPref.getBoolean("notifications_new_message", true);
    }

    public Boolean getVibrationSetting() {
        SharedPreferences sharedPref =
                PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPref.getBoolean("notifications_new_message_vibrate", true);
    }

    public Boolean getLEDSetting() {
        SharedPreferences sharedPref =
                PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPref.getBoolean("notifications_new_message_led", true);
    }

    public void setWebCache(String s) {
        SharedPreferences sp = getSharedPreferences("web_cache", MODE_PRIVATE);
        SharedPreferences.Editor ed1 = sp.edit();
        ed1.putString("web_cache", s);
        ed1.apply();
    }

    public String getWebcache() {
        SharedPreferences sp = this.getSharedPreferences("web_cache", MODE_PRIVATE);
        return sp.getString("web_cache", "");
    }

    public String getClassSetting() {
        SharedPreferences sharedPref =
                PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPref.getString("general_list", "0");
    }

    public boolean getLogged_in() {
        SharedPreferences sp2 = this.getSharedPreferences("logged_in", MODE_PRIVATE);
        return sp2.getBoolean("logged_in", false);
    }

    private class JsoupAsyncTask extends AsyncTask<ArrayList<String>, Void, String> {
        final StringBuilder builder = new StringBuilder();
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
                        Log.e("mydsbService", e.toString());
                        //Toast.makeText(mydsbService.this, e.toString(), Toast.LENGTH_SHORT).show();
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
            String timetable = builder.toString();
            timetable = timetable.replaceAll("<th class=\"list\" align=\"center\">Stunde</th>", "");
            timetable = timetable.replaceAll("<th class=\"list\" align=\"center\" width=\"9\">\\(Lehrer\\)</th>", "");
            timetable = timetable.replaceAll("<th class=\"list\" align=\"center\"><b>Fach</b></th>", "");
            timetable = timetable.replaceAll("<th class=\"list\" align=\"center\" width=\"10\"><b>Vertreter</b></th>", "");
            timetable = timetable.replaceAll("<th class=\"list\" align=\"center\" width=\"9\">Raum</th>", "");
            timetable = timetable.replaceAll("<th class=\"list\" align=\"center\">Art</th>", "");
            timetable = timetable.replaceAll("<th class=\"list\" align=\"center\">Vertretungs-Text</th>", "");
            String old_webchache = getWebcache();
            if (!timetable.equals(old_webchache)) {
                Log.i("bcheck", "new table");
                create_notification();
                setWebCache(timetable);
            } else {
                Log.i("bcheck", "nothing new");
            }
        }

    }

}


