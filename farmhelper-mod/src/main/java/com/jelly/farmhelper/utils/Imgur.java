package com.jelly.farmhelper.utils;


import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class Imgur {
    public static String upload(String base64) throws IOException {
        if (base64 == null) {
            return null;
        }
        String clientid = "efce6070269a7f1";
        String encodedParams = URLEncoder.encode("image") + "=" + URLEncoder.encode(base64.replaceAll("\"", ""));
        byte[] b = encodedParams.getBytes( StandardCharsets.UTF_8 );
        HttpURLConnection connection = (HttpURLConnection) new URL("https://api.imgur.com/3/image").openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", "Client-ID " + clientid);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.connect();

        try (DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
            wr.write( b );
            wr.flush();
        }



        if (connection.getResponseCode() != 200) {
            return null;
        }

        JsonObject imgurJson = new Gson().fromJson(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8), JsonObject.class);
        return imgurJson.getAsJsonObject("data").get("link").getAsString();
    }
}