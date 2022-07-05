package com.jelly.farmhelper.remote.command;

import com.google.gson.JsonObject;
import com.jelly.farmhelper.network.DiscordWebhook;
import com.jelly.farmhelper.remote.RemoteControlHandler;
import com.jelly.farmhelper.utils.Clock;
import net.minecraft.client.Minecraft;

import java.awt.*;
import java.util.UUID;

abstract public class BaseCommand {
    public static final Minecraft mc = Minecraft.getMinecraft();
    public boolean nullCheck() {
        return mc.thePlayer != null && mc.theWorld != null;
    }
    public static void send(String content) {
        RemoteControlHandler.client.send(content);
    }

    public static void send(JsonObject content) {
        RemoteControlHandler.client.send(content.toString());
    }

    public static String toJson(DiscordWebhook.EmbedObject embed) {
        return String.valueOf(DiscordWebhook.toJson(embed));
    }
    public static String getScreenshot() {
        String s = UUID.randomUUID().toString();
        RemoteControlHandler.queuedScreenshots.add(s);
        Clock timeout = new Clock();
        timeout.schedule(5000);
        while (RemoteControlHandler.takenScreenshots.get(s) == null) {
                if (timeout.getRemainingTime() < 0) {
                break;
            }
        }
        return RemoteControlHandler.takenScreenshots.get(s);
    }
    public static DiscordWebhook.EmbedObject embed() {
        return new DiscordWebhook.EmbedObject()
                .setColor(new Color(9372933))
                .setFooter("➤ FarmHelper Remote Control ↳ by yonezu#5542", "https://media.discordapp.net/attachments/946792534544379924/965437127594749972/Jelly.png")
                .setAuthor("Instance name ↳ " + mc.getSession().getUsername(), "https://crafatar.com/avatars/" + mc.getSession().getPlayerID(), "https://crafatar.com/avatars/" + mc.getSession().getPlayerID());
    }
}
