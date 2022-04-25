package com.jelly.FarmHelper.utils;

import com.jelly.FarmHelper.FarmHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class PlayerUtils {
    private static Minecraft mc = Minecraft.getMinecraft();
    public static int keybindA = mc.gameSettings.keyBindLeft.getKeyCode();
    public static int keybindD = mc.gameSettings.keyBindRight.getKeyCode();
    public static int keybindW = mc.gameSettings.keyBindForward.getKeyCode();
    public static int keybindS = mc.gameSettings.keyBindBack.getKeyCode();
    public static int keybindAttack = mc.gameSettings.keyBindAttack.getKeyCode();
    public static int keybindUseItem = mc.gameSettings.keyBindUseItem.getKeyCode();
    public static int keyBindSpace = mc.gameSettings.keyBindJump.getKeyCode();
    public static int keyBindShift = mc.gameSettings.keyBindSneak.getKeyCode();

    public static void openSBMenu() {
        try {
            mc.thePlayer.closeScreen();
            mc.thePlayer.inventory.currentItem = 8;
            Thread.sleep(500);
            KeyBinding.onTick(keybindUseItem);
            while (!(mc.currentScreen instanceof GuiContainer)) {
                KeyBinding.onTick(keybindUseItem);
                Thread.sleep(100);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static void openSack() {
        int sackSlot = InventoryUtils.findItemInventory("Large Enchanted Agronomy Sack");
        openSBMenu();
        try {
            ItemStack stack = mc.thePlayer.openContainer.getSlot(31).getStack();
            ItemStack clickStack = mc.thePlayer.openContainer.getSlot(sackSlot + 46).getStack();
            while (stack == null || !stack.getDisplayName().contains("Close")) {
                LogUtils.debugLog("t" + Thread.currentThread().isInterrupted());
                if (Thread.currentThread().isInterrupted()) throw new Exception("Detected interrupt - stopping");
                // clickStack = mc.thePlayer.openContainer.getSlot(sackSlot + 45).getStack();
                clickStack = mc.thePlayer.openContainer.getSlot(sackSlot + 46).getStack();

                stack = mc.thePlayer.openContainer.getSlot(31).getStack();
                if (clickStack != null && clickStack.getDisplayName().contains("Large Enchanted Agronomy Sack")) {
                    InventoryUtils.clickWindow(mc.thePlayer.openContainer.windowId, sackSlot + 45, 1);
                }
                Thread.sleep(1000);
            }
            LogUtils.debugLog("Opened sack");
            return;
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static boolean openTrades() {
        openSBMenu();
        InventoryUtils.waitForItemClick(48, "Go Back", 22, "Trades");
        return (mc.thePlayer.openContainer.getSlot(49).getStack().getItem() == Item.getItemFromBlock(Blocks.hopper));
    }

    public static boolean openBazaar() {
        try {
            mc.thePlayer.closeScreen();
            Thread.sleep(100);
            mc.thePlayer.sendChatMessage("/bz");
            while (!(mc.currentScreen instanceof GuiContainer)) {
                Thread.sleep(100);
            }
            return true;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void goToBlock(int x, int z) {
        try {
            double xdiff = x + 0.5 - mc.thePlayer.posX;
            double zdiff = z + 0.5 - mc.thePlayer.posZ;
            double distance = Math.sqrt(Math.pow(xdiff, 2) + Math.pow(zdiff, 2));
            double speed = Math.sqrt((Math.pow(Math.abs(mc.thePlayer.posX - mc.thePlayer.lastTickPosX), 2) + (Math.pow(Math.abs(mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ), 2))));
            double targetYaw = AngleUtils.get360RotationYaw((float) Math.toDegrees(Math.atan2(-xdiff, zdiff)));
            LogUtils.debugLog("Calculated yaw: " + targetYaw);

            AngleUtils.smoothRotateTo((float) targetYaw, 2);
            Thread.sleep(100);

            while (Math.abs(distance) > 0.2) {
                if (Thread.currentThread().isInterrupted()) throw new Exception("Detected interrupt - stopping");
                xdiff = x + 0.5 - mc.thePlayer.posX;
                zdiff = z + 0.5 - mc.thePlayer.posZ;
                targetYaw = AngleUtils.get360RotationYaw((float) Math.toDegrees(Math.atan2(-xdiff, zdiff)));
                if (1.4 * speed < distance) AngleUtils.hardRotate((float) targetYaw);
                updateKeys(true, false, false, false, false, 1.4 * speed >= distance);
                distance = Math.sqrt(Math.pow((x + 0.5 - mc.thePlayer.posX), 2) + Math.pow((z + 0.5 - mc.thePlayer.posZ), 2));
                speed = Math.sqrt((Math.pow(Math.abs(mc.thePlayer.posX - mc.thePlayer.lastTickPosX), 2) + (Math.pow(Math.abs(mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ), 2))));
                Thread.sleep(20);
            }
            updateKeys(false, false, false, false, false, false);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static void updateKeys(boolean wBool, boolean sBool, boolean aBool, boolean dBool, boolean atkBool, boolean shiftBool) {
        KeyBinding.setKeyBindState(keybindW, wBool);
        KeyBinding.setKeyBindState(keybindS, sBool);
        KeyBinding.setKeyBindState(keybindA, aBool);
        KeyBinding.setKeyBindState(keybindD, dBool);
        KeyBinding.setKeyBindState(keybindAttack, atkBool);
        KeyBinding.setKeyBindState(keyBindShift, shiftBool);
    }
}
