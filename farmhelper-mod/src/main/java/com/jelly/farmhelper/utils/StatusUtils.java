package com.jelly.farmhelper.utils;

import com.jelly.farmhelper.features.AutoCookie;
import com.jelly.farmhelper.features.AutoPot;
import com.jelly.farmhelper.features.Failsafe;
import com.jelly.farmhelper.features.Scheduler;
import com.jelly.farmhelper.macros.MacroHandler;

public class StatusUtils {
    public static  String status = "Idling";
    public static  String cookieFail ="AutoCookie Fail: 0";
    public static  String connecting = "Connecting to Socket";
    public static  String potFail = "AutoPot Fail: 0";

    public static void updateStateString() {

        if (!MacroHandler.isMacroing) {
            setStateString("Idling");
        }
        else if (Failsafe.emergency && Failsafe.restartAfterFailsafeCooldown.isScheduled() && !Failsafe.restartAfterFailsafeCooldown.passed()) {
            setStateString("Restart after " + Utils.formatTime(Failsafe.restartAfterFailsafeCooldown.getEndTime() - System.currentTimeMillis()));
        }
        else if (!Failsafe.emergency && Failsafe.jacobWait.passed()) {
            setStateString(Scheduler.getStatusString());
        }
        else if (Failsafe.jacobWait.getEndTime() - System.currentTimeMillis() > 0) {
            setStateString("Jacob failsafe for " + Utils.formatTime(Failsafe.jacobWait.getEndTime() - System.currentTimeMillis()));
        }
        else setStateString("Idling");
        cookieFail = "AutoCookie Fail: " + AutoCookie.failCount + (AutoCookie.failCount == 3 ? " (OFF)" : "");
        potFail = "AutoPot Fail: " + AutoPot.failCount + (AutoPot.failCount == 3 ? " (OFF)" : "");
    }

    public static void setStateString(String stateString) {
        status = stateString;
    }
}
