package com.jelly.farmhelper.remote.command.commands;

import com.google.gson.JsonObject;
import com.jelly.farmhelper.remote.command.BaseCommand;
import com.jelly.farmhelper.remote.command.Command;
import com.jelly.farmhelper.remote.event.WebsocketMessage;
import com.jelly.farmhelper.utils.Clock;
import net.minecraft.util.ChatComponentText;

import static com.jelly.farmhelper.utils.Utils.formatTime;

@Command(label = "reconnect")
public class ReconnectCommand extends BaseCommand {
    public static boolean isEnabled = false;

    public static Clock reconnectClock = new Clock();

    @Override
    public void execute(WebsocketMessage event) {
        JsonObject args = event.args;
        int delay = 5_000;
        if (args.has("delay")) {
            delay = args.get("delay").getAsInt();
        }
        reconnectClock.schedule(delay);
        mc.getNetHandler().getNetworkManager().closeChannel(new ChatComponentText("Reconnecting in " + formatTime(delay)));
        isEnabled = true;
        JsonObject data = new JsonObject();
        data.addProperty("username", mc.getSession().getUsername());
        data.addProperty("image", getScreenshot());
        data.addProperty("delay", delay);
        data.addProperty("uuid", mc.getSession().getPlayerID());
        WebsocketMessage response = new WebsocketMessage(label, data);
        send(response);
    }
}
