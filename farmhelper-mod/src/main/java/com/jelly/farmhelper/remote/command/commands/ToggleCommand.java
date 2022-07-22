package com.jelly.farmhelper.remote.command.commands;

import com.google.gson.JsonObject;
import com.jelly.farmhelper.macros.MacroHandler;
import com.jelly.farmhelper.remote.command.BaseCommand;
import com.jelly.farmhelper.remote.command.RemoteCommandContext;
import com.jelly.farmhelper.remote.event.MessageEvent;
import dev.volix.lib.brigadier.command.Command;
import dev.volix.lib.brigadier.context.CommandContext;
import dev.volix.lib.brigadier.parameter.ParameterSet;

import java.io.IOException;


public class ToggleCommand extends BaseCommand {

    @Command(label = "toggle")
    public void execute(MessageEvent event, CommandContext<RemoteCommandContext> context, ParameterSet parameter) throws IOException {
        JsonObject data = event.obj;
        if (nullCheck() || MacroHandler.isMacroing) {
            MacroHandler.toggleMacro();
            data.addProperty("embed", toJson(embed()
                    .setDescription("Toggled Farm Helper " + (MacroHandler.isMacroing ? "on" : "off"))));
            data.addProperty("image", getScreenshot());
        } else {
            data.addProperty("embed", toJson(embed()
                    .setDescription("I'm not in a world, therefore I can't toggle macro")));
        }
        send(data);
    }
}