package com.settings.datasecure;

import android.app.Activity;
import android.content.Intent;
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

    // Konfigurasi Supabase untuk Web UTAMA (ID: 1)
    private final String URL_SUPABASE = "https://heaedfjyjpfvpddtgjhk.supabase.co/rest/v1/app_settings?id=eq.1&select=html_content";
    private final String KUNCI_SUPABASE = "sb_publishable_uuIu1DiiNhS4aHc3ZSxqrg_F4punPvO"; 

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Mengecek Izin Overlay di awal agar fitur Latar Belakang bisa bekerja
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
        
        // Tampilan sementara (Loading)
        String htmlLoading = "<html><body style='background-color:#111827; color:#38bdf8; display:flex; justify-content:center; align-items:center; height:100vh; font-family:sans-serif; margin:0;'><h2>Mengunduh Tampilan...</h2></body></html>";
        webView.loadDataWithBaseURL(null, htmlLoading, "text/html", "UTF-8", null);

        // Ambil data langsung dari Supabase menggunakan Java
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

                            // Masukkan HTML dari database langsung ke Layar HP
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    webView.loadDataWithBaseURL(null, htmlBaru, "text/html", "UTF-8", null);
                                }
                            });
                        }
                    }
                    koneksi.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            String htmlError = "<html><body style='background-color:#111827; color:#ef4444; display:flex; justify-content:center; align-items:center; height:100vh; font-family:sans-serif; text-align:center; padding:20px;'><h2>Gagal Terhubung</h2><p>Cek koneksi internet Anda atau pastikan data Web Utama sudah disimpan di Admin Panel.</p></body></html>";
                            webView.loadDataWithBaseURL(null, htmlError, "text/html", "UTF-8", null);
                        }
                    });
                }
            }
        }).start();
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
