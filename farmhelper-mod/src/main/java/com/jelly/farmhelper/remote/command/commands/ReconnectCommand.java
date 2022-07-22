package com.jelly.farmhelper.remote.command.commands;

import com.google.gson.JsonObject;
import com.jelly.farmhelper.features.BanwaveChecker;
import com.jelly.farmhelper.features.Failsafe;
import com.jelly.farmhelper.remote.command.BaseCommand;
import com.jelly.farmhelper.remote.command.RemoteCommandContext;
import com.jelly.farmhelper.remote.event.MessageEvent;
import com.jelly.farmhelper.utils.Clock;
import dev.volix.lib.brigadier.command.Command;
import dev.volix.lib.brigadier.context.CommandContext;
import dev.volix.lib.brigadier.parameter.ParameterSet;
import net.minecraft.util.ChatComponentText;

import java.io.IOException;

import static com.jelly.farmhelper.utils.Utils.formatTime;

public class ReconnectCommand extends BaseCommand {
    public static boolean isEnabled = false;

    public static Clock reconnectClock = new Clock();

    @Command(label = "reconnect")
    public void execute(MessageEvent event, CommandContext<RemoteCommandContext> context, ParameterSet parameter) throws IOException {
        JsonObject obj = event.obj;
        if (nullCheck()) {
            long ms = obj.get("reconnectTime").getAsLong();
            reconnectClock.schedule(ms);
            mc.getNetHandler().getNetworkManager().closeChannel(new ChatComponentText("Reconnecting in " + formatTime(ms)));
            isEnabled = true;
            obj.addProperty("embed", toJson(embed()
                    .setDescription("I disconnected, reconnecting in " + formatTime(ms))));
        } else {
            obj.addProperty("embed", toJson(embed()
                    .setDescription("Can't reconnect since I was already disconnected")));
            obj.addProperty("image", getScreenshot());

        }
        send(obj);
    }

    @Command(parent = "reconnect", label = "cancel")
    public void cancel(MessageEvent event, CommandContext<RemoteCommandContext> context, ParameterSet parameter) throws IOException {
        JsonObject obj = event.obj;
        if (nullCheck()) {
            obj.addProperty("embed", toJson(embed()
                    .setDescription("I'm already connected, can't cancel reconnect.")));
        } else if (reconnectClock.isScheduled()) {
            reconnectClock.reset();
            obj.addProperty("embed", toJson(embed()
                    .setDescription("Reconnecting...")));
        } else {
            obj.addProperty("embed", toJson(embed()
                    .setDescription("Can't reconnect, I was not disconnected by the bot. It's probably because " +
                            (BanwaveChecker.banwaveOn ?
                            "there's a banwave going on" :
                            !Failsafe.jacobWait.passed() ?
                                    "there's a Jacob Contest":
                                    "of a disconnection error?"))));
            obj.addProperty("image", getScreenshot());
        }
        send(obj);
    }
}
