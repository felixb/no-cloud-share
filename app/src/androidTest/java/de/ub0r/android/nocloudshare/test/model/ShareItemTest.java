package de.ub0r.android.nocloudshare.test.model;

import com.google.gson.Gson;

import android.net.Uri;
import android.test.AndroidTestCase;

import de.ub0r.android.logg0r.Log;
import de.ub0r.android.nocloudshare.model.GsonFactory;
import de.ub0r.android.nocloudshare.model.ShareItem;

/**
 * @author flx
 */
public class ShareItemTest extends AndroidTestCase {

    private static final String TAG = "ShareItemTest";

    public void testSerialization() {
        Gson gson = GsonFactory.getInstance();

        ShareItem item0 = new ShareItem(Uri.parse("file:///tmp/foooo.png"), "image/png");
        String json = gson.toJson(item0);
        Log.d(TAG, "json: ", json);
        ShareItem item1 = gson.fromJson(json, ShareItem.class);
        checkEquals(item0, item1);

        item0 = new ShareItem("test", "blubb", "text/plain");
        json = gson.toJson(item0);
        Log.d(TAG, "json: ", json);
        item1 = gson.fromJson(json, ShareItem.class);
        checkEquals(item0, item1);
    }

    private void checkEquals(final ShareItem item0, final ShareItem item1) {
        assertEquals(item0, item1);
        assertEquals(item0.getName(), item1.getName());
        assertEquals(item0.hasContent(), item1.hasContent());
        assertEquals(item0.getUri(), item1.getUri());
        assertEquals(item0.getContent(), item1.getContent());
        assertEquals(item0.getCreation(), item1.getCreation());
        assertEquals(item0.getExpiration(), item1.getExpiration());
        assertEquals(item0.getExternalPath(), item1.getExternalPath());
    }

    public void testNewShareItem() {
        new ShareItem("test", "fooo baaar", "text/plain");
        new ShareItem(Uri.parse("file:///tmp/foo.png"), "image/png");
        new ShareItem(Uri.parse("content://some.provider/foo.png"), "image/png");
        try {
            new ShareItem(Uri.parse("http://some.url.com/foo.png"), "image/png");
            fail("should have failed");
        } catch (IllegalArgumentException e) {
            // nothing to do
        }
    }
}
