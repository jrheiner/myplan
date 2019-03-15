package de.myplan.android.util;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Date;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

public final class Preferences {

    private final SharedPreferences sp;

    public Preferences(@NonNull Context context) {
        sp = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public String getApiKey() {
        return sp.getString("api_key", null);
    }

    public void resetApiKey() {
        sp.edit().remove("api_key").apply();
    }

    public Date getLastUpdate() {
        try {
            return new Date(sp.getLong("last_update", 0));
        } catch (ClassCastException e) {
            sp.edit().remove("last_update").putLong("last_update", 0).apply();
            return new Date(0);
        }
    }

    public void setLastUpdate(Date date) {
        sp.edit().putLong("last_update", date.getTime()).apply();
    }

    public int getTheme() {
        final String value = sp.getString("general_theme", "0");
        if (value == null) return AppCompatDelegate.MODE_NIGHT_AUTO;
        switch (value) {
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
