package com.jelly.farmhelper.remote;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import com.jelly.farmhelper.remote.event.MessageEvent;
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
        MessageEvent ctx = new MessageEvent(this, (JSONObject) new JSONParser().parse(message));
        List<String> c = Arrays.asList( ((JSONObject) ctx.obj.get("metadata")).get("args").toString().split(" "));
        if (c.size() == 1) {
            Brigadier.getInstance().executeCommand(ctx, c.get(0), c.toArray(new String[0]));
        } else if (c.size() > 1) {
            Brigadier.getInstance().executeCommand(ctx, c.get(0), c.subList(1, c.size()).toArray(new String[0]));
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        // When the client disconnects (they uncheck the "enable remote control" box)
        if (code == -1) {
            connecting.set("WebSocket is down, trying again");
        } else {
            if (!remote) {
                connecting.set("Connecting to the websocket..");
            } else {
                connecting.set("Wrong password for Socket, trying again");
            }
        }
    }

    @Override
    public void onError(Exception ex) {

    }
}