package de.ub0r.android.nocloudshare;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.android.volley.toolbox.Volley;

import org.jetbrains.annotations.NotNull;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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
import de.ub0r.android.nocloudshare.views.MeasuringRelativeLayout;

/**
 * @author flx
 */
public class ShareFragment extends Fragment {

    private static final String TAG = "ShareFragment";

    // TODO customize width
    public static final String BARCODE_URL
            = "https://chart.googleapis.com/chart?cht=qr&chs=400x400&chl=";

    private BitmapLruCache mCache;

    private ImageLoader mLoader;

    private DateFormat mFormat;

    private boolean mViewOnly;

    private ShareItemContainer mContainer;

    private ShareItem mItem;

    private String mBaseUrl;

    private Intent mOpenIntent;

    private boolean mState;

    private Intent mIntent;

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

    public static ShareFragment getInstance(final Intent intent) {
        Bundle args = new Bundle();
        args.putParcelable("intent", intent);
        ShareFragment f = new ShareFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mIntent = getArguments().getParcelable("intent");
        Log.d(TAG, "intent: ", mIntent);
        mViewOnly = Intent.ACTION_VIEW.equals(mIntent.getAction());
        mBaseUrl = HttpService.getBaseUrl(getActivity());
        mState = savedInstanceState != null && savedInstanceState.getBoolean("mState");

        mCache = BitmapLruCache.getDefaultBitmapLruCache(getActivity());
        RequestQueue queue = Volley.newRequestQueue(getActivity());
        mLoader = new ImageLoader(queue, mCache);
        mFormat = android.text.format.DateFormat.getTimeFormat(getActivity());
        mContainer = ShareItemContainer.getInstance(getActivity());
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_share, container, false);
        ButterKnife.inject(this, v);

        // get list of shared items from intent
        // it's mostly only one item
        List<ShareItem> list = handleIntent(mIntent);
        if (list == null) {
            return v;
        }

        // create thumbnails
        createThumbnails(list);

        mItem = list.get(0);
        if (!mViewOnly) {
            mContainer.persist(getActivity());
            invalidateData(false);
        }

        if (getActivity() instanceof ShareActivity) {
            ShareActivity a = (ShareActivity) getActivity();
            a.getSupportActionBar().setSubtitle(mItem.getName());
        }

        // trigger updateViews after measure layout again
        ((MeasuringRelativeLayout) mUrlImageView.getParent())
                .registerOnMeasureListener(new Runnable() {
                    @Override
                    public void run() {
                        updateViews();
                    }
                });

        setHasOptionsMenu(true);

        return v;
    }

    private List<ShareItem> handleIntent(final Intent i) {
        String action = i.getAction();
        String mimeType = i.getType();

        ArrayList<ShareItem> list = new ArrayList<>();
        long expirationPeriod = SettingsActivity
                .getExpirationPeriod(PreferenceManager.getDefaultSharedPreferences(getActivity()));

        if (mViewOnly) {
            list.add(mContainer.find(i.getStringExtra(ShareActivity.EXTRA_HASH)));
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
                item.setInfos(getActivity());
                item.setExpireIn(expirationPeriod);
                list.add(item);
            }
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            List<Uri> uris = i.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            if (uris != null) {
                for (Uri u : uris) {
                    ShareItem item = mContainer.add(u, mimeType);
                    item.setInfos(getActivity());
                    item.setExpireIn(expirationPeriod);
                    list.add(item);
                }
            }
        }

        if (list.size() == 0) {
            Log.e(TAG, "#list: 0, intent: ", mIntent);
            Toast.makeText(getActivity(), R.string.error_unknown, Toast.LENGTH_LONG).show();
            finishOrInvalidate();
            return null;
        }

        if (list.size() > 1) {
            // add index for collection
            ShareItem item = new ShareItem("index.html",
                    ShareItemContainer.buildIndex(getActivity(), list, mBaseUrl), "text/html");
            item.setExpireIn(expirationPeriod);
            list.add(0, item);
            mContainer.add(1 + mContainer.size() - list.size(), item);
        }
        return list;
    }

    private void createThumbnails(final List<ShareItem> list) {
        Log.d(TAG, "createThumbnails(#", list.size(), ")");
        for (ShareItem item : list) {
            String thumb = item.getThumbnailName();
            if (thumb == null) {
                Log.d(TAG, "thumb == null");
                continue;
            }
            if (mCache.contains(thumb)) {
                Log.d(TAG, "thumbnail already present: ", thumb);
                continue;
            }
            createThumbnail(item, thumb);
        }
    }

    private void createThumbnail(final ShareItem item, final String thumb) {
        Log.d(TAG, "createThumbnail(", item, ", ", thumb, ")");
        try {
            // get orig image size
            InputStream is = getActivity().getContentResolver().openInputStream(item.getUri());
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
            is = getActivity().getContentResolver().openInputStream(item.getUri());
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
    public void onSaveInstanceState(@NotNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("mState", mState);
    }

    private void updateViews() {
        String url = mBaseUrl + mItem.getExternalPath();
        Log.i(TAG, "share url: ", url);
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
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.activity_share, menu);
        // update remove/expire
        if (mItem == null) {
            menu.removeItem(R.id.action_expire);
            menu.removeItem(R.id.action_remove);
            return;
        }
        if (mItem.isExpired()) {
            menu.removeItem(R.id.action_expire);
        } else {
            menu.removeItem(R.id.action_remove);
        }

        // update open
        if (mItem.getUri() != null) {
            Intent intent = new Intent(Intent.ACTION_VIEW, mItem.getUri());
            if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                mOpenIntent = intent;
            } else {
                mOpenIntent = null;
                menu.removeItem(R.id.action_open);
            }
        } else {
            mOpenIntent = null;
            menu.removeItem(R.id.action_open);
        }

        // update share
        MenuItem item = menu.findItem(R.id.action_share);
        assert item != null;
        ShareActionProvider sap = (ShareActionProvider) MenuItemCompat.getActionProvider(item);
        assert sap != null;
        Intent intent = new Intent(Intent.ACTION_SEND);
        String url = mBaseUrl + mItem.getExternalPath();
        intent.putExtra(Intent.EXTRA_TEXT, url);
        intent.setType("text/plain");
        sap.setShareIntent(intent);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (getActivity() instanceof ShareActivity) {
                    Intent i = new Intent(getActivity(), ShareListActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(i);
                    finishOrInvalidate();
                    return true;
                } else {
                    return false;
                }
            case R.id.action_open:
                startActivity(mOpenIntent);
                return true;
            case R.id.action_extend:
                mItem.extend();
                mContainer.persist(getActivity());
                invalidateData(true);
                Toast.makeText(getActivity(), R.string.extended, Toast.LENGTH_LONG).show();
                return true;
            case R.id.action_expire:
                mItem.expire();
                mContainer.persist(getActivity());
                invalidateData(true);
                Toast.makeText(getActivity(), R.string.expired, Toast.LENGTH_LONG).show();
                return true;
            case R.id.action_remove:
                mContainer.remove(mItem);
                mContainer.persist(getActivity());
                Toast.makeText(getActivity(), R.string.removed, Toast.LENGTH_LONG).show();
                finishOrInvalidate();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @OnClick(R.id.url)
    void onUrlClick() {
        ClipboardManager cm = (ClipboardManager) getActivity().getSystemService(
                Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(
                ClipData.newPlainText(mItem.getName(), mBaseUrl + mItem.getExternalPath()));
        Toast.makeText(getActivity(), R.string.copied_to_clipboard, Toast.LENGTH_LONG).show();
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
        int maxWidth;
        int maxHeight;
        MeasuringRelativeLayout parentView = (MeasuringRelativeLayout) mUrlImageView.getParent();
        maxWidth = parentView.getMeasuredWidth();
        maxHeight = parentView.getMeasuredHeight();
        if (maxWidth < origSize || maxHeight < origSize) {
            return 1f;
        }

        return Math.min(
                Math.min(maxWidth / origSize, maxHeight / origSize),
                3);
    }

    private void finishOrInvalidate() {
        if (getActivity() instanceof ShareActivity) {
            getActivity().finish();
        } else if (getActivity() instanceof ShareListActivity) {
            ((ShareListActivity) getActivity()).invalidateData();
        } else {
            throw new IllegalStateException("unknown activity: " + getActivity().getClass());
        }
    }

    private void invalidateData(final boolean updateViews) {
        if (updateViews) {
            updateViews();
        }
        if (getActivity() instanceof ShareActivity) {
            getActivity().invalidateOptionsMenu();
            HttpService.startService(getActivity());
        } else if (getActivity() instanceof ShareListActivity) {
            ((ShareListActivity) getActivity()).invalidateData();
        } else {
            throw new IllegalStateException("unknown activity: " + getActivity().getClass());
        }
    }

    public String getHash() {
        return mItem == null ? null : mItem.getHash();
    }
}
