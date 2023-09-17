package com.jelly.farmhelper.remote.util;

import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.scanners.SubTypesScanner;

import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

public class RemoteUtils {
    public static <T> ArrayList<T> registerCommands(String packageName, Class<T> baseClass) {
        Reflections reflections = new Reflections(packageName, Scanners.SubTypes);
        Set<Class<? extends T>> classes = reflections.getSubTypesOf(baseClass);
        System.out.println("Found " + classes.size() + " classes in package " + packageName);
        ArrayList<T> commands = new ArrayList<>();
        for (Class<? extends T> clazz : classes) {
            try {
                if (!baseClass.isAssignableFrom(clazz)) {
                    continue;
                }
                T command = clazz.asSubclass(baseClass).getDeclaredConstructor().newInstance();
                commands.add(command);
                System.out.println("Registered command " + clazz.getName());
            } catch (Exception e) {
                System.out.println("Failed to register command " + clazz.getName());
                e.printStackTrace();
            }
        }
        return commands;
    }

    public static <T> Optional<T> getCommand(ArrayList<T> commands, Predicate<T> predicate) {
        return commands.stream().filter(predicate).findFirst();
    }
}
