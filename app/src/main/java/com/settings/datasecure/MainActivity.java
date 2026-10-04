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

public class MainActivity extends Activity {
    
    private WebView webView;
    // Kode rahasia untuk melacak proses minta izin
    private static final int KODE_IZIN_OVERLAY = 1234;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Cek apakah HP menggunakan Android 6.0 ke atas dan belum punya izin Overlay
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            
            // Memunculkan pesan kecil di layar bawah
            Toast.makeText(this, "Mohon izinkan aplikasi tampil di atas aplikasi lain", Toast.LENGTH_LONG).show();
            
            // Membuka halaman Pengaturan (Settings) secara otomatis
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, KODE_IZIN_OVERLAY);
            
        } else {
            // Jika izin sudah diberikan sejak awal, langsung tampilkan website
            tampilkanWebsite();
        }
    }

    // Fungsi ini berjalan saat pengguna menekan tombol "Back" dari menu Pengaturan
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == KODE_IZIN_OVERLAY) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Cek ulang apakah pengguna benar-benar mengaktifkannya
                if (Settings.canDrawOverlays(this)) {
                    tampilkanWebsite();
                } else {
                    Toast.makeText(this, "Izin ditolak. Aplikasi tidak bisa berjalan maksimal.", Toast.LENGTH_SHORT).show();
                    // Anda bisa memanggil fungsi untuk meminta izin lagi jika mau
                }
            }
        }
    }

    // Fungsi terpisah untuk memuat WebView (Website)
    private void tampilkanWebsite() {
        webView = new WebView(this);
        setContentView(webView);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);

        webView.setWebViewClient(new WebViewClient());
        
        // MASUKKAN ALAMAT WEBSITE ANDA DI SINI
        webView.loadUrl("https://nama-website-anda.com"); 
    }

    // Fungsi untuk tombol Back di HP
    @Override
    public void onBackPressed() {
        // Mencegah error jika webView belum sempat dibuat (karena izin belum diberikan)
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
