package com.jelly.farmhelper.remote.command.commands;

import com.google.gson.JsonObject;
import com.jelly.farmhelper.macros.MacroHandler;
import com.jelly.farmhelper.network.DiscordWebhook;
import com.jelly.farmhelper.remote.command.BaseCommand;
import com.jelly.farmhelper.remote.command.RemoteCommandContext;
import com.jelly.farmhelper.remote.event.MessageEvent;
import com.jelly.farmhelper.utils.Imgur;
import com.jelly.farmhelper.utils.ProfitUtils;
import dev.volix.lib.brigadier.command.Command;
import dev.volix.lib.brigadier.context.CommandContext;
import dev.volix.lib.brigadier.parameter.ParameterSet;

import java.io.IOException;

import static com.jelly.farmhelper.utils.LogUtils.getRuntimeFormat;

public class TotalProfit extends BaseCommand {
    @Command(label = "totalprofit")
    public void execute(MessageEvent event, CommandContext<RemoteCommandContext> context, ParameterSet parameter) throws IOException {
        JsonObject data = event.obj;
        data.addProperty("profit", MacroHandler.isMacroing ? Math.round(ProfitUtils.realProfit) : 0);
        data.addProperty("profithr", MacroHandler.isMacroing ? Math.round(ProfitUtils.getHourProfit(ProfitUtils.realProfit)) : 0);
        send(data);
    }
}
