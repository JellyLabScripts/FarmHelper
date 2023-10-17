package com.jelly.farmhelper.features;

import cc.polyfrost.oneconfig.utils.Multithreading;
import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.macros.MacroHandler;
import com.jelly.farmhelper.player.Rotation;
import com.jelly.farmhelper.utils.Clock;
import com.jelly.farmhelper.utils.KeyBindUtils;
import com.jelly.farmhelper.utils.LogUtils;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.concurrent.TimeUnit;

public class AntiAfk {
    private static AntiAfk instance;
    public static AntiAfk getInstance() {
        if (instance == null) {
            instance = new AntiAfk();
        }
        return instance;
    }

    private final Minecraft mc = Minecraft.getMinecraft();

    @Getter
    @Setter
    private boolean enabled;

    private final Rotation rotation = new Rotation();
    private final Clock delay = new Clock();

    public enum Mode {
        NONE,
        ROTATE_START,
    }

    @Getter
    @Setter
    private Mode mode = Mode.NONE;

    public void enable() {
        LogUtils.sendFailsafeMessage("You are now AFK! Will move camera in a moment to fix that...");
        LogUtils.sendWarning("Anti-AFK mode enabled");
        enabled = true;
        Multithreading.schedule(() -> {
            KeyBindUtils.stopMovement();
            MacroHandler.disableCurrentMacro(true);
            setMode(Mode.ROTATE_START);
            delay.schedule(750 + (long) (Math.random() * 500));
        }, (long) (Math.random() * 750 + 500), TimeUnit.MILLISECONDS);
    }

    @SubscribeEvent
    public void onReceivedChat(ClientChatReceivedEvent event) {
        if (!MacroHandler.isMacroing) return;
        if (!FarmHelper.config.antiAFK) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (enabled) return;

        String message = StringUtils.stripControlCodes(event.message.getUnformattedText());
        if (message.startsWith("You have been put in AFK mode!")) {
            enable();
        }
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (rotation.rotating) {
            rotation.update();
            return;
        }
        if (delay.isScheduled() && !delay.passed()) return;
        if (!enabled) return;
        switch (mode) {
            case NONE:
                break;
            case ROTATE_START:
                float randomYaw = (float) (Math.random() * 20 - 10);
                if (randomYaw < 4 && randomYaw > -4) randomYaw = 4 * (randomYaw < 0 ? -1 : 1);
                float randomPitch = (float) (Math.random() * 20 - 10);
                if (randomPitch < 4 && randomPitch > -4) randomPitch = 4 * (randomPitch < 0 ? -1 : 1);
                rotation.easeTo(mc.thePlayer.rotationYaw + randomYaw, mc.thePlayer.rotationPitch + randomPitch, (long) (FarmHelper.config.rotationTime * 1000 + Math.random() * FarmHelper.config.rotationTimeRandomness * 1000));
                setMode(Mode.NONE);
                enabled = false;
                Multithreading.schedule(() -> {
                    LogUtils.sendWarning("Anti-AFK mode disabled");
                    MacroHandler.enableCurrentMacro();
                }, (long) (750 + Math.random() * 500), TimeUnit.MILLISECONDS);
                break;
        }
    }
}
