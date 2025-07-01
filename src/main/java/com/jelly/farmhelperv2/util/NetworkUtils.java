package com.jelly.farmhelperv2.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

import com.google.gson.*;

import javax.net.ssl.HttpsURLConnection;

public class NetworkUtils {

    private static final JsonParser jsonParser = new JsonParser();

    /**
     * Sends a JSON-formatted POST request to the failsafe server with the provided statistics
     * and returns the server's decision as a string.
     *
     * @param failsafeType the name or category of the failsafe to be triggered (used in URL path)
     * @param jsonFormat   a format string (e.g., "{\"key1\": %.2f, \"key2\": %d}") to be filled with data
     * @param jsonArgs     the arguments to populate the format string, corresponding to placeholders in jsonFormat
     * @return the response from the server (e.g., "trigger" or "ignore"), or {@code null} if the request fails
     */
    public static String requestFailsafe(String failsafeType, String jsonFormat, Object... jsonArgs) {
        try {
            URL url = new URL("http://failsafejl.uk/check_failsafe/" + failsafeType);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            // Prepare JSON payload
            String payload = String.format(Locale.US, jsonFormat, jsonArgs);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = payload.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // Read response
            int responseCode = connection.getResponseCode();

            if (responseCode == 200) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }

                    return response.toString();
                }
            } else {
                LogUtils.sendDebug("[Failsafe] Server error, response code: " + responseCode);
                return null;
            }
        } catch (Exception e) {
            LogUtils.sendDebug("[Failsafe] Failed to connect to failsafe server: " + e.getMessage());
            return null;
        }
    }

    /**
     * Parses the server's JSON response from the failsafe check and performs
     * the appropriate debug, warning, and webhook actions.
     *
     * @param webhook whether to send webhook log
     * @param jsonResponse the raw JSON string returned by the failsafe server
     * @return whether to trigger the failsafe
     */
    public static boolean performFailsafeResponse(boolean webhook, String jsonResponse) {
        Gson gson = new Gson();
        try {
            JsonObject json = gson.fromJson(jsonResponse, JsonObject.class);

            if (json.has("debug") && json.get("debug").isJsonArray()) {
                JsonArray debugArray = json.getAsJsonArray("debug");
                for (int i = 0; i < debugArray.size(); i++) {
                    LogUtils.sendDebug("[Failsafe] " + debugArray.get(i).getAsString());
                }
            }

            if (json.has("warning") && json.get("warning").isJsonArray()) {
                JsonArray warningArray = json.getAsJsonArray("warning");
                for (int i = 0; i < warningArray.size(); i++) {
                    LogUtils.sendWarning("[Failsafe] " + warningArray.get(i).getAsString());
                }
            }

            if (json.has("webhook") && json.get("webhook").isJsonArray() && webhook) {
                JsonArray webhookArray = json.getAsJsonArray("webhook");
                for (int i = 0; i < webhookArray.size(); i++) {
                    LogUtils.webhookLog(webhookArray.get(i).getAsString());
                }
            }


            String action = json.has("action") ? json.get("action").getAsString() : "ignore";
            return "trigger".equalsIgnoreCase(action);

        } catch (JsonSyntaxException e) {
            LogUtils.sendDebug("Invalid JSON from server: " + e.getMessage());
        } catch (Exception e) {
            LogUtils.sendDebug("Error parsing failsafe response: " + e.getMessage());
        }
        return false;
    }

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

