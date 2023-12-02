package com.jelly.farmhelperv2.remote.struct;

import com.jelly.farmhelperv2.FarmHelper;
import com.jelly.farmhelperv2.remote.WebsocketHandler;
import com.jelly.farmhelperv2.remote.command.commands.ClientCommand;
import com.jelly.farmhelperv2.remote.util.RemoteUtils;
import com.jelly.farmhelperv2.util.LogUtils;
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
            LogUtils.sendDebug("Command: " + command);

            Optional<ClientCommand> commandInstance = RemoteUtils.getCommand(WebsocketHandler.commands, (cmd) -> cmd.label.equalsIgnoreCase(command));
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
