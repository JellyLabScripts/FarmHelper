package com.yyonezu.remotecontrol.command.commands;

import com.github.kaktushose.jda.commands.annotations.Command;
import com.github.kaktushose.jda.commands.annotations.CommandController;
import com.github.kaktushose.jda.commands.annotations.constraints.Max;
import com.github.kaktushose.jda.commands.annotations.constraints.Min;
import com.github.kaktushose.jda.commands.dispatching.CommandEvent;
import com.google.gson.JsonObject;
import com.yyonezu.remotecontrol.command.type.Instance;
import com.yyonezu.remotecontrol.event.wait.Waiter;
import com.yyonezu.remotecontrol.struct.BaseCommand;
import com.yyonezu.remotecontrol.utils.EmbedUtils;
import com.yyonezu.remotecontrol.websocket.WebSocketServer;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.util.concurrent.TimeUnit;

@CommandController(value = {"setspeed"}, category = "Misc")
public class SetSpeedCommand extends BaseCommand {
    @Command(name = "setspeed", usage = "{prefix}setspeed instance_ign <1-400>", desc = "Sets Rancher's Boots speed", isSuper = true)
    public void setspeed(CommandEvent ev, Instance instance, @Min(1) @Max(400) int speed) {
        JsonObject data = getBaseMessage(ev, instance);
        data.addProperty("speed", speed);
        instance.getSession().getRemote().sendStringByFuture(data.toString());
        register(new Waiter(
                condition -> (condition.matchesMetadata(data)),
                action -> {
                    MessageEmbed embed = EmbedUtils.jsonToEmbed(action.message.get("embed").getAsString());
                    ev.getChannel().sendMessageEmbeds(embed).queue();
                },
                true,
                17L,
                TimeUnit.SECONDS,
                () -> ev.reply("Could not get anything from " + instance.getUser() + "... maybe try again?")
        ));
    }

    @Command(value = "all", usage = "{prefix}setspeed all <speed>", desc = "Set Rancher's Boots speed for every instance")
    public void all(CommandEvent ev, @Min(1) @Max(400) int speed) {
        if (WebSocketServer.minecraftInstances.values().size() == 0) {
            ev.reply("There isn't any instances connected");
        } else {
            for (String ign : WebSocketServer.minecraftInstances.values()) {
                Instance instance = new Instance(ign);
                this.setspeed(ev, instance, speed);
            }
        }
    }
}
