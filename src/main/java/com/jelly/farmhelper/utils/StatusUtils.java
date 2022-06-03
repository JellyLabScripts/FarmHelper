package com.jelly.farmhelper.utils;

import com.jelly.farmhelper.features.Failsafe;
import com.jelly.farmhelper.features.Scheduler;
import com.jelly.farmhelper.macros.MacroHandler;
import gg.essential.elementa.state.BasicState;

public class StatusUtils {
    public static final BasicState<String> status = new BasicState<>("Idling");

    public static void updateStateString() {
        if (!MacroHandler.isMacroOn) {
            setStateString("Idling");
        } else if (Failsafe.jacobWait.passed()) {
            setStateString(Scheduler.getStatusString());
        } else {
            setStateString("Jacob failsafe for " + Utils.formatTime(Failsafe.jacobWait.getEndTime()));
        }
    }

    public static void setStateString(String stateString) {
        status.set(stateString);
    }
}
