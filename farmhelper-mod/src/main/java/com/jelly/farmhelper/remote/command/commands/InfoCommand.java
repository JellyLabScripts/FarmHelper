package com.jelly.farmhelper.remote.command.commands;

import com.google.gson.JsonObject;
import com.jelly.farmhelper.network.DiscordWebhook;
import com.jelly.farmhelper.remote.command.BaseCommand;
import com.jelly.farmhelper.remote.command.RemoteCommandContext;
import com.jelly.farmhelper.remote.event.MessageEvent;
import com.jelly.farmhelper.utils.ProfitUtils;
import dev.volix.lib.brigadier.command.Command;
import dev.volix.lib.brigadier.context.CommandContext;
import dev.volix.lib.brigadier.parameter.ParameterSet;

import java.io.IOException;

import static com.jelly.farmhelper.utils.LogUtils.getRuntimeFormat;

public class InfoCommand extends BaseCommand {
    @Command(label = "info")
    public void execute(MessageEvent event, CommandContext<RemoteCommandContext> context, ParameterSet parameter) throws IOException {
        JsonObject data = event.obj;
        DiscordWebhook.EmbedObject embed = embed()
                .addField("Username", mc.getSession().getUsername(), true)
                .addField("Runtime", getRuntimeFormat(), true)
                .addField("Total Profit", ProfitUtils.profit.get(), false)
                .addField("Profit / hr", ProfitUtils.profitHr.get(), false)
                .addField(ProfitUtils.getHighTierName().equals("Unknown") ? "Unknown crop?" : ProfitUtils.getHighTierName(), ProfitUtils.cropCount.get(), true)
                .addField("Counter", ProfitUtils.counter.get(), true);

        data.addProperty("image", getScreenshot());
        data.addProperty("embed", toJson(embed));
        send(data);
    }
}
