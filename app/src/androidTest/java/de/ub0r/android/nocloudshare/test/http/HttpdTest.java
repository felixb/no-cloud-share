package de.ub0r.android.nocloudshare.test.http;

import android.content.Context;
import android.content.SharedPreferences;
import android.test.AndroidTestCase;

import de.ub0r.android.nocloudshare.http.Httpd;
import de.ub0r.android.nocloudshare.model.ShareItemContainer;

/**
 * @author flx
 */
public class HttpdTest extends AndroidTestCase {

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

    public void testParseHash() {
        assertEquals("12345-6789-1011", Httpd.parseHash("/12345-6789-1011/fooo.png"));
        assertNull(Httpd.parseHash("/fooo.png"));
        assertNull(Httpd.parseHash(""));
        assertNull(Httpd.parseHash(null));

    }
}
