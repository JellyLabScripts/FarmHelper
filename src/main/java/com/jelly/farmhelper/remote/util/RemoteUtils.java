package com.jelly.farmhelper.remote.util;

import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Predicate;

public class RemoteUtils {
    public static <T> Optional<T> getCommand(ArrayList<T> commands, Predicate<T> predicate) {
        return commands.stream().filter(predicate).findFirst();
    }
}
