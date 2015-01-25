package de.ub0r.android.nocloudshare;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.view.MenuItem;

import de.ub0r.android.logg0r.Log;

public class SettingsActivity extends ActionBarActivity {

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
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_share);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            Fragment f = new SettingsFragment();
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.container, f);
            ft.commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
