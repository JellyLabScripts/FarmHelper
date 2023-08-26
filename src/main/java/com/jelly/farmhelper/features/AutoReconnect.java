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

import javax.rmi.CORBA.Util;

public class AutoReconnect {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static double waitTime = 0;
    private static final Clock cooldown = new Clock();
    enum reconnectingState {
        NONE,
        JOINING_SKYBLOCK,
        JOINING_LOBBY,
        ENABLING_MACRO
    }
    public static reconnectingState currentState = reconnectingState.NONE;
    public static boolean reconnecting = false;

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
        if ((mc.currentScreen instanceof GuiDisconnected)) {
            MacroHandler.disableCurrentMacro(true);
            if (waitTime >= (FarmHelper.config.delayBeforeReconnecting * 20)) {
                waitTime = 0;
                if (FailsafeNew.emergency) {
                    FailsafeNew.emergency = false;
                    FailsafeNew.restartAfterFailsafeCooldown.reset();
                    FailsafeNew.stopAllFailsafeThreads();
                    Utils.disablePingAlert();
                }
                try {
                    reconnecting = true;
                    FMLClientHandler.instance().connectToServer(new GuiMultiplayer(new GuiMainMenu()), new ServerData("bozo", FarmHelper.gameState.serverIP != null ? FarmHelper.gameState.serverIP : "mc.hypixel.net", false));
//                    cooldown.schedule(6500);
//                    currentState = reconnectingState.JOINING_LOBBY;
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Failed to reconnect to server!");
                }
            } else {
                waitTime++;
            }
        }
//        if (cooldown.isScheduled() && cooldown.passed()) {
//            if (mc.thePlayer == null || mc.theWorld == null) return;
//            if (mc.currentScreen == null) {
//                switch (currentState) {
//                    case JOINING_LOBBY:
//                        if (LocationUtils.currentIsland != LocationUtils.Island.LOBBY) return;
//                        final ItemStack held = mc.thePlayer.inventory.getCurrentItem();
//                        if (held != null) {
//                            final String displayName = held.getDisplayName();
//                            if (displayName.equals("§aGame Menu §7(Right Click)")) {
//                                mc.thePlayer.sendChatMessage("/skyblock");
//                            }
//                        }
//                        currentState = reconnectingState.JOINING_SKYBLOCK;
//                        cooldown.schedule(3500);
//                        break;
//                    case JOINING_SKYBLOCK:
//                        cooldown.schedule(3500);
//                        if (LocationUtils.currentIsland != LocationUtils.Island.GARDEN && LocationUtils.currentIsland != LocationUtils.Island.PRIVATE_ISLAND) {
//                            mc.thePlayer.sendChatMessage("/skyblock"); // if it gets stuck in the lobby
//                            return;
//                        }
//                        currentState = reconnectingState.ENABLING_MACRO;
//                        break;
//                    case ENABLING_MACRO:
//                        if (LocationUtils.currentIsland != LocationUtils.Island.GARDEN && LocationUtils.currentIsland != LocationUtils.Island.PRIVATE_ISLAND) return;
//                        currentState = reconnectingState.NONE;
//                        if (FarmHelper.config.enableScheduler) Scheduler.start();
//                        MacroHandler.enableCurrentMacro();
//                        break;
//                }
//            }
//        }
    }

    @SubscribeEvent
    public final void onLoadWorld(WorldEvent.Load event) {
        if (mc.theWorld == null || mc.thePlayer == null) return;
        if (currentState == reconnectingState.NONE) return;
        if (FarmHelper.config.autoUngrabMouse) UngrabUtils.ungrabMouse();
    }
}
