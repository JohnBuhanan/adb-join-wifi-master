package com.geohot.towelroot;

import android.content.Context;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

/**
 * Created by john on 12/24/17.
 */

class WifiActivity extends AppCompatActivity implements CheckSSIDBroadcastReceiver.SSIDFoundListener {
    private static final String TAG = WifiActivity.class.getSimpleName();

    CheckSSIDBroadcastReceiver broadcastReceiver;

    WifiManager mWifiManager;

    private static final String WEP_PASSWORD = "WEP";
    private static final String WPA_PASSWORD = "WPA";
    Thread mThread;

    String mSSID = "Meet the Meeples 2.4";
    String mPasswordType = WPA_PASSWORD;
    String mPassword = "Fucking password";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
    }

    public void connectToWifi() {
        broadcastReceiver = new CheckSSIDBroadcastReceiver(mSSID);
        broadcastReceiver.setSSIDFoundListener(this);

        IntentFilter f = new IntentFilter();
        f.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        f.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(broadcastReceiver, f);

        // Check if mWifi is enabled, and act accordingly
        mWifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);

        if (!mWifiManager.isWifiEnabled())
            mWifiManager.setWifiEnabled(true);
        else
            WifiEnabled();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (broadcastReceiver != null)
            unregisterReceiver(broadcastReceiver);
        broadcastReceiver = null;
    }

    @Override
    public void SSIDFound() {
        Log.d(TAG, "Device Connected to " + mSSID);
        if (mThread != null) {
            mThread.interrupt();
            try {
                mThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        finish();
    }

    @Override
    public void WifiEnabled() {
        Log.d(TAG, "WifiEnabled");
        if (mThread != null)
            return;

        int networkId = checkIfSSIDExists();
        if (networkId == -1) {
            networkId = addWifiConfiguration(networkId);
        }
        if (networkId == -1) {
            Log.d(TAG, "Invalid mWifi network");
            finish();
            return;
        }
        final int final_networkId = networkId;

        mThread = new Thread() {
            @Override
            public void run() {
                mWifiManager.disconnect();
                try {
                    while (!isInterrupted()) {
                        Log.d(TAG, "Joining");
                        mWifiManager.enableNetwork(final_networkId, true);
                        mWifiManager.reconnect();
                        // Wait and see if it worked. Otherwise try again.
                        sleep(10000);
                    }
                } catch (InterruptedException ignored) {
                }
            }
        };
        mThread.start();
    }

    private int addWifiConfiguration(int networkId) {
        WifiConfiguration wfc = new WifiConfiguration();
        wfc.SSID = "\"".concat(mSSID).concat("\"");
        wfc.status = WifiConfiguration.Status.DISABLED;
        wfc.priority = 100;
        if (mPasswordType == null) // no password
        {
            wfc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            wfc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
            wfc.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            wfc.allowedAuthAlgorithms.clear();
            wfc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            wfc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
            wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        } else if (mPasswordType.equals(WEP_PASSWORD)) // WEP
        {
            wfc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            wfc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
            wfc.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            wfc.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            wfc.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
            wfc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            wfc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);

            // if hex string
            // wfc.wepKeys[0] = password;

            wfc.wepKeys[0] = "\"".concat(mPassword).concat("\"");
            wfc.wepTxKeyIndex = 0;
        } else if (mPasswordType.equals(WPA_PASSWORD)) // WPA(2)
        {
            wfc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
            wfc.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            wfc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            wfc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            wfc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
            wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);

            wfc.preSharedKey = "\"".concat(mPassword).concat("\"");
        }
        int result = mWifiManager.addNetwork(wfc);
        if (result != -1) {
            networkId = result;
        }
        return networkId;
    }

    private int checkIfSSIDExists() {
        for (WifiConfiguration i : mWifiManager.getConfiguredNetworks()) {

            if (i.SSID != null && i.SSID.equals("\"".concat(mSSID).concat("\""))) {
                Log.d(TAG, "mWifi network already exists.");
                return i.networkId;
            }
        }
        return -1;
    }
}
