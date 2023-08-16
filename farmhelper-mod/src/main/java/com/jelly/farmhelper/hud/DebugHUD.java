package com.jelly.farmhelper.hud;

import cc.polyfrost.oneconfig.config.annotations.Switch;
import cc.polyfrost.oneconfig.config.core.OneColor;
import cc.polyfrost.oneconfig.hud.TextHud;
import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.features.AutoReconnect;
import com.jelly.farmhelper.features.Failsafe;
import com.jelly.farmhelper.utils.LocationUtils;

import java.awt.*;
import java.util.List;

import static com.jelly.farmhelper.FarmHelper.gameState;

public class DebugHUD extends TextHud {
    public DebugHUD() {
        super(true, 1f, 10f, 1, true, true, 1, 5, 5, new OneColor(0, 0, 0, 150), false, 2, new OneColor(0, 0, 0, 127));
    }

    @Switch(
        name = "Rainbow Text", category = "HUD",
        description = "Rainbow text for the debug HUD"
    )
    public static boolean rainbowStatusText = false;
    public static String prevState = "";
    public static String currentState = "";
    public static boolean rotating = false;
    public static boolean jacobsContestTriggered = false;
    public static long farmClockremainingTime = 0;
    public static boolean farmClockisPaused = false;
    public static long breakClockremainingTime = 0;
    public static boolean breakClockisPaused = false;

    @Override
    protected void getLines(List<String> lines, boolean example) {

        if (rainbowStatusText) {
            Color chroma = Color.getHSBColor((float) ((System.currentTimeMillis() / 10) % 500) / 500, 1, 1);
            color.setFromOneColor(new OneColor(chroma.getRed(), chroma.getGreen(), chroma.getBlue(), 255));
        } else {
            Color chroma = Color.WHITE;
            color.setFromOneColor(new OneColor(chroma.getRed(), chroma.getGreen(), chroma.getBlue(), 255));

        }
        if (FarmHelper.config.debugMode) {
            lines.add("prevState: " + prevState);
            lines.add("currentState: " + currentState);
            lines.add("rotating: " + rotating);
            lines.add("Failsafe.emergency: " + Failsafe.emergency);
            lines.add("jacobsContestTriggered: " + jacobsContestTriggered);
            lines.add("farmClock remainingTime: " + farmClockremainingTime);
            lines.add("farmClock isPaused: " + farmClockisPaused);
            lines.add("breakClock remainingTime: " + breakClockremainingTime);
            lines.add("breakClock isPaused: " + breakClockisPaused);
            lines.add("gameState.currentLocation: " + gameState.currentLocation);
            lines.add("LocationUtils.currentIsland: " + LocationUtils.currentIsland);
            lines.add("AutoReconnect.currentState: " + AutoReconnect.currentState);
        }
    }
}