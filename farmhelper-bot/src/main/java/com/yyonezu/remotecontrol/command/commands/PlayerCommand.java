package com.yyonezu.remotecontrol.command.commands;

import com.github.kaktushose.jda.commands.annotations.*;
import com.github.kaktushose.jda.commands.dispatching.CommandEvent;
import com.github.kaktushose.jda.commands.embeds.EmbedDTO;
import com.google.gson.JsonObject;
import com.yyonezu.remotecontrol.command.type.Instance;
import com.yyonezu.remotecontrol.event.wait.EventWaiter;
import com.yyonezu.remotecontrol.struct.BaseCommand;
import com.yyonezu.remotecontrol.event.wait.Waiter;
import com.yyonezu.remotecontrol.utils.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.json.simple.JSONObject;

import java.util.concurrent.TimeUnit;

@CommandController("say")

// Discord command
public class PlayerCommand extends BaseCommand {

    @Command(name = "say", usage = "{prefix}say instance_ign whatever you want", desc = "Make the player say something in chat", category = "Misc")
    public void say(CommandEvent ev, Instance instance, @Concat String message) {
        JSONObject data = getBaseMessage(ev, instance);
        data.put("message", message);
        instance.getSession().getRemote().sendStringByFuture(data.toString());

        register(new Waiter(
                condition -> (condition.matchesMetadata(data)),
                action -> {
                    MessageEmbed eb = EmbedUtils.jsonToEmbed(((JSONObject)action.message.get("embed")).get("embed").toString());
                    ev.getChannel().sendMessageEmbeds(eb).queue();
                },
                true,
                5L,
                TimeUnit.SECONDS,

                () -> ev.reply("Could not get anything from " + instance.getUser() + "... maybe try again?")
        ));
    }
}