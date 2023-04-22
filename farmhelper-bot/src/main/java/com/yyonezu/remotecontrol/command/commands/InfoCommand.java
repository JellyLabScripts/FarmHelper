package com.yyonezu.remotecontrol.command.commands;

import com.github.kaktushose.jda.commands.annotations.interactions.Interaction;
import com.github.kaktushose.jda.commands.annotations.interactions.SlashCommand;
import com.github.kaktushose.jda.commands.dispatching.commands.CommandEvent;
import com.google.gson.JsonObject;
import com.yyonezu.remotecontrol.command.type.Instance;
import com.yyonezu.remotecontrol.event.wait.Waiter;
import com.yyonezu.remotecontrol.struct.BaseCommand;
import com.yyonezu.remotecontrol.utils.EmbedUtils;
import com.yyonezu.remotecontrol.websocket.WebSocketServer;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.util.concurrent.TimeUnit;

@Interaction
public class InfoCommand extends BaseCommand {
    @SlashCommand(value = "Info command", desc = "Check information of an instance")
    public void infoCommand(CommandEvent ev, Instance instance) {
        JsonObject data = getBaseMessage(ev, instance);
        instance.getSession().getRemote().sendStringByFuture(data.toString());
        register(new Waiter(
                condition -> (condition.matchesMetadata(data)),
                action -> {
                    MessageEmbed embed = EmbedUtils.jsonToEmbed(action.message.get("embed").getAsString());
                    if (action.message.get("image") != null) {
                        addImageToEmbedAndSend(action.message.get("image").getAsString(), embed, ev);
                    } else {
                        ev.getChannel().sendMessageEmbeds(embed).queue();
                    }
                },
                true,
                7L,
                TimeUnit.SECONDS,
                () -> ev.reply("Could not get anything from " + instance.getUser() + "... maybe try again?")
        ));
    }


    @SlashCommand(value="all", desc = "Check information of all instances")
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