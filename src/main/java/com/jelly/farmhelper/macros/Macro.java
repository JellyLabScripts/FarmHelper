package com.jelly.farmhelper.macros;

import com.jelly.farmhelper.utils.LogUtils;

public interface Macro {
    static void enable() {
        LogUtils.debugLog("Missing enable handler");
    }

    static void disable() {
        LogUtils.debugLog("Missing disable handler");
    }
}
