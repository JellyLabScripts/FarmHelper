package com.jelly.farmhelper.remote;

import com.google.gson.JsonObject;
import com.jelly.farmhelper.config.interfaces.RemoteControlConfig;
import com.jelly.farmhelper.remote.command.Adapter;
import com.jelly.farmhelper.remote.command.BaseCommand;
import com.jelly.farmhelper.remote.event.MessageEvent;
import dev.volix.lib.brigadier.Brigadier;
import lombok.SneakyThrows;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.simple.JSONObject;
import org.reflections.Reflections;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.jelly.farmhelper.utils.StatusUtils.connecting;

public class RemoteControlHandler {
    public static int tick = 1;
    public static Client client;
    Minecraft mc = Minecraft.getMinecraft();

    public RemoteControlHandler() {
        registerCommands();
    }

    public void connect() {
        try {
            JSONObject j = new JSONObject();
            j.put("password", RemoteControlConfig.websocketPassword);
            j.put("name", mc.getSession().getUsername());

            String data = Base64.getEncoder().encodeToString(j.toString().getBytes(StandardCharsets.UTF_8));
            client = new Client(new URI("ws://localhost:7070/farmhelperws"));
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
        if (!RemoteControlConfig.enableRemoteControl) {
            if (client != null && client.isOpen()) {
                client.close();
            }
            return;
        }

        if (client != null && client.isOpen()) {
            connecting.set("Connected to Socket");
            return;
        }

        if (tick % 200 == 0) {
            connect();
            tick = 1;
        } else {
            tick++;
        }
    }
}
