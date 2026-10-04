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
private static final int KODE_IZIN_OVERLAY = 1234;

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    // Cek Izin Overlay (Tampil di atas aplikasi lain)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
        Toast.makeText(this, "Mohon izinkan aplikasi tampil di atas aplikasi lain", Toast.LENGTH_LONG).show();
        
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, KODE_IZIN_OVERLAY);
    } else {
        tampilkanWebsite();
    }
}

@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    
    if (requestCode == KODE_IZIN_OVERLAY) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {
                tampilkanWebsite();
            } else {
                Toast.makeText(this, "Izin ditolak. Aplikasi tidak bisa berjalan maksimal.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}

private void tampilkanWebsite() {
    webView = new WebView(this);
    setContentView(webView);

    WebSettings webSettings = webView.getSettings();
    webSettings.setJavaScriptEnabled(true);
    webSettings.setDomStorageEnabled(true);
    
    // Mengizinkan file lokal membaca internet/API (Sangat penting untuk Supabase)
    webSettings.setAllowFileAccess(true);
    webSettings.setAllowContentAccess(true);

    webView.setWebViewClient(new WebViewClient());
    
    // MENGUBAH URL ONLINE MENJADI FILE LOKAL YANG KITA BUAT DI ASSETS
    webView.loadUrl("file:///android_asset/index.html"); 
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
