package de.ub0r.android.nocloudshare;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;

import de.ub0r.android.logg0r.Log;

public class ShareActivity extends Activity {

    private static final String TAG = "ShareActivity";

    public static final String EXTRA_HASH = "hash";

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getResources().getBoolean(R.bool.activity_share_list_two_pane)) {
            Log.v(TAG, "dual pane, finish()");
            finish();
            return;
        }

        setContentView(R.layout.activity_share);
        //noinspection ConstantConditions
        getActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            Fragment f = ShareFragment.getInstance(getIntent());
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.container, f);
            ft.commit();
        }
    }
}
