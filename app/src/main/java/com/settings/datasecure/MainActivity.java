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
                    Toast.makeText(this, "Izin ditolak. Aplikasi butuh izin ini.", Toast.LENGTH_SHORT).show();
                    tampilkanLayarUtama();
                }
            }
        }
    }

    private void tampilkanLayarUtama() {
        webView = new WebView(this);
        setContentView(webView);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webView.setWebViewClient(new WebViewClient());
        
        // REVISI 1: Layar polos saat pertama kali dibuka (Tanpa tulisan loading)
        webView.setBackgroundColor(Color.parseColor("#111827"));

        // REVISI 3: Sistem Penyimpanan Otomatis (Cache). 
        // Mengambil HTML yang terakhir kali disimpan agar aplikasi langsung tampil seketika
        final SharedPreferences memori = getSharedPreferences("DataSecureCache", MODE_PRIVATE);
        final String htmlTersimpan = memori.getString("html_utama", "");
        
        if (!htmlTersimpan.isEmpty()) {
            webView.loadDataWithBaseURL(null, htmlTersimpan, "text/html", "UTF-8", null);
        }

        // Cek update ke server di belakang layar
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

                        JSONArray dataJson = new JSONArray(response.toString());
                        if (dataJson.length() > 0) {
                            JSONObject barisData = dataJson.getJSONObject(0);
                            final String htmlBaru = barisData.getString("html_content");

                            // Hanya merender ulang ke layar jika Admin memberikan HTML yang berbeda
                            if (!htmlBaru.equals(htmlTersimpan)) {
                                // Simpan HTML terbaru ke memori HP
                                memori.edit().putString("html_utama", htmlBaru).apply();
                                
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        webView.loadDataWithBaseURL(null, htmlBaru, "text/html", "UTF-8", null);
                                    }
                                });
                            }
                        }
                    }
                    koneksi.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                    // Jika internet mati dan memori kosong
                    if (htmlTersimpan.isEmpty()) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                String htmlError = "<html><body style='background-color:#111827; color:#ef4444; display:flex; justify-content:center; align-items:center; height:100vh;'><h2>Tidak Ada Internet</h2></body></html>";
                                webView.loadDataWithBaseURL(null, htmlError, "text/html", "UTF-8", null);
                            }
                        });
                    }
                }
            }
        }).start();
    }

    // REVISI 2: Mengubah sifat tombol kembali (Back)
    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            // Perintah ini akan menyembunyikan aplikasi (Minimize) seperti tombol Home,
            // sehingga aplikasi TIDAK tertutup (mati) dan tidak perlu loading ulang saat dibuka lagi.
            moveTaskToBack(true);
        }
    }
}
