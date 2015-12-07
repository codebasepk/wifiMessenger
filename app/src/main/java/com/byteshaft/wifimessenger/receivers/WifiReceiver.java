package com.byteshaft.wifimessenger.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.byteshaft.wifimessenger.activities.MainActivity;
import com.byteshaft.wifimessenger.services.LongRunningService;
import com.byteshaft.wifimessenger.utils.AppGlobals;

public class WifiReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo network = cm.getActiveNetworkInfo();

        if (network != null) {
            if (network.getType() == ConnectivityManager.TYPE_WIFI) {
                // Ensure service is turned on from app
                if (AppGlobals.isServiceOn()) {
                    context.startService(new Intent(context, LongRunningService.class));
                    if (MainActivity.isRunning()) {
                        MainActivity.getInstance().menuItemService.setTitle("Disable Service");
                        MainActivity.getInstance().menuItemRefresh.setVisible(true);
                    }
                }
            }
        } else {
            context.stopService(new Intent(context, LongRunningService.class));
            boolean wasEnabled = AppGlobals.isServiceOn();
            if (MainActivity.isRunning()) {
//                MainActivity.getInstance().switchService(false);
                MainActivity.getInstance().menuItemService.setTitle("Enable Service");
                MainActivity.getInstance().menuItemRefresh.setVisible(false);
                // Hack to reenable service so that the app works after wifi is on.
                AppGlobals.setService(wasEnabled);
            }
        }
    }
}
