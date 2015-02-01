package de.ub0r.android.nocloudshare.adapter;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.ub0r.android.logg0r.Log;
import de.ub0r.android.nocloudshare.BuildConfig;
import de.ub0r.android.nocloudshare.R;
import de.ub0r.android.nocloudshare.http.BitmapLruCache;
import de.ub0r.android.nocloudshare.model.ShareItem;

/**
 * @author flx
 */
public class ShareItemAdapter extends RecyclerView.Adapter<ShareItemAdapter.ViewHolder> {

    static class ViewHolder extends RecyclerView.ViewHolder {

        @InjectView(R.id.background)
        RelativeLayout backgroundView;

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
            super(view);
            ButterKnife.inject(this, view);
        }
    }

    private static final String TAG = "ShareItemAdapter";

    private static final int LAYOUT = R.layout.item_share;

    private final DateFormat mFormat;

    private final String mCreationTs, mExpirationTs;

    private final int mExpiredTextColor, mNotExpiredTextColor;

    private final BitmapLruCache mCache;

    private final Drawable mBackGround;

    private final List<ShareItem> mDataSet;

    private final SparseBooleanArray mSelectedItems = new SparseBooleanArray();

    private boolean mInSelectionMode;

    public ShareItemAdapter(final Context context, final List<ShareItem> objects) {
        mFormat = android.text.format.DateFormat.getTimeFormat(context);
        mDataSet = objects;
        mCreationTs = context.getString(R.string.creation_ts);
        mExpirationTs = context.getString(R.string.expiration_ts);
        mExpiredTextColor = context.getResources().getColor(R.color.expired);
        mNotExpiredTextColor = context.getResources().getColor(R.color.not_expired);
        mCache = BitmapLruCache.getDefaultBitmapLruCache(context);

        int[] attrs = new int[]{R.attr.selectableItemBackground};
        TypedArray ta = context.obtainStyledAttributes(attrs);
        mBackGround = ta.getDrawable(0);
        ta.recycle();
    }

    @Override
    public ViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(LAYOUT, parent, false);
        return new ViewHolder(v);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onBindViewHolder(final ViewHolder h, final int pos) {
        ShareItem item = mDataSet.get(pos);

        h.nameTextView.setText(item.getName());
        h.mimeTextView.setText(item.getMimeType());
        Date creation = new Date(item.getCreation());
        h.creationTextView.setText(String.format(mCreationTs, mFormat.format(creation)));
        Date expiration = new Date(item.getExpiration());
        h.expirationTextView.setText(String.format(mExpirationTs, mFormat.format(expiration)));
        h.expirationTextView
                .setTextColor(item.isExpired() ? mExpiredTextColor : mNotExpiredTextColor);
        h.backgroundView.setActivated(mSelectedItems.get(pos, false));
        if (mInSelectionMode || mSelectedItems.get(pos, false)) {
            h.backgroundView.setBackgroundResource(R.drawable.selector_list_item);
        } else if (BuildConfig.VERSION_CODE >= Build.VERSION_CODES.JELLY_BEAN) {
            h.backgroundView.setBackground(mBackGround);
        } else {
            //noinspection deprecation
            h.backgroundView.setBackgroundDrawable(mBackGround);
        }

        String thumb = item.getThumbnailName();
        if (thumb == null) {
            h.thumbnailImageView.setVisibility(View.GONE);
        } else {
            Bitmap b = mCache.getBitmap(thumb);
            if (b == null) {
                h.thumbnailImageView.setVisibility(View.GONE);
            } else {
                h.thumbnailImageView.setVisibility(View.VISIBLE);
                h.thumbnailImageView.setImageBitmap(b);
            }
        }
    }

    @Override
    public int getItemCount() {
        return mDataSet.size();
    }

    public void setSelectedItem(final int pos, boolean selected) {
        if (pos < 0 || pos >= mDataSet.size()) {
            Log.e(TAG, "setSelectedItem(%d): invalid index", pos);
            return;
        }
        if (!mInSelectionMode) {
            // disable all selected items when not in CAB mode
            int l = mSelectedItems.size();
            for (int i = 0; i < l; i++) {
                int p = mSelectedItems.keyAt(i);
                mSelectedItems.delete(p);
                notifyItemChanged(p);
            }
        }
        if (selected) {
            mSelectedItems.put(pos, true);
        } else {
            mSelectedItems.delete(pos);
        }
        notifyItemChanged(pos);
    }

    public void toggleSelection(final int pos) {
        setSelectedItem(pos, !mSelectedItems.get(pos, false));
    }

    public void clearSelections() {
        mSelectedItems.clear();
        notifyDataSetChanged();
    }

    public int getSelectedItemCount() {
        return mSelectedItems.size();
    }

    public List<ShareItem> getSelectedItems() {
        List<ShareItem> items = new ArrayList<>(mSelectedItems.size());
        for (int i = 0; i < mSelectedItems.size(); i++) {
            items.add(mDataSet.get(mSelectedItems.keyAt(i)));
        }
        return items;
    }

    public void setInSelectionMode(final boolean mode) {
        mInSelectionMode = mode;
        clearSelections();
    }
}
