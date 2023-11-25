package com.github.may2beez.farmhelperv2.feature;

public interface IFeature {
    String getName();
    boolean isRunning();
    boolean shouldPauseMacroExecution();
    boolean shouldStartAtMacroStart();
    void start();
    void stop();
    void resetStatesAfterMacroDisabled();
    boolean isToggled();
    boolean shouldCheckForFailsafes();
}
