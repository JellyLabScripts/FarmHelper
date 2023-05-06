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
    public static boolean checkDesync = true;
    @Config
    public static boolean autoFocusOnStaffCheck;
    @Config
    public static double rotationSens = 1.0;
    @Config()
    public static boolean autoTpOnWorldChange = true;
    @Config()
    public static boolean autoSetspawn = true;
    @Config()
    public static double autoSetSpawnMinDelay = 5.0;
    @Config()
    public static double autoSetSpawnMaxDelay = 10.0;
    @Config()
    public static boolean banwaveDisconnect = true;
    @Config()
    public static double banThreshold = 10.0;
    @Config()
    public static double reconnectDelay = 5.0;
    @Config()
    public static boolean restartAfterFailsafe = false;
    @Config()
    public static boolean setSpawnBeforeEvacuate = true;
}
