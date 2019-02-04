package com.ota.jimmychen.ota;

import android.location.Address;
import android.text.TextUtils;
import android.util.Log;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class NetworkScanner {
    private static final String ACTIVITY_TAG = NetworkScanner.class.getSimpleName();

    private Networking network = new Networking();

    private static final int CORE_POOL_SIZE = 1;
    private static final int MAX_IMUM_POOL_SIZE = 255;
    private static final int KEEP_ALIVE_TIME = 2000;

    private String mDevAddress;
    private String mLocAddress;
    private String mPing = "ping -c 1 -w 3 ";
    private Runtime mRun = Runtime.getRuntime();
    private Process mProcess = null;
    private List<String> mIPList = new ArrayList<>();
    private ThreadPoolExecutor mExecutor;

    public void scan() {
        mDevAddress = getLocAddress();
        mLocAddress = getIpPrefix();

        if (TextUtils.isEmpty(mLocAddress)) {
            Log.e(ACTIVITY_TAG, "Scan error");
            return;
        }

        mExecutor = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_IMUM_POOL_SIZE, KEEP_ALIVE_TIME, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(CORE_POOL_SIZE));
        for (int i = 1; i < 255; i++) {
            final int lastAddress = i;

            Runnable run = new Runnable() {
                @Override
                public void run() {
                    String currentIp = mLocAddress + lastAddress;
                    String ping_cmd = mPing + currentIp;
                    if (mDevAddress.equals(currentIp)) { return; }

                    try {
                        mProcess = mRun.exec(ping_cmd);
                        int result = mProcess.waitFor();
                        if (result == 0) { mIPList.add(currentIp); }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        if (mProcess != null) mProcess.destroy();
                    }
                }
            };
            mExecutor.execute(run);
        }
        mExecutor.shutdown();

        while (true) {
            try {
                if (mExecutor.isTerminated()) {
                    break;
                }
            } catch (Exception e) { e.printStackTrace(); }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) { e.printStackTrace(); }
        }
    }

    public void destroy() {
        if (mExecutor != null) {
            mExecutor.shutdownNow();
        }
    }

    public String getLocAddress() {
        String ipAddress = "";

        try {
            Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
            while (en.hasMoreElements()) {
                NetworkInterface netInterface = en.nextElement();
                Enumeration<InetAddress> address = netInterface.getInetAddresses();
                while (address.hasMoreElements()) {
                    InetAddress ip = address.nextElement();
                    if (!ip.isLoopbackAddress() && (ip instanceof Inet4Address)) {
                        ipAddress = ip.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) { e.printStackTrace(); }
        Log.i(ACTIVITY_TAG, "local ip address is " + ipAddress);

        return ipAddress;
    }

    public String getIpPrefix() {
        if (mDevAddress.equals("")) { return null; }
        return mDevAddress.substring(0, mDevAddress.lastIndexOf(".") + 1);
    }

    public List<String> getAvailableIP() { return mIPList; }

    public String getAcAddress() {
        for (int i = 0; i < mIPList.size(); i++) {
            String ip = mIPList.get(i);
            if (network.checkIP(ip)) return ip;
        }
        return null;
    }
}
