package com.settings.datasecure;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class PenerimaBoot extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // Perintah ini berjalan otomatis saat HP selesai di-restart
            Intent serviceIntent = new Intent(context, LayananBackground.class);
            context.startService(serviceIntent);
        }
    }
}
