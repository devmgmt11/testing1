package com.settings.datasecure;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import android.app.Notification;

public class LayananFirebase extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // Fungsi ini akan otomatis dipanggil walau HP dalam keadaan tidur pulas / aplikasi ditutup!
        
        String judul = "Pemberitahuan";
        String pesan = "Ada pesan baru masuk.";

        // Mengambil data dari Push Notification Firebase
        if (remoteMessage.getNotification() != null) {
            judul = remoteMessage.getNotification().getTitle();
            pesan = remoteMessage.getNotification().getBody();
        } else if (remoteMessage.getData().size() > 0) {
            // Jika dikirim via data payload dari server
            judul = remoteMessage.getData().get("title");
            pesan = remoteMessage.getData().get("body");
        }

        tampilkanNotifikasiKeLayar(judul, pesan);
    }

    private void tampilkanNotifikasiKeLayar(String judul, String pesan) {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        String channelId = "ChannelPemberitahuan";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Pemberitahuan Penting",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.enableVibration(true);
            manager.createNotificationChannel(channel);
        }

        // Jika notifikasi di-klik, akan membuka MainActivity
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_ONE_SHOT);

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, channelId);
        } else {
            builder = new Notification.Builder(this)
                .setPriority(Notification.PRIORITY_HIGH)
                .setDefaults(Notification.DEFAULT_ALL);
        }

        builder.setContentTitle(judul)
               .setContentText(pesan)
               .setSmallIcon(android.R.drawable.ic_dialog_email) 
               .setAutoCancel(true)
               .setContentIntent(pendingIntent);

        manager.notify((int) System.currentTimeMillis(), builder.build());
    }

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        // Token tidak perlu kita kirim ke Supabase, karena kita akan 
        // mendaftarkan user berdasarkan "Topik Email" secara dinamis di MainActivity.
    }
}
