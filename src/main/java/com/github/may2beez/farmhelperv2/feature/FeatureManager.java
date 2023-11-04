package com.github.may2beez.farmhelperv2.feature;

import com.github.may2beez.farmhelperv2.feature.impl.*;
import com.github.may2beez.farmhelperv2.util.LogUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FeatureManager {
    private static FeatureManager instance;
    public static FeatureManager getInstance() {
        if (instance == null) {
            instance = new FeatureManager();
        }
        return instance;
    }

    private final ArrayList<IFeature> features = new ArrayList<>();

    public void fillFeatures() {
        List<IFeature> featuresList = Arrays.asList(
                AntiStuck.getInstance(),
                AutoCookie.getInstance(),
                AutoGodPot.getInstance(),
                AutoReconnect.getInstance(),
                AutoSell.getInstance(),
                BanInfoWS.getInstance(),
                DesyncChecker.getInstance(),
                Failsafe.getInstance(),
                Freelock.getInstance(),
                LagDetector.getInstance(),
                LeaveTimer.getInstance(),
                PerformanceMode.getInstance(),
                PetSwapper.getInstance(),
                ProfitCalculator.getInstance(),
                Scheduler.getInstance(),
                UngrabMouse.getInstance(),
                VisitorsMacro.getInstance()
        );
        features.addAll(featuresList);
    }

    public boolean shouldPauseMacroExecution() {
        return features.stream().anyMatch(feature -> {
            if (feature.isRunning()) {
                return feature.shouldPauseMacroExecution();
            }
            return false;
        });
    }

    public void disableAll() {
        features.forEach(feature -> {
            if (feature.isToggled() && feature.isRunning()) {
                feature.stop();
                LogUtils.sendDebug("Disabled feature: " + feature.getName());
            }
        });
    }

    public void disableAllExcept(IFeature ...sender) {
        features.forEach(feature -> {
            if (feature.isToggled() && feature.isRunning() && !Arrays.asList(sender).contains(feature)) {
                feature.stop();
                LogUtils.sendDebug("Disabled feature: " + feature.getName());
            }
        });
    }

    public void resetAllStates() {
        features.forEach(IFeature::resetStatesAfterMacroDisabled);
    }

    public boolean isAnyOtherFeatureEnabled(IFeature ...sender) {
        return features.stream().anyMatch(feature -> feature.shouldPauseMacroExecution() && feature.isRunning() && !Arrays.asList(sender).contains(feature));
    }

    public void enableAll() {
        features.forEach(feature -> {
            if (feature.shouldStartAtMacroStart() && feature.isToggled()) {
                feature.start();
                LogUtils.sendDebug("Enabled feature: " + feature.getName());
            }
        });
    }

    public void disableCurrentlyRunning(IFeature sender) {
        features.forEach(feature -> {
            if (feature.isRunning() && feature != sender) {
                feature.stop();
                LogUtils.sendDebug("Disabled feature: " + feature.getName());
            }
        });
    }
}
