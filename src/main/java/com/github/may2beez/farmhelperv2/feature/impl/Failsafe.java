package com.github.may2beez.farmhelperv2.feature.impl;

import com.github.may2beez.farmhelperv2.feature.IFeature;
import com.github.may2beez.farmhelperv2.util.FailsafeUtils;
import com.github.may2beez.farmhelperv2.util.LogUtils;
import com.github.may2beez.farmhelperv2.util.RenderUtils;
import com.github.may2beez.farmhelperv2.util.helper.AudioManager;
import com.github.may2beez.farmhelperv2.util.helper.Clock;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.awt.*;
import java.util.ArrayList;

@Getter
public class Failsafe implements IFeature {
    private final Minecraft mc = Minecraft.getMinecraft();

    private static Failsafe instance;
    public static Failsafe getInstance() {
        if (instance == null) {
            instance = new Failsafe();
        }
        return instance;
    }

    public enum EmergencyType {
        NONE,
        TEST("This is a test emergency!", 6),
        ROTATION_CHECK("You've got§l ROTATED§r§d by staff member!", 3),
        TELEPORT_CHECK("You've got§l TELEPORTED§r§d by staff member!", 3),
        DIRT_CHECK("You've got§l DIRT CHECKED§r§d by staff member!", 3),
        ITEM_CHANGE_CHECK("Your §lITEM HAS CHANGED§r§d!", 4),
        WORLD_CHANGE_CHECK("Your §lWORLD HAS CHANGED§r§d!", 2),
        BEDROCK_CAGE_CHECK("You've got§l BEDROCK CAGED§r§d by staff member!", 1),
        BANWAVE("Banwave has been detected!", 5);

        final String label;
        final int priority;
        EmergencyType(String s, int priority) {
            label = s;
            this.priority = priority;
        }
        EmergencyType() {
            label = name();
            priority = 0;
        }
    }

    private EmergencyType emergency = EmergencyType.NONE;
    private final ArrayList<EmergencyType> emergencyQueue = new ArrayList<>();

    @Override
    public String getName() {
        return "Failsafe";
    }

    @Override
    public boolean isRunning() {
        return emergency != EmergencyType.NONE;
    }

    @Override
    public boolean shouldPauseMacroExecution() {
        return true;
    }

    @Override
    public boolean shouldStartAtMacroStart() {
        return false;
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
        emergency = EmergencyType.NONE;
        emergencyQueue.clear();
        chooseEmergencyDelay.reset();
    }

    @Override
    public void resetStatesAfterMacroDisabled() {
        stop();
        dontRestartMacroAfterFailsafe.reset();
    }

    @Override
    public boolean isToggled() {
        return true;
    }

    public boolean isEmergency() {
        return emergency != EmergencyType.NONE;
    }

    private final Clock chooseEmergencyDelay = new Clock();
    private final Clock dontRestartMacroAfterFailsafe = new Clock();

    @Setter
    private boolean hadEmergency = false;

    @SubscribeEvent
    public void onTickChooseEmergency(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
//        if (!MacroHandler.getInstance().isMacroToggled()) return;
        if (chooseEmergencyDelay.isScheduled() && !chooseEmergencyDelay.passed()) return;
        EmergencyType tempEmergency = getHighestPriorityEmergency();
        if (tempEmergency == EmergencyType.NONE) return;

        AudioManager.getInstance().playSound();
        emergency = tempEmergency;
        emergencyQueue.clear();
        chooseEmergencyDelay.reset();
        LogUtils.sendDebug("[Failsafe] Emergency chosen: " + emergency.label);
        LogUtils.sendFailsafeMessage(emergency.label);
        FailsafeUtils.getInstance().sendNotification(emergency.label, TrayIcon.MessageType.WARNING);
    }

    @SubscribeEvent
    public void onTickFailsafe(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
//        if (!MacroHandler.getInstance().isMacroToggled()) return;
        if (chooseEmergencyDelay.isScheduled() && !chooseEmergencyDelay.passed()) return;
        if (!isEmergency()) return;

        switch (emergency) {
            case TEST:
                stop();
                break;
            case ROTATION_CHECK:
                break;
            case TELEPORT_CHECK:
                break;
            case DIRT_CHECK:
                break;
            case ITEM_CHANGE_CHECK:
                break;
            case WORLD_CHANGE_CHECK:
                break;
            case BEDROCK_CAGE_CHECK:
                break;
            case BANWAVE:
                break;
        }
    }

    public void addEmergency(EmergencyType emergencyType) {
        emergencyQueue.add(emergencyType);
        if (!chooseEmergencyDelay.isScheduled())
            chooseEmergencyDelay.schedule(1_000);
        LogUtils.sendDebug("[Failsafe] Emergency added: " + emergencyType.name());
        LogUtils.sendWarning("[Failsafe] Probability of emergency: " + LogUtils.capitalize(emergencyType.name()));
    }

    private EmergencyType getHighestPriorityEmergency() {
        EmergencyType highestPriority = EmergencyType.NONE;
        for (EmergencyType emergencyType : emergencyQueue) {
            if (emergencyType.priority > highestPriority.priority) {
                highestPriority = emergencyType;
            }
        }
        return highestPriority;
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        if (!chooseEmergencyDelay.isScheduled()) return;

        String text = "Failsafe in: " + String.format("%.1f", chooseEmergencyDelay.getRemainingTime() / 1000.0) + "s";
        RenderUtils.drawCenterTopText(text, event, Color.MAGENTA);
    }
}
