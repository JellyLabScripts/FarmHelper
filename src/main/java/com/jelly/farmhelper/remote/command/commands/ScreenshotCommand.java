package com.jelly.farmhelper.remote.command.commands;

import com.google.gson.JsonObject;
import com.jelly.farmhelper.macros.MacroHandler;
import com.jelly.farmhelper.remote.command.BaseCommand;
import com.jelly.farmhelper.remote.command.RemoteCommandContext;
import com.jelly.farmhelper.remote.event.MessageEvent;
import com.jelly.farmhelper.utils.PlayerUtils;
import dev.volix.lib.brigadier.command.Command;
import dev.volix.lib.brigadier.context.CommandContext;
import dev.volix.lib.brigadier.parameter.ParameterSet;

import java.io.IOException;


public class ScreenshotCommand extends BaseCommand {

    @Command(label = "screenshot")
    public void screenshot(MessageEvent event, CommandContext<RemoteCommandContext> context, ParameterSet parameter) throws InterruptedException, IOException {
        JsonObject obj = event.obj;
        String screenshot = getScreenshot();
        obj.addProperty("embed", toJson(embed()
                .setDescription("Sent a screenshot")));
        obj.addProperty("image", screenshot);
        send(obj);
    }

    @Command(label = "inventory", parent = "screenshot")
    public void inventory(MessageEvent event, CommandContext<RemoteCommandContext> context, ParameterSet parameter) throws InterruptedException, IOException {
        JsonObject obj = event.obj;
        boolean wasMacroing = false;
        if (nullCheck() && !MacroHandler.randomizing) {
            if (MacroHandler.isMacroing) {
                wasMacroing = true;
                MacroHandler.isMacroing = false;
                MacroHandler.disableCurrentMacro();
            }

            PlayerUtils.openInventory();
            String screenshot = getScreenshot();
            mc.thePlayer.closeScreen();
            obj.addProperty("embed", toJson(embed()
                    .setDescription("Sent a screenshot")));
            obj.addProperty("image", screenshot);
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
