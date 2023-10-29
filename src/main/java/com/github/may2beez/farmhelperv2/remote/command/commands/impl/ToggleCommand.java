package com.github.may2beez.farmhelperv2.remote.command.commands.impl;

import com.github.may2beez.farmhelperv2.feature.impl.Failsafe;
import com.github.may2beez.farmhelperv2.handler.GameStateHandler;
import com.github.may2beez.farmhelperv2.handler.MacroHandler;
import com.github.may2beez.farmhelperv2.remote.command.commands.ClientCommand;
import com.github.may2beez.farmhelperv2.remote.command.commands.Command;
import com.github.may2beez.farmhelperv2.remote.struct.RemoteMessage;
import com.google.gson.JsonObject;

@Command(label = "toggle")
public class ToggleCommand extends ClientCommand {

    @Override
    public void execute(RemoteMessage message) {
        JsonObject data = new JsonObject();
        data.addProperty("username", mc.getSession().getUsername());
        data.addProperty("uuid", mc.getSession().getPlayerID());
        data.addProperty("toggled", !MacroHandler.getInstance().isMacroToggled());

        if (GameStateHandler.getInstance().getLocation() == GameStateHandler.Location.LOBBY && !MacroHandler.getInstance().isMacroToggled()) {
            data.addProperty("info", "You are in the lobby! Teleporting");
            mc.thePlayer.sendChatMessage("/skyblock");
            new Thread(() -> {
                try {
                    Thread.sleep(2500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (!GameStateHandler.getInstance().inGarden()) {
                    mc.thePlayer.sendChatMessage("/warp garden");
                }

                try {
                    Thread.sleep(2500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (!GameStateHandler.getInstance().inGarden()) {
                    data.addProperty("info", "Can't teleport to garden!");
                } else {
                    MacroHandler.getInstance().toggleMacro();
                    data.addProperty("info", "You are in garden! Macroing");
                }
                RemoteMessage response = new RemoteMessage(label, data);
                send(response);
            }).start();
            return;
        } else if (!GameStateHandler.getInstance().inGarden() && !MacroHandler.getInstance().isMacroToggled()) {
            data.addProperty("info", "You are outside the garden! Teleporting");
            mc.thePlayer.sendChatMessage("/warp garden");
            new Thread(() -> {
                try {
                    Thread.sleep(2500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (!GameStateHandler.getInstance().inGarden()) {
                    data.addProperty("info", "Can't teleport to garden!");
                } else {
                    MacroHandler.getInstance().toggleMacro();
                    data.addProperty("info", "You are in garden! Macroing");
                }
                RemoteMessage response = new RemoteMessage(label, data);
                send(response);
            }).start();
            return;
        } else {
            if (Failsafe.getInstance().isHadEmergency()) {
                Failsafe.getInstance().setHadEmergency(false);
                Failsafe.getInstance().getRestartMacroAfterFailsafeDelay().reset();
            }
            MacroHandler.getInstance().toggleMacro();
        }


        RemoteMessage response = new RemoteMessage(label, data);
        send(response);
    }
}