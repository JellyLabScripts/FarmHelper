package com.jelly.farmhelper.remote.command.commands;

import com.google.gson.JsonObject;
import com.jelly.farmhelper.macros.MacroHandler;
import com.jelly.farmhelper.remote.command.BaseCommand;
import com.jelly.farmhelper.remote.command.Command;
import com.jelly.farmhelper.remote.event.WebsocketMessage;
import com.jelly.farmhelper.utils.PlayerUtils;
import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraft.client.settings.KeyBinding;

import java.lang.reflect.Method;

import static com.jelly.farmhelper.utils.Utils.clickWindow;

@Command(label = "setspeed")
public class SetSpeedCommand extends BaseCommand {
    private static String speed = "-1";
    private static JsonObject data;

    @Override
    public void execute(WebsocketMessage message) {
//        data = event.obj;
//        speed = data.get("speed").getAsString();
//        if (nullCheck() && !MacroHandler.randomizing) {
//            if (PlayerUtils.getRancherBootSpeed() == -1) {
//                data.addProperty("embed", toJson(embed().setDescription("You need to have Rancher's boots equipped for this command to work.")));
//            } else if (PlayerUtils.getRancherBootSpeed() == Integer.parseInt(speed)) {
//                data.addProperty("embed", toJson(embed().setDescription("Your Rancher's boots are already at " + speed + " speed.")));
//            } else {
//                try {
//                    new Thread(setRancherSpeed).start();
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        } else {
//            data.addProperty("embed", toJson(embed().setDescription("I'm not in a world, therefore I can't set Rancher's boots speed.")));
//        }
//        send(data);
    }

    static Runnable setRancherSpeed = () -> {
        boolean wasMacroing = false;
        try {
            if (MacroHandler.isMacroing) {
                MacroHandler.isMacroing = false;
                MacroHandler.disableCurrentMacro();
                wasMacroing = true;
            }
            Thread.sleep(500);
            PlayerUtils.openInventory();
            Thread.sleep(500);
            clickWindow(mc.thePlayer.openContainer.windowId, 40, 0, 1);
            Thread.sleep(500);
            clickWindow(mc.thePlayer.openContainer.windowId, 8, 0, 0);
            Thread.sleep(500);
            clickWindow(mc.thePlayer.openContainer.windowId, 40, 0, 0);
            Thread.sleep(500);
            mc.thePlayer.closeScreen();
            Thread.sleep(500);
            mc.thePlayer.inventory.currentItem = 4;
            Thread.sleep(500);
            KeyBinding.onTick(mc.gameSettings.keyBindAttack.getKeyCode());
            Thread.sleep(1000);
            Method m = ((GuiEditSign) mc.currentScreen).getClass().getDeclaredMethod("func_73869_a", char.class, int.class);
            m.setAccessible(true);
            m.invoke(mc.currentScreen, '\r', 14);
            Thread.sleep(300);
            m.invoke(mc.currentScreen, '\r', 14);
            Thread.sleep(300);
            m.invoke(mc.currentScreen, '\r', 14);
            Thread.sleep(300);
            for (char c : speed.toCharArray()) {
                Thread.sleep(300);
                m.invoke(mc.currentScreen, c, 16);
            }

            Thread.sleep(300);
            mc.thePlayer.closeScreen();
            Thread.sleep(100);
            mc.thePlayer.inventory.currentItem = 4;
            KeyBinding.onTick(mc.gameSettings.keyBindUseItem.getKeyCode());

            Thread.sleep(100);
            if (PlayerUtils.getItemInHotbar("Hoe") == -1) {
                PlayerUtils.openInventory();
                Thread.sleep(300);
                clickWindow(mc.thePlayer.openContainer.windowId, PlayerUtils.getSlotForItem("Hoe"), 0, 1);
                Thread.sleep(200);
                mc.thePlayer.closeScreen();
            }

            Thread.sleep(200);
            mc.thePlayer.inventory.currentItem = PlayerUtils.getItemInHotbar("Hoe");
            data.addProperty("embed", toJson(embed().setDescription("Done, I'm macroing at " + speed + " speed now.")));
            if (wasMacroing) {
                MacroHandler.isMacroing = true;
                MacroHandler.enableCurrentMacro();
            }
        } catch (Exception e) {
            data.addProperty("embed", toJson(embed().setDescription("Something went wrong while setting Rancher's boots speed.")));
        }
        send(data);
    };
}
