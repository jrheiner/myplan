package m.android.mydsb;

import android.app.PendingIntent;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;


public class mydsbService extends JobService {

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
            //Toast.makeText(getApplicationContext(),"JobService task running", Toast.LENGTH_SHORT).show();

            // TODO WIP
            create_notification();

            jobFinished((JobParameters) msg.obj, false);
            return true;
        }

    });

    public void create_notification() {
        // Create an explicit intent for an Activity in your app
        Intent intent = new Intent(this, User.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        String CHANNEL_ID = "1";

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications_black_24dp)
                .setContentTitle("Vertretungsplan aktualisiert!")
                .setContentText("")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

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

}


