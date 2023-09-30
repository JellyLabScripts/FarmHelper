package com.github.may2beez.farmhelperv2.feature;

public interface IFeature {
    String getName();
    boolean isEnabled();
    boolean shouldPauseMacroExecution();
    void stop();
    void resetStatesAfterMacroDisabled();
    boolean isActivated();
}
