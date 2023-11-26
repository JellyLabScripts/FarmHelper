package com.jelly.farmhelperv2.feature.impl;

import com.jelly.farmhelperv2.feature.IFeature;
import com.jelly.farmhelperv2.handler.MacroHandler;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class Freelock implements IFeature {
    private final Minecraft mc = Minecraft.getMinecraft();
    private static Freelock instance;

    public static Freelock getInstance() {
        if (instance == null) {
            instance = new Freelock();
        }
        return instance;
    }

    @Override
    public String getName() {
        return "Freelock";
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
        cameraPrevYaw = mc.thePlayer.prevRotationYaw;
        cameraPrevPitch = mc.thePlayer.prevRotationPitch;
        cameraYaw = mc.thePlayer.rotationYaw + 180;
        cameraPitch = mc.thePlayer.rotationPitch;
        mc.gameSettings.thirdPersonView = 1;
        if (UngrabMouse.getInstance().isRunning() && MacroHandler.getInstance().isCurrentMacroEnabled()) {
            UngrabMouse.getInstance().regrabMouse();
            mouseWasGrabbed = true;
        }
    }

    @Override
    public void stop() {
        if (!enabled) return;
        enabled = false;
        mc.gameSettings.thirdPersonView = 0;
        if (UngrabMouse.getInstance().isToggled() && mouseWasGrabbed && MacroHandler.getInstance().isCurrentMacroEnabled()) {
            UngrabMouse.getInstance().ungrabMouse();
        }
        mouseWasGrabbed = false;
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
