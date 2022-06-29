package com.yyonezu.remotecontrol.utils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.json.simple.JSONObject;

import java.awt.*;

public class EmbedUtils {
    public static MessageEmbed jsonToEmbed(String jsonstring){
        EmbedBuilder embedBuilder = new EmbedBuilder();
        JsonObject json = new Gson().fromJson(jsonstring, JsonObject.class);
        JsonPrimitive titleObj = json.getAsJsonPrimitive("title");
        if (titleObj != null){ // Make sure the object is not null before adding it onto the embed.
            embedBuilder.setTitle(titleObj.getAsString());
        }

        JsonObject authorObj = json.getAsJsonObject("author");
        if (authorObj != null) {
            String authorName = authorObj.get("name").getAsString();
            String authorIconUrl = authorObj.get("icon_url").getAsString();
            if (authorIconUrl != null) // Make sure the icon_url is not null before adding it onto the embed. If its null then add just the author's name.
                embedBuilder.setAuthor(authorName, authorIconUrl);
            else
                embedBuilder.setAuthor(authorName);
        }

        JsonPrimitive descObj = json.getAsJsonPrimitive("description");
        if (descObj != null){
            embedBuilder.setDescription(descObj.getAsString());
        }

        JsonPrimitive colorObj = json.getAsJsonPrimitive("color");
        if (colorObj != null){
            Color color = new Color(colorObj.getAsInt());
            embedBuilder.setColor(color);
        }

        JsonArray fieldsArray = json.getAsJsonArray("fields");
        if (fieldsArray != null) {
            // Loop over the fields array and add each one by order to the embed.
            fieldsArray.forEach(ele -> {
                String name = ele.getAsJsonObject().get("name").getAsString();
                String content = ele.getAsJsonObject().get("value").getAsString();
                boolean inline = ele.getAsJsonObject().get("inline").getAsBoolean();
                embedBuilder.addField(name, content, inline);
            });
        }

        JsonPrimitive thumbnailObj = json.getAsJsonPrimitive("thumbnail");
        if (thumbnailObj != null){
            embedBuilder.setThumbnail(thumbnailObj.getAsString());
        }

        JsonObject footerObj = json.getAsJsonObject("footer");
        if (footerObj != null){
            String content = footerObj.get("text").getAsString();
            String footerIconUrl = footerObj.get("icon_url").getAsString();

            if (footerIconUrl != null)
                embedBuilder.setFooter(content, footerIconUrl);
            else
                embedBuilder.setFooter(content);
        }

        return embedBuilder.build();
    }
}