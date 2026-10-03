package com.settings.datasecure;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {
    
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Membuat komponen WebView
        webView = new WebView(this);
        setContentView(webView);

        // Mengaktifkan JavaScript agar fitur web HTML berjalan lancar
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true); // Berguna jika web Anda pakai local storage

        // Mencegah web terbuka di browser luar (Chrome) saat link diklik
        webView.setWebViewClient(new WebViewClient());

        // MASUKKAN ALAMAT WEBSITE ANDA DI SINI
        webView.loadUrl("file:///android_asset/index.html"); 
    }

    // Kode ini memastikan tombol 'Back' di HP akan mengembalikan halaman web ke sebelumnya, bukan langsung menutup aplikasi
    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
