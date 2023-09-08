package com.jelly.farmhelper.remote.struct;

import com.google.gson.JsonObject;
import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.remote.waiter.WaiterHandler;
import com.jelly.farmhelper.utils.LogUtils;
import net.minecraft.client.Minecraft;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;

public class WebsocketServer extends WebSocketServer {
    public HashMap<WebSocket, String> minecraftInstances = new HashMap<>();

    public enum WebsocketServerState {
        NOT_CONNECTED,
        CONNECTING,
        CONNECTED
    }

    public WebsocketServerState websocketServerState = WebsocketServerState.NOT_CONNECTED;

    public WebsocketServer(int port) {
        super(new InetSocketAddress(port));
        websocketServerState = WebsocketServerState.CONNECTING;
        minecraftInstances.put(null, Minecraft.getMinecraft().getSession().getUsername());
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        if (!handshake.hasFieldValue("auth")) {
            conn.close(4169);
            return;
        }
        LogUtils.sendDebug("Client connected: " + conn.getRemoteSocketAddress().getAddress());
        try {
            String base64Header = handshake.getFieldValue("auth");
            System.out.println(base64Header);
            JsonObject decoded = FarmHelper.gson.fromJson(base64Header, JsonObject.class);
            if (minecraftInstances.containsValue(decoded.get("name").getAsString())) {
                conn.close(4069);
            }
            minecraftInstances.put(conn, decoded.get("name").getAsString());
        } catch (Exception ex) {
            ex.printStackTrace();
            conn.close();
        }
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        LogUtils.sendDebug("Client disconnected: " + conn.getRemoteSocketAddress().getAddress());
        minecraftInstances.remove(conn);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println("Message: " + message);
        RemoteMessage remoteMessage = FarmHelper.gson.fromJson(message, RemoteMessage.class);
        WaiterHandler.onMessage(remoteMessage);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
        LogUtils.sendDebug("Websocket server error with client: " + ex.getMessage());
    }

    @Override
    public void onStart() {
        websocketServerState = WebsocketServerState.CONNECTED;
        LogUtils.sendSuccess("Websocket server started on port " + getPort());
    }
}
