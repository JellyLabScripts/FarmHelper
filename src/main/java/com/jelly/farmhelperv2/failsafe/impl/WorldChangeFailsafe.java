package com.jelly.farmhelperv2.failsafe.impl;

import cc.polyfrost.oneconfig.utils.Multithreading;
import com.jelly.farmhelperv2.config.FarmHelperConfig;
import com.jelly.farmhelperv2.config.page.FailsafeNotificationsPage;
import com.jelly.farmhelperv2.failsafe.Failsafe;
import com.jelly.farmhelperv2.failsafe.FailsafeManager;
import com.jelly.farmhelperv2.feature.impl.LagDetector;
import com.jelly.farmhelperv2.handler.GameStateHandler;
import com.jelly.farmhelperv2.handler.MacroHandler;
import com.jelly.farmhelperv2.util.LogUtils;
import com.jelly.farmhelperv2.util.helper.AudioManager;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.concurrent.TimeUnit;

public class WorldChangeFailsafe extends Failsafe {
    private static WorldChangeFailsafe instance;

    public static WorldChangeFailsafe getInstance() {
        if (instance == null) {
            instance = new WorldChangeFailsafe();
        }
        return instance;
    }

    @Override
    public int getPriority() {
        return 2;
    }

    @Override
    public FailsafeManager.EmergencyType getType() {
        return FailsafeManager.EmergencyType.WORLD_CHANGE_CHECK;
    }

    @Override
    public boolean shouldSendNotification() {
        return FailsafeNotificationsPage.notifyOnWorldChangeFailsafe;
    }

    @Override
    public boolean shouldPlaySound() {
        return FailsafeNotificationsPage.alertOnWorldChangeFailsafe;
    }

    @Override
    public boolean shouldTagEveryone() {
        return FailsafeNotificationsPage.tagEveryoneOnWorldChangeFailsafe;
    }

    @Override
    public boolean shouldAltTab() {
        return FailsafeNotificationsPage.autoAltTabOnWorldChangeFailsafe;
    }

    @Override
    public void onTickDetection(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!MacroHandler.getInstance().isMacroToggled()) return;
        if (FailsafeManager.getInstance().triggeredFailsafe.isPresent()
                && FailsafeManager.getInstance().triggeredFailsafe.get().getType() == FailsafeManager.EmergencyType.WORLD_CHANGE_CHECK)
            return;

        if (GameStateHandler.getInstance().getLocation() == GameStateHandler.Location.LIMBO) {
            FailsafeManager.getInstance().possibleDetection(this);
            sendOnce = false;
        }
    }

    @Override
    public void onChatDetection(ClientChatReceivedEvent event) {
        chatOne(event);
        chatTwo(event);
    }

    public void chatOne(ClientChatReceivedEvent event) {
        if (FailsafeManager.getInstance().firstCheckReturn()) return;
        if (FailsafeManager.getInstance().triggeredFailsafe.isPresent()
                && FailsafeManager.getInstance().triggeredFailsafe.get().getType() != FailsafeManager.EmergencyType.WORLD_CHANGE_CHECK)
            return;

        String message = StringUtils.stripControlCodes(event.message.getUnformattedText());
        if (message.contains(":")) return;
        if (message.contains("You were spawned in Limbo.") || message.contains("/limbo") || message.startsWith("A kick occurred in your connection")) {
            LogUtils.sendWarning("[Failsafe] Got kicked to Limbo!");
            FailsafeManager.getInstance().possibleDetection(this);
        }
    }

    public void chatTwo(ClientChatReceivedEvent event) {
        if (event.type != 0) return;
        if (FailsafeManager.getInstance().triggeredFailsafe.isPresent()
                && FailsafeManager.getInstance().triggeredFailsafe.get().getType() != FailsafeManager.EmergencyType.WORLD_CHANGE_CHECK)
            return;

        String message = StringUtils.stripControlCodes(event.message.getUnformattedText());
        if (message.contains(":")) return;
        if (message.contains("DYNAMIC") || message.contains("Something went wrong trying to send ") || message.contains("don't spam") || message.contains("A disconnect occurred ") || message.contains("An exception occurred ") || message.contains("Couldn't warp ") || message.contains("You are sending commands ") || message.contains("Cannot join ") || message.contains("There was a problem ") || message.contains("You cannot join ") || message.contains("You were kicked while ") || message.contains("You are already playing") || message.contains("You cannot join SkyBlock from here!")) {
            LogUtils.sendWarning("[Failsafe] Can't warp to the garden! Will try again in a moment.");
            FailsafeManager.getInstance().scheduleDelay(10000);
        }
    }

    private boolean sendOnce = false;

    @Override
    public void duringFailsafeTrigger() {
        if (!FarmHelperConfig.autoTPOnWorldChange) {
            LogUtils.sendDebug("[Failsafe] Auto TP on world change is disabled! Disabling macro and disconnecting...");
            MacroHandler.getInstance().disableMacro();
            Multithreading.schedule(() -> {
                try {
                    mc.getNetHandler().getNetworkManager().closeChannel(new ChatComponentText("Your world has been changed and you've got \"Auto TP On world is disabled\""));
                    AudioManager.getInstance().resetSound();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, 1_500, TimeUnit.MILLISECONDS);
            return;
        }

        switch (worldChangeState) {
            case NONE:
                MacroHandler.getInstance().pauseMacro();
                FailsafeManager.getInstance().scheduleRandomDelay(250, 500);
                worldChangeState = WorldChangeState.WAIT_BEFORE_START;
                break;
            case WAIT_BEFORE_START:
                FailsafeManager.getInstance().scheduleRandomDelay(500, 500);
                worldChangeState = WorldChangeState.END;
                break;
            case END:
                if (GameStateHandler.getInstance().getLocation() == GameStateHandler.Location.TELEPORTING) {
                    FailsafeManager.getInstance().scheduleDelay(1000);
                    return;
                }
                if (GameStateHandler.getInstance().getLocation() == GameStateHandler.Location.LOBBY && !LagDetector.getInstance().isLagging()) {
                    LogUtils.sendDebug("[Failsafe] In lobby, sending /skyblock command...");
                    mc.thePlayer.sendChatMessage("/skyblock");
                    if (sendOnce) {
                        FailsafeManager.getInstance().scheduleRandomDelay(60_000, 4000);
                    } else {
                        FailsafeManager.getInstance().scheduleRandomDelay(4_500, 5000);
                        sendOnce = true;
                    }
                    return;
                }
                if (GameStateHandler.getInstance().getLocation() == GameStateHandler.Location.LIMBO) {
                    LogUtils.sendDebug("[Failsafe] In Limbo, sending /l command...");
                    mc.thePlayer.sendChatMessage("/l");
                    sendOnce = false;
                    FailsafeManager.getInstance().scheduleRandomDelay(4500, 1000);
                    return;
                }
                if (GameStateHandler.getInstance().inGarden()) {
                    LogUtils.sendDebug("[Failsafe] Came back to the garden. Farming...");
                    FailsafeManager.getInstance().stopFailsafes();
                    MacroHandler.getInstance().resumeMacro();
                    return;
                } else if (!LagDetector.getInstance().isLagging()) {
                    LogUtils.sendDebug("[Failsafe] Sending /warp garden command...");
                    MacroHandler.getInstance().triggerWarpGarden(true, true);
                    FailsafeManager.getInstance().scheduleRandomDelay(8500, 1000);
                }
                break;
        }
    }

    @Override
    public void endOfFailsafeTrigger() {
        worldChangeState = WorldChangeState.NONE;
        sendOnce = false;
    }

    private WorldChangeState worldChangeState = WorldChangeState.NONE;

    enum WorldChangeState {
        NONE,
        WAIT_BEFORE_START,
        LOOK_AROUND,
        END
    }
}
