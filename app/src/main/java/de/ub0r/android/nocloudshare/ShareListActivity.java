package de.ub0r.android.nocloudshare;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import org.jetbrains.annotations.NotNull;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import de.ub0r.android.logg0r.Log;
import de.ub0r.android.nocloudshare.adapter.ShareItemAdapter;
import de.ub0r.android.nocloudshare.model.ShareItem;
import de.ub0r.android.nocloudshare.model.ShareItemContainer;
import de.ub0r.android.nocloudshare.views.RecyclerItemClickListener;

public class ShareListActivity extends ActionBarActivity
        implements RecyclerItemClickListener.OnItemClickListener,
        ActionMode.Callback {

    private static final String TAG = "ShareListActivity";

    private static final int REQUEST_PICK = 1;

    private static final String PREF_SHOWN_INTRO = "intro_has_been_shown";

    private ShareItemContainer mContainer;

    private String mSelectedHash;

    private int mSelectedItem;

    private boolean mIsTwoPane;

    private boolean mOnCreateRun = false;

    private ActionMode mActionMode;

    private ShareItemAdapter mAdapter;

    @InjectView(android.R.id.list)
    RecyclerView mListView;

    @InjectView(android.R.id.empty)
    View mEmptyView;

    @InjectView(R.id.ads)
    AdView mAdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_list);
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

        mListView.setHasFixedSize(true);
        mListView.setLayoutManager(new LinearLayoutManager(this));
        mContainer = ShareItemContainer.getInstance(this);
        mIsTwoPane = getResources().getBoolean(R.bool.activity_share_list_two_pane);
        if (savedInstanceState != null) {
            mSelectedHash = savedInstanceState.getString("mSelectedHash", null);
            mSelectedItem = savedInstanceState.getInt("mSelectedItem", -1);
        } else {
            mSelectedHash = null;
            mSelectedItem = -1;
        }
        mOnCreateRun = true;

        mAdapter = new ShareItemAdapter(this, mContainer);
        mListView.setAdapter(mAdapter);
        mListView.addOnItemTouchListener(new RecyclerItemClickListener(this, mListView, this));

        if (savedInstanceState == null) {
            String action = getIntent().getAction();
            if (Intent.ACTION_VIEW.equals(action) || Intent.ACTION_SEND.equals(action)
                    || Intent.ACTION_SEND_MULTIPLE.equals(action)) {
                Intent intent = new Intent(getIntent());
                showItem(intent);
            } else {
                SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(this);
                if (!p.getBoolean(PREF_SHOWN_INTRO, false)) {
                    startActivity(new Intent(this, IntroActivity.class));
                    p.edit().putBoolean(PREF_SHOWN_INTRO, true).apply();
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mAdView.resume();
        if (mOnCreateRun) {
            updateListViewVisibility();
            showSelectedItem();
            mOnCreateRun = false;
        } else {
            invalidateData();
        }
    }

    @Override
    protected void onPause() {
        mAdView.pause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mAdView.destroy();
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(@NotNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("mSelectedHash", mSelectedHash);
        outState.putInt("mSelectedItem", mSelectedItem);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.activity_share_list, menu);
        if (!mContainer.hasExpiredShares() || mIsTwoPane) {
            menu.removeItem(R.id.action_remove_expired);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_about:
                startActivity(new Intent(this, AboutActivity.class));
                return true;
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.action_remove_expired:
                removeExpired();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @OnClick(R.id.add_item)
    void onAddItemClick() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        startActivityForResult(i, REQUEST_PICK);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (REQUEST_PICK == requestCode && resultCode == RESULT_OK && data != null) {
            Log.d(TAG, "returned data: %s", data.getData());
            showItem(data);
        }
    }

    public void onItemClick(final View view, final int pos) {
        if (mActionMode == null) {
            showItem(pos);
        } else {
            mAdapter.toggleSelection(pos);
            onItemCheckedStateChanged(mActionMode);
        }
    }

    @Override
    public void onItemLongClick(final View view, final int pos) {
        if (mActionMode != null) {
            onItemClick(view, pos);
            return;
        }
        mActionMode = startSupportActionMode(this);
        mAdapter.toggleSelection(pos);
        onItemCheckedStateChanged(mActionMode);
    }

    private void onItemCheckedStateChanged(final ActionMode mode) {
        int c = mAdapter.getSelectedItemCount();
        if (c == 0) {
            mode.finish();
            return;
        }

        mode.setTitle(getString(R.string.action_mode_selected, c));

        List<ShareItem> items = mAdapter.getSelectedItems();
        boolean hasActive = false;
        boolean hasExpired = false;
        for (ShareItem item : items) {
            if (item.isExpired()) {
                hasExpired = true;
            } else {
                hasActive = true;
            }
            if (hasActive && hasExpired) {
                break;
            }
        }
        Menu menu = mode.getMenu();
        menu.findItem(R.id.action_extend).setVisible(hasExpired);
        menu.findItem(R.id.action_expire).setVisible(hasActive);
    }

    @Override
    public boolean onActionItemClicked(final ActionMode mode, final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_extend:
                extendSelectedItems();
                mode.finish();
            case R.id.action_expire:
                expireSelectedItems();
                mode.finish();
                return true;
            case R.id.action_remove:
                removeSelectedItems();
                mode.finish();
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean onCreateActionMode(final ActionMode mode, final Menu menu) {
        mAdapter.setInSelectionMode(true);
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.activity_share_list_actionmode, menu);
        return true;
    }

    @Override
    public void onDestroyActionMode(final ActionMode mode) {
        mAdapter.setInSelectionMode(false);
        mActionMode = null;
        mAdapter.setSelectedItem(mSelectedItem, true);
    }

    @Override
    public boolean onPrepareActionMode(final ActionMode mode, final Menu menu) {
        // nothing to do
        return false;
    }

    @OnClick(R.id.btn_intro)
    void onIntroClick() {
        startActivity(new Intent(this, IntroActivity.class));
    }

    private void setSelectedItem(final int pos) {
        mSelectedItem = pos;
        if (!mIsTwoPane) {
            return;
        }
        mAdapter.setSelectedItem(pos, true);
    }

    public void showItem(final int pos) {
        Log.d(TAG, "showItem(", pos, ")");
        ShareItem item = mContainer.get(pos);
        showItem(item);
        setSelectedItem(pos);
    }

    private void showItem(final ShareItem item) {
        Log.d(TAG, "showItem(", item.getHash(), ")");
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.putExtra(ShareActivity.EXTRA_HASH, item.getHash());
        showItem(intent);
    }

    private void showItem(final Intent intent) {
        intent.setClass(this, ShareActivity.class);
        mSelectedHash = intent.getStringExtra(ShareActivity.EXTRA_HASH);
        if (mIsTwoPane) {
            Fragment f = getActiveFragment();
            if (f != null && f instanceof ShareFragment) {
                // do not show already active fragments
                String activeHash = ((ShareFragment) f).getHash();
                if (mSelectedHash != null && mSelectedHash.equals(activeHash)) {
                    Log.d(TAG, "fragment is already active: ", activeHash);
                    return;
                }
            }
            f = ShareFragment.getInstance(intent);
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.container, f, "details");
            ft.commit();
            mSelectedHash = ((ShareFragment) f).getHash();
        } else {
            startActivity(intent);
        }

        if (mSelectedHash != null) {
            int pos = -1;
            int l = mContainer.size();
            for (int i = 0; i < l; ++i) {
                if (mSelectedHash.equals(mContainer.get(i).getHash())) {
                    pos = i;
                    break;
                }
            }
            if (mSelectedItem >= 0 && mSelectedItem != pos) {
                setSelectedItem(pos);
            }
        }
    }

    private void showSelectedItem() {
        if (!mIsTwoPane) {
            return;
        }
        Log.d(TAG, "showSelectedItem(): ", mSelectedHash);
        int l = mContainer.size();
        Log.d(TAG, "#mContainer: ", l);
        if (mSelectedHash == null) {
            Fragment f = getActiveFragment();
            if (f != null && f instanceof ShareFragment) {
                mSelectedHash = ((ShareFragment) f).getHash();
                Log.d(TAG, "fragment's hash: ", mSelectedHash);
            }
        }
        if (mSelectedHash != null && l > 0) {
            boolean found = false;
            for (int i = 0; i < l; ++i) {
                if (mSelectedHash.equals(mContainer.get(i).getHash())) {
                    showItem(i);
                    found = true;
                    break;
                }
            }
            if (!found) {
                Log.w(TAG, "selected item not found!");
                showItem(0);
            }
        } else if (l > 0) {
            Log.d(TAG, "show first item");
            showItem(0);
        } else {
            hideItem();
        }
    }

    private Fragment getActiveFragment() {
        FragmentManager fm = getFragmentManager();
        return fm.findFragmentByTag("details");
    }

    private void hideItem() {
        if (!mIsTwoPane) {
            return;
        }
        Log.d(TAG, "hideItem()");
        Fragment f = getActiveFragment();
        if (f != null) {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.remove(f);
            ft.commitAllowingStateLoss();
        }
    }

    private void removeExpired() {
        List<ShareItem> list = mContainer.getExpiredShares();
        mContainer.removeAll(list);
        Toast.makeText(this, getString(R.string.removed_num, list.size()), Toast.LENGTH_LONG)
                .show();
        mContainer.persist(this);
        invalidateData();
    }

    private void extendSelectedItems() {
        List<ShareItem> list = mAdapter.getSelectedItems();
        for (ShareItem item : list) {
            item.extend();
        }
        Toast.makeText(this, getString(R.string.extended_num, list.size()), Toast.LENGTH_LONG)
                .show();
        mContainer.persist(this);
        invalidateData();
    }

    private void expireSelectedItems() {
        List<ShareItem> list = mAdapter.getSelectedItems();
        for (ShareItem item : list) {
            item.expire();
        }
        Toast.makeText(this, getString(R.string.expired_num, list.size()), Toast.LENGTH_LONG)
                .show();
        mContainer.persist(this);
        invalidateData();
    }

    private void removeSelectedItems() {
        List<ShareItem> list = mAdapter.getSelectedItems();
        mContainer.removeAll(list);
        Toast.makeText(this, getString(R.string.removed_num, list.size()), Toast.LENGTH_LONG)
                .show();
        mContainer.persist(this);
        invalidateData();
    }

    private void updateListViewVisibility() {
        int c = mContainer.size();
        if (c == 0) {
            mListView.setVisibility(View.INVISIBLE);
            mEmptyView.setVisibility(View.VISIBLE);
        } else {
            mListView.setVisibility(View.VISIBLE);
            mEmptyView.setVisibility(View.GONE);
        }

    }

    public void invalidateData() {
        Log.d(TAG, "invalidateData()");
        mAdapter.notifyDataSetChanged();
        invalidateOptionsMenu();
        updateListViewVisibility();
        showSelectedItem();
        HttpService.startService(this);
    }
}
