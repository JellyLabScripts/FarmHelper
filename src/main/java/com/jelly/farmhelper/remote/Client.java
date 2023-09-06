package com.jelly.farmhelper.remote;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.remote.command.BaseCommand;
import com.jelly.farmhelper.remote.event.WebsocketMessage;
import com.jelly.farmhelper.utils.LogUtils;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.Optional;

import static com.jelly.farmhelper.utils.StatusUtils.connecting;

public class Client extends WebSocketClient {
    public Client(URI serverUri) {
        super(serverUri);
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("Connected to websocket");
    }

    @Override
    public void onMessage(String message) {
        System.out.println(message);
        if (message.equals("VERSIONERROR")) {
            LogUtils.scriptLog("RemoteControl wont work on this instance as mod/bot versions don't match. Download latest version from discord.");
            FarmHelper.config.enableRemoteControl = false;
            FarmHelper.config.save();
            this.close(-1);
        }
        try {
            WebsocketMessage websocketMessage = FarmHelper.gson.fromJson(message, WebsocketMessage.class);
            String command = websocketMessage.command;
            System.out.println("Command: " + command);

            Optional<BaseCommand> commandInstance = RemoteControlHandler.getCommand(command);
            System.out.println(commandInstance);
            commandInstance.ifPresent(baseCommand -> baseCommand.execute(websocketMessage));
        } catch (Exception e) {
            e.printStackTrace();
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