package com.jelly.farmhelperv2.remote.command.commands.impl;

import cc.polyfrost.oneconfig.utils.Multithreading;
import com.google.gson.JsonObject;
import com.jelly.farmhelperv2.handler.MacroHandler;
import com.jelly.farmhelperv2.remote.command.commands.ClientCommand;
import com.jelly.farmhelperv2.remote.command.commands.Command;
import com.jelly.farmhelperv2.remote.struct.RemoteMessage;
import com.jelly.farmhelperv2.util.InventoryUtils;
import com.jelly.farmhelperv2.util.PlayerUtils;
import net.minecraft.client.gui.inventory.GuiInventory;

import java.util.concurrent.TimeUnit;

@Command(label = "screenshot")

public class ScreenshotCommand extends ClientCommand {

    @Override
    public void execute(RemoteMessage message) {
        JsonObject args = message.args;
        try {
            boolean inventory = args.get("inventory").getAsBoolean();
            if (!inventory) {
                screenshot();
            } else {
                inventory();
            }

        } catch (Exception e) {
            e.printStackTrace();
            screenshot();
        }
    }

    public void screenshot() {
        JsonObject data = new JsonObject();
        data.addProperty("username", mc.getSession().getUsername());
        data.addProperty("image", getScreenshot());
        data.addProperty("uuid", mc.getSession().getPlayerID());
        RemoteMessage response = new RemoteMessage(label, data);
        send(response);
    }

    public void inventory() {
        JsonObject data = new JsonObject();

        boolean wasMacroing;
        if (MacroHandler.getInstance().isMacroToggled()) {
            wasMacroing = true;
            MacroHandler.getInstance().pauseMacro();
        } else {
            wasMacroing = false;
        }

        Multithreading.schedule(() -> {
            try {
                if (mc.currentScreen == null)
                    InventoryUtils.openInventory();
                Thread.sleep(1000);
                String screenshot = getScreenshot();
                Thread.sleep(1000);
                if (mc.currentScreen instanceof GuiInventory)
                    PlayerUtils.closeScreen();
                if (wasMacroing) {
                    MacroHandler.getInstance().resumeMacro();
                }

                data.addProperty("username", mc.getSession().getUsername());
                data.addProperty("image", screenshot);
                data.addProperty("uuid", mc.getSession().getPlayerID());
                RemoteMessage response = new RemoteMessage(label, data);
                send(response);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, 0, TimeUnit.MILLISECONDS);
    }
}
