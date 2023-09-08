package com.jelly.farmhelper.hud;

import cc.polyfrost.oneconfig.config.core.OneColor;
import cc.polyfrost.oneconfig.hud.TextHud;
import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.features.*;
import com.jelly.farmhelper.macros.MacroHandler;
import com.jelly.farmhelper.utils.LocationUtils;

import java.util.List;

import static com.jelly.farmhelper.FarmHelper.gameState;

public class DebugHUD extends TextHud {
    public DebugHUD() {
        super(true, 1f, 10f, 1, true, true, 1, 5, 5, new OneColor(0, 0, 0, 150), false, 2, new OneColor(0, 0, 0, 127));
    }

    public static String prevState = "";
    public static String currentState = "";
    public static boolean rotating = false;
    public static boolean jacobsContestTriggered = false;
    public static long farmClockremainingTime = 0;
    public static boolean farmClockisPaused = false;
    public static long breakClockremainingTime = 0;
    public static boolean breakClockisPaused = false;
    public static boolean hasLineChanged = false;
    public static boolean isHuggingAWall = false;

    @Override
    protected void getLines(List<String> lines, boolean example) {

        if (FarmHelper.config.debugMode) {
            lines.add("prevState: " + prevState);
            lines.add("currentState: " + currentState);
            lines.add("rotating: " + rotating);
            lines.add("Failsafe.emergency: " + FailsafeNew.emergency);
            lines.add("jacobsContestTriggered: " + jacobsContestTriggered);
            lines.add("hasPetChangedDuringThisContest: " + PetSwapper.hasPetChangedDuringThisContest);
            lines.add("Scheduler.isFarming(): " + Scheduler.isFarming());
            lines.add("farmClock remainingTime: " + farmClockremainingTime);
            lines.add("farmClock isPaused: " + farmClockisPaused);
            lines.add("breakClock remainingTime: " + breakClockremainingTime);
            lines.add("breakClock isPaused: " + breakClockisPaused);
            lines.add("gameState.currentLocation: " + gameState.currentLocation);
            lines.add("LocationUtils.currentIsland: " + LocationUtils.currentIsland);
            lines.add("AutoReconnect.currentState: " + AutoReconnect.currentState);
            lines.add("hasLineChanged: " + hasLineChanged);
            lines.add("isHuggingAWall: " + isHuggingAWall);
            lines.add("lagging: " + LagDetection.lagging);
            lines.add("lastPacket: " + LagDetection.lastPacket);
            if (MacroHandler.currentMacro != null)
                lines.add("isStuck: " + MacroHandler.currentMacro.isStuck());
        }
    }
}