package com.settings.datasecure;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class LayananBackground extends Service {
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // START_STICKY adalah sistem Anti-Kill bawaan Android.
        // Jika sistem terpaksa mematikan aplikasi ini karena RAM penuh, 
        // Android akan otomatis menghidupkannya kembali saat memori lega.
        return START_STICKY; 
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
