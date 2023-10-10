package com.github.may2beez.farmhelperv2.util.helper;

import lombok.Getter;
import lombok.Setter;

@Getter
public class SignUtils {
    private static SignUtils instance;

    public static SignUtils getInstance() {
        if (instance == null) {
            instance = new SignUtils();
        }
        return instance;
    }

    @Setter
    private String textToWriteOnString = "";
}
