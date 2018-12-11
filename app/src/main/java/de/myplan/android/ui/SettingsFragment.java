package de.myplan.android.ui;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import de.myplan.android.R;

public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
        setPreferenceVisibility(findPreference("timetable_settings"));

        SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (key.equals("general_list")) {
                    setPreferenceVisibility(findPreference("timetable_settings"));
                } else if (key.equals("general_theme")) {
                    if (!getThemeSettings().equals(String.valueOf(AppCompatDelegate.getDefaultNightMode()))) {
                        switch (getThemeSettings()) {
                            //TODO dynamically change theme
                            case "-1":
                                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                                break;
                            case "0":
                                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO);
                                break;
                            case "1":
                                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                                break;
                            case "2":
                                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                                break;
                        }
                    }
                }
            }
        };
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(listener);
    }

    private void setPreferenceVisibility(Preference PrefCat) {
        if ((Integer.parseInt(getClassSetting()) > 25)) {
            PrefCat.setVisible(true);
        } else {
            PrefCat.setVisible(false);
            //disable filter when hiding it
            disableTimetableFilter();
        }
    }

    private void disableTimetableFilter() {
        SharedPreferences.Editor ed = android.preference.PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
        ed.putBoolean("general_timetable_pref", false);
        ed.apply();
    }

    private String getClassSetting() {
        SharedPreferences sharedPref =
                PreferenceManager.getDefaultSharedPreferences(getActivity());
        return sharedPref.getString("general_list", "0");
    }

    private String getThemeSettings() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        return sharedPref.getString("general_theme", "0");
    }
}
