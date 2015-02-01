package de.ub0r.android.nocloudshare.model;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import de.ub0r.android.logg0r.Log;

/**
 * @author flx
 */
public class ShareItem {

    private static final String TAG = "ShareItem";

    private static final long EXTEND_PERIOD = 15 * 60 * 1000;

    public static final int THUMBNAIL_SIZE = 512;

    private final String mHash;

    private final Uri mUri;

    private final String mContent;

    private String mMimeType;

    private String mName;

    private final long mCreation;

    private long mExpiration;

    public ShareItem(final String name, final String content, final String mimeType) {
        if (content == null) {
            throw new NullPointerException("content must not be null");
        }
        mHash = genHash();
        mName = name;
        mUri = null;
        mContent = content;
        mMimeType = mimeType;
        mCreation = System.currentTimeMillis();
    }

    public ShareItem(final Uri uri, final String mimeType) {
        if (uri == null) {
            throw new NullPointerException("uri must not be null");
        }
        if (!"file".equals(uri.getScheme())
                && !"content".equals(uri.getScheme())) {
            throw new IllegalArgumentException("uri must be a local uri");
        }

        mHash = genHash();
        mName = uri.getLastPathSegment();
        mUri = uri;
        mContent = null;
        mMimeType = mimeType;
        mCreation = System.currentTimeMillis();
    }

    private String genHash() {
        return UUID.randomUUID().toString();
    }

    public String getHash() {
        return mHash;
    }

    public void setName(final String name) {
        mName = name;
    }

    public String getName() {
        return mName;
    }

    public Uri getUri() {
        return mUri;
    }

    public boolean hasContent() {
        return mContent != null;
    }

    public String getContent() {
        return mContent;
    }

    public String getMimeType() {
        return mMimeType;
    }

    public void setMimeType(final String type) {
        mMimeType = type;
    }

    public String getExternalPath() {
        return "/" + mHash + "/" + Uri.encode(getName());
    }

    public long getCreation() {
        return mCreation;
    }

    public long getExpiration() {
        return mExpiration;
    }

    public void setExpireIn(final long expiration) {
        mExpiration = System.currentTimeMillis() + expiration;
    }

    public void extend() {
        mExpiration = Math.max(System.currentTimeMillis(), mExpiration) + EXTEND_PERIOD;
    }

    public void expire() {
        mExpiration = System.currentTimeMillis() - 1L;
    }

    public boolean isExpired() {
        return mExpiration < System.currentTimeMillis();
    }

    private void getInfos(final Context context, final String colName, final String colTitle,
            final String colType) {
        assert mUri != null;
        List<String> l = new ArrayList<>();
        if (colName != null) {
            l.add(colName);
        }
        if (colTitle != null) {
            l.add(colTitle);
        }
        if (colType != null) {
            l.add(colType);
        }
        Cursor c = context.getContentResolver()
                .query(mUri, l.toArray(new String[l.size()]), null, null, null);
        if (c != null && c.moveToFirst()) {
            String name = null;
            String title = null;
            String mimeType = null;

            if (colName != null) {
                name = c.getString(c.getColumnIndex(colName));
            }
            if (colTitle != null) {
                title = c.getString(c.getColumnIndex(colTitle));
            }
            if (colType != null) {
                mimeType = c.getString(c.getColumnIndex(colType));
            }

            if (name != null) {
                setName(name);
            } else if (title != null) {
                setName(title);
            }
            if (mimeType != null) {
                setMimeType(mimeType);
            }
        }
        if (c != null) {
            c.close();
        }
    }

    public void setInfos(final Context context) {
        if (mUri == null) {
            return;
        }
        try {
            getInfos(context, MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.TITLE,
                    MediaStore.MediaColumns.MIME_TYPE);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "failed fetching meta data", e);
            try {
                getInfos(context, MediaStore.MediaColumns.DISPLAY_NAME, null,
                        MediaStore.MediaColumns.MIME_TYPE);
            } catch (IllegalArgumentException e1) {
                Log.e(TAG, "failed fetching meta data", e1);
            }
        }
    }

    public String getThumbnailName() {
        if (mUri == null || mMimeType == null || !mMimeType.startsWith("image/")) {
            Log.d(TAG, "mUri: ", mUri);
            Log.d(TAG, "mMimeType: ", mMimeType);
            return null;
        }
        return "thumb_" + mHash;
    }

    @Override
    public String toString() {
        return getClass().getName() + ": " + mName;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == null || !(o instanceof ShareItem)) {
            return false;
        }
        ShareItem i = (ShareItem) o;
        return mUri != null && mUri.equals(i.getUri())
                || mContent != null && mContent.equals(i.getContent());
    }
}
