package com.jelly.farmhelperv2.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;

public class KeyBindUtils {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public static final KeyBinding[] allKeys = {
            mc.gameSettings.keyBindAttack,
            mc.gameSettings.keyBindUseItem,
            mc.gameSettings.keyBindBack,
            mc.gameSettings.keyBindForward,
            mc.gameSettings.keyBindLeft,
            mc.gameSettings.keyBindRight,
            mc.gameSettings.keyBindJump,
            mc.gameSettings.keyBindSneak,
            mc.gameSettings.keyBindSprint,
    };

    public static final KeyBinding[] allKeys2 = {
            mc.gameSettings.keyBindBack,
            mc.gameSettings.keyBindForward,
            mc.gameSettings.keyBindLeft,
            mc.gameSettings.keyBindRight,
            mc.gameSettings.keyBindJump,
    };

    public static void rightClick() {
        if (!ReflectionUtils.invoke(mc, "func_147121_ag")) {
            ReflectionUtils.invoke(mc, "rightClickMouse");
        }
    }

    public static void leftClick() {
        if (!ReflectionUtils.invoke(mc, "func_147116_af")) {
            ReflectionUtils.invoke(mc, "clickMouse");
        }
    }

    public static void middleClick() {
        if (!ReflectionUtils.invoke(mc, "func_147112_ai")) {
            ReflectionUtils.invoke(mc, "middleClickMouse");
        }
    }

    public static void onTick(KeyBinding key) {
        if (mc.currentScreen == null) {
            KeyBinding.onTick(key.getKeyCode());
        }
    }

    public static void setKeyBindState(KeyBinding key, boolean pressed) {
        if (pressed) {
            if (mc.currentScreen != null) {
                realSetKeyBindState(key, false);
                return;
            }
        }
        realSetKeyBindState(key, pressed);
    }

    private static void realSetKeyBindState(KeyBinding key, boolean pressed) {
        if (pressed) {
            if (!key.isKeyDown()) {
                KeyBinding.onTick(key.getKeyCode());
            }
            KeyBinding.setKeyBindState(key.getKeyCode(), true);

        } else {
            KeyBinding.setKeyBindState(key.getKeyCode(), false);
        }

    }

    public static void stopMovement() {
        stopMovement(false);
    }

    public static void stopMovement(boolean ignoreAttack) {
        realSetKeyBindState(mc.gameSettings.keyBindForward, false);
        realSetKeyBindState(mc.gameSettings.keyBindBack, false);
        realSetKeyBindState(mc.gameSettings.keyBindRight, false);
        realSetKeyBindState(mc.gameSettings.keyBindLeft, false);
        if (!ignoreAttack) {
            realSetKeyBindState(mc.gameSettings.keyBindAttack, false);
        }
        realSetKeyBindState(mc.gameSettings.keyBindUseItem, false);
        realSetKeyBindState(mc.gameSettings.keyBindSneak, false);
        realSetKeyBindState(mc.gameSettings.keyBindJump, false);
        realSetKeyBindState(mc.gameSettings.keyBindSprint, false);
    }

    public static void holdThese(KeyBinding... keyBinding) {
        releaseAllExcept(keyBinding);
        for (KeyBinding key : keyBinding) {
            if (key != null)
                realSetKeyBindState(key, true);
        }
    }

    public static void releaseAllExcept(KeyBinding... keyBinding) {
        for (KeyBinding key : allKeys) {
            if (key != null && !contains(keyBinding, key))
                realSetKeyBindState(key, false);
        }
    }

    public static boolean contains(KeyBinding[] keyBinding, KeyBinding key) {
        for (KeyBinding keyBind : keyBinding) {
            if (keyBind == key)
                return true;
        }
        return false;
    }

    public static boolean areAllKeybindsReleased() {
        for (KeyBinding key : allKeys2) {
            if (key != null && key.isKeyDown())
                return false;
        }
        return true;
    }

    public static KeyBinding[] getHoldingKeybinds() {
        KeyBinding[] keybinds = new KeyBinding[allKeys.length];
        int i = 0;
        for (KeyBinding key : allKeys) {
            if (key != null && key.isKeyDown()) {
                keybinds[i] = key;
                i++;
            }
        }
        return keybinds;
    }
}