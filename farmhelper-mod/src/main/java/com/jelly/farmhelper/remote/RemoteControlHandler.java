package com.jelly.farmhelper.remote;

import com.google.gson.JsonObject;
import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.config.interfaces.RemoteControlConfig;
import com.jelly.farmhelper.remote.command.Adapter;
import com.jelly.farmhelper.remote.command.BaseCommand;
import dev.volix.lib.brigadier.Brigadier;
import lombok.SneakyThrows;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ScreenShotHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.reflections.Reflections;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static com.jelly.farmhelper.utils.StatusUtils.connecting;

public class RemoteControlHandler {
    public static int tick = 1;
    public static Client client;
    public static List<String> queuedScreenshots = new ArrayList<>();
    public static HashMap<String, String> takenScreenshots = new HashMap<>();

    static Minecraft mc = Minecraft.getMinecraft();

    public RemoteControlHandler() {
        registerCommands();
    }

    public void connect() {
        try {
            JsonObject j = new JsonObject();
            j.addProperty("password", RemoteControlConfig.websocketPassword);
            j.addProperty("name", mc.getSession().getUsername());
            j.addProperty("modversion", FarmHelper.MODVERSION);
            j.addProperty("botversion", FarmHelper.BOTVERSION);
            String data = Base64.getEncoder().encodeToString(j.toString().getBytes(StandardCharsets.UTF_8));
            client = new Client(new URI("ws://localhost:58637/farmhelperws"));
            client.addHeader("auth", data);
            client.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public void registerCommands() {
        Brigadier.getInstance().setAdapter(new Adapter());
        Set<Class<? extends BaseCommand>> classes = new Reflections("com.jelly.farmhelper.remote.command.commands").getSubTypesOf(BaseCommand.class);
        for (Class<?> clazz: classes) {
            try {
                Brigadier.getInstance().register(clazz.newInstance()).execute();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    @SneakyThrows
    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.START) return;
        if (!queuedScreenshots.isEmpty()) {
                for (int i = 0; i < queuedScreenshots.size(); i++) {
                    String s = queuedScreenshots.get(i);
                    try {
                        ScreenShotHelper.saveScreenshot(mc.mcDataDir, s, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
                        File screenshotDirectory = new File(mc.mcDataDir, "screenshots");
                        File screenshotFile = new File(screenshotDirectory, s);
                        byte[] bytes = Files.readAllBytes(Paths.get(screenshotFile.getAbsolutePath()));
                        String base64 = Base64.getEncoder().encodeToString(bytes);
                        takenScreenshots.put(s, base64);
                        screenshotFile.delete();
                    } catch (Exception z) {
                        takenScreenshots.put(s, null);
                    }
                }
                queuedScreenshots.clear();
        }

        if (!RemoteControlConfig.enableRemoteControl) {
            if (client != null && client.isOpen()) {
                client.closeConnection(69, "a");
            }
            return;
        }

        if (client != null && client.isOpen()) {
            connecting.set("Connected to Socket");
            return;
        }

        if (tick % 20 == 0) {
            connect();
            tick = 1;
        } else {
            tick++;
        }
    }
}
