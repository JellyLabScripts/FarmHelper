package com.jelly.farmhelper.config.interfaces;

import com.jelly.farmhelper.config.annotations.Config;

public class FailsafeConfig {
    @Config
    public static boolean notifications;
    @Config
    public static boolean pingSound;
    @Config
    public static boolean fakeMovements;
    @Config()
    public static boolean banwaveDisconnect = true;
    @Config()
    public static double banThreshold = 10.0;
    @Config()
    public static double reconnectDelay = 5.0;
}
