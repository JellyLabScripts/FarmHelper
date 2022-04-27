package com.jelly.FarmHelper.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.netty.handler.codec.base64.Base64;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class Screenshot {
    public static String takeScreenshot() {
        try {
            Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            BufferedImage capture = (new Robot()).createScreenCapture(screenRect);
            String filename = String.valueOf(System.currentTimeMillis()).substring(String.valueOf(System.currentTimeMillis()).length() - 3);
            File temp = File.createTempFile(filename, ".png");
            ImageIO.write(capture, "png", temp);
            temp.deleteOnExit();
            return uploadScreenshot(temp);
        } catch (Exception var4) {
            var4.printStackTrace();
            return null;
        }
    }

    public static String uploadScreenshot(File file) {
        String link = "";

        try {
            URL url = new URL("https://api.imgur.com/3/image");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            BufferedImage image = ImageIO.read(file);
            ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
            ImageIO.write(image, "png", byteArray);
            // String dataImage = Base64.encode(byteImage);
            String dataImage = byteArray.toString();
            String data = URLEncoder.encode("image", "UTF-8") + "=" + URLEncoder.encode(dataImage, "UTF-8");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Client-ID 9a47f5fc66bc069");
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.connect();
            StringBuilder stb = new StringBuilder();
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(data);
            wr.flush();
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));

            String line;
            while ((line = rd.readLine()) != null) {
                stb.append(line).append("\n");
            }

            wr.close();
            rd.close();
            JsonElement jsonElement = (new JsonParser()).parse(stb.toString());
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            link = jsonObject.get("data").getAsJsonObject().get("link").toString();
        } catch (Exception var15) {
            var15.printStackTrace();
        }

        return link;
    }
}

