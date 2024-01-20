package com.jelly.farmhelperv2.remote.command.commands;

import com.jelly.farmhelperv2.FarmHelper;
import com.jelly.farmhelperv2.remote.WebsocketHandler;
import com.jelly.farmhelperv2.remote.struct.RemoteMessage;
import com.jelly.farmhelperv2.util.LogUtils;
import com.jelly.farmhelperv2.util.helper.Clock;
import com.jelly.farmhelperv2.util.helper.TickTask;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ScreenShotHelper;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Command(label = "base")
abstract public class ClientCommand {
    public static final Minecraft mc = Minecraft.getMinecraft();
    public final String label;

    public ClientCommand() {
        Command command = this.getClass().getAnnotation(Command.class);
        this.label = command.label();
    }

    public static void send(RemoteMessage message) {
        WebsocketHandler.getInstance().send(FarmHelper.gson.toJson(message));
    }

    public static void send(String content) {
        WebsocketHandler.getInstance().send(content);
    }

    public static String getScreenshot() {
        AtomicReference<String> base64img = new AtomicReference<>(null);
        disablePatcherShit();
        disableEssentialsShit();
        TickTask.getInstance().setTask(() -> {
            String random = UUID.randomUUID().toString();
            ScreenShotHelper.saveScreenshot(mc.mcDataDir, random, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
            File screenshotDirectory = new File(mc.mcDataDir.getAbsoluteFile(), "screenshots");
            File screenshotFile = new File(screenshotDirectory, random);
            byte[] bytes;
            try {
                bytes = Files.readAllBytes(Paths.get(screenshotFile.getAbsolutePath()));
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            base64img.set(Base64.getEncoder().encodeToString(bytes));
        });

        Clock timeout = new Clock();
        timeout.schedule(5000);
        while (base64img.get() == null) {
            if (timeout.getRemainingTime() < 0) {
                break;
            }
        }
        return base64img.get();
    }

    private static void disablePatcherShit() {
        try {
            Class<?> klazz = Class.forName("club.sk1er.patcher.config.PatcherConfig");
            Field field = klazz.getDeclaredField("screenshotManager");
            if (field.getBoolean(klazz)) {
                LogUtils.sendWarning("Disabling Patcher Mod's Screenshot Manager.");
                field.setBoolean(klazz, false);
            }
        } catch (Exception e) {
            System.out.println("[Remote Control] Patcher not found. " + e.getMessage());
        }
    }

    private static void disableEssentialsShit() {
        try {
            Class<?> klazz = Class.forName("gg.essential.config.EssentialConfig");
            Field field = klazz.getDeclaredField("essentialScreenshotsState");
            field.setAccessible(true);
            if (field.getBoolean(klazz)) {
                LogUtils.sendWarning("Disabling Essential Mod's Screenshot Manager");
                field.setBoolean(klazz, false);
            }
        } catch (Exception e) {
            System.out.println("[Remote Control] Essentials not found." + e.getMessage());
        }
    }

    public void execute(RemoteMessage message) {

    }
}
