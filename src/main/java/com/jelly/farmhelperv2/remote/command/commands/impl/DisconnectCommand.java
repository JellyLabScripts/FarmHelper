package com.jelly.farmhelperv2.remote.command.commands.impl;

import cc.polyfrost.oneconfig.utils.Multithreading;
import com.google.gson.JsonObject;
import com.jelly.farmhelperv2.handler.MacroHandler;
import com.jelly.farmhelperv2.remote.command.commands.ClientCommand;
import com.jelly.farmhelperv2.remote.command.commands.Command;
import com.jelly.farmhelperv2.remote.struct.RemoteMessage;
import net.minecraft.util.ChatComponentText;

import java.util.concurrent.TimeUnit;

@Command(label = "disconnect")
public class DisconnectCommand extends ClientCommand {

    @Override
    public void execute(RemoteMessage event) {
        if (MacroHandler.getInstance().isMacroToggled())
            MacroHandler.getInstance().disableMacro();
        try {
            mc.getNetHandler().getNetworkManager().closeChannel(new ChatComponentText("Disconnected through Discord Remote Control bot"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        Multithreading.schedule(() -> {
            JsonObject data = new JsonObject();
            data.addProperty("username", mc.getSession().getUsername());
            data.addProperty("image", getScreenshot());
            data.addProperty("uuid", mc.getSession().getPlayerID());
            RemoteMessage response = new RemoteMessage(label, data);
            send(response);
        }, 1_500, TimeUnit.MILLISECONDS);
    }
}