package com.jelly.farmhelper.features;

import com.jelly.farmhelper.macros.MacroHandler;
import com.jelly.farmhelper.utils.BlockUtils;
import com.jelly.farmhelper.utils.Clock;
import com.jelly.farmhelper.utils.KeyBindUtils;
import com.jelly.farmhelper.utils.LocationUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class Antistuck {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public static boolean stuck;
    private static double lastX;
    private static double lastZ;
    private static double lastY;
    public static final Clock cooldown = new Clock();

    public static boolean unstuckThreadIsRunning = false;
    public static boolean unstuckLastMoveBack = false;
    public static final Runnable unstuckRunnable = () -> {
        try {
            KeyBindUtils.stopMovement();
            Thread.sleep(20);
            KeyBindUtils.holdThese(mc.gameSettings.keyBindSneak);
            Thread.sleep(100);
            KeyBindUtils.holdThese(mc.gameSettings.keyBindLeft, mc.gameSettings.keyBindSneak);
            Thread.sleep(500);
            KeyBindUtils.stopMovement();
            Thread.sleep(20);
            KeyBindUtils.holdThese(mc.gameSettings.keyBindRight, mc.gameSettings.keyBindSneak);
            Thread.sleep(500);
            KeyBindUtils.stopMovement();
            Thread.sleep(20);
            KeyBindUtils.holdThese(unstuckLastMoveBack ? mc.gameSettings.keyBindForward : mc.gameSettings.keyBindBack, mc.gameSettings.keyBindSneak);
            Thread.sleep(500);
            KeyBindUtils.stopMovement();
            Thread.sleep(20);
            KeyBindUtils.holdThese(unstuckLastMoveBack ? mc.gameSettings.keyBindBack : mc.gameSettings.keyBindForward, mc.gameSettings.keyBindSneak);
            Thread.sleep(200);
            KeyBindUtils.stopMovement();
            Thread.sleep(200);
            Antistuck.stuck = false;
            Antistuck.cooldown.schedule(3500);
            unstuckThreadIsRunning = false;
            unstuckThreadInstance = null;
        } catch (Throwable e) {
            e.printStackTrace();
        }
    };
    public static Thread unstuckThreadInstance = null;

    @SubscribeEvent
    public final void tick(TickEvent.ClientTickEvent event) {
        if(event.phase == TickEvent.Phase.END)
            return;

        if (MacroHandler.currentMacro == null || !MacroHandler.currentMacro.enabled || mc.thePlayer == null || mc.theWorld == null || LocationUtils.currentIsland != LocationUtils.Island.GARDEN || mc.currentScreen != null) {
            lastX = 10000;
            lastZ = 10000;
            lastY = 10000;
            stuck = false;
            cooldown.reset();
            return;
        }
        if(!MacroHandler.isMacroing)
            return;
        if (cooldown.passed()) {
            Block blockIn = BlockUtils.getRelativeBlock(0, 0, 0);
            stuck = (!blockIn.equals(Blocks.end_portal_frame) && !BlockUtils.isWalkable(blockIn) && !(blockIn instanceof BlockDoor || blockIn instanceof BlockTrapDoor)) || (Math.abs(mc.thePlayer.posX - lastX) < 1 && Math.abs(mc.thePlayer.posZ - lastZ) < 1 && Math.abs(mc.thePlayer.posY - lastY) < 1);
            lastX = mc.thePlayer.posX;
            lastZ = mc.thePlayer.posZ;
            lastY = mc.thePlayer.posY;
            cooldown.schedule(3000);
        }
    }
}
