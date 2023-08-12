package com.jelly.farmhelper.features;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.hud.DebugHUD;
import com.jelly.farmhelper.macros.MacroHandler;
import com.jelly.farmhelper.remote.command.commands.ReconnectCommand;
import com.jelly.farmhelper.utils.Clock;
import com.jelly.farmhelper.utils.UngrabUtils;
import com.jelly.farmhelper.world.GameState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

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

    @SubscribeEvent
    public final void tick(TickEvent.ClientTickEvent event) {
        if (ReconnectCommand.isEnabled) {
            if (ReconnectCommand.reconnectClock.getRemainingTime() > 0) {
                return;
            } else if (!((Failsafe.jacobWait.passed() && FarmHelper.config.enableJacobFailsafes) && (BanwaveChecker.banwaveOn && FarmHelper.config.enableLeaveOnBanwave))) {
                ReconnectCommand.isEnabled = false;
                FMLClientHandler.instance().connectToServer(new GuiMultiplayer(new GuiMainMenu()), new ServerData("bozo", FarmHelper.gameState.serverIP, false));
            }
        }

        if (event.phase == TickEvent.Phase.END || !MacroHandler.isMacroing)
            return;
        if(BanwaveChecker.banwaveOn && FarmHelper.config.enableLeaveOnBanwave)
            return;
        if(!Failsafe.jacobWait.passed() && FarmHelper.config.enableJacobFailsafes)
            return;
        if ((mc.currentScreen instanceof GuiDisconnected)) {
            if (Failsafe.emergency) {
                Failsafe.emergency = false;
                Failsafe.restartAfterFailsafeCooldown.reset();
            }
            if (waitTime >= (FarmHelper.config.delayBeforeReconnecting * 20)) {
                waitTime = 0;
                try {
                    FMLClientHandler.instance().connectToServer(new GuiMultiplayer(new GuiMainMenu()), new ServerData("bozo", FarmHelper.gameState.serverIP != null ? FarmHelper.gameState.serverIP : "mc.hypixel.net", false));
                    cooldown.schedule(1100);
                    currentState = reconnectingState.JOINING_LOBBY;
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Failed to reconnect to server!");
                }
            } else {
                waitTime++;
            }
        }
        if (cooldown.isScheduled() && cooldown.passed()) {
            if (mc.thePlayer == null || mc.theWorld == null) return;
            if (mc.currentScreen == null) {
                if (FarmHelper.gameState.currentLocation == GameState.location.LOBBY) {
                    switch (currentState) {
                        case JOINING_LOBBY:
                            final ItemStack held = mc.thePlayer.inventory.getCurrentItem();
                            if (held != null) {
                                final String displayName = held.getDisplayName();
                                if (displayName.equals("§aGame Menu §7(Right Click)")) {
                                    mc.thePlayer.sendChatMessage("/play sb");
                                }
                            }
                            currentState = reconnectingState.JOINING_SKYBLOCK;
                            cooldown.schedule(1100);
                            break;
                        case JOINING_SKYBLOCK:
                            currentState = reconnectingState.ENABLING_MACRO;
                            cooldown.schedule(1500);
                            break;
                        case ENABLING_MACRO:
                            currentState = reconnectingState.NONE;
                            MacroHandler.enableCurrentMacro();
                            break;
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public final void onLoadWorld(WorldEvent.Load event) {
        if (mc.theWorld == null || mc.thePlayer == null) return;
        if (currentState == reconnectingState.NONE) return;
        if (FarmHelper.config.autoUngrabMouse) UngrabUtils.ungrabMouse();
    }
}
