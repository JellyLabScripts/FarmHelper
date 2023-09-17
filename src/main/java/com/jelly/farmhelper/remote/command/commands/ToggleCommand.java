package com.jelly.farmhelper.remote.command.commands;

import com.google.gson.JsonObject;
import com.jelly.farmhelper.macros.MacroHandler;
import com.jelly.farmhelper.remote.struct.ClientCommand;
import com.jelly.farmhelper.remote.struct.Command;
import com.jelly.farmhelper.remote.struct.RemoteMessage;
import com.jelly.farmhelper.utils.LocationUtils;


@Command(label = "toggle")
public class ToggleCommand extends ClientCommand {

    @Override
    public void execute(RemoteMessage message) {
        JsonObject data = new JsonObject();
        data.addProperty("username", mc.getSession().getUsername());
        data.addProperty("uuid", mc.getSession().getPlayerID());
        data.addProperty("toggled", !MacroHandler.isMacroing);

        if (LocationUtils.currentIsland != LocationUtils.Island.GARDEN && !MacroHandler.isMacroing) {
            data.addProperty("error", "You must be in the garden to start the macro!");
        } else {
            MacroHandler.toggleMacro();
        }

        RemoteMessage response = new RemoteMessage(label, data);
        send(response);
    }
}