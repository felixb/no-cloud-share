package de.ub0r.android.nocloudshare;

import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * @author flx
 */
public class SettingsFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_settings);
    }
}
