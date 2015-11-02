package com.byteshaft.wifimessenger.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.byteshaft.wifimessenger.utils.AppGlobals;
import com.byteshaft.wifimessenger.utils.ServiceHelpers;

import java.net.InetAddress;

public class LongRunningService extends Service {


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        InetAddress ip = ServiceHelpers.getBroadcastIp();
        String username = AppGlobals.getName();
        ServiceHelpers.broadcastName("ADD:", username, ip);
        ServiceHelpers.startPeerDiscovery();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ServiceHelpers.stopNameBroadcast();
        ServiceHelpers.stopDiscover();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
