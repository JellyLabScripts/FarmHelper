package com.jelly.farmhelper.features;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.config.interfaces.SchedulerConfig;
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
    private static final Clock farmClock = new Clock();
    private static final Clock breakClock = new Clock();
    private static State currentState;

    enum State {
        FARMING,
        BREAK
    }

    public static boolean isFarming() {
        return !SchedulerConfig.scheduler || currentState == State.FARMING;
    }

    public static String getStatusString() {
        if (SchedulerConfig.scheduler) {
            return (currentState == State.FARMING ? "Farming" : "Break") + " for "
                + Utils.formatTime((currentState == State.FARMING ? farmClock.getEndTime() : breakClock.getEndTime()) - System.currentTimeMillis());
        } else {
            return "Farming";
        }
    }

    public static void start() {
        currentState = State.FARMING;
        farmClock.schedule(TimeUnit.MINUTES.toMillis((long) SchedulerConfig.farmTime));
    }

    @SubscribeEvent
    public final void tick(TickEvent.ClientTickEvent event) {
        if (!SchedulerConfig.scheduler || event.phase == TickEvent.Phase.END || mc.thePlayer == null || mc.theWorld == null || FarmHelper.tickCount % 5 != 0)
            return;

        if (MacroHandler.isMacroOn && MacroHandler.currentMacro.enabled && currentState == State.FARMING && farmClock.passed()) {
            LogUtils.debugLog("[Scheduler] Farming time has passed, stopping");
            MacroHandler.disableCurrentMacro();
            currentState = State.BREAK;
            breakClock.schedule(TimeUnit.MINUTES.toMillis((long) SchedulerConfig.breakTime));
        } else if (MacroHandler.isMacroOn && currentState == State.BREAK && breakClock.passed()) {
            LogUtils.debugLog("[Scheduler] Break time has passed, starting");
            currentState = State.FARMING;
            farmClock.schedule(TimeUnit.MINUTES.toMillis((long) SchedulerConfig.farmTime));
        }
    }
}
