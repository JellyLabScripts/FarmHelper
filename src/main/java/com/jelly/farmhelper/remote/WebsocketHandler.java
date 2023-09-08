package com.jelly.farmhelper.remote;

import cc.polyfrost.oneconfig.utils.Notifications;
import com.google.gson.JsonObject;
import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.remote.struct.BaseCommand;
import com.jelly.farmhelper.remote.struct.RemoteMessage;
import com.jelly.farmhelper.remote.struct.WebsocketClient;
import com.jelly.farmhelper.remote.struct.WebsocketServer;
import com.jelly.farmhelper.remote.util.RemoteUtils;
import com.jelly.farmhelper.remote.waiter.WaiterHandler;
import com.jelly.farmhelper.utils.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.java_websocket.enums.ReadyState;

import java.net.*;
import java.util.ArrayList;

public class WebsocketHandler {
    public enum WebsocketState {
        SERVER,
        CLIENT,
        NONE
    }

    public static int PORT = 2137;
    public static WebsocketState websocketState = WebsocketState.NONE;
    public static WebsocketServer websocketServer;
    public static WebsocketClient websocketClient;
    public final Minecraft mc = Minecraft.getMinecraft();
    private int reconnectAttempts = 0;

    public static final ArrayList<BaseCommand> commands = new ArrayList<>();

    public WebsocketHandler() {
        commands.addAll(RemoteUtils.registerCommands("com.jelly.farmhelper.remote.command.commands", BaseCommand.class));
    }

    public static boolean isServerAlive() {
        try {
            URI uri = new URI("ws://localhost:" + PORT);
            websocketClient = new WebsocketClient(uri);
            JsonObject data = new JsonObject();
            data.addProperty("name", Minecraft.getMinecraft().getSession().getUsername());
            websocketClient.addHeader("auth", FarmHelper.gson.toJson(data));
            System.out.println("Connecting to websocket server..");
            return websocketClient.connectBlocking();
        } catch (URISyntaxException | InterruptedException e) {
            websocketClient = null;
            System.out.println("Failed to connect to websocket server..");
            return false;
        }
    }

    public static void send(String json) {
        if (websocketState == WebsocketState.CLIENT && websocketClient != null && websocketClient.isOpen()) {
            websocketClient.send(json);
        } else if (websocketState == WebsocketState.SERVER && websocketServer != null && websocketServer.websocketServerState == WebsocketServer.WebsocketServerState.CONNECTED) {
            WaiterHandler.onMessage(FarmHelper.gson.fromJson(json, RemoteMessage.class));
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!Loader.isModLoaded("farmhelperjdadependency")) {
            if (FarmHelper.config.enableRemoteControl) {
                FarmHelper.config.enableRemoteControl = false;
                LogUtils.sendError("Farm Helper JDA Dependency is not installed, disabling remote control..");
                Notifications.INSTANCE.send("Farm Helper", "Farm Helper JDA Dependency is not installed, disabling remote control..");
            }
            return;
        }
        if (!DiscordBotHandler.finishedLoading) return;

        switch (websocketState) {
            case NONE: {
                if (websocketClient != null && websocketClient.isOpen()) {
                    try {
                        websocketClient.closeBlocking();
                        websocketClient = null;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
                if (websocketServer != null && websocketServer.websocketServerState == WebsocketServer.WebsocketServerState.CONNECTED) {
                    try {
                        websocketServer.stop();
                        websocketServer = null;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                break;
            }
            case CLIENT: {
                if (websocketClient == null) {
                    try {
                        URI uri = new URI("ws://localhost:" + PORT);
                        websocketClient = new WebsocketClient(uri);
                        JsonObject data = new JsonObject();
                        data.addProperty("name", mc.getSession().getUsername());
                        websocketClient.addHeader("auth", FarmHelper.gson.toJson(data));
                        System.out.println("Connecting to websocket server..");
                        websocketClient.connectBlocking();
                        Notifications.INSTANCE.send("Farm Helper", "Connected to websocket server as a client!");
                    } catch (URISyntaxException | InterruptedException e) {
                        System.out.println("Failed to connect to websocket server..");
                        e.printStackTrace();
                    }
                } else if (!websocketClient.isOpen() && websocketClient.getReadyState() != ReadyState.NOT_YET_CONNECTED) {
                    if (reconnectAttempts > 5) {
                        reconnectAttempts = 0;
                        websocketState = WebsocketState.NONE;
                        Notifications.INSTANCE.send("Farm Helper", "Failed to connect to websocket server, disabling remote control..");
                        LogUtils.sendError("Failed to connect to websocket server, disabling remote control..");
                        FarmHelper.config.enableRemoteControl = false;
                        return;
                    }
                    try {
                        reconnectAttempts++;
                        websocketClient.reconnectBlocking();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                break;
            }
            case SERVER: {
                if (websocketServer == null) {
                    websocketServer = new WebsocketServer(PORT);
                    websocketServer.start();
                    Notifications.INSTANCE.send("Farm Helper", "Started websocket server on port " + PORT);
                } else if (websocketServer.websocketServerState == WebsocketServer.WebsocketServerState.NOT_CONNECTED) {
                    try {
                        websocketServer.stop();
                        websocketServer = null;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                break;
            }
        }
    }
}
