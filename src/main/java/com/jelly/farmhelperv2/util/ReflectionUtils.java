package com.jelly.farmhelperv2.util;

import net.minecraft.client.Minecraft;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Arrays;

public class ReflectionUtils {

    public static boolean invoke(Object object, String methodName) {
        try {
            final Method method = object.getClass().getDeclaredMethod(methodName);
            method.setAccessible(true);
            method.invoke(object);
            return true;
        } catch (Exception ignored) {
        }
        return false;
    }

    public static Object field(Object object, String name) {
        try {
            Field field = object.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(object);
        } catch (Exception ignored) {
        }
        return null;
    }

    public static boolean hasPackageInstalled(String name) {
        Package[] packages = Package.getPackages();

        for (Package pack : packages) {
            if (pack.getName().contains(name)) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasModFile(String name) {
        Path modsDir = Minecraft.getMinecraft().mcDataDir.toPath().resolve("mods");
        String[] modFiles = modsDir.toFile().list();
        return modFiles != null && Arrays.stream(modFiles).anyMatch(modFile -> modFile.toLowerCase().contains(name.toLowerCase()) && !modFile.toLowerCase().endsWith(".disabled"));
    }

    public static Object getNestedField(Object obj, String... fieldNames) throws Exception {
        Object currentObj = obj;
        for (String fieldName : fieldNames) {
            if (fieldName.endsWith("()")) {
                // It's a method call
                String methodName = fieldName.substring(0, fieldName.length() - 2);
                Method method = currentObj.getClass().getMethod(methodName);
                currentObj = method.invoke(currentObj);
            } else {
                // It's a field
                Field field = currentObj.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                currentObj = field.get(currentObj);
            }
            if (currentObj == null) {
                throw new NullPointerException("Field " + fieldName + " is null");
            }
        }
        return currentObj;
    }

    public static void setNestedField(Object obj, Object value, String... fieldNames) throws Exception {
        Object parentObj = getNestedField(obj, Arrays.copyOf(fieldNames, fieldNames.length - 1));
        String lastFieldName = fieldNames[fieldNames.length - 1];
        Field field = parentObj.getClass().getDeclaredField(lastFieldName);
        field.setAccessible(true);
        field.set(parentObj, value);
    }

    public static boolean getNestedBoolean(Object obj, String... fieldNames) throws Exception {
        Object result = getNestedField(obj, fieldNames);
        if (result instanceof Boolean) {
            return (Boolean) result;
        }
        throw new IllegalArgumentException("Field is not a boolean");
    }

    public static void setNestedBoolean(Object obj, boolean value, String... fieldNames) throws Exception {
        setNestedField(obj, value, fieldNames);
    }
}
