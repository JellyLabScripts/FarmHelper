package com.jelly.farmhelperv2.remote.struct;

import com.google.gson.JsonObject;
import com.jelly.farmhelperv2.FarmHelper;
import com.jelly.farmhelperv2.remote.waiter.WaiterHandler;
import com.jelly.farmhelperv2.util.LogUtils;
import net.minecraft.client.Minecraft;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.HashMap;

public class WebsocketServer extends WebSocketServer {
    public HashMap<WebSocket, String> minecraftInstances = new HashMap<>();
    public WebsocketServerState websocketServerState;

    public WebsocketServer(int port) {
        super(new InetSocketAddress(port));
        minecraftInstances.clear();
        websocketServerState = WebsocketServerState.CONNECTING;
        minecraftInstances.put(null, Minecraft.getMinecraft().getSession().getUsername());
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        if (!handshake.hasFieldValue("auth")) {
            conn.close(4169);
            return;
        }
        LogUtils.sendDebug("[Remote Control] Client connected: " + conn.getRemoteSocketAddress().getAddress());
        try {
            String base64Header = handshake.getFieldValue("auth");
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
        LogUtils.sendDebug("[Remote Control] Client disconnected: " + conn.getRemoteSocketAddress().getAddress());
        minecraftInstances.remove(conn);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println("[Remote Control] Message: " + message);
        RemoteMessage remoteMessage = FarmHelper.gson.fromJson(message, RemoteMessage.class);
        WaiterHandler.onMessage(remoteMessage);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
        LogUtils.sendDebug("[Remote Control] Websocket server error with client: " + ex.getMessage());
    }

    @Override
    public void onStart() {
        websocketServerState = WebsocketServerState.CONNECTED;
        LogUtils.sendSuccess("[Remote Control] Websocket server started on port " + getPort());
    }

    public enum WebsocketServerState {
        NOT_CONNECTED,
        CONNECTING,
        CONNECTED
    }
}
