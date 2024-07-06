package com.jelly.farmhelperv2.feature;

import com.jelly.farmhelperv2.util.LogUtils;

public interface IFeature {
    String getName();

    boolean isRunning();

    boolean shouldPauseMacroExecution();

    boolean shouldStartAtMacroStart();

    default void start() {
        if (!shouldPauseMacroExecution()) return;
        LogUtils.sendDebug("Enabled pausing feature: " + getName());
        FeatureManager.getInstance().getPauseExecutionFeatures().add(this);
    }

    default void resume() {}

    default void stop() {
        if (!shouldPauseMacroExecution()) return;
        FeatureManager.getInstance().getPauseExecutionFeatures().remove(this);
    }

    void resetStatesAfterMacroDisabled();

    boolean isToggled();

    boolean shouldCheckForFailsafes();
}
