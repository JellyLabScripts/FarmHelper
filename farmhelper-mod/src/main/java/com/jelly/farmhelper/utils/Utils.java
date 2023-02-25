package com.jelly.farmhelper.utils;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.awt.*;
import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static com.jelly.farmhelper.utils.KeyBindUtils.updateKeys;

public class Utils {
    static Minecraft mc = Minecraft.getMinecraft();
    public static void openURL(String url) {
        String os = System.getProperty("os.name").toLowerCase();
        try {
            if (Desktop.isDesktopSupported()) { // Probably Windows
                Desktop desktop = Desktop.getDesktop();
                desktop.browse(new URI(url));
            } else { // Definitely Non-windows
                Runtime runtime = Runtime.getRuntime();
                if (os.contains("mac")) { // Apple
                    runtime.exec("open " + url);
                } else if (os.contains("nix") || os.contains("nux")) { // Linux
                    runtime.exec("xdg-open " + url);
                }
            }
        } catch (Exception e) {

        }
    }

    public static void sendPingAlert() {
        new Thread(() -> {
            try {
                /*InputStream audioSource = Objects.requireNonNull(Utils.class.getClassLoader().getResourceAsStream("assets/farmhelper/sounds/ding.mp3"));
                InputStream bufferedIn = new BufferedInputStream(audioSource);
                AudioInputStream audioStream = AudioSystem.getAudioInputStream(bufferedIn);*/
                /*Media hit = new Media(Objects.requireNonNull(Utils.class.getClassLoader().getResource("assets/farmhelper/sounds/ding.mp3")).toURI().toString());
                MediaPlayer mediaPlayer = new MediaPlayer(hit);
                mediaPlayer.play();*/
                for (int i = 0; i < 15; i++) {
                    mc.theWorld.playSound(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, "random.orb", 10.0F, 1.0F, false);
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }catch (Exception e){
                e.printStackTrace();
            }
        }).start();
    }

    public static void clickWindow(int windowID, int slotID, int mouseButtonClicked, int mode) throws Exception {
        if (Minecraft.getMinecraft().thePlayer.openContainer instanceof ContainerChest || Minecraft.getMinecraft().currentScreen instanceof GuiInventory) {
            Minecraft.getMinecraft().playerController.windowClick(windowID, slotID, mouseButtonClicked, mode, Minecraft.getMinecraft().thePlayer);
            LogUtils.scriptLog("Pressing slot : " + slotID);
        } else {
            LogUtils.scriptLog(EnumChatFormatting.RED + "Didn't open window! This shouldn't happen and the script has been disabled. Please immediately report to the developer.");
            updateKeys(false, false, false, false, false, false, false);
            throw new Exception();
        }
    }

    public static String formatNumber(int number) {
        String s = Integer.toString(number);
        return String.format("%,d", number);
    }

    public static String formatNumber(float number) {
        String s = Integer.toString(Math.round(number));
        return String.format("%,d", Math.round(number));
    }

    public static String formatTime(long millis) {
        return String.format("%dh %dm %ds",
            TimeUnit.MILLISECONDS.toHours(millis),
            TimeUnit.MILLISECONDS.toMinutes(millis) -
                TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
            TimeUnit.MILLISECONDS.toSeconds(millis) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
        );
    }

    public static int nextInt(int upperbound) {
        Random r = new Random();
        return r.nextInt(upperbound);
    }
}