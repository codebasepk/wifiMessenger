package com.byteshaft.wifimessenger.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.byteshaft.wifimessenger.services.LongRunningService;
import com.byteshaft.wifimessenger.utils.AppGlobals;

public class BootStateListener extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (AppGlobals.isServiceOn()) {
            context.startService(new Intent(context, LongRunningService.class));
        }
    }
}
