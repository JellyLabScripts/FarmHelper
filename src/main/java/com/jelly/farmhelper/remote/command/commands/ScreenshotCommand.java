package com.jelly.farmhelper.remote.command.commands;

import com.google.gson.JsonObject;
import com.jelly.farmhelper.macros.MacroHandler;
import com.jelly.farmhelper.remote.command.BaseCommand;
import com.jelly.farmhelper.remote.command.Command;
import com.jelly.farmhelper.remote.event.WebsocketMessage;
import com.jelly.farmhelper.utils.PlayerUtils;

@Command(label = "screenshot")

public class ScreenshotCommand extends BaseCommand {

    @Override
    public void execute(WebsocketMessage message) {
        JsonObject args = message.args;
        try {
            boolean inventory = args.get("inventory").getAsBoolean();
            if (!inventory) {
                screenshot();
            } else {
                inventory();
            }

        } catch (Exception e) {
            e.printStackTrace();
            screenshot();
        }
    }

    public void screenshot() {
        JsonObject data = new JsonObject();
        data.addProperty("username", mc.getSession().getUsername());
        data.addProperty("image", getScreenshot());
        data.addProperty("uuid", mc.getSession().getPlayerID());
        WebsocketMessage response = new WebsocketMessage(label, data);
        send(response);
    }

    public void inventory() {
        JsonObject data = new JsonObject();

        boolean wasMacroing = false;
        if (MacroHandler.isMacroing) {
            wasMacroing = true;
            MacroHandler.disableCurrentMacro();
        }

        PlayerUtils.openInventory();
        String screenshot = getScreenshot();
        mc.thePlayer.closeScreen();

        if (wasMacroing) {
            MacroHandler.enableCurrentMacro();
        }

        data.addProperty("username", mc.getSession().getUsername());
        data.addProperty("image", screenshot);
        data.addProperty("uuid", mc.getSession().getPlayerID());
        WebsocketMessage response = new WebsocketMessage(label, data);
        send(response);
    }
}
