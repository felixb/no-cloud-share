package de.ub0r.android.nocloudshare;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
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
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.ub0r.android.nocloudshare.http.BitmapLruCache;
import de.ub0r.android.nocloudshare.model.ShareItem;
import de.ub0r.android.nocloudshare.model.ShareItemContainer;

public class ShareListActivity extends ListActivity implements AdapterView.OnItemClickListener {

    class ShareItemAdapter extends ArrayAdapter<ShareItem> {

        class ViewHolder {

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

            String thumb = item.getThmubnailName();
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

    private ShareItemContainer mContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_list);
        mContainer = ShareItemContainer.getInstance(this);

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

        HttpService.startService(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        notifyDataSetInvalidated();
    }

    private void notifyDataSetInvalidated() {
        ((ShareItemAdapter) getListAdapter()).notifyDataSetInvalidated();
        invalidateOptionsMenu();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.activity_share_list, menu);
        if (!mContainer.hasExpiredShares()) {
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
        ShareItem item = mContainer.get(pos);
        Intent i = new Intent(Intent.ACTION_VIEW, null, this, ShareActivity.class);
        i.putExtra(ShareActivity.EXTRA_HASH, item.getHash());
        startActivity(i);
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

    private void invalidateData() {
        notifyDataSetInvalidated();
        HttpService.startService(this);
    }
}
