package com.github.may2beez.farmhelperv2.remote.command.commands.impl;

import com.github.may2beez.farmhelperv2.remote.command.commands.ClientCommand;
import com.github.may2beez.farmhelperv2.remote.command.commands.Command;
import com.github.may2beez.farmhelperv2.remote.struct.RemoteMessage;
import com.github.may2beez.farmhelperv2.util.LogUtils;
import com.github.may2beez.farmhelperv2.util.helper.Clock;
import com.google.gson.JsonObject;
import net.minecraft.util.ChatComponentText;

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
        mc.getNetHandler().getNetworkManager().closeChannel(new ChatComponentText("Reconnecting in " + LogUtils.formatTime(delay)));
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
