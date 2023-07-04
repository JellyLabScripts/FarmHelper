package com.jelly.farmhelper.network;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import javax.net.ssl.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

public class APIHelper {
    public static JSONObject readJsonFromUrl(String urlToRead, String requestKey, String requestValue) throws Exception {
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

            JSONParser parser = new org.json.simple.parser.JSONParser();
            return (JSONObject) parser.parse(result.toString());
        } catch (Exception e) {
            StringBuilder result = new StringBuilder();
            URL url = new URL(urlToRead);

            // Create a trust manager that trusts all certificates
            TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                }
            };

            // Install the trust manager
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());

            // Set the hostname verifier to trust all hostnames
            HostnameVerifier allHostsValid = (hostname, session) -> true;
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

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

            JSONParser parser = new org.json.simple.parser.JSONParser();
            return (JSONObject) parser.parse(result.toString());
        }
    }
}
