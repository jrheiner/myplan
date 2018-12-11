package de.myplan.android.ui;

import android.os.Bundle;
import androidx.preference.PreferenceFragmentCompat;

import de.myplan.android.R;

public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }
}
