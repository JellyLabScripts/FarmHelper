package com.jelly.farmhelper.remote;

import com.google.gson.JsonObject;
import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.remote.analytic.AnalyticBaseCommand;
import com.jelly.farmhelper.remote.command.Adapter;
import com.jelly.farmhelper.remote.command.BaseCommand;
import dev.volix.lib.brigadier.Brigadier;
import lombok.SneakyThrows;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.reflections.Reflections;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;

import static com.jelly.farmhelper.utils.StatusUtils.connecting;

public class RemoteControlHandler {
    public static int tick = 1;
    public static Client client;
    public static Client analytic;
    static Minecraft mc = Minecraft.getMinecraft();
    public RemoteControlHandler() {
        registerCommands();
    }

    public void connect() {
        try {
            JsonObject requestJson = new JsonObject();
            requestJson.addProperty("password", FarmHelper.config.webSocketPassword);
            requestJson.addProperty("name", mc.getSession().getUsername());
            requestJson.addProperty("modversion", FarmHelper.MODVERSION);
            requestJson.addProperty("botversion", FarmHelper.BOTVERSION);
            String data = Base64.getEncoder().encodeToString(requestJson.toString().getBytes(StandardCharsets.UTF_8));

            client = new Client(new URI("ws://" + FarmHelper.config.webSocketIP.split(":")[0] + ":58637/farmhelperws"));
            client.addHeader("auth", data);
            client.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public void registerCommands() {
        Brigadier.getInstance().setAdapter(new Adapter());
        Set<Class<? extends BaseCommand>> classes = new Reflections("com.jelly.farmhelper.remote.command.commands").getSubTypesOf(BaseCommand.class);
        Set<Class<? extends AnalyticBaseCommand>> analyticClasses = new Reflections("com.jelly.farmhelper.remote.analytic.commands").getSubTypesOf(AnalyticBaseCommand.class);

        for (Class<?> clazz : analyticClasses) {
            try {
                Brigadier.getInstance().register(clazz.newInstance()).execute();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

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

        if (!FarmHelper.config.enableRemoteControl) {
            if (client != null && client.isOpen()) {
                client.closeConnection(69, "connecting to remote control");
            }
            return;
        }

        if (client != null && client.isOpen()) {
            connecting = "Connected to Socket";
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
