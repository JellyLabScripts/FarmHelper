package com.jelly.farmhelper.network;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class APIHelper {
    public static JSONObject readJsonFromUrl(String urlToRead, String requestKey, String requestValue) throws Exception {
        StringBuilder result = new StringBuilder();
        URL url = new URL(urlToRead);
        HttpURLConnection conn = ((HttpURLConnection)url.openConnection());
        conn.setRequestProperty(requestKey, requestValue);
        conn.setRequestMethod("GET");
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream()))) {
            for (String line; (line = reader.readLine()) != null; ) {
                result.append(line);
            }
        }
        JSONParser parser = new org.json.simple.parser.JSONParser();
        return (JSONObject) parser.parse(result.toString());
    }
}
