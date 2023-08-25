package com.jelly.farmhelper.remote.command;

import com.google.gson.JsonObject;
import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.network.DiscordWebhook;
import com.jelly.farmhelper.remote.RemoteControlHandler;
import com.jelly.farmhelper.utils.Clock;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ScreenShotHelper;

import java.awt.*;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

abstract public class BaseCommand {
    public static final Minecraft mc = Minecraft.getMinecraft();
    private static boolean patcherEnabled = false;
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
        AtomicReference<String> base64img = new AtomicReference<>(null);
        disablePatcherShit();
        disableEssentialsShit();
        FarmHelper.ticktask = () -> {
            FarmHelper.ticktask = null;
            String random = UUID.randomUUID().toString();
            ScreenShotHelper.saveScreenshot(mc.mcDataDir, random, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
            File screenshotDirectory = new File(mc.mcDataDir.getAbsoluteFile(), "screenshots");
            File screenshotFile = new File(screenshotDirectory, random);
            byte[] bytes = Files.readAllBytes(Paths.get(screenshotFile.getAbsolutePath()));
            base64img.set(Base64.getEncoder().encodeToString(bytes));
            screenshotFile.getAbsoluteFile().setWritable(true);
            while(!screenshotFile.getAbsoluteFile().delete());

        };

        Clock timeout = new Clock();
        timeout.schedule(5000);
        while (base64img.get() == null) {
                if (timeout.getRemainingTime() < 0) {
                break;
            }
        }
        return base64img.get();
    }
    public static DiscordWebhook.EmbedObject embed() {
        return new DiscordWebhook.EmbedObject()
                .setColor(new Color(9372933))
                .setFooter("➤ FarmHelper Remote Control ↳ by yonezu#5542", "https://media.discordapp.net/attachments/946792534544379924/965437127594749972/Jelly.png")
                .setAuthor("Instance name ↳ " + mc.getSession().getUsername(), "https://crafatar.com/avatars/" + mc.getSession().getPlayerID(), "https://crafatar.com/avatars/" + mc.getSession().getPlayerID());
    }

    private static void disablePatcherShit() {
        if (patcherEnabled) {
            try {
                Class<?> klazz = Class.forName("club.sk1er.patcher.config.PatcherConfig");
                patcherEnabled = true;
                Field field = klazz.getDeclaredField("screenshotManager");
                field.setBoolean(klazz, false);
            } catch (Exception e) {
                e.printStackTrace();
                patcherEnabled = false;
            }
        }
    }

    private static void disableEssentialsShit()  {
        try {
            Class<?> klazz = Class.forName("gg.essential.config.EssentialConfig");
            Field field = klazz.getDeclaredField("essentialScreenshots");
            field.setAccessible(true);
            field.setBoolean(klazz, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
