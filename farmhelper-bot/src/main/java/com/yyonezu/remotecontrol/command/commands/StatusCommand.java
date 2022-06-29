package com.yyonezu.remotecontrol.command.commands;

import com.github.kaktushose.jda.commands.annotations.Command;
import com.github.kaktushose.jda.commands.annotations.CommandController;
import com.github.kaktushose.jda.commands.annotations.Cooldown;
import com.github.kaktushose.jda.commands.dispatching.CommandEvent;
import com.yyonezu.remotecontrol.struct.BaseCommand;
import com.yyonezu.remotecontrol.websocket.WebSocketServer;
import org.eclipse.jetty.websocket.api.Session;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@CommandController("status")
public class StatusCommand extends BaseCommand {
    @Cooldown(value = 5, timeUnit = TimeUnit.SECONDS)
    @Command(name = "Status command", usage = "{prefix}status", desc = "Check how many players are connected!", category = "Misc")
    public void statusCommand(CommandEvent event) {
        List<String> users = new ArrayList<>();
        for (Map.Entry<Session, String> entry : WebSocketServer.minecraftInstances.entrySet()) {
            users.add(entry.getValue());
        }
        if (users.size() == 0) {
            event.reply("There are no instances connected, try opening Minecraft.");
        } else {
            event.reply("There are currently " + users.size() + " instances connected. These are ``" + String.join(", ", users) + "``");
        }
    }
}
