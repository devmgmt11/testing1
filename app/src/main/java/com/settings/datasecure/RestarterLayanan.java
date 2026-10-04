package com.settings.datasecure;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class RestarterLayanan extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Menerima sinyal bahwa aplikasi telah di-kill, lalu segera membangkitkannya kembali
        Intent layananIntent = new Intent(context, LayananBackground.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(layananIntent);
        } else {
            context.startService(layananIntent);
        }
    }
}
