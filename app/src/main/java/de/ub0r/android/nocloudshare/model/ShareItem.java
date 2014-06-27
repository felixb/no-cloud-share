package de.ub0r.android.nocloudshare.model;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import java.util.UUID;

/**
 * @author flx
 */
public class ShareItem {

    private static final long EXTEND_PERIOD = 15 * 60 * 1000;

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
        return "/" + mHash + "/" + getName();
    }

    public long getCreation() {
        return mCreation;
    }

    public long getExpiration() {
        return mExpiration;
    }

    public void setExpireIn() {
        mExpiration = Math.max(System.currentTimeMillis(), mExpiration) + EXTEND_PERIOD;
    }

    public void setExpireIn(final long expiration) {
        mExpiration = System.currentTimeMillis() + expiration;
    }

    public boolean isExpired() {
        return mExpiration < System.currentTimeMillis();
    }

    public void setInfos(final Context context) {
        if (mUri == null) {
            return;
        }
        String[] proj = {MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.TITLE,
                MediaStore.MediaColumns.MIME_TYPE};
        Cursor c = context.getContentResolver().query(mUri, proj, null, null, null);
        if (c != null && c.moveToFirst()) {
            String name = c.getString(0);
            if (name == null) {
                name = c.getString(1);
            }
            if (name != null) {
                setName(name);
            }
            String mimeType = c.getString(2);
            if (mimeType != null) {
                setMimeType(mimeType);
            }
        }
        if (c != null) {
            c.close();
        }
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
