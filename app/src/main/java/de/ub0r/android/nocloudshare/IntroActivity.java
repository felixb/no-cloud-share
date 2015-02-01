package de.ub0r.android.nocloudshare;

import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import de.ub0r.android.nocloudshare.adapter.IntroAdapter;

/**
 * @author flx
 */
public class IntroActivity extends ActionBarActivity implements ViewPager.OnPageChangeListener {

    @InjectView(R.id.viewpager)
    ViewPager mViewPager;

    @InjectView(R.id.prev)
    Button mPrevButton;

    @InjectView(R.id.next)
    Button mNextButton;

    @InjectView(R.id.finish)
    Button mFinishButton;

    private IntroAdapter mAdapter;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);
        ButterKnife.inject(this);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mAdapter = new IntroAdapter(this);
        mViewPager.setAdapter(mAdapter);
        mViewPager.setOnPageChangeListener(this);
        onPageSelected(mViewPager.getCurrentItem());
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

    @OnClick(R.id.prev)
    void onPrevClick() {
        int pos = mViewPager.getCurrentItem();
        if (pos > 0) {
            mViewPager.setCurrentItem(pos - 1, true);
        }
    }

    @OnClick(R.id.next)
    void onNextClick() {
        int pos = mViewPager.getCurrentItem();
        int length = mViewPager.getChildCount();
        if (pos < length - 1) {
            mViewPager.setCurrentItem(pos + 1, true);
        }
    }

    @OnClick(R.id.finish)
    void onFinishClick() {
        finish();
    }

    @Override
    public void onPageScrolled(final int pos, final float posOffset, final int posOffsetPix) {
        // nothing to do
    }

    @Override
    public void onPageSelected(final int pos) {
        int length = mAdapter.getCount();
        mPrevButton.setEnabled(pos > 0);
        if (pos == length - 1) {
            mNextButton.setVisibility(View.GONE);
            mFinishButton.setVisibility(View.VISIBLE);
        } else {
            mNextButton.setVisibility(View.VISIBLE);
            mFinishButton.setVisibility(View.GONE);
        }
    }

    @Override
    public void onPageScrollStateChanged(final int state) {
        // nothing to do
    }

}
