package com.github.may2beez.farmhelperv2.feature.impl;

import com.github.may2beez.farmhelperv2.config.FarmHelperConfig;
import com.github.may2beez.farmhelperv2.feature.IFeature;
import com.github.may2beez.farmhelperv2.util.helper.CircularFifoQueue;
import com.github.may2beez.farmhelperv2.util.helper.Clock;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class BanwaveChecker implements IFeature {
    private static BanwaveChecker instance;
    public static BanwaveChecker getInstance() {
        if (instance == null) {
            instance = new BanwaveChecker();
        }
        return instance;
    }

    private final Clock updateClock = new Clock();
    private final CircularFifoQueue<Integer> staffBanLast15Mins = new CircularFifoQueue<>(15);

    @Override
    public String getName() {
        return "Banwave Checker";
    }

    @Override
    public boolean isRunning() {
        return isToggled();
    }

    @Override
    public boolean shouldPauseMacroExecution() {
        return false;
    }

    @Override
    public boolean shouldStartAtMacroStart() {
        return false; // it's running all the time
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public void resetStatesAfterMacroDisabled() {

    }

    @Override
    public boolean isToggled() {
        return FarmHelperConfig.banwaveCheckerEnabled;
    }

    @SubscribeEvent
    public void onTickCheckBans(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!isRunning()) return;

    }
}
