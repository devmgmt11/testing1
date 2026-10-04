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
