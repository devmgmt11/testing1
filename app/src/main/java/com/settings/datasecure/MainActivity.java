package com.settings.datasecure;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {
    
    private WebView webView;
    private static final int KODE_IZIN_OVERLAY = 1234;
    private static final int KODE_IZIN_NOTIF = 1235; 
    private Handler handler = new Handler(Looper.getMainLooper());
    private String htmlUtamaTerakhir = ""; 
    private boolean bersihkanRiwayat = false; 

    private final String DOMAIN_APP = "https://datasecure.app/";
    private final String URL_SUPABASE = "https://heaedfjyjpfvpddtgjhk.supabase.co/rest/v1/app_settings?id=eq.1&select=html_content";
    private final String KUNCI_SUPABASE = "sb_publishable_uuIu1DiiNhS4aHc3ZSxqrg_F4punPvO"; 

    public class JembatanWeb {
        Context mContext;
        JembatanWeb(Context c) { mContext = c; }

        @JavascriptInterface
        public void buatNotif(String judul, String pesan) {
            NotificationManager manager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager == null) return;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        "ChannelPemberitahuanWeb",
                        "Pemberitahuan dari Web",
                        NotificationManager.IMPORTANCE_HIGH
                );
                channel.enableVibration(true);
                manager.createNotificationChannel(channel);
            }

            Notification.Builder builder;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder = new Notification.Builder(mContext, "ChannelPemberitahuanWeb");
            } else {
                builder = new Notification.Builder(mContext)
                    .setPriority(Notification.PRIORITY_HIGH)
                    .setDefaults(Notification.DEFAULT_ALL);
            }

            builder.setContentTitle(judul)
                   .setContentText(pesan)
                   .setSmallIcon(android.R.drawable.ic_dialog_alert)
                   .setAutoCancel(true);

            manager.notify((int) System.currentTimeMillis(), builder.build());
        }
                @JavascriptInterface
        public void simpanEmailSession(String email) {
            SharedPreferences memori = mContext.getSharedPreferences("DataSecureCache", Context.MODE_PRIVATE);
            memori.edit().putString("login_email", email).apply();
            
            // FUNGSI FIREBASE: Berlangganan (Subscribe) ke Firebase berdasarkan email
            try {
                String emailGrup = email.replace("@", "_").replace(".", "_");
                com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("user_" + emailGrup);
            } catch (Exception e) {}
        }

        @JavascriptInterface
        public void hapusEmailSession() {
            SharedPreferences memori = mContext.getSharedPreferences("DataSecureCache", Context.MODE_PRIVATE);
            String emailLama = memori.getString("login_email", "");
            
            // FUNGSI FIREBASE: Cabut langganan (Unsubscribe) saat logout
            if (!emailLama.isEmpty()) {
                try {
                    String emailGrup = emailLama.replace("@", "_").replace(".", "_");
                    com.google.firebase.messaging.FirebaseMessaging.getInstance().unsubscribeFromTopic("user_" + emailGrup);
                } catch (Exception e) {}
            }
            memori.edit().putString("login_email", "").apply();
        }

        // --- FITUR BARU: Menyimpan sesi email ke memori inti Android ---
        @JavascriptInterface
        public void simpanEmailSession(String email) {
            SharedPreferences memori = mContext.getSharedPreferences("DataSecureCache", Context.MODE_PRIVATE);
            memori.edit().putString("login_email", email).apply();
        }

        @JavascriptInterface
        public void hapusEmailSession() {
            SharedPreferences memori = mContext.getSharedPreferences("DataSecureCache", Context.MODE_PRIVATE);
            memori.edit().putString("login_email", "").apply();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
            // Daftar ulang ke Firebase jika app direstart
        SharedPreferences memori = getSharedPreferences("DataSecureCache", Context.MODE_PRIVATE);
        String emailAktif = memori.getString("login_email", "");
        if (!emailAktif.isEmpty()) {
            try {
                String emailGrup = emailAktif.replace("@", "_").replace(".", "_");
                com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("user_" + emailGrup);
            } catch (Exception e) {}
        }
        periksaDanMintaIzin();
    }

    private void periksaDanMintaIzin() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Mohon izinkan aplikasi tampil di atas aplikasi lain", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, KODE_IZIN_OVERLAY);
            return;
        }

        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, KODE_IZIN_NOTIF);
                return; 
            }
        }

        tampilkanLayarUtama();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == KODE_IZIN_OVERLAY) {
            periksaDanMintaIzin(); 
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == KODE_IZIN_NOTIF) {
            periksaDanMintaIzin();
        }
    }

    private String bacaHtmlDariAsset(String namaFile) {
        StringBuilder htmlStr = new StringBuilder();
        try {
            InputStream is = getAssets().open(namaFile);
            BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            String baris;
            while ((baris = br.readLine()) != null) {
                htmlStr.append(baris).append("\n");
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return htmlStr.toString();
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
        webSettings.setDatabaseEnabled(true); 
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE); 

        webView.addJavascriptInterface(new JembatanWeb(this), "AndroidApp");

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.setAcceptThirdPartyCookies(webView, true);
        }

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
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
            bersihkanRiwayat = true; 
            webView.loadDataWithBaseURL(DOMAIN_APP, htmlUtamaTerakhir, "text/html", "UTF-8", null);
        } else {
            String htmlBawaan = bacaHtmlDariAsset("default.html");
            if (!htmlBawaan.isEmpty()) {
                bersihkanRiwayat = true;
                webView.loadDataWithBaseURL(DOMAIN_APP, htmlBawaan, "text/html", "UTF-8", null);
            }
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

                                    if (!htmlBaru.equals(htmlUtamaTerakhir) && !htmlBaru.isEmpty()) {
                                        htmlUtamaTerakhir = htmlBaru;
                                        memori.edit().putString("html_utama", htmlBaru).apply();
                                        
                                        handler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                bersihkanRiwayat = true; 
                                                webView.loadDataWithBaseURL(DOMAIN_APP, htmlBaru, "text/html", "UTF-8", null);
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
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            moveTaskToBack(true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        CookieManager.getInstance().flush();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        CookieManager.getInstance().flush(); 
    }
}
