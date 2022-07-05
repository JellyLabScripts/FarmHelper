package com.jelly.farmhelper.utils;

import com.jelly.farmhelper.features.AutoCookie;
import com.jelly.farmhelper.features.AutoPot;
import com.jelly.farmhelper.features.Failsafe;
import com.jelly.farmhelper.features.Scheduler;
import com.jelly.farmhelper.macros.MacroHandler;
import gg.essential.elementa.state.BasicState;

public class StatusUtils {
    public static final BasicState<String> status = new BasicState<>("Idling");
    public static final BasicState<String> cookieFail = new BasicState<>("AutoCookie Fail: 0");
    public static final BasicState<String> connecting = new BasicState<>("Connecting to Socket");
    public static final BasicState<String> potFail = new BasicState<>("AutoPot Fail: 0");

    public static void updateStateString() {

        if (!MacroHandler.isMacroing) {
            setStateString("Idling");
        } else if (Failsafe.jacobWait.passed()) {
            setStateString(Scheduler.getStatusString());
        } else {
            setStateString("Jacob failsafe for " + Utils.formatTime(Failsafe.jacobWait.getEndTime() - System.currentTimeMillis()));
        }
        cookieFail.set("AutoCookie Fail: " + AutoCookie.failCount + (AutoCookie.failCount == 3 ? " (OFF)" : ""));
        potFail.set("AutoPot Fail: " + AutoPot.failCount + (AutoPot.failCount == 3 ? " (OFF)" : ""));
    }

    public static void setStateString(String stateString) {
        status.set(stateString);
    }
}
