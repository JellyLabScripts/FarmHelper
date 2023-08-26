package com.jelly.farmhelper.remote.command.commands;

import com.google.gson.JsonObject;
import com.jelly.farmhelper.remote.command.BaseCommand;
import com.jelly.farmhelper.remote.command.Command;
import com.jelly.farmhelper.remote.event.WebsocketMessage;

@Command(label = "screenshot")

public class ScreenshotCommand extends BaseCommand {

    @Override
    public void execute(WebsocketMessage message) {
        String command = message.command;
        JsonObject args = message.args;
        try {
            String subCommand = args.get("subCommand").getAsString();
            if (subCommand.equalsIgnoreCase("screenshot")) {
                screenshot(message);
            } else if (subCommand.equalsIgnoreCase("inventory")) {
                inventory(message);
            }

        } catch (Exception e) {
            e.printStackTrace();
            screenshot(message);
        }
    }

    public void screenshot(WebsocketMessage event) {
//        JsonObject obj = event.obj;
//        String screenshot = getScreenshot();
//        obj.addProperty("embed", toJson(embed()
//                .setDescription("Sent a screenshot")));
//        obj.addProperty("image", screenshot);
//        send(obj);
    }

    public void inventory(WebsocketMessage event) {
//        JsonObject obj = event.obj;
//        boolean wasMacroing = false;
//        if (nullCheck() && !MacroHandler.randomizing) {
//            if (MacroHandler.isMacroing) {
//                wasMacroing = true;
//                MacroHandler.isMacroing = false;
//                MacroHandler.disableCurrentMacro();
//            }
//
//            PlayerUtils.openInventory();
//            String screenshot = getScreenshot();
//            mc.thePlayer.closeScreen();
//            obj.addProperty("embed", toJson(embed()
//                    .setDescription("Sent a screenshot")));
//            obj.addProperty("image", screenshot);
//            if (wasMacroing) {
//                MacroHandler.isMacroing = true;
//                MacroHandler.enableCurrentMacro();
//            }
//        } else {
//            obj.addProperty("embed", toJson(embed()
//                    .setDescription("I'm not in a world, therefore I can't do that"))
//            );
//        }
//        send(obj);
    }
}
