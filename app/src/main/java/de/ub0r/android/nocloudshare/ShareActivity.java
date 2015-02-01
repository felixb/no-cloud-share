package de.ub0r.android.nocloudshare;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.ub0r.android.logg0r.Log;

public class ShareActivity extends ActionBarActivity {

    private static final String TAG = "ShareActivity";

    public static final String EXTRA_HASH = "hash";

    @InjectView(R.id.ads)
    AdView mAdView;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getResources().getBoolean(R.bool.activity_share_list_two_pane)) {
            Log.v(TAG, "dual pane, finish()");
            finish();
            return;
        }

        setContentView(R.layout.activity_share);
        ButterKnife.inject(this);

        mAdView.setVisibility(View.GONE);
        if (BuildConfig.ADS) {
            mAdView.loadAd(new AdRequest.Builder().build());
            mAdView.setAdListener(new AdListener() {
                @Override
                public void onAdLoaded() {
                    mAdView.setVisibility(View.VISIBLE);
                    super.onAdLoaded();
                }
            });
        }

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            Fragment f = ShareFragment.getInstance(getIntent());
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.container, f);
            ft.commit();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mAdView != null) {
            mAdView.resume();
        }
    }

    @Override
    protected void onPause() {
        if (mAdView != null) {
            mAdView.pause();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (mAdView != null) {
            mAdView.destroy();
        }
        super.onDestroy();
    }
}
