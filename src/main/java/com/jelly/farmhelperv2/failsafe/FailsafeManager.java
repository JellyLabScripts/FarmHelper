package com.jelly.farmhelperv2.failsafe;

import cc.polyfrost.oneconfig.utils.Multithreading;
import com.jelly.farmhelperv2.config.FarmHelperConfig;
import com.jelly.farmhelperv2.event.ReceivePacketEvent;
import com.jelly.farmhelperv2.failsafe.impl.RotationFailsafe;
import com.jelly.farmhelperv2.feature.FeatureManager;
import com.jelly.farmhelperv2.feature.impl.Scheduler;
import com.jelly.farmhelperv2.handler.MacroHandler;
import com.jelly.farmhelperv2.macro.AbstractMacro;
import com.jelly.farmhelperv2.util.FailsafeUtils;
import com.jelly.farmhelperv2.util.InventoryUtils;
import com.jelly.farmhelperv2.util.LogUtils;
import com.jelly.farmhelperv2.util.PlayerUtils;
import com.jelly.farmhelperv2.util.helper.AudioManager;
import com.jelly.farmhelperv2.util.helper.Clock;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.util.StringUtils;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.Display;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class FailsafeManager {
    private final Minecraft mc = Minecraft.getMinecraft();
    private static FailsafeManager instance;

    public static FailsafeManager getInstance() {
        if (instance == null) {
            instance = new FailsafeManager();
        }
        return instance;
    }
    public final List<Failsafe> failsafes = new ArrayList<>();
    public Optional<Failsafe> triggeredFailsafe = Optional.empty();
    private final ArrayList<Failsafe> emergencyQueue = new ArrayList<>();
    private final Clock chooseEmergencyDelay = new Clock();
    private final Clock restartMacroAfterFailsafeDelay = new Clock();
    private boolean sendingFailsafeInfo = false;


    @Getter
    @Setter
    private boolean hadEmergency = false;

    public FailsafeManager() {
        failsafes.addAll(
                Arrays.asList(
                    new RotationFailsafe()
                )
        );
    }

    public void stopFailsafes() {
        triggeredFailsafe = Optional.empty();
        emergencyQueue.clear();
        sendingFailsafeInfo = false;
        chooseEmergencyDelay.reset();
    }

    public void resetAfterMacroDisable() {
        stopFailsafes();
        restartMacroAfterFailsafeDelay.reset();
    }

    @SubscribeEvent
    public void onReceivedPacketDetection(ReceivePacketEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!MacroHandler.getInstance().isMacroToggled()) return;
        if (triggeredFailsafe.isPresent()) return;
        if (FeatureManager.getInstance().shouldIgnoreFalseCheck()) return;

        failsafes.forEach(failsafe -> failsafe.onReceivedPacketDetection(event));
    }

    @SubscribeEvent
    public void onTickDetection(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!MacroHandler.getInstance().isMacroToggled()) return;
        if (triggeredFailsafe.isPresent()) return;
        if (FeatureManager.getInstance().shouldIgnoreFalseCheck()) return;

        failsafes.forEach(failsafe -> failsafe.onTickDetection(event));
    }

    public void possibleDetection(Failsafe failsafe) {
        if (emergencyQueue.contains(failsafe)) return;

        MacroHandler.getInstance().getCurrentMacro().ifPresent(AbstractMacro::saveState);
        emergencyQueue.add(failsafe);
        if (!chooseEmergencyDelay.isScheduled())
            chooseEmergencyDelay.schedule(FarmHelperConfig.failsafeStopDelay + (long) (Math.random() * 500));
        LogUtils.sendDebug("[Failsafe] Emergency added: " + failsafe.getType().name());
        LogUtils.sendWarning("[Failsafe] Probability of emergency: " + LogUtils.capitalize(failsafe.getType().name()));
        if (!sendingFailsafeInfo) {
            sendingFailsafeInfo = true;
            Multithreading.schedule(() -> {
                Failsafe tempFailsafe = getHighestPriorityEmergency();
                if (tempFailsafe.getType() == EmergencyType.NONE) {
                    // Should never happen, but yeh...
                    LogUtils.sendDebug("[Failsafe] No emergency chosen!");
                    stopFailsafes();
                    return;
                }

                if (FarmHelperConfig.enableFailsafeSound && failsafe.shouldPlaySound()) {
                    AudioManager.getInstance().playSound();
                }

                if (FarmHelperConfig.autoAltTab && failsafe.shouldAltTab()) {
                    FailsafeUtils.bringWindowToFront();
                }
                Multithreading.schedule(() -> {
                    if (FarmHelperConfig.autoAltTab && failsafe.shouldAltTab() && !Display.isActive()) {
                        FailsafeUtils.bringWindowToFrontUsingRobot();
                        System.out.println("Bringing window to front using Robot because Winapi failed as usual.");
                    }
                }, 750, TimeUnit.MILLISECONDS);

                LogUtils.sendFailsafeMessage(tempFailsafe.getType().label, failsafe.shouldTagEveryone());
                if (failsafe.shouldSendNotification())
                    FailsafeUtils.getInstance().sendNotification(StringUtils.stripControlCodes(tempFailsafe.getType().label), TrayIcon.MessageType.WARNING);
            }, 800, TimeUnit.MILLISECONDS);
        }
    }

    private Failsafe getHighestPriorityEmergency() {
        Failsafe highestPriority = emergencyQueue.get(0);
        for (Failsafe emergencyType : emergencyQueue) {
            if (emergencyType.getPriority() < highestPriority.getPriority()) {
                highestPriority = emergencyType;
            }
        }
        return highestPriority;
    }

    @SubscribeEvent
    public void onTickChooseEmergency(TickEvent.ClientTickEvent event) {
        if (!MacroHandler.getInstance().isMacroToggled() && emergencyQueue.stream().noneMatch(f -> f.getType().equals(EmergencyType.TEST))) return;
        if (triggeredFailsafe.isPresent()) return;
        if (!chooseEmergencyDelay.isScheduled()) return;
        if (chooseEmergencyDelay.passed()) {
            triggeredFailsafe = Optional.of(getHighestPriorityEmergency());
            if (triggeredFailsafe.get().getType() == EmergencyType.NONE) {
                // Should never happen, but yeh...
                LogUtils.sendDebug("[Failsafe] No emergency chosen!");
                stopFailsafes();
                return;
            }
            emergencyQueue.clear();
            chooseEmergencyDelay.reset();
            hadEmergency = true;
            LogUtils.sendDebug("[Failsafe] Emergency chosen: " + StringUtils.stripControlCodes(triggeredFailsafe.get().getType().name()));
            FeatureManager.getInstance().disableCurrentlyRunning(null);
            if (!Scheduler.getInstance().isFarming()) {
                Scheduler.getInstance().farmingTime();
            }
        }
    }

    @SubscribeEvent
    public void onFailsafeTriggered(TickEvent.ClientTickEvent event) {
        if (!triggeredFailsafe.isPresent()) {
            return;
        }
        triggeredFailsafe.get().duringFailsafeTrigger();
    }

    @SubscribeEvent
    public void onTickRestartMacro(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!restartMacroAfterFailsafeDelay.isScheduled()) return;

        if (restartMacroAfterFailsafeDelay.passed()) {
            if (mc.currentScreen != null) {
                PlayerUtils.closeScreen();
            }
            if (FarmHelperConfig.alwaysTeleportToGarden) {
                MacroHandler.getInstance().getCurrentMacro().ifPresent(AbstractMacro::triggerWarpGarden);
            }
            restartMacroAfterFailsafeDelay.reset();
            Multithreading.schedule(() -> {
                LogUtils.sendDebug("[Failsafe] Restarting the macro...");
                MacroHandler.getInstance().enableMacro();
                com.jelly.farmhelperv2.feature.impl.Failsafe.getInstance().setHadEmergency(false);
                com.jelly.farmhelperv2.feature.impl.Failsafe.getInstance().getRestartMacroAfterFailsafeDelay().reset();
            }, FarmHelperConfig.alwaysTeleportToGarden ? 1_500 : 0, TimeUnit.MILLISECONDS);
        }
    }

    public void restartMacroAfterDelay() {
        if (FarmHelperConfig.enableRestartAfterFailSafe) {
            MacroHandler.getInstance().pauseMacro();
            Multithreading.schedule(() -> {
                InventoryUtils.openInventory();
                LogUtils.sendDebug("[Failsafe] Finished " + (triggeredFailsafe.map(failsafe -> (failsafe.getType().label + " ")).orElse("")) + "failsafe");
                if (FarmHelperConfig.enableRestartAfterFailSafe) {
                    LogUtils.sendDebug("[Failsafe] Restarting the macro in " + FarmHelperConfig.restartAfterFailSafeDelay + " minutes.");
                    restartMacroAfterFailsafeDelay.schedule(FarmHelperConfig.restartAfterFailSafeDelay * 1_000L * 60L);
                }
            }, (long) (1_500 + Math.random() * 1_000), TimeUnit.MILLISECONDS);
        } else {
            MacroHandler.getInstance().disableMacro();
        }
    }

    public enum EmergencyType {
        NONE(""),
        TEST("This is a test emergency!"),
        ROTATION_CHECK("You've got§l ROTATED§r§d by staff member!"),
        TELEPORT_CHECK("You've got§l TELEPORTED§r§d by staff member!"),
        DIRT_CHECK("You've got§l DIRT CHECKED§r§d by staff member!"),
        ITEM_CHANGE_CHECK("Your §lITEM HAS CHANGED§r§d!"),
        WORLD_CHANGE_CHECK("Your §lWORLD HAS CHANGED§r§d!"),
        BEDROCK_CAGE_CHECK("You've got§l BEDROCK CAGED§r§d by staff member!"),
        EVACUATE("Server is restarting! Evacuate!"),
        BANWAVE("Banwave has been detected!"),
        DISCONNECT("You've been§l DISCONNECTED§r§d from the server!"),
        LOWER_AVERAGE_BPS("Your BPS is lower than average!"),
        JACOB("You've extended the §lJACOB COUNTER§r§d!");

        final String label;

        EmergencyType(String s) {
            label = s;
        }
    }
}
