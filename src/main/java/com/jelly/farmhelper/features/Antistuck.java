package com.jelly.farmhelper.features;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.macros.MacroHandler;
import com.jelly.farmhelper.utils.*;
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

    public static boolean unstuckThreadIsRunning = false;
    public static boolean unstuckLastMoveBack = false;
    public static int unstuckTries = 0;
    public static final Runnable unstuckRunnable = () -> {
        try {
            Antistuck.stuck = true;
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
            unstuckTries++;
            Antistuck.stuck = false;
            Antistuck.notMovingTimer.schedule();
            unstuckThreadIsRunning = false;
            unstuckThreadInstance = null;
        } catch (Throwable e) {
            e.printStackTrace();
        }
    };
    public static Thread unstuckThreadInstance = null;
    public static Timer notMovingTimer = new Timer();

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END)
            return;

        if (!MacroHandler.isMacroing || MacroHandler.currentMacro == null || !MacroHandler.currentMacro.enabled || mc.thePlayer == null || mc.theWorld == null || LocationUtils.currentIsland != LocationUtils.Island.GARDEN || mc.currentScreen != null || unstuckThreadIsRunning || VisitorsMacro.isEnabled()) {
            notMovingTimer.schedule();
            lastX = 10000;
            lastZ = 10000;
            lastY = 10000;
            return;
        }

        double dx = Math.abs(mc.thePlayer.posX - lastX);
        double dz = Math.abs(mc.thePlayer.posZ - lastZ);
        double dy = Math.abs(mc.thePlayer.posY - lastY);
        if (dx < 1 && dz < 1 && dy < 1 && !FailsafeNew.emergency && notMovingTimer.isScheduled()) {
            if (notMovingTimer.hasPassed(3000L)) {
                notMovingTimer.reset();
                stuck = true;
            }
        } else {
            notMovingTimer.schedule();
            stuck = false;
            lastX = mc.thePlayer.posX;
            lastZ = mc.thePlayer.posZ;
            lastY = mc.thePlayer.posY;
        }
    }
}
