package de.ub0r.android.nocloudshare.http;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.ub0r.android.logg0r.Log;
import de.ub0r.android.nocloudshare.HttpService;
import de.ub0r.android.nocloudshare.R;
import de.ub0r.android.nocloudshare.SettingsActivity;
import de.ub0r.android.nocloudshare.model.ShareItem;
import de.ub0r.android.nocloudshare.model.ShareItemContainer;
import fi.iki.elonen.NanoHTTPD;

/**
 * @author flx
 */
public class Httpd extends NanoHTTPD {

    private class CheckResponse extends Response {

        public CheckResponse(final IStatus status, final String mimeType, final InputStream data) {
            super(status, mimeType, data);
        }

        public CheckResponse(final IStatus status, final String mimeType, final String txt) {
            super(status, mimeType, txt);
        }

        @Override
        protected void send(final OutputStream outputStream) {
            super.send(outputStream);

            // TODO remove activity_share?

            if (!mContainer.hasActiveShares()) {
                HttpService.stopService(mContext);
            }
        }
    }

    private static final String TAG = "Httpd";

    private static final Pattern PATH_PATTERN = Pattern.compile("/([^/]+)/");

    private final Context mContext;

    private final ShareItemContainer mContainer;

    private final String mBaseUrl;

    private final boolean mShowIndex;

    public Httpd(final int port, final Context context, final ShareItemContainer container) {
        super(port);
        mContext = context;
        mContainer = container;
        mBaseUrl = HttpService.getBaseUrl(context);
        SharedPreferences p = PreferenceManager
                .getDefaultSharedPreferences(context);
        mShowIndex = p
                .getBoolean(SettingsActivity.PREFS_SHOW_INDEX, SettingsActivity.DEFAULT_SHOW_INDEX);
        Log.i(TAG, "httpd running");
        Log.d(TAG, "baseUrl: ", mBaseUrl);
        Log.d(TAG, "showIndex: ", mShowIndex);
    }

    @Override
    public Response serve(final IHTTPSession session) {
        Log.d(TAG, "serve: ", session.getMethod(), " ", session.getUri());

        // allow only GET
        if (session.getMethod() != Method.GET) {
            return new CheckResponse(Response.Status.METHOD_NOT_ALLOWED, "text/plain", "RTFM");
        }

        if ("/style.css".equals(session.getUri())) {
            return new CheckResponse(Response.Status.OK, "text/css",
                    mContext.getResources().openRawResource(R.raw.style)
            );
        }

        if (mShowIndex &&
                ("/".equals(session.getUri()) || "/index.html".equals(session.getUri()))) {
            return new CheckResponse(Response.Status.OK, "text/html",
                    ShareItemContainer.buildIndex(mContext, mContainer, mBaseUrl));
        }

        // check if existing
        ShareItem item = mContainer.find(parseHash(session.getUri()));
        if (item == null) {
            return new CheckResponse(Response.Status.NOT_FOUND, "text/plain", "not found");
        }

        if (item.isExpired()) {
            return new CheckResponse(Response.Status.NOT_FOUND, "text/plain", "expired");
        }

        if (item.hasContent()) {
            return new CheckResponse(Response.Status.OK, item.getMimeType(), item.getContent());
        } else {
            Uri u = item.getUri();
            if ("file".equals(u.getScheme())) {
                try {
                    InputStream is = new FileInputStream(new File(u.getPath()));
                    return new CheckResponse(Response.Status.OK, item.getMimeType(), is);
                } catch (FileNotFoundException e) {
                    Log.e(TAG, "file not found: " + u, e);
                    return new CheckResponse(Response.Status.NOT_FOUND, "text/plain",
                            "not found: " + u);
                }
            } else if ("content".equals(u.getScheme())) {
                try {
                    InputStream is = mContext.getContentResolver().openInputStream(u);
                    return new CheckResponse(Response.Status.OK, item.getMimeType(), is);
                } catch (FileNotFoundException e) {
                    Log.e(TAG, "file not found: " + u, e);
                    return new CheckResponse(Response.Status.NOT_FOUND, "text/plain",
                            "not found: " + u);
                }
            } else {
                Log.e(TAG, "unsupported scheme: " + u.getScheme());
                return new CheckResponse(Response.Status.INTERNAL_ERROR, "text/plain",
                        "scheme not supported: " + u.getScheme());
            }
        }
    }

    public static String parseHash(final String path) {
        if (path == null) {
            return null;
        }
        Matcher m = PATH_PATTERN.matcher(path);
        if (m.find()) {
            return m.group(1);
        } else {
            return null;
        }
    }
}
