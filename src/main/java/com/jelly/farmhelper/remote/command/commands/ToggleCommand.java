package com.jelly.farmhelper.remote.command.commands;

import com.google.gson.JsonObject;
import com.jelly.farmhelper.macros.MacroHandler;
import com.jelly.farmhelper.remote.struct.ClientCommand;
import com.jelly.farmhelper.remote.struct.Command;
import com.jelly.farmhelper.remote.struct.RemoteMessage;
import com.jelly.farmhelper.utils.LocationUtils;
import com.jelly.farmhelper.utils.PlayerUtils;


@Command(label = "toggle")
public class ToggleCommand extends ClientCommand {

    @Override
    public void execute(RemoteMessage message) {
        JsonObject data = new JsonObject();
        data.addProperty("username", mc.getSession().getUsername());
        data.addProperty("uuid", mc.getSession().getPlayerID());
        data.addProperty("toggled", !MacroHandler.isMacroing);

        if (LocationUtils.currentIsland == LocationUtils.Island.LOBBY && !MacroHandler.isMacroing) {
            data.addProperty("info", "You are in the lobby! Teleporting");
            mc.thePlayer.sendChatMessage("/skyblock");
            new Thread(() -> {
                try {
                    Thread.sleep(2500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (LocationUtils.currentIsland != LocationUtils.Island.GARDEN) {
                    mc.thePlayer.sendChatMessage("/warp garden");
                }

                try {
                    Thread.sleep(2500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (LocationUtils.currentIsland != LocationUtils.Island.GARDEN) {
                    data.addProperty("info", "Can't teleport to garden!");
                } else {
                    MacroHandler.toggleMacro(true);
                    data.addProperty("info", "You are in garden! Macroing");
                }
                RemoteMessage response = new RemoteMessage(label, data);
                send(response);
            }).start();
        } else if (LocationUtils.currentIsland != LocationUtils.Island.GARDEN && !MacroHandler.isMacroing) {
            data.addProperty("info", "You are outside the garden! Teleporting");
            mc.thePlayer.sendChatMessage("/warp garden");
            new Thread(() -> {
                try {
                    Thread.sleep(2500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (LocationUtils.currentIsland != LocationUtils.Island.GARDEN) {
                    data.addProperty("info", "Can't teleport to garden!");
                } else {
                    MacroHandler.toggleMacro(true);
                    data.addProperty("info", "You are in garden! Macroing");
                }
                RemoteMessage response = new RemoteMessage(label, data);
                send(response);
            }).start();
        } else {
            MacroHandler.toggleMacro();
        }


        RemoteMessage response = new RemoteMessage(label, data);
        send(response);
    }
}