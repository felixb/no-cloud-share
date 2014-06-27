package de.ub0r.android.nocloudshare.test.model;

import com.google.gson.Gson;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.test.AndroidTestCase;

import de.ub0r.android.logg0r.Log;
import de.ub0r.android.nocloudshare.model.GsonFactory;
import de.ub0r.android.nocloudshare.model.ShareItem;
import de.ub0r.android.nocloudshare.model.ShareItemContainer;

/**
 * @author flx
 */
public class ShareItemContainerTest extends AndroidTestCase {

    private static final String TAG = "ShareItemContainerTest";

    private SharedPreferences getPrefs() {
        SharedPreferences p = getContext()
                .getSharedPreferences("test_container", Context.MODE_PRIVATE);
        SharedPreferences.Editor e = p.edit();
        e.clear().apply();
        return p;
    }

    private ShareItemContainer getContainer() {
        ShareItemContainer container = ShareItemContainer.getInstance(getPrefs());
        container.clear();
        return container;
    }

    public void testSerialization() {
        ShareItemContainer container0 = getContainer();
        container0.add(new ShareItem(Uri.parse("file:///tmp/foo.png"), "image/png"));
        container0.add(new ShareItem(Uri.parse("file:///tmp/bar.png"), "image/png"));

        Gson gson = GsonFactory.getInstance();
        String json = gson.toJson(container0);
        Log.d(TAG, "json: ", json);

        ShareItemContainer container1 = gson.fromJson(json, ShareItemContainer.class);
        assertEquals(container0, container1);
    }

    public void testPersist() {
        ShareItemContainer container0 = getContainer();
        container0.add(new ShareItem(Uri.parse("file:///tmp/foo.png"), "image/png"));
        container0.add(new ShareItem(Uri.parse("file:///tmp/bar.png"), "image/png"));

        SharedPreferences p = getPrefs();
        SharedPreferences.Editor e = p.edit();
        container0.persist(e);
        e.apply();

        ShareItemContainer container1 = ShareItemContainer.getInstance(p);
        assertEquals(container0, container1);

        e.clear().apply();
    }

    public void testDoubleAdd() {
        ShareItemContainer container0 = getContainer();
        container0.add(new ShareItem(Uri.parse("file:///tmp/foo.png"), "image/png"));
        container0.add(new ShareItem(Uri.parse("file:///tmp/bar.png"), "image/png"));
        container0.add(new ShareItem(Uri.parse("file:///tmp/foo.png"), "image/png"));
        assertEquals(2, container0.size());
    }

    public void testDoubleAddUri() {
        ShareItemContainer container0 = getContainer();
        Uri u = Uri.parse("file:///tmp/foo.png");
        ShareItem item0 = container0.add(u, "image/png");
        ShareItem item1 = container0.add(u, "image/png");
        assertEquals(item0, item1);
        assertSame(item0, item1);
    }

    public void testFind() {
        ShareItemContainer container0 = getContainer();
        ShareItem item = new ShareItem(Uri.parse("file:///tmp/foo.png"), "image/png");
        container0.add(item);
        assertNotNull(container0.find(item.getHash()));
        assertNull(container0.find(item.getHash() + "xxx"));

    }
}
