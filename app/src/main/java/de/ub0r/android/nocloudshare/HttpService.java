package de.ub0r.android.nocloudshare;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.text.format.Formatter;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;

import de.ub0r.android.logg0r.Log;
import de.ub0r.android.nocloudshare.http.Httpd;
import de.ub0r.android.nocloudshare.model.ShareItem;
import de.ub0r.android.nocloudshare.model.ShareItemContainer;

public class HttpService extends Service {

    private static final String TAG = "HttpService";

    private Httpd mHttpd;

    private ShareItemContainer mContainer;

    private Handler mHandler;

    private PowerManager.WakeLock mWakeLock;

    public HttpService() {
    }

    public static void startService(final Context context) {
        Log.d(TAG, "starting service");
        context.startService(new Intent(context, HttpService.class));
    }

    public static void stopService(final Context context) {
        Log.d(TAG, "stopping service");
        context.stopService(new Intent(context, HttpService.class));
    }

    private static String getIp(final Context context) {
        WifiManager wifiMgr = (WifiManager) context.getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
        if (wifiInfo == null) {
            Toast.makeText(context, R.string.error_fetch_ip, Toast.LENGTH_LONG).show();
            return "not.connected.to.wifi";
        }
        int ip = wifiInfo.getIpAddress();
        // FIXME
        return Formatter.formatIpAddress(ip);
    }

    private static int getPort(final Context context) {
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        try {
            return Integer.parseInt(
                    p.getString(SettingsActivity.PREFS_PORT, SettingsActivity.DEFAULT_PORT));
        } catch (NumberFormatException e) {
            return Integer.parseInt(SettingsActivity.DEFAULT_PORT);
        }
    }

    public static String getBaseUrl(final Context context) {
        return "http://" + getIp(context) + ":" + getPort(context);
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("unsupported method: onBind()");
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        startHttpd();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        stopHttpd();
        super.onDestroy();
    }

    private void startHttpd() {
        mContainer = ShareItemContainer.getInstance(this);
        if (!mContainer.hasActiveShares()) {
            Log.d(TAG, "no shares, exit");
            stopSelf();
            return;
        }

        if (!isWifiConnected()) {
            Log.d(TAG, "no wifi, exit");
            stopSelf();
            return;
        }

        // get wake lock
        if (mWakeLock == null) {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
            mWakeLock.acquire();
            Log.d(TAG, "got WakeLock");
        }

        // start httpd
        if (mHttpd == null) {
            Log.i(TAG, "start httpd");
            mHttpd = new Httpd(getPort(this), this, mContainer);
            try {
                mHttpd.start();
            } catch (IOException e) {
                Toast.makeText(this, R.string.error_httpd, Toast.LENGTH_LONG).show();
                Log.e(TAG, "error starting httpd", e);
                mHttpd = null;
                stopSelf();
            }
        }
        showNotification();

        scheduleTermination();
    }

    private void stopHttpd() {
        if (mHttpd != null) {
            Log.i(TAG, "stop httpd");
            mHttpd.stop();
            mHttpd = null;
        }

        if (mWakeLock != null) {
            mWakeLock.release();
            Log.d(TAG, "WakeLock released");
        }

        cancelNotification();
    }

    private boolean isWifiConnected() {
        WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
        if (!wm.isWifiEnabled()) {
            return false;
        }
        WifiInfo info = wm.getConnectionInfo();
        return !(info == null || info.getBSSID() == null);
    }

    private void scheduleTermination() {
        // schedule termination after everything expired

        if (mHandler == null) {
            mHandler = new Handler(getMainLooper());
        }

        long maxExpiration = 0;
        for (ShareItem item : mContainer) {
            maxExpiration = Math.max(maxExpiration, item.getExpiration());
        }

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                startHttpd();
            }
        }, 1000 + maxExpiration - System.currentTimeMillis());
    }

    private void showNotification() {
        List<ShareItem> items = mContainer.getActiveShares();
        ShareItem singleItem = items.size() == 1 ? items.get(0) : null;

        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification.Builder b = new Notification.Builder(this);
        b.setContentTitle(getString(R.string.app_name));
        b.setSmallIcon(R.drawable.ic_stat_share);
        if (singleItem == null) {
            b.setContentText(getString(R.string.active_shares, items.size()));
            b.setContentIntent(PendingIntent
                    .getActivity(this, 0, new Intent(this, ShareListActivity.class),
                            PendingIntent.FLAG_UPDATE_CURRENT));
        } else {
            b.setContentText(getString(R.string.active_share, singleItem.getName()));
            Intent intent = new Intent(Intent.ACTION_VIEW, null, this, ShareActivity.class);
            intent.putExtra(ShareActivity.EXTRA_HASH, singleItem.getHash());
            b.setContentIntent(PendingIntent.getActivity(this, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT));
        }

        Notification n;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            //noinspection deprecation
            n = b.getNotification();
        } else {
            n = b.build();
        }
        nm.notify(0, n);
    }

    private void cancelNotification() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.cancel(0);
    }
}
