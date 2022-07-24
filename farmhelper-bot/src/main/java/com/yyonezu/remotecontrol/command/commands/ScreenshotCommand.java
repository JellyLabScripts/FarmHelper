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

@CommandController(value = {"screenshot", "ss"}, category = "Misc")
public class ScreenshotCommand extends BaseCommand {
    @Command(name = "screenshot", usage = "{prefix}screenshot instance_ign", desc = "Player sends a screenshot", isSuper = true)
    public void screenshot(CommandEvent ev, Instance instance) {
        JsonObject data = getBaseMessage(ev, instance);
        instance.getSession().getRemote().sendStringByFuture(data.toString());
        register(new Waiter(
                condition -> {
                    System.out.println(condition.message.get("metadata").toString());
                    return condition.matchesMetadata(data);
                },
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

    @Command(value = "inventory", usage = "{prefix}screenshot inventory instance_ign", desc = "Player sends an inventory screenshot")
    public void inventory(CommandEvent ev, Instance instance) {

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
    @Command(value = "all", usage = "{prefix}screenshot all", desc = "Get a screenshot from all players")
    public void all(CommandEvent ev) {
        if (WebSocketServer.minecraftInstances.values().size() == 0) {
            ev.reply("There isn't any instances connected");
        } else {
            for (String ign : WebSocketServer.minecraftInstances.values()) {
                Instance instance = new Instance(ign);
                this.screenshot(ev, instance);
            }
        }
    }

    @Command(value = "inventory all", usage = "{prefix}screenshot all", desc = "Get an inventory screenshot from all players")
    public void inventoryall(CommandEvent ev) {
        if (WebSocketServer.minecraftInstances.values().size() == 0) {
            ev.reply("There isn't any instances connected");
        } else {
            for (String ign : WebSocketServer.minecraftInstances.values()) {
                Instance instance = new Instance(ign);
                this.inventory(ev, instance);
            }
        }
    }
}