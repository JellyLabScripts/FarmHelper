package com.jelly.farmhelper.remote.analytic.commands;

import com.google.gson.JsonObject;
import com.jelly.farmhelper.remote.analytic.AnalyticBaseCommand;
import com.jelly.farmhelper.remote.command.RemoteCommandContext;
import com.jelly.farmhelper.remote.event.MessageEvent;
import dev.volix.lib.brigadier.command.Command;
import dev.volix.lib.brigadier.context.CommandContext;
import dev.volix.lib.brigadier.parameter.ParameterSet;

import java.io.IOException;

public class InfoCommand extends AnalyticBaseCommand {
    @Command(label = "analyticsinfo")
    public void analyticsinfo(MessageEvent event, CommandContext<RemoteCommandContext> context, ParameterSet parameter) throws InterruptedException, IOException {
        JsonObject obj = event.obj;
        obj.add("info", gatherMacroingData());
        send(obj);
    }
}
