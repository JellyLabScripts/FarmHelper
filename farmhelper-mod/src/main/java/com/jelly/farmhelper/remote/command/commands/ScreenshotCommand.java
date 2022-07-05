package com.jelly.farmhelper.remote.command.commands;

import com.google.gson.JsonObject;
import com.jelly.farmhelper.macros.MacroHandler;
import com.jelly.farmhelper.remote.command.BaseCommand;
import com.jelly.farmhelper.remote.command.RemoteCommandContext;
import com.jelly.farmhelper.remote.event.MessageEvent;
import com.jelly.farmhelper.utils.Imgur;
import com.jelly.farmhelper.utils.InventoryUtils;
import dev.volix.lib.brigadier.command.Command;
import dev.volix.lib.brigadier.context.CommandContext;
import dev.volix.lib.brigadier.parameter.ParameterSet;

import java.io.IOException;


public class ScreenshotCommand extends BaseCommand {

    @Command(label = "screenshot")
    public void screenshot(MessageEvent event, CommandContext<RemoteCommandContext> context, ParameterSet parameter) throws InterruptedException, IOException {
        JsonObject obj = event.obj;
        String link = Imgur.upload(getScreenshot());
        if (link != null) {
            obj.addProperty("embed", toJson(embed()
                    .setDescription("Sent a screenshot")
                    .setImage(link)));
        } else {
            obj.addProperty("embed", toJson(embed()
                    .setDescription("Could not send a screenshot, check if Patcher's ScreenshotManager is enabled using /patcher ingame. If it isn't then it could be imgur timeout, try again")));
        }
        send(obj);
    }

    @Command(label = "inventory", parent = "screenshot")
    public void inventory(MessageEvent event, CommandContext<RemoteCommandContext> context, ParameterSet parameter) throws InterruptedException, IOException {
        JsonObject obj = event.obj;
        boolean wasMacroing = false;
        if (nullCheck()) {
            if (MacroHandler.isMacroing) {
                wasMacroing = true;
                MacroHandler.isMacroing = false;
            }
            InventoryUtils.openInventory();
            String link = Imgur.upload(getScreenshot());
            mc.thePlayer.closeScreen();
            if (link != null) {
                obj.addProperty("embed", toJson(embed()
                        .setDescription("Sent a screenshot")
                        .setImage(link)));
            } else {
                obj.addProperty("embed", toJson(embed()
                        .setDescription("Could not send a screenshot, check if Patcher's ScreenshotManager is enabled using /patcher ingame. If it isn't then it could be imgur timeout, try again")));
            }
            if (wasMacroing) {
                MacroHandler.isMacroing = true;
                MacroHandler.enableCurrentMacro();
            }
        } else {
            obj.addProperty("embed", toJson(embed()
                    .setDescription("I'm not in a world, therefore I can't do that"))
            );
        }
        send(obj);
    }
}
