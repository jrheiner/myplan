package de.myplan.android.util;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Date;

import androidx.preference.PreferenceManager;

public final class Preferences {

    private final SharedPreferences sp;

    public Preferences(Context context) {
        sp = PreferenceManager.getDefaultSharedPreferences(context);
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

}
