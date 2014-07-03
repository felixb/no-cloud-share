package de.ub0r.android.nocloudshare;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.android.volley.toolbox.Volley;

import org.jetbrains.annotations.NotNull;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ShareActionProvider;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
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

    @InjectView(R.id.shade)
    View mShadeView;

    private boolean mState;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent i = getIntent();
        Log.d(TAG, "intent: ", i);
        mViewOnly = Intent.ACTION_VIEW.equals(i.getAction());
        mState = savedInstanceState != null && savedInstanceState.getBoolean("mState");

        mCache = BitmapLruCache.getDefaultBitmapLruCache(this);
        mQueue = Volley.newRequestQueue(this);
        mLoader = new ImageLoader(mQueue, mCache);
        mFormat = android.text.format.DateFormat.getTimeFormat(this);
        mContainer = ShareItemContainer.getInstance(this);

        setContentView(R.layout.activity_share);
        ButterKnife.inject(this);
        //noinspection ConstantConditions
        getActionBar().setDisplayHomeAsUpEnabled(true);

        // get list of shared items from intent
        // it's mostly only one item
        List<ShareItem> list = handleIntent(i);
        if (list == null) {
            return;
        }

        // create thumbnails
        createThumbnails(list);

        mItem = list.get(0);
        if (!mViewOnly) {
            mContainer.persist(this);
            HttpService.startService(this);
        }

        getActionBar().setSubtitle(mItem.getName());

        updateViews();
    }

    private List<ShareItem> handleIntent(final Intent i) {
        String action = i.getAction();
        String mimeType = i.getType();

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
            return null;
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
        return list;
    }

    private void createThumbnails(final List<ShareItem> list) {
        for (ShareItem item : list) {
            String thumb = item.getThmubnailName();
            if (thumb == null) {
                continue;
            }
            if (mCache.getBitmap(thumb) != null) {
                continue;
            }
            createThumbnail(item, thumb);
        }
    }

    private void createThumbnail(final ShareItem item, final String thumb) {
        try {
            // get orig image size
            InputStream is = getContentResolver().openInputStream(item.getUri());
            if (is == null) {
                return;
            }
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, null, o);

            // calculate scale
            int scale = 1;
            if (o.outHeight > ShareItem.THUMBNAIL_SIZE
                    || o.outWidth > ShareItem.THUMBNAIL_SIZE) {
                scale = (int) Math.pow(2, (int) Math.round(Math.log(ShareItem.THUMBNAIL_SIZE
                        / (double) Math.max(o.outHeight, o.outWidth)) / Math.log(0.5)));
            }
            is.close();

            // decode image with scale
            is = getContentResolver().openInputStream(item.getUri());
            o = new BitmapFactory.Options();
            o.inSampleSize = scale;
            Bitmap b = BitmapFactory.decodeStream(is, null, o);
            is.close();

            // put image into cache
            mCache.putBitmap(thumb, b);
        } catch (IOException e) {
            Log.e(TAG, "unable to create thumbnail", e);
        }
    }

    @Override
    protected void onSaveInstanceState(@NotNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("mState", mState);
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

        // start with desired animation state
        if (mState) {
            float scale = getImageScale();
            mUrlImageView.setScaleX(scale);
            mUrlImageView.setScaleY(scale);
            mShadeView.setAlpha(1f);
        } else {
            mUrlImageView.setScaleX(1f);
            mUrlImageView.setScaleY(1f);
            mShadeView.setAlpha(0f);
        }
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
        AnimatorSet set = new AnimatorSet();
        if (mState) {
            // animate close view
            set.playTogether(
                    ObjectAnimator.ofFloat(mShadeView, View.ALPHA, 0f),
                    ObjectAnimator.ofFloat(mUrlImageView, View.SCALE_X, 1f),
                    ObjectAnimator.ofFloat(mUrlImageView, View.SCALE_Y, 1f)
            );
        } else {
            // animate open view
            float scale = getImageScale();
            set.playTogether(
                    ObjectAnimator.ofFloat(mShadeView, View.ALPHA, 1f),
                    ObjectAnimator.ofFloat(mUrlImageView, View.SCALE_X, scale),
                    ObjectAnimator.ofFloat(mUrlImageView, View.SCALE_Y, scale)
            );
        }
        set.start();
        mState ^= true;
    }

    private float getImageScale() {
        float origSize = getResources().getDimensionPixelSize(R.dimen.share_barcode_size);
        float maxWidth;
        float maxHeight;
        Display display = getWindowManager().getDefaultDisplay();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            Point size = new Point();
            display.getSize(size);
            maxWidth = size.x;
            maxHeight = size.y;
        } else {
            //noinspection deprecation
            maxWidth = display.getWidth();
            //noinspection deprecation
            maxHeight = display.getHeight();
        }

        return Math.min(
                Math.min(maxWidth / origSize, maxHeight / origSize),
                3);
    }
}
