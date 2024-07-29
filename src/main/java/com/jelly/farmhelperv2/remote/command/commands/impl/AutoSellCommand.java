package com.jelly.farmhelperv2.remote.command.commands.impl;

import com.google.gson.JsonObject;
import com.jelly.farmhelperv2.feature.impl.AutoSell;
import com.jelly.farmhelperv2.handler.GameStateHandler;
import com.jelly.farmhelperv2.remote.command.commands.ClientCommand;
import com.jelly.farmhelperv2.remote.command.commands.Command;
import com.jelly.farmhelperv2.remote.struct.RemoteMessage;

@Command(label = "autosell")
public class AutoSellCommand extends ClientCommand {

    @Override
    public void execute(RemoteMessage message) {
        JsonObject data = new JsonObject();
        data.addProperty("username", mc.getSession().getUsername());
        data.addProperty("uuid", mc.getSession().getPlayerID());
        data.addProperty("toggled", !AutoSell.getInstance().isEnabled());

        if (GameStateHandler.getInstance().getCookieBuffState() != GameStateHandler.BuffState.ACTIVE)
            data.addProperty("info", "You don't have cookie buff active!");
        else if (GameStateHandler.getInstance().getServerClosingSeconds().isPresent())
            data.addProperty("info", "Server is closing in " + GameStateHandler.getInstance().getServerClosingSeconds().get() + " seconds!");
        else if (GameStateHandler.getInstance().getLocation() != GameStateHandler.Location.GARDEN)
            data.addProperty("info", "You are not in the garden!");
        else {
            AutoSell.getInstance().enable(true);
            data.addProperty("info", "AutoSell enabled");
        }

        RemoteMessage response = new RemoteMessage(label, data);
        send(response);
    }
}