package de.myplan.android.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import de.myplan.android.BuildConfig;
import de.myplan.android.R;

public class AppInfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        if (savedInstanceState == null) {
            AppCompatDelegate.setDefaultNightMode(getThemeSettingsAsNightMode());
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_info);
        this.setTitle("App-Info");
        TextView app_info_version = findViewById(R.id.app_info_version);
        app_info_version.setText(String.format("%s %s", getString(R.string.app_info_version), BuildConfig.VERSION_NAME));
        TextView app_info_github = findViewById(R.id.app_info_github);
        app_info_github.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private String getThemeSettings() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPref.getString("general_theme", "0");
    }

    private int getThemeSettingsAsNightMode() {
        switch (getThemeSettings()) {
            case "-1":
                return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            case "0":
                return AppCompatDelegate.MODE_NIGHT_AUTO;
            case "1":
                return AppCompatDelegate.MODE_NIGHT_NO;
            case "2":
                return AppCompatDelegate.MODE_NIGHT_YES;
            default:
                return AppCompatDelegate.MODE_NIGHT_AUTO;
        }
    }
}
