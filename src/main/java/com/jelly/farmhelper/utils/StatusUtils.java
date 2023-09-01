package com.jelly.farmhelper.utils;

import com.jelly.farmhelper.features.*;
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
        else if (FailsafeNew.restartAfterFailsafeCooldown.isScheduled() && !FailsafeNew.restartAfterFailsafeCooldown.passed()) {
            setStateString("Restarting in " + Utils.formatTime(FailsafeNew.restartAfterFailsafeCooldown.getEndTime() - System.currentTimeMillis()));
        }
        else if (FailsafeNew.cooldown.isScheduled() && !FailsafeNew.cooldown.passed()) {
            setStateString("Waiting for " + Utils.formatTime(FailsafeNew.cooldown.getEndTime() - System.currentTimeMillis()));
        }
        else if (!FailsafeNew.emergency && !FailsafeNew.isJacobFailsafeExceeded) {
            setStateString(Scheduler.getStatusString());
        }
        else if (FailsafeNew.isJacobFailsafeExceeded && FailsafeNew.cooldown.getEndTime() - System.currentTimeMillis() > 0) {
            setStateString("Jacob failsafe for " + Utils.formatTime(FailsafeNew.cooldown.getEndTime() - System.currentTimeMillis()));
        }
        else if (FailsafeNew.emergency) {
            setStateString("Emergency");
        }
        else setStateString("Idling");
        cookieFail = "AutoCookie Fail: " + AutoCookie.failCount + (AutoCookie.failCount == 3 ? " (OFF)" : "");
        potFail = "AutoPot Fail: " + AutoPot.failCount + (AutoPot.failCount == 3 ? " (OFF)" : "");
    }

    public static void setStateString(String stateString) {
        status = stateString;
    }
}
