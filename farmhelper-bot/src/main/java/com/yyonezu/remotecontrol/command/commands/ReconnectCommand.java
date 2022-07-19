package com.yyonezu.remotecontrol.command.commands;

import com.github.kaktushose.jda.commands.annotations.Command;
import com.github.kaktushose.jda.commands.annotations.CommandController;
import com.github.kaktushose.jda.commands.dispatching.CommandEvent;
import com.google.gson.JsonObject;
import com.yyonezu.remotecontrol.command.type.Instance;
import com.yyonezu.remotecontrol.event.wait.Waiter;
import com.yyonezu.remotecontrol.struct.BaseCommand;
import com.yyonezu.remotecontrol.utils.EmbedUtils;
import com.yyonezu.remotecontrol.websocket.WebSocketServer;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.util.concurrent.TimeUnit;

import static com.yyonezu.remotecontrol.utils.Utils.toMillis;

@CommandController(value = "reconnect", category = "Misc")
public class ReconnectCommand extends BaseCommand {
    @Command(name = "Reconnect command", usage = "{prefix}reconnect instance_ign time", desc = "Reconnect in X time (e.g: 1h30m, 7h14m3s, 10m)", isSuper = true)
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

    @Command(value="all", usage = "{prefix}reconnect all time", desc = "Reconnects in X time all instances")
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

    @Command(value = "cancel", usage = "{prefix}reconnect cancel instance_ign", desc = "Cancels out a current reconnect command")
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

    @Command(value="cancel all", usage = "{prefix}reconnect cancel all", desc = "Cancels out all current reconnects")
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
