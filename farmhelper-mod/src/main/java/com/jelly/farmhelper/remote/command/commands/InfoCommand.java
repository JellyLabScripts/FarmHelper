package com.jelly.farmhelper.remote.command.commands;

import com.google.gson.JsonObject;
import com.jelly.farmhelper.features.ProfitCalculator;
import com.jelly.farmhelper.macros.MacroHandler;
import com.jelly.farmhelper.network.DiscordWebhook;
import com.jelly.farmhelper.remote.command.BaseCommand;
import com.jelly.farmhelper.remote.command.RemoteCommandContext;
import com.jelly.farmhelper.remote.event.MessageEvent;
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
                .addField("Total Profit", ProfitCalculator.profit, false)
                .addField("Profit / hr", ProfitCalculator.profitHr, false)
                .addField("Crop type", String.valueOf(MacroHandler.crop), true);

        data.addProperty("image", getScreenshot());
        data.addProperty("embed", toJson(embed));
        send(data);
    }
}
