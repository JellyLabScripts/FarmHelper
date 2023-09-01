package com.jelly.farmhelper.features;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.macros.MacroHandler;
import com.jelly.farmhelper.remote.command.commands.ReconnectCommand;
import com.jelly.farmhelper.utils.Clock;
import com.jelly.farmhelper.utils.UngrabUtils;
import com.jelly.farmhelper.utils.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class AutoReconnect {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static double waitTime = 0;
    public static final Clock reconnectingCooldown = new Clock();
    enum reconnectingState {
        NONE,
        JOINING_LOBBY
    }
    public static reconnectingState currentState = reconnectingState.NONE;

    @SubscribeEvent
    public final void tick(TickEvent.ClientTickEvent event) {
        if (ReconnectCommand.isEnabled) {
            if (ReconnectCommand.reconnectClock.getRemainingTime() > 0) {
                return;
            } else if (!((FailsafeNew.isJacobFailsafeExceeded && FarmHelper.config.enableJacobFailsafes) && (BanwaveChecker.banwaveOn && FarmHelper.config.enableLeaveOnBanwave))) {
                ReconnectCommand.isEnabled = false;
                FMLClientHandler.instance().connectToServer(new GuiMultiplayer(new GuiMainMenu()), new ServerData("bozo", FarmHelper.gameState.serverIP, false));
            }
        }

        if (event.phase == TickEvent.Phase.END || !MacroHandler.isMacroing)
            return;
        if (BanwaveChecker.banwaveOn && FarmHelper.config.enableLeaveOnBanwave)
            return;
        if (FailsafeNew.isJacobFailsafeExceeded && FarmHelper.config.enableJacobFailsafes)
            return;
        if (mc.currentScreen instanceof GuiDisconnected) {
            MacroHandler.disableCurrentMacro(true);
            if (waitTime >= (FarmHelper.config.delayBeforeReconnecting * 20)) {
                waitTime = 0;
                if (FailsafeNew.emergency) {
                    FailsafeNew.resetFailsafes();
                    Utils.disablePingAlert();
                }
                try {
                    FMLClientHandler.instance().connectToServer(new GuiMultiplayer(new GuiMainMenu()), new ServerData("bozo", FarmHelper.gameState.serverIP != null ? FarmHelper.gameState.serverIP : "mc.hypixel.net", false));
                    reconnectingCooldown.schedule(3000);
                    currentState = reconnectingState.JOINING_LOBBY;
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Failed to reconnect to server!");
                }
            } else {
                waitTime++;
            }
        }
        if (reconnectingCooldown.isScheduled() && reconnectingCooldown.passed()) {
            if (mc.thePlayer == null || mc.theWorld == null) return;
            if (mc.currentScreen == null) return;
            if (currentState != reconnectingState.JOINING_LOBBY) return;
//            if (LocationUtils.currentIsland != LocationUtils.Island.LOBBY) return;
            currentState = reconnectingState.NONE;
            reconnectingCooldown.reset();
        }
    }

    @SubscribeEvent
    public final void onLoadWorld(WorldEvent.Load event) {
        if (mc.theWorld == null || mc.thePlayer == null) return;
        if (currentState == reconnectingState.NONE) return;
        if (FarmHelper.config.autoUngrabMouse) UngrabUtils.ungrabMouse();
        if (FarmHelper.config.enableScheduler) Scheduler.pause();
    }
}
