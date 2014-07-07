package de.ub0r.android.nocloudshare;

import org.jetbrains.annotations.NotNull;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import de.ub0r.android.logg0r.Log;
import de.ub0r.android.nocloudshare.http.BitmapLruCache;
import de.ub0r.android.nocloudshare.model.ShareItem;
import de.ub0r.android.nocloudshare.model.ShareItemContainer;
import de.ub0r.android.nocloudshare.views.CheckableRelativeLayout;

public class ShareListActivity extends ListActivity implements AdapterView.OnItemClickListener {

    class ShareItemAdapter extends ArrayAdapter<ShareItem> {

        class ViewHolder {

            @InjectView(R.id.background)
            CheckableRelativeLayout backgroundView;

            @InjectView(R.id.item_name)
            TextView nameTextView;

            @InjectView(R.id.item_mime)
            TextView mimeTextView;

            @InjectView(R.id.item_creation)
            TextView creationTextView;

            @InjectView(R.id.item_expiration)
            TextView expirationTextView;

            @InjectView(R.id.item_thumbnail)
            ImageView thumbnailImageView;

            ViewHolder(final View view) {
                ButterKnife.inject(this, view);
            }
        }

        private static final int LAYOUT = R.layout.item_share;

        private final LayoutInflater mInflater;

        private final DateFormat mFormat;

        public ShareItemAdapter(final Context context, final List<ShareItem> objects) {
            super(context, LAYOUT, objects);
            mInflater = LayoutInflater.from(context);
            mFormat = android.text.format.DateFormat.getTimeFormat(context);
        }

        @Override
        public View getView(final int position, final View convertView, final ViewGroup parent) {
            View v;
            ViewHolder h;
            if (convertView == null) {
                v = mInflater.inflate(LAYOUT, parent, false);
                assert v != null;
                h = new ViewHolder(v);
                v.setTag(h);
            } else {
                v = convertView;
                h = (ViewHolder) v.getTag();
            }

            ShareItem item = getItem(position);

            h.nameTextView.setText(item.getName());
            h.mimeTextView.setText(item.getMimeType());
            Date creation = new Date(item.getCreation());
            h.creationTextView.setText(getContext().getString(R.string.creation_ts,
                    mFormat.format(creation)));
            Date expiration = new Date(item.getExpiration());
            h.expirationTextView.setText(
                    getContext().getString(R.string.expiration_ts, mFormat.format(expiration)));
            h.expirationTextView.setTextColor(getContext().getResources()
                    .getColor(item.isExpired() ? R.color.expired : R.color.not_expired));
            // selector background
            h.backgroundView.setBackgroundResource(mIsTwoPane && position == mSelectedItem
                    ? R.drawable.apptheme_list_selector_holo_light_selected
                    : R.drawable.apptheme_list_selector_holo_light);

            String thumb = item.getThumbnailName();
            if (thumb == null) {
                h.thumbnailImageView.setVisibility(View.GONE);
            } else {
                BitmapLruCache cache = BitmapLruCache
                        .getDefaultBitmapLruCache(getContext());
                Bitmap b = cache.getBitmap(thumb);
                if (b == null) {
                    h.thumbnailImageView.setVisibility(View.GONE);
                } else {
                    h.thumbnailImageView.setVisibility(View.VISIBLE);
                    h.thumbnailImageView.setImageBitmap(b);
                }
            }

            return v;
        }
    }

    private static final String TAG = "ShareListActivity";

    private static final String PREF_SHOWN_INTRO = "intro_has_been_shown";

    private ShareItemContainer mContainer;

    private String mSelectedHash;

    private int mSelectedItem;

    private boolean mIsTwoPane;

    private boolean mOnCreateRun = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_list);
        ButterKnife.inject(this);
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

        setListAdapter(new ShareItemAdapter(this, mContainer));
        getListView().setOnItemClickListener(this);
        getListView().setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            @Override
            public void onItemCheckedStateChanged(final ActionMode mode, final int position,
                    final long id, final boolean checked) {
                mode.setTitle(getString(R.string.action_mode_selected,
                        getListView().getCheckedItemCount()));

                List<ShareItem> items = getCheckedItems();
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
                MenuInflater inflater = mode.getMenuInflater();
                inflater.inflate(R.menu.activity_share_list_actionmode, menu);
                return true;
            }

            @Override
            public void onDestroyActionMode(final ActionMode mode) {
                // nothing to do
            }

            @Override
            public boolean onPrepareActionMode(final ActionMode mode, final Menu menu) {
                // nothing to do
                return false;
            }
        });

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
                    p.edit().putBoolean(PREF_SHOWN_INTRO, true).commit();
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mOnCreateRun) {
            showSelectedItem();
            mOnCreateRun = false;
        } else {
            invalidateData();
        }
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

    @Override
    public void onItemClick(final AdapterView<?> adapter, final View view, final int pos,
            final long id) {
        showItem(pos);
    }

    @OnClick(R.id.btn_intro)
    void onIntroClick() {
        startActivity(new Intent(this, IntroActivity.class));
    }

    private void setSelectedItemBackground(final int pos) {
        if (!mIsTwoPane) {
            mSelectedItem = pos;
            return;
        }
        ListView lv = getListView();
        int l = lv.getCount();
        if (mSelectedItem >= 0 && mSelectedItem < l) {
            lv.getChildAt(mSelectedItem)
                    .setBackgroundResource(R.drawable.apptheme_list_selector_holo_light);
        }
        if (pos >= 0 && pos < l) {
            lv.getChildAt(pos).setBackgroundResource(
                    R.drawable.apptheme_list_selector_holo_light_selected);
        }
        mSelectedItem = pos;
    }

    public void showItem(final int pos) {
        Log.d(TAG, "showItem(", pos, ")");
        ShareItem item = mContainer.get(pos);
        showItem(item);

        setSelectedItemBackground(pos);
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
                if (mSelectedHash.equals(activeHash)) {
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
                setSelectedItemBackground(pos);
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
        List<ShareItem> list = getCheckedItems();
        for (ShareItem item : list) {
            item.extend();
        }
        Toast.makeText(this, getString(R.string.extended_num, list.size()), Toast.LENGTH_LONG)
                .show();
        mContainer.persist(this);
        invalidateData();
    }

    private void expireSelectedItems() {
        List<ShareItem> list = getCheckedItems();
        for (ShareItem item : list) {
            item.expire();
        }
        Toast.makeText(this, getString(R.string.expired_num, list.size()), Toast.LENGTH_LONG)
                .show();
        mContainer.persist(this);
        invalidateData();
    }

    private void removeSelectedItems() {
        List<ShareItem> list = getCheckedItems();
        mContainer.removeAll(list);
        Toast.makeText(this, getString(R.string.removed_num, list.size()), Toast.LENGTH_LONG)
                .show();
        mContainer.persist(this);
        invalidateData();
    }

    private List<ShareItem> getCheckedItems() {
        SparseBooleanArray checked = getListView().getCheckedItemPositions();
        int l = checked.size();
        ArrayList<ShareItem> list = new ArrayList<ShareItem>(l);
        for (int i = 0; i < l; ++i) {
            if (checked.valueAt(i)) {
                list.add(mContainer.get(checked.keyAt(i)));
            }
        }
        return list;
    }

    public void invalidateData() {
        Log.d(TAG, "invalidateData()");
        ((ShareItemAdapter) getListAdapter()).notifyDataSetInvalidated();
        invalidateOptionsMenu();
        showSelectedItem();
        HttpService.startService(this);
    }
}
