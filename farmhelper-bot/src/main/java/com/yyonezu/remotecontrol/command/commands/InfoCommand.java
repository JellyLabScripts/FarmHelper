package com.yyonezu.remotecontrol.command.commands;

import com.github.kaktushose.jda.commands.annotations.Command;
import com.github.kaktushose.jda.commands.annotations.CommandController;
import com.github.kaktushose.jda.commands.annotations.Cooldown;
import com.github.kaktushose.jda.commands.dispatching.CommandEvent;
import com.google.gson.JsonObject;
import com.yyonezu.remotecontrol.command.type.Instance;
import com.yyonezu.remotecontrol.event.wait.Waiter;
import com.yyonezu.remotecontrol.struct.BaseCommand;
import com.yyonezu.remotecontrol.utils.EmbedUtils;
import com.yyonezu.remotecontrol.websocket.WebSocketServer;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.eclipse.jetty.websocket.api.Session;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@CommandController(value = {"info", "i", "information"}, category = "Misc")
public class InfoCommand extends BaseCommand {
    @Command(name = "Info command", usage = "{prefix}info instance_ign", desc = "Check information of an instance", isSuper = true)
    public void infoCommand(CommandEvent ev, Instance instance) {
        JsonObject data = getBaseMessage(ev, instance);
        instance.getSession().getRemote().sendStringByFuture(data.toString());
        register(new Waiter(
                condition -> (condition.matchesMetadata(data)),
                action -> {
                    MessageEmbed eb = EmbedUtils.jsonToEmbed(action.message.get("embed").getAsString());
                    ev.getChannel().sendMessageEmbeds(eb).queue();
                },
                true,
                7L,
                TimeUnit.SECONDS,
                () -> ev.reply("Could not get anything from " + instance.getUser() + "... maybe try again?")
        ));
    }


    @Command(value="all", usage = "{prefix}info all", desc = "Check information of all instances")
    public void all(CommandEvent ev) {
        if (WebSocketServer.minecraftInstances.values().size() == 0) {
            ev.reply("There isn't any instances connected");
        } else {
            for (String ign : WebSocketServer.minecraftInstances.values()) {
                Instance instance = new Instance(ign);
                this.infoCommand(ev, instance);
            }
        }
    }
}