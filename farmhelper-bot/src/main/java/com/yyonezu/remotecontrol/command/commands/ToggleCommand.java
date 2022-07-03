package com.yyonezu.remotecontrol.command.commands;

import com.github.kaktushose.jda.commands.annotations.Command;
import com.github.kaktushose.jda.commands.annotations.CommandController;
import com.github.kaktushose.jda.commands.annotations.Concat;
import com.github.kaktushose.jda.commands.dispatching.CommandEvent;
import com.google.gson.JsonObject;
import com.yyonezu.remotecontrol.command.type.Instance;
import com.yyonezu.remotecontrol.event.wait.Waiter;
import com.yyonezu.remotecontrol.struct.BaseCommand;
import com.yyonezu.remotecontrol.utils.EmbedUtils;
import com.yyonezu.remotecontrol.websocket.WebSocketServer;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.json.simple.JSONObject;

import java.util.concurrent.TimeUnit;

@CommandController(value = {"toggle", "t"}, category = "Core")
public class ToggleCommand extends BaseCommand {

    @Command(name = "toggle", usage = "{prefix}toggle instance_ign", desc = "Toggle Farm Helper", isSuper = true)
    public void toggle(CommandEvent ev, Instance instance) {
        JsonObject data = getBaseMessage(ev, instance);
        instance.getSession().getRemote().sendStringByFuture(data.toString());

        register(new Waiter(
                condition -> (condition.matchesMetadata(data)),
                action -> {
                    MessageEmbed eb = EmbedUtils.jsonToEmbed(action.message.get("embed").getAsString());
                    ev.getChannel().sendMessageEmbeds(eb).queue();
                },
                true,
                5L,
                TimeUnit.SECONDS,

                () -> ev.reply("Could not get anything from " + instance.getUser() + "... maybe try again?")
        ));
    }

    @Command(value = "all", usage = "{prefix}toggle all", desc = "Toggles all macro instances")
    public void all(CommandEvent ev) {
        if (WebSocketServer.minecraftInstances.values().size() == 0) {
            ev.reply("There isn't any instances connected");
        } else {
            for (String ign : WebSocketServer.minecraftInstances.values()) {
                Instance instance = new Instance(ign);
                this.toggle(ev, instance);
            }
        }
    }
}