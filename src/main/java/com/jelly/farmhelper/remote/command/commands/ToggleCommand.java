package com.jelly.farmhelper.remote.command.commands;

import com.google.gson.JsonObject;
import com.jelly.farmhelper.macros.MacroHandler;
import com.jelly.farmhelper.remote.command.BaseCommand;
import com.jelly.farmhelper.remote.command.Command;
import com.jelly.farmhelper.remote.event.WebsocketMessage;
import com.jelly.farmhelper.utils.LocationUtils;
import com.jelly.farmhelper.utils.LogUtils;
import net.minecraft.util.EnumChatFormatting;


@Command(label = "toggle")
public class ToggleCommand extends BaseCommand {

    @Override
    public void execute(WebsocketMessage message) {
        JsonObject data = new JsonObject();
        data.addProperty("username", mc.getSession().getUsername());
        data.addProperty("uuid", mc.getSession().getPlayerID());
        data.addProperty("toggled", !MacroHandler.isMacroing);

        if (LocationUtils.currentIsland != LocationUtils.Island.GARDEN && !MacroHandler.isMacroing) {
            data.addProperty("error", "You must be in the garden to start the macro!");
        } else {
            MacroHandler.toggleMacro();
        }

        WebsocketMessage response = new WebsocketMessage(label, data);
        send(response);
    }
}