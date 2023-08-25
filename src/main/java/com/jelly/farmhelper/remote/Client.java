package com.jelly.farmhelper.remote;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.remote.event.MessageEvent;
import com.jelly.farmhelper.utils.LogUtils;
import dev.volix.lib.brigadier.Brigadier;
import lombok.SneakyThrows;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

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
            FarmHelper.config.enableRemoteControl = false;
            FarmHelper.config.save();
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
        if (code == -1 && !remote) {
            connecting = "WebSocket is not online, trying again";
        } else {
            if (code == 69) {
                connecting = "Connecting to the websocket..";
            } else if (code == 1006) {
                connecting = "Wrong password for Socket, trying again";
            }
        }
    }

    @Override
    public void onError(Exception ex) {

    }
}