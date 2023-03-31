package com.jelly.farmhelper.remote.command.commands;

import com.google.gson.JsonObject;
import com.jelly.farmhelper.features.ProfitCalculator;
import com.jelly.farmhelper.macros.MacroHandler;
import com.jelly.farmhelper.remote.command.BaseCommand;
import com.jelly.farmhelper.remote.command.RemoteCommandContext;
import com.jelly.farmhelper.remote.event.MessageEvent;
import dev.volix.lib.brigadier.command.Command;
import dev.volix.lib.brigadier.context.CommandContext;
import dev.volix.lib.brigadier.parameter.ParameterSet;

import java.io.IOException;

public class TotalProfitCommand extends BaseCommand {
    @Command(label = "totalprofit")
    public void execute(MessageEvent event, CommandContext<RemoteCommandContext> context, ParameterSet parameter) throws IOException {
        JsonObject data = event.obj;
        data.addProperty("profit", MacroHandler.isMacroing ? Math.round(ProfitCalculator.realProfit) : 0);
        data.addProperty("profithr", MacroHandler.isMacroing ? Math.round(ProfitCalculator.getHourProfit(ProfitCalculator.realProfit)) : 0);
        send(data);
    }
}
