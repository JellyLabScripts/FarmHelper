package com.yyonezu.remotecontrol.command.commands;

import com.github.kaktushose.jda.commands.annotations.interactions.Interaction;
import com.github.kaktushose.jda.commands.annotations.interactions.SlashCommand;
import com.github.kaktushose.jda.commands.dispatching.commands.CommandEvent;
import com.yyonezu.remotecontrol.struct.BaseCommand;
import com.yyonezu.remotecontrol.websocket.WebSocketServer;
import org.eclipse.jetty.websocket.api.Session;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Interaction
public class StatusCommand extends BaseCommand {
    @SlashCommand(value = "Status command", desc = "Check how many players are connected!")
    public void status(CommandEvent event) {
        List<String> users = new ArrayList<>();
        for (Map.Entry<Session, String> entry : WebSocketServer.minecraftInstances.entrySet()) {
            users.add(entry.getValue());
        }
        if (users.size() == 0) {
            event.reply(embed().setDescription("There are no instances connected, maybe try opening Minecraft?"));
        } else {
            event.reply(embed().setDescription("There are currently " + users.size() + " instances connected. These are ``" + String.join(", ", users) + "``"));
        }
    }
}
