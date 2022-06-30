package com.yyonezu.remotecontrol.command.commands;

import com.github.kaktushose.jda.commands.annotations.Command;
import com.github.kaktushose.jda.commands.annotations.CommandController;
import com.github.kaktushose.jda.commands.annotations.Concat;
import com.github.kaktushose.jda.commands.dispatching.CommandEvent;
import com.yyonezu.remotecontrol.command.type.Instance;
import com.yyonezu.remotecontrol.event.wait.Waiter;
import com.yyonezu.remotecontrol.struct.BaseCommand;
import com.yyonezu.remotecontrol.utils.EmbedUtils;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.json.simple.JSONObject;

import java.util.concurrent.TimeUnit;

@CommandController("toggle")

// Discord command
public class ToggleCommand extends BaseCommand {

    @Command(name = "toggle", usage = "{prefix}toggle instance_ign", desc = "Toggle Farm Helper", category = "Core")
    public void toggle(CommandEvent ev, Instance instance) {
        JSONObject data = getBaseMessage(ev, instance);
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