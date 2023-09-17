package com.jelly.farmhelper.remote.command.commands;

import com.google.gson.JsonObject;
import com.jelly.farmhelper.remote.struct.ClientCommand;
import com.jelly.farmhelper.remote.struct.Command;
import com.jelly.farmhelper.remote.struct.RemoteMessage;
import com.jelly.farmhelper.utils.Clock;
import net.minecraft.util.ChatComponentText;

import static com.jelly.farmhelper.utils.Utils.formatTime;

@Command(label = "reconnect")
public class ReconnectCommand extends ClientCommand {
    public static boolean isEnabled = false;

    public static Clock reconnectClock = new Clock();

    @Override
    public void execute(RemoteMessage event) {
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
        RemoteMessage response = new RemoteMessage(label, data);
        send(response);
    }
}
