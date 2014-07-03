package de.ub0r.android.nocloudshare.model;

import com.google.gson.Gson;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

import de.ub0r.android.nocloudshare.R;

/**
 * @author flx
 */
public class ShareItemContainer extends ArrayList<ShareItem> {

    private static final String TAG = "ShareItemContainer";

    private static final String PREFS_FILENAME = "container";

    private static final String PREFS_KEY = "items";

    private static ShareItemContainer sInstance = null;

    private ShareItemContainer() {
        // hide constructor
        super();
    }

    public static String buildIndex(final Context context, final List<ShareItem> items,
            final String baseUrl) {
        StringBuilder sb = new StringBuilder();
        sb.append(
                "<!DOCTYPE html>\n<head>\n<link rel=\"stylesheet\" type=\"text/css\" href=\"style.css\">\n<title>")
                .append(context.getString(R.string.app_name))
                .append("</title>\n</head>\n<body>\n<div id=\"content\">\n<p>")
                .append(context.getString(R.string.html_pretext))
                .append("</p>\n<ul>\n");
        for (ShareItem item : items) {
            if (item.isExpired()) {
                continue;
            }
            sb.append("<li><a href=\"")
                    .append(baseUrl)
                    .append(item.getExternalPath())
                    .append("\">")
                    .append(item.getName())
                    .append("</a></li>\n");
        }
        sb.append("</ul>\n<p>")
                .append(context.getString(R.string.html_help))
                .append("</p>\n</div>\n</body>\n</html>");
        return sb.toString();
    }

    public static synchronized ShareItemContainer getInstance(final Context context) {
        if (sInstance == null) {
            SharedPreferences p = context
                    .getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE);
            sInstance = getInstance(p);
        }
        return sInstance;
    }

    public static ShareItemContainer getInstance(final SharedPreferences p) {
        String json = p.getString(PREFS_KEY, null);
        if (TextUtils.isEmpty(json)) {
            return new ShareItemContainer();
        } else {
            Gson gson = GsonFactory.getInstance();
            return gson.fromJson(json, ShareItemContainer.class);
        }
    }

    public void persist(final Context context) {
        SharedPreferences p = context.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE);
        persist(p.edit()).apply();
    }

    public SharedPreferences.Editor persist(final SharedPreferences.Editor editor) {
        Gson gson = GsonFactory.getInstance();
        editor.putString(PREFS_KEY, gson.toJson(this));
        return editor;
    }

    public ShareItem find(final String hash) {
        if (hash == null) {
            return null;
        }
        for (ShareItem item : this) {
            if (hash.equals(item.getHash())) {
                return item;
            }
        }
        return null;
    }

    @Override
    public boolean add(final ShareItem item) {
        return !contains(item) && super.add(item);
    }

    public ShareItem add(final Uri uri, final String mimeType) {
        for (ShareItem item : this) {
            if (item.hasContent()) {
                continue;
            }
            if (uri.equals(item.getUri())) {
                return item;
            }
        }
        ShareItem item = new ShareItem(uri, mimeType);
        add(item);
        return item;
    }

    public boolean hasActiveShares() {
        for (ShareItem item : this) {
            if (!item.isExpired()) {
                return true;
            }
        }
        return false;
    }

    public List<ShareItem> getActiveShares() {
        ArrayList<ShareItem> list = new ArrayList<ShareItem>();
        for (ShareItem item : this) {
            if (!item.isExpired()) {
                list.add(item);
            }
        }
        return list;
    }

    public List<ShareItem> getExpiredShares() {
        ArrayList<ShareItem> list = new ArrayList<ShareItem>();
        for (ShareItem item : this) {
            if (item.isExpired()) {
                list.add(item);
            }
        }
        return list;
    }

    public boolean hasExpiredShares() {
        for (ShareItem item : this) {
            if (item.isExpired()) {
                return true;
            }
        }
        return false;
    }
}
