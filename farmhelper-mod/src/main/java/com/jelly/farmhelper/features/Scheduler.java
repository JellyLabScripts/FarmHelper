package com.jelly.farmhelper.features;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.events.BlockChangeEvent;
import com.jelly.farmhelper.macros.MacroHandler;
import com.jelly.farmhelper.utils.Clock;
import com.jelly.farmhelper.utils.LogUtils;
import com.jelly.farmhelper.utils.Utils;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.concurrent.TimeUnit;

public class Scheduler {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static Clock farmClock = new Clock();
    private static Clock breakClock = new Clock();
    private static State currentState;

    enum State {
        FARMING,
        BREAK
    }

    public static boolean isFarming() {
        return !FarmHelper.config.enableScheduler || currentState == State.FARMING;
    }

    public static String getStatusString() {
        if (FarmHelper.config.enableScheduler) {
            return (currentState == State.FARMING ? "Farming" : "Break") + " for "
                + Utils.formatTime((currentState == State.FARMING ? Math.max(farmClock.getEndTime(), 0) : Math.max(breakClock.getEndTime(), 0)) - System.currentTimeMillis());
        } else {
            return "Farming";
        }
    }

    public static void start() {
        currentState = State.FARMING;
        farmClock.schedule(TimeUnit.MINUTES.toMillis(FarmHelper.config.schedulerFarmingTime));
    }

    @SubscribeEvent
    public final void tick(TickEvent.ClientTickEvent event) {
        if (!FarmHelper.config.enableScheduler || event.phase == TickEvent.Phase.END || mc.thePlayer == null || mc.theWorld == null || FarmHelper.tickCount % 5 != 0 || MacroHandler.currentMacro == null || MacroHandler.currentMacro.cantPauseNow())
            return;

        if (!MacroHandler.randomizing && MacroHandler.isMacroing && MacroHandler.currentMacro.enabled && currentState == State.FARMING && farmClock.passed()) {
            LogUtils.debugLog("[Scheduler] Farming time has passed, stopping");
            MacroHandler.disableCurrentMacro(true);
            currentState = State.BREAK;
            breakClock.schedule(TimeUnit.MINUTES.toMillis((long)(FarmHelper.config.schedulerBreakTime + (Math.random() * FarmHelper.config.schedulerBreakTimeRandomness))));
        } else if (!MacroHandler.randomizing && MacroHandler.isMacroing && currentState == State.BREAK && breakClock.passed()) {
            LogUtils.debugLog("[Scheduler] Break time has passed, starting");
            currentState = State.FARMING;
            farmClock.schedule(TimeUnit.MINUTES.toMillis((long)(FarmHelper.config.schedulerFarmingTime + (Math.random() * FarmHelper.config.schedulerFarmingTimeRandomness))));
        }
    }
}
