package de.myplan.android.ui;

import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import de.myplan.android.BuildConfig;
import de.myplan.android.R;
import de.myplan.android.util.Preferences;

public class AppInfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        if (savedInstanceState == null) {
            AppCompatDelegate.setDefaultNightMode(new Preferences(this).getTheme());
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_info);
        this.setTitle("App-Info");
        TextView app_info_version = findViewById(R.id.app_info_version);
        app_info_version.setText(String.format("%s %s", getString(R.string.app_info_version), BuildConfig.VERSION_NAME));
        TextView app_info_github = findViewById(R.id.app_info_github);
        app_info_github.setMovementMethod(LinkMovementMethod.getInstance());
    }

}
