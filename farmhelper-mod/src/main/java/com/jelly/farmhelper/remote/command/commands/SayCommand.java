package com.jelly.farmhelper.remote.command.commands;

import com.jelly.farmhelper.network.DiscordWebhook;
import com.jelly.farmhelper.remote.command.BaseCommand;
import com.jelly.farmhelper.remote.event.MessageEvent;
import com.jelly.farmhelper.remote.command.RemoteCommandContext;
import com.jelly.farmhelper.utils.LogUtils;
import dev.volix.lib.brigadier.command.Command;
import dev.volix.lib.brigadier.context.CommandContext;
import dev.volix.lib.brigadier.parameter.ParameterSet;
import org.json.simple.JSONObject;

public class SayCommand extends BaseCommand {
    @Command(label = "say")
    public void execute(MessageEvent event, CommandContext<RemoteCommandContext> context, ParameterSet parameter) {
        JSONObject data = event.obj;
        if (nullCheck()) {
            String message = data.get("message").toString();
            LogUtils.scriptLog(message);
            data.put("embed", DiscordWebhook.toJson(embed().setDescription("Just sent " + message + " in chat!")));
            send(data.toJSONString());

        } else {
            data.put("embed", DiscordWebhook.toJson(embed().setDescription("I'm not in a world, therefore I can't say anything in chat")));
            send(data.toJSONString());

        }
    }
}
