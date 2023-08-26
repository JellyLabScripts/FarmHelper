package com.jelly.farmhelper.network;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

public class APIHelper {
    public static JsonObject readJsonFromUrl(String urlToRead, String requestKey, String requestValue) throws Exception {
        try {
            StringBuilder result = new StringBuilder();
            URL url = new URL(urlToRead);

            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

            if (requestKey != null && requestValue != null) {
                conn.setRequestProperty(requestKey, requestValue);
            }

            conn.setRequestMethod("GET");

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                for (String line; (line = reader.readLine()) != null; ) {
                    result.append(line);
                }
            }

            return (JsonObject) JsonParser.parseString(result.toString());
        } catch (Exception e) {
//            StringBuilder result = new StringBuilder();
//            URL url = new URL(urlToRead);
//
//            // Create a trust manager that trusts all certificates
//            TrustManager[] trustAllCerts = new TrustManager[] {
//                new X509TrustManager() {
//                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
//                        return null;
//                    }
//                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
//                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
//                }
//            };
//
//            // Install the trust manager
//            SSLContext sslContext = SSLContext.getInstance("SSL");
//            sslContext.init(null, trustAllCerts, new SecureRandom());
//            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
//
//            // Set the hostname verifier to trust all hostnames
//            HostnameVerifier allHostsValid = (hostname, session) -> true;
//            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
//
//            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
//
//            if (requestKey != null && requestValue != null) {
//                conn.setRequestProperty(requestKey, requestValue);
//            }
//
//            conn.setRequestMethod("GET");
//
//            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
//                for (String line; (line = reader.readLine()) != null; ) {
//                    result.append(line);
//                }
//            }
//
//            JsonParser parser = new org.json.simple.parser.JsonParser();
//            return (JsonObject) parser.parse(result.toString());
            return null;
        }
    }
}
