package de.ub0r.android.nocloudshare;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.text.TextUtils;

import de.ub0r.android.logg0r.Log;

public class SettingsActivity extends PreferenceActivity {

    private static final String TAG = "SettingsActivity";

    public static final String PREFS_PORT = "port";

    public static final String DEFAULT_PORT = "8808";

    private static final String PREFS_EXPIRATION_MINUTES = "expiration";

    private static final String DEFAULT_EXPIRATION_MINUTES = "15";

    public static final String PREFS_SHOW_INDEX = "show_index";

    public static final boolean DEFAULT_SHOW_INDEX = false;

    public static long getExpirationPeriod(final SharedPreferences p) {
        String s = p.getString(PREFS_EXPIRATION_MINUTES, DEFAULT_EXPIRATION_MINUTES);
        if (TextUtils.isEmpty(s)) {
            s = DEFAULT_EXPIRATION_MINUTES;
        }
        long l;
        try {
            l = Long.parseLong(s);
        } catch (NumberFormatException e) {
            Log.d(TAG, "could not parse expiration period: ", s);
            l = Long.parseLong(SettingsActivity.DEFAULT_EXPIRATION_MINUTES);
        }
        return l * 60L * 1000L;
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        //noinspection deprecation
        addPreferencesFromResource(R.xml.pref_settings);
    }
}
