package de.ub0r.android.nocloudshare;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.android.volley.toolbox.Volley;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ShareActionProvider;
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

public class ShareActivity extends Activity {

    private static final String TAG = "ShareActivity";

    public static final String EXTRA_HASH = "hash";

    // TODO customize width
    public static final String BARCODE_URL
            = "https://chart.googleapis.com/chart?cht=qr&chs=400x400&chl=";

    private RequestQueue mQueue;

    private BitmapLruCache mCache;

    private ImageLoader mLoader;

    private DateFormat mFormat;

    private boolean mViewOnly;

    private ShareItemContainer mContainer;

    private ShareItem mItem;

    private String mBaseUrl;

    @InjectView(R.id.url)
    TextView mUrlTextView;

    @InjectView(R.id.url_barcode)
    NetworkImageView mUrlImageView;

    @InjectView(R.id.item_creation)
    TextView mCreationTextView;

    @InjectView(R.id.item_expiration)
    TextView mExpirationTextView;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent i = getIntent();
        Log.d(TAG, "intent: ", i);
        String action = i.getAction();
        mViewOnly = Intent.ACTION_VIEW.equals(action);

        mCache = BitmapLruCache.getDefaultBitmapLruCache(this);
        mQueue = Volley.newRequestQueue(this);
        mLoader = new ImageLoader(mQueue, mCache);
        mFormat = android.text.format.DateFormat.getTimeFormat(this);

        setContentView(R.layout.activity_share);
        ButterKnife.inject(this);
        //noinspection ConstantConditions
        getActionBar().setDisplayHomeAsUpEnabled(true);

        String mimeType = i.getType();
        mContainer = ShareItemContainer.getInstance(this);
        ArrayList<ShareItem> list = new ArrayList<ShareItem>();
        long expirationPeriod = SettingsActivity
                .getExpirationPeriod(PreferenceManager.getDefaultSharedPreferences(this));

        if (mViewOnly) {
            list.add(mContainer.find(i.getStringExtra(EXTRA_HASH)));
        } else if (i.getStringExtra(Intent.EXTRA_HTML_TEXT) != null) {
            String name = i.getStringExtra(Intent.EXTRA_SUBJECT);
            if (name == null) {
                name = "text.html";
            } else {
                name = name.replace(" ", "_"); // fixme
            }
            @SuppressLint("InlinedApi")
            ShareItem item = new ShareItem(name, i.getStringExtra(Intent.EXTRA_HTML_TEXT),
                    "text/html");
            item.setExpireIn(expirationPeriod);
            mContainer.add(item);
            list.add(item);
        } else if (i.getStringExtra(Intent.EXTRA_TEXT) != null) {
            String name = i.getStringExtra(Intent.EXTRA_SUBJECT);
            if (name == null) {
                name = "text.txt";
            } else {
                name = name.replace(" ", "_"); // fixme
            }
            ShareItem item = new ShareItem(name, i.getStringExtra(Intent.EXTRA_TEXT), mimeType);
            item.setExpireIn(expirationPeriod);
            mContainer.add(item);
            list.add(item);
        } else if (i.getData() != null) {
            ShareItem item = mContainer.add(i.getData(), mimeType);
            item.setExpireIn(expirationPeriod);
            list.add(item);
        } else if (Intent.ACTION_SEND.equals(action)) {
            Uri u = i.getParcelableExtra(Intent.EXTRA_STREAM);
            if (u != null) {
                ShareItem item = mContainer.add(u, mimeType);
                item.setInfos(this);
                item.setExpireIn(expirationPeriod);
                list.add(item);
            }
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            List<Uri> uris = i.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            if (uris != null) {
                for (Uri u : uris) {
                    ShareItem item = mContainer.add(u, mimeType);
                    item.setInfos(this);
                    item.setExpireIn(expirationPeriod);
                    list.add(item);
                }
            }
        }

        if (list.size() == 0) {
            Log.e(TAG, "#list: 0, intent: ", getIntent());
            Toast.makeText(this, R.string.error_unknown, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        mBaseUrl = HttpService.getBaseUrl(this);

        if (list.size() > 1) {
            // add index for collection
            ShareItem item = new ShareItem("index.html",
                    ShareItemContainer.buildIndex(this, list, mBaseUrl), "text/html");
            item.setExpireIn(expirationPeriod);
            list.set(0, item);
            mContainer.add(item);
        }

        mItem = list.get(0);
        if (!mViewOnly) {
            mContainer.persist(this);
            HttpService.startService(this);
        }

        getActionBar().setSubtitle(mItem.getName());

        updateViews();
    }

    private void updateViews() {
        String url = mBaseUrl + mItem.getExternalPath();
        Log.i(TAG, "new activity_share url: ", url);
        String bcUrl = BARCODE_URL + Uri.encode(url);

        mUrlTextView.setText(url);
        mUrlImageView.setImageUrl(bcUrl, mLoader);

        Date creation = new Date(mItem.getCreation());
        mCreationTextView.setText(this.getString(R.string.creation_ts,
                mFormat.format(creation)));
        Date expiration = new Date(mItem.getExpiration());
        mExpirationTextView.setText(
                this.getString(R.string.expiration_ts, mFormat.format(expiration)));
        mExpirationTextView.setTextColor(this.getResources()
                .getColor(mItem.isExpired() ? R.color.expired : R.color.not_expired));
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.activity_share, menu);
        if (mItem == null) {
            menu.removeItem(R.id.action_expire);
            menu.removeItem(R.id.action_remove);
            return true;
        }
        if (mItem.isExpired()) {
            menu.removeItem(R.id.action_expire);
        } else {
            menu.removeItem(R.id.action_remove);
        }
        MenuItem item = menu.findItem(R.id.action_share);
        assert item != null;
        ShareActionProvider sap = (ShareActionProvider) item.getActionProvider();
        assert sap != null;
        Intent intent = new Intent(Intent.ACTION_SEND);
        String url = mBaseUrl + mItem.getExternalPath();
        intent.putExtra(Intent.EXTRA_TEXT, url);
        intent.setType("text/plain");
        sap.setShareIntent(intent);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent i = new Intent(this, ShareListActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
                finish();
                return true;
            case R.id.action_extend:
                mItem.setExpireIn();
                mContainer.persist(this);
                updateViews();
                invalidateOptionsMenu();
                HttpService.startService(this);
                return true;
            case R.id.action_expire:
                mItem.setExpireIn(-1);
                mContainer.persist(this);
                updateViews();
                invalidateOptionsMenu();
                HttpService.startService(this);
                return true;
            case R.id.action_remove:
                mContainer.remove(mItem);
                mContainer.persist(this);
                HttpService.startService(this);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @OnClick(R.id.url)
    void onUrlClick() {
        ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        cm.setPrimaryClip(
                ClipData.newPlainText(mItem.getName(), mBaseUrl + mItem.getExternalPath()));
        Toast.makeText(this, R.string.copied_to_clipboard, Toast.LENGTH_LONG).show();
    }

    @OnClick(R.id.url_barcode)
    void onBarcodeClick() {
        Intent i = new Intent(this, BarcodeActivity.class);
        i.putExtra(EXTRA_HASH, mItem.getHash());
        startActivity(i);
    }
}
