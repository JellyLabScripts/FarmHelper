package com.github.may2beez.farmhelperv2.feature;

import lombok.Getter;
import lombok.Setter;

@Getter
public class Failsafe {

    private static Failsafe instance;
    public static Failsafe getInstance() {
        if (instance == null) {
            instance = new Failsafe();
        }
        return instance;
    }

    @Setter
    private boolean emergency = false;

    public void resetFailSafe() {
        emergency = false;
    }
}
