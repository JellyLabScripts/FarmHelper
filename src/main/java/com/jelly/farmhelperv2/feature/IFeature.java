package com.jelly.farmhelperv2.feature;

public interface IFeature {
    String getName();

    boolean isRunning();

    boolean shouldPauseMacroExecution();

    boolean shouldStartAtMacroStart();

    default void start() {
        if (!shouldPauseMacroExecution()) return;
        FeatureManager.getInstance().getPauseExecutionFeatures().add(this);
    }

    default void stop() {
        if (!shouldPauseMacroExecution()) return;
        FeatureManager.getInstance().getPauseExecutionFeatures().remove(this);
    }

    void resetStatesAfterMacroDisabled();

    boolean isToggled();

    boolean shouldCheckForFailsafes();
}
