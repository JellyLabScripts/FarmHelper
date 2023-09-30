package com.github.may2beez.farmhelperv2.feature.impl;

import com.github.may2beez.farmhelperv2.feature.IFeature;
import com.github.may2beez.farmhelperv2.handler.GameStateHandler;
import com.github.may2beez.farmhelperv2.handler.MacroHandler;
import com.github.may2beez.farmhelperv2.util.LogUtils;
import com.github.may2beez.farmhelperv2.util.helper.Clock;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

@Getter
public class AntiStuck implements IFeature {
    private final Minecraft mc = Minecraft.getMinecraft();

    private static AntiStuck instance;
    public static AntiStuck getInstance() {
        if (instance == null) {
            instance = new AntiStuck();
        }
        return instance;
    }

    @Setter
    private boolean enabled = false;

    @Setter
    private int failedAttempts = 0;

    private final Clock resetFailedAttemptsClock = new Clock();

    @Override
    public String getName() {
        return "AntiStuck";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean shouldPauseMacroExecution() {
        return true;
    }

    @Override
    public void stop() {
        if (enabled) {
            LogUtils.sendWarning("[Anti Stuck] Disabled");
        }
        enabled = false;
    }

    @Override
    public void resetStatesAfterMacroDisabled() {
        failedAttempts = 0;
        resetFailedAttemptsClock.reset();
    }

    @Override
    public boolean isActivated() {
        return true;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!isActivated()) return;
        if (!MacroHandler.getInstance().isMacroing()) return;
        if (!GameStateHandler.getInstance().inGarden()) return;


    }
}
