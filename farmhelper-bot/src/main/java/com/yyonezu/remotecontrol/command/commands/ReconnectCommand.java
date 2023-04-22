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

import static com.yyonezu.remotecontrol.utils.Utils.toMillis;

@Interaction
public class ReconnectCommand extends BaseCommand {
    @SlashCommand(value = "Reconnect command", desc = "Reconnect in X time (e.g: 1h30m, 7h14m3s, 10m)")
    public void reconnect(CommandEvent ev, Instance instance, String time) {
        JsonObject obj = getBaseMessage(ev, instance);
        try {
            obj.addProperty("reconnectTime", toMillis(time));
        } catch (Exception e) {
            ev.reply(embed().setDescription("Invalid time, must be in the format xHyMzS"));
            return;
        }
        instance.getSession().getRemote().sendStringByFuture(obj.toString());

        register(new Waiter(
                condition -> (condition.matchesMetadata(obj)),
                action -> {
                    MessageEmbed embed = EmbedUtils.jsonToEmbed(action.message.get("embed").getAsString());
                    if (action.message.get("image") != null) {
                        addImageToEmbedAndSend(action.message.get("image").getAsString(), embed, ev);
                    } else {
                        ev.getChannel().sendMessageEmbeds(embed).queue();
                    }
                },
                true,
                5L,
                TimeUnit.SECONDS,
                () -> ev.reply("Could not get anything from " + instance.getUser() + "... maybe try again?")
        ));
    }

    @SlashCommand(value="all", desc = "Reconnects in X time all instances")
    public void all(CommandEvent ev, String time) {
        if (WebSocketServer.minecraftInstances.values().size() == 0) {
            ev.reply("There isn't any instances connected");
        } else {
            for (String ign : WebSocketServer.minecraftInstances.values()) {
                Instance instance = new Instance(ign);
                this.reconnect(ev, instance, time);
            }
        }
    }

    @SlashCommand(value = "cancel", desc = "Cancels out a current reconnect command")
    public void cancel(CommandEvent ev, Instance instance) {
        JsonObject obj = getBaseMessage(ev, instance);
        instance.getSession().getRemote().sendStringByFuture(obj.toString());
        register(new Waiter(
                condition -> (condition.matchesMetadata(obj)),
                action -> {
                    MessageEmbed embed = EmbedUtils.jsonToEmbed(action.message.get("embed").getAsString());
                    if (action.message.get("image") != null) {
                        addImageToEmbedAndSend(action.message.get("image").getAsString(), embed, ev);
                    } else {
                        ev.getChannel().sendMessageEmbeds(embed).queue();
                    }
                },
                true,
                5L,
                TimeUnit.SECONDS,
                () -> ev.reply("Could not get anything from " + instance.getUser() + "... maybe try again?")
        ));
    }

    @SlashCommand(value="cancel all", desc = "Cancels out all current reconnects")
    public void cancelall(CommandEvent ev) {
        if (WebSocketServer.minecraftInstances.values().size() == 0) {
            ev.reply("There isn't any instances connected");
        } else {
            for (String ign : WebSocketServer.minecraftInstances.values()) {
                Instance instance = new Instance(ign);
                this.cancel(ev, instance);
            }
        }
    }
}
