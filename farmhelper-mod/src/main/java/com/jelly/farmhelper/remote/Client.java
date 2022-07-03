package com.jelly.farmhelper.remote;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.jelly.farmhelper.config.DefaultConfig;
import com.jelly.farmhelper.config.FarmHelperConfig;
import com.jelly.farmhelper.config.interfaces.RemoteControlConfig;
import com.jelly.farmhelper.remote.event.MessageEvent;
import com.jelly.farmhelper.utils.LogUtils;
import dev.volix.lib.brigadier.Brigadier;
import lombok.SneakyThrows;
import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import static com.jelly.farmhelper.utils.StatusUtils.connecting;


public class Client extends WebSocketClient {

    public Client(URI serverUri) {
        super(serverUri);
    }


    @Override
    public void onOpen(ServerHandshake handshakedata) {

    }

    @Override
    @SneakyThrows
    public void onMessage(String message) {
        if (message.equals("VERSIONERROR")) {
            LogUtils.scriptLog("RemoteControl wont work on this instance as mod/bot versions don't match. Download latest version from discord.");
            FarmHelperConfig.set("enableRemoteControl", false);
            this.close(-1);
        }
        MessageEvent ctx = new MessageEvent(this, new Gson().fromJson(message, JsonObject.class));
        List<String> c = Arrays.asList( ctx.obj.get("metadata").getAsJsonObject().get("args").getAsString().split(" "));
        if (c.size() == 1) {
            Brigadier.getInstance().executeCommand(ctx, c.get(0), c.toArray(new String[0]));
        } else if (c.size() > 1) {
            Brigadier.getInstance().executeCommand(ctx, c.get(0), c.subList(1, c.size()).toArray(new String[0]));
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        // When the client disconnects (they uncheck the "enable remote control" box)
        if (code == -1 && remote) {
            connecting.set("WebSocket is down, trying again");
        } else {
            if (code == -1) {
                connecting.set("Connecting to the websocket..");
            } else if (remote) {
                connecting.set("Wrong password for Socket, trying again");
            }
        }
    }

    @Override
    public void onError(Exception ex) {

    }
}