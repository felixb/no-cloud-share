package de.ub0r.android.nocloudshare;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
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
                mContainer.removeExpired();
                mContainer.persist(this);
                notifyDataSetInvalidated();
                HttpService.startService(this);
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
}
