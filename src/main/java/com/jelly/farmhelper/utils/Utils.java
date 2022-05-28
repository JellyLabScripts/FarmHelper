package com.jelly.farmhelper.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.util.*;
import java.util.Random;

public class Utils {

    public static String formatNumber(int number) {
        String s = Integer.toString(number);
        return String.format("%,d", number);
    }

    public static String formatNumber(float number) {
        String s = Integer.toString(Math.round(number));
        return String.format("%,d", Math.round(number));
    }

    public static int nextInt(int upperbound) {
        Random r = new Random();
        return r.nextInt(upperbound);
    }
}