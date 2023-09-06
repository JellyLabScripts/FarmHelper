package com.jelly.farmhelper.remote.command.commands;

import com.google.gson.JsonObject;
import com.jelly.farmhelper.features.VisitorsMacro;
import com.jelly.farmhelper.macros.MacroHandler;
import com.jelly.farmhelper.remote.command.BaseCommand;
import com.jelly.farmhelper.remote.command.Command;
import com.jelly.farmhelper.remote.event.WebsocketMessage;
import com.jelly.farmhelper.utils.PlayerUtils;
import com.jelly.farmhelper.utils.Utils;
import net.minecraft.client.settings.KeyBinding;

import java.lang.reflect.Method;
import java.util.Objects;

import static com.jelly.farmhelper.utils.Utils.clickWindow;

@Command(label = "setspeed")
public class SetSpeedCommand extends BaseCommand {
    private static int speed = -1;
    private static JsonObject data;

    @Override
    public void execute(WebsocketMessage message) {
        JsonObject args = message.args;
        speed = args.get("speed").getAsInt();
        data = new JsonObject();
        data.addProperty("username", mc.getSession().getUsername());
        data.addProperty("uuid", mc.getSession().getPlayerID());
        data.addProperty("speed", speed);

        if (PlayerUtils.getRancherBootSpeed() == -1) {
            data.addProperty("error", "You need to have Rancher's boots equipped for this command to work.");
        } else if (PlayerUtils.getRancherBootSpeed() == speed) {
            data.addProperty("error", "Your Rancher's boots are already at " + speed + " speed.");
        } else {
            try {
                new Thread(setRancherSpeed).start();
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        WebsocketMessage response = new WebsocketMessage(label, data);
        send(response);
    }

    Runnable setRancherSpeed = () -> {
        boolean wasMacroing = false;
        try {
            if (MacroHandler.isMacroing) {
                MacroHandler.disableCurrentMacro();
                wasMacroing = true;
            }
            Thread.sleep(500);
            PlayerUtils.openInventory();
            Thread.sleep(500);
            if (mc.thePlayer.openContainer == null) {
                throw new Exception("Inventory not open");
            }
            clickWindow(mc.thePlayer.openContainer.windowId, 40, 0, 1);
            Thread.sleep(1000);
            clickWindow(mc.thePlayer.openContainer.windowId, 8, 0, 0);
            Thread.sleep(1000);
            clickWindow(mc.thePlayer.openContainer.windowId, 40, 0, 0);
            Thread.sleep(1000);
            mc.thePlayer.closeScreen();
            Thread.sleep(500);
            mc.thePlayer.inventory.currentItem = 4;
            Thread.sleep(500);
            KeyBinding.onTick(mc.gameSettings.keyBindAttack.getKeyCode());
            Thread.sleep(1000);
            Utils.signText = String.valueOf(speed);
            Thread.sleep(1500);
            if (!Objects.equals(Utils.signText, "")) {
                throw new Exception("Sign text not empty");
            }
        } catch (Exception e) {
            e.printStackTrace();
            data.addProperty("error", "Something went wrong while setting Rancher's boots speed.");
        } finally {
            try {
                Thread.sleep(300);
                mc.thePlayer.closeScreen();
                Thread.sleep(100);
                mc.thePlayer.inventory.currentItem = 4;
                KeyBinding.onTick(mc.gameSettings.keyBindUseItem.getKeyCode());

                Thread.sleep(100);
                if (PlayerUtils.getFarmingTool(MacroHandler.crop, true, false) == -1) {
                    PlayerUtils.openInventory();
                    Thread.sleep(300);
                    clickWindow(mc.thePlayer.openContainer.windowId, PlayerUtils.getFarmingTool(MacroHandler.crop, false, true), 0, 1);
                    Thread.sleep(200);
                    mc.thePlayer.closeScreen();
                }

                Thread.sleep(200);
                mc.thePlayer.inventory.currentItem = PlayerUtils.getFarmingTool(MacroHandler.crop, false, false);

                if (wasMacroing) {
                    MacroHandler.isMacroing = true;
                    MacroHandler.enableCurrentMacro();
                }
            } catch (Exception e) {
                e.printStackTrace();
                data.addProperty("error", "Critical error while setting Rancher's boots speed.");
            }
        }
        WebsocketMessage response = new WebsocketMessage(label, data);
        send(response);
    };
}
