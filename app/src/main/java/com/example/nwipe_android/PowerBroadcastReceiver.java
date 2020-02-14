package com.example.nwipe_android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import static android.content.Intent.ACTION_POWER_CONNECTED;
import static android.content.Intent.ACTION_POWER_DISCONNECTED;

public class PowerBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        MainActivity mainActivity = (MainActivity)context;

        if (intent.getAction().equals(ACTION_POWER_DISCONNECTED)) {
            mainActivity.showPowerDisconnectedMessage();
        } else if (intent.getAction().equals(ACTION_POWER_CONNECTED)) {
            mainActivity.clearPowerDisconnectedMessage();
        }

    }
}
