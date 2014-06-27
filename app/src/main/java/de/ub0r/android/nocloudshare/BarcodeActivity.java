package de.ub0r.android.nocloudshare;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.android.volley.toolbox.Volley;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import de.ub0r.android.nocloudshare.http.BitmapLruCache;
import de.ub0r.android.nocloudshare.model.ShareItem;
import de.ub0r.android.nocloudshare.model.ShareItemContainer;

public class BarcodeActivity extends Activity {

    @InjectView(R.id.url_barcode)
    NetworkImageView mUrlImageView;

    private RequestQueue mQueue;

    private BitmapLruCache mCache;

    private ImageLoader mLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barcode);
        ButterKnife.inject(this);
        //noinspection ConstantConditions
        getActionBar().setDisplayHomeAsUpEnabled(true);

        mCache = BitmapLruCache.getDefaultBitmapLruCache(this);
        mQueue = Volley.newRequestQueue(this);
        mLoader = new ImageLoader(mQueue, mCache);

        ShareItemContainer container = ShareItemContainer.getInstance(this);
        ShareItem item = container.find(getIntent().getStringExtra(ShareActivity.EXTRA_HASH));
        String baseUrl = HttpService.getBaseUrl(this);
        String url = baseUrl + item.getExternalPath();
        String bcUrl = ShareActivity.BARCODE_URL + Uri.encode(url);
        mUrlImageView.setImageUrl(bcUrl, mLoader);
        getActionBar().setSubtitle(item.getName());
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

    @OnClick(R.id.url_barcode)
    void onBarcodeClick() {
        finish();
    }
}
