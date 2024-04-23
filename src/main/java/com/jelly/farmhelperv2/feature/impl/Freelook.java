package com.jelly.farmhelperv2.feature.impl;

import com.jelly.farmhelperv2.config.FarmHelperConfig;
import com.jelly.farmhelperv2.feature.IFeature;
import com.jelly.farmhelperv2.handler.MacroHandler;
import com.jelly.farmhelperv2.util.RenderUtils;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.awt.*;
import java.util.ArrayList;

public class Freelook implements IFeature {
    private final Minecraft mc = Minecraft.getMinecraft();
    private static Freelook instance;

    public static Freelook getInstance() {
        if (instance == null) {
            instance = new Freelook();
        }
        return instance;
    }

    @Getter
    @Setter
    private float distance = 4;

    @Override
    public String getName() {
        return "Freelook";
    }

    @Override
    public boolean isRunning() {
        return enabled;
    }

    @Override
    public boolean shouldPauseMacroExecution() {
        return false;
    }

    @Override
    public boolean shouldStartAtMacroStart() {
        return false;
    }

    public void toggle() {
        if (isRunning()) {
            stop();
        } else {
            start();
        }
    }

    private boolean mouseWasGrabbed = false;

    @Override
    public void start() {
        if (enabled || mc.gameSettings.thirdPersonView == 1) return;
        enabled = true;
        distance = 4;
        cameraPrevYaw = mc.thePlayer.prevRotationYaw;
        cameraPrevPitch = mc.thePlayer.prevRotationPitch;
        cameraYaw = mc.thePlayer.rotationYaw + 180;
        cameraPitch = mc.thePlayer.rotationPitch;
        mc.gameSettings.thirdPersonView = 1;
        if (UngrabMouse.getInstance().isToggled() && MacroHandler.getInstance().isMacroToggled()) {
            UngrabMouse.getInstance().regrabMouse();
            mouseWasGrabbed = true;
        }
        IFeature.super.start();
    }

    @Override
    public void stop() {
        if (!enabled) return;
        enabled = false;
        distance = 4;
        mc.gameSettings.thirdPersonView = 0;
        if (UngrabMouse.getInstance().isToggled() && mouseWasGrabbed && MacroHandler.getInstance().isMacroToggled()) {
            UngrabMouse.getInstance().ungrabMouse();
        }
        mouseWasGrabbed = false;
        IFeature.super.stop();
    }


    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        if (!enabled) return;
        ArrayList<String> textLines = new ArrayList<>();
        textLines.add("Freelook");
        textLines.add("Press " + FarmHelperConfig.freelookKeybind.getDisplay() + " to disable");
        RenderUtils.drawMultiLineText(textLines, event, Color.YELLOW, 1f);
    }

    @Override
    public void resetStatesAfterMacroDisabled() {

    }

    @Override
    public boolean isToggled() {
        return false;
    }

    @Override
    public boolean shouldCheckForFailsafes() {
        return false;
    }

    private boolean enabled = false;
    @Getter
    @Setter
    private float cameraYaw = 0;
    @Getter
    @Setter
    private float cameraPitch = 0;
    @Getter
    @Setter
    private float cameraPrevYaw;
    @Getter
    @Setter
    private float cameraPrevPitch;

    @SubscribeEvent
    public void onCameraSetup(EntityViewRenderEvent.CameraSetup event) {
        if (!isRunning()) return;

        event.pitch = cameraPitch;
        event.yaw = cameraYaw;
    }

    public float getPitch(float original) {
        return isRunning() ? cameraPitch : original;
    }

    public float getYaw(float original) {
        return isRunning() ? cameraYaw : original;
    }

    public float getPrevPitch(float original) {
        return isRunning() ? cameraPrevPitch : original;
    }

    public float getPrevYaw(float original) {
        return isRunning() ? cameraPrevYaw : original;
    }
}
