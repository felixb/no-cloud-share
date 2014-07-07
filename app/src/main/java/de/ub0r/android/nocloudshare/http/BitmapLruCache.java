package de.ub0r.android.nocloudshare.http;

import com.android.volley.toolbox.ImageLoader.ImageCache;
import com.jakewharton.disklrucache.DiskLruCache;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.os.Environment;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import de.ub0r.android.logg0r.Log;

/**
 * Volley compatible persistent LRU cache for images.
 *
 * @author flx
 */
public class BitmapLruCache implements ImageCache {

    private static final String TAG = "BitmapLruCache";

    private static final int APP_VERSION = 1;

    private static final int VALUE_COUNT = 1;

    private static final int IO_BUFFER_SIZE = 8 * 1024;

    private DiskLruCache mDiskCache;

    private CompressFormat mCompressFormat = CompressFormat.PNG;

    private int mCompressQuality = 100;

    private static BitmapLruCache sInstance = null;

    public synchronized static BitmapLruCache getDefaultBitmapLruCache(final Context context) {
        if (sInstance == null) {
            sInstance = new BitmapLruCache(context);
        }
        return sInstance;
    }

    private static boolean isExternalStorageRemovable() {
        return Environment.isExternalStorageRemovable();
    }

    private BitmapLruCache(final Context context) {
        this(context, "BitmapCacheDefault", 1024 * 1024 * 10, CompressFormat.PNG, 100);
    }

    @SuppressWarnings("SameParameterValue")
    private BitmapLruCache(final Context context, final String uniqueName, final int diskCacheSize,
            final CompressFormat compressFormat, final int quality) {
        try {
            final File diskCacheDir = getDiskCacheDir(context, uniqueName);
            mDiskCache = DiskLruCache.open(diskCacheDir, APP_VERSION, VALUE_COUNT, diskCacheSize);
            mCompressFormat = compressFormat;
            mCompressQuality = quality;
        } catch (IOException e) {
            Log.e(TAG, "error initializing cache", e);
        }
    }

    public boolean contains(final String url) {
        Log.v(TAG, "contains(", url, ")");
        if (mDiskCache == null) {
            Log.w(TAG, "disk cache does not exist");
            return false;
        }

        final String key = getKey(url);
        DiskLruCache.Snapshot snapshot = null;
        try {
            snapshot = mDiskCache.get(key);
            return snapshot != null;
        } catch (IOException e) {
            Log.e(TAG, "contains failed", e);
        } finally {
            if (snapshot != null) {
                snapshot.close();
            }
        }
        return false;
    }

    @Override
    public Bitmap getBitmap(final String url) {
        Log.v(TAG, "get(", url, ")");
        if (mDiskCache == null) {
            Log.w(TAG, "disk cache does not exist");
            return null;
        }

        final String key = getKey(url);
        Bitmap bitmap = null;
        DiskLruCache.Snapshot snapshot = null;
        try {
            snapshot = mDiskCache.get(key);
            if (snapshot == null) {
                return null;
            }
            final InputStream in = snapshot.getInputStream(0);
            if (in != null) {
                final BufferedInputStream buffIn = new BufferedInputStream(in, IO_BUFFER_SIZE);
                bitmap = BitmapFactory.decodeStream(buffIn);
                buffIn.close();
                Log.v(TAG, "image read from disk: ", key);
            }
        } catch (IOException e) {
            Log.e(TAG, "get failed", e);
        } finally {
            if (snapshot != null) {
                snapshot.close();
            }
        }
        return bitmap;
    }

    @Override
    public void putBitmap(final String url, final Bitmap data) {
        Log.v(TAG, "put(", url, ")");
        if (mDiskCache == null) {
            Log.w(TAG, "disk cache does not exist");
            return;
        }

        final String key = getKey(url);
        DiskLruCache.Editor editor = null;
        try {
            editor = mDiskCache.edit(key);
            if (editor == null) {
                return;
            }

            if (writeBitmapToFile(data, editor)) {
                mDiskCache.flush();
                editor.commit();
                Log.v(TAG, "image put on disk cache ", key);
            } else {
                editor.abort();
                Log.w(TAG, "ERROR on: image put on disk cache ", key);
            }
        } catch (IOException e) {
            Log.e(TAG, "put failed: ", key, e);
            try {
                if (editor != null) {
                    editor.abort();
                }
            } catch (IOException ignored) {
            }
        }
    }

    public void putBitmapAsync(final String url, final Bitmap data) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                putBitmap(url, data);
            }
        });
    }

    private String getKey(final String url) {
        String key = url.replaceAll("[^a-z0-9_-]", "").replace("https", "").replace("http", "");
        if (key.length() > 64) {
            key = key.substring(0, 64);
        }
        return key;
    }

    public void clear() {
        Log.d(TAG, "clear()");
        if (mDiskCache == null) {
            Log.w(TAG, "disk cache does not exist");
            return;
        }

        try {
            mDiskCache.delete();
        } catch (IOException e) {
            Log.e(TAG, "error clearing cache", e);
        }
        try {
            mDiskCache.close();
        } catch (IOException e) {
            Log.e(TAG, "error closing cache", e);
        }
        sInstance = null;
    }

    private File getDiskCacheDir(final Context context, final String uniqueName) {
        // Check if media is mounted or storage is built-in, if so, try and use
        // external cache dir
        // otherwise use internal cache dir
        Log.d(TAG, "external media state: ", Environment.getExternalStorageState());
        Log.d(TAG, "is external removable: ", isExternalStorageRemovable());
        File cachePath = Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                || !isExternalStorageRemovable() ? context.getExternalCacheDir()
                : context.getCacheDir();
        if (cachePath == null) {
            cachePath = context.getCacheDir();
        }
        Log.d(TAG, "getDiskCacheDir(): ", cachePath);
        return new File(cachePath, uniqueName);
    }

    private boolean writeBitmapToFile(final Bitmap bitmap, final DiskLruCache.Editor editor)
            throws IOException {
        OutputStream out = null;
        try {
            out = new BufferedOutputStream(editor.newOutputStream(0), IO_BUFFER_SIZE);
            return bitmap.compress(mCompressFormat, mCompressQuality, out);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }
}
