package com.jelly.farmhelperv2.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

public class APIUtils {
    static JsonParser jsonParser = new JsonParser();

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

            return (JsonObject) jsonParser.parse(result.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
