package com.github.may2beez.farmhelperv2.feature.impl;

import com.github.may2beez.farmhelperv2.feature.IFeature;
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
        enabled = true;
        cameraYaw = mc.thePlayer.rotationYaw + 180f;
        cameraPitch = mc.thePlayer.rotationPitch;
        mc.gameSettings.thirdPersonView = 1;
        if (UngrabMouse.getInstance().isRunning()) {
            UngrabMouse.getInstance().regrabMouse();
            mouseWasGrabbed = true;
        }
    }

    @Override
    public void stop() {
        enabled = false;
        mc.gameSettings.thirdPersonView = 0;
        cameraPitch = mc.thePlayer.rotationPitch;
        cameraYaw = mc.thePlayer.rotationYaw;
        if (UngrabMouse.getInstance().isToggled() && mouseWasGrabbed) {
            UngrabMouse.getInstance().ungrabMouse();
            mouseWasGrabbed = false;
        }
    }

    @Override
    public void resetStatesAfterMacroDisabled() {

    }

    @Override
    public boolean isToggled() {
        return false;
    }

    private boolean enabled = false;
    @Getter
    @Setter
    private float cameraYaw = 0;
    @Getter
    @Setter
    private float cameraPitch = 0;

    @SubscribeEvent
    public void onCameraSetup(EntityViewRenderEvent.CameraSetup event) {
        if (!isRunning()) return;

        event.pitch = cameraPitch;
        event.yaw = cameraYaw;
    }
}
