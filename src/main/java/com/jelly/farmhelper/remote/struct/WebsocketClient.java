package com.jelly.farmhelper.remote.struct;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.remote.WebsocketHandler;
import com.jelly.farmhelper.remote.util.RemoteUtils;
import com.jelly.farmhelper.utils.LogUtils;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.Optional;

public class WebsocketClient extends WebSocketClient {
    public WebsocketClient(URI serverUri) {
        super(serverUri);
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        LogUtils.sendSuccess("Connected to websocket server");
    }

    @Override
    public void onMessage(String message) {
        try {
            RemoteMessage remoteMessage = FarmHelper.gson.fromJson(message, RemoteMessage.class);
            String command = remoteMessage.command;
            System.out.println("Command: " + command);

            Optional<ClientCommand> commandInstance = RemoteUtils.getCommand(WebsocketHandler.commands, (cmd) -> cmd.label.equalsIgnoreCase(command));
            System.out.println(commandInstance);
            commandInstance.ifPresent(clientCommand -> clientCommand.execute(remoteMessage));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        if (code == -1) {
            LogUtils.sendDebug("Server doesn't exist");
            return;
        }
        if (code == 4169) {
            LogUtils.sendDebug("Server doesn't have auth header");
            return;
        }
        if (code == 4069) {
            LogUtils.sendDebug("Server already has client with this name");
            return;
        }
        LogUtils.sendError("Disconnected from websocket server");
        LogUtils.sendDebug("Reason: " + reason + " Code: " + code + " Remote: " + remote);
    }

    @Override
    public void onError(Exception ex) {
        ex.printStackTrace();
    }
}
