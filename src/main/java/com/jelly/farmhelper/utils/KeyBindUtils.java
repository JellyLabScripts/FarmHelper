package com.jelly.farmhelper.utils;


import com.jelly.farmhelper.config.FarmHelperConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class KeyBindUtils
{
    private static Minecraft mc;
    private static Method clickMouse;
    private static Method rightClickMouse;
    public static KeyBinding[] customKeyBinds = new KeyBinding[2];

    public static void setup() {
        customKeyBinds[0] = new KeyBinding("Open GUI", ((Long) FarmHelperConfig.get("openGUIKeybind")).intValue(), "FarmHelper");
        customKeyBinds[1] = new KeyBinding("Toggle script", ((Long) FarmHelperConfig.get("startScriptKeybind")).intValue(), "FarmHelper");
        for (int i = 0; i < customKeyBinds.length; ++i) {
            ClientRegistry.registerKeyBinding(customKeyBinds[i]);
        }

        try {
            KeyBindUtils.clickMouse = Minecraft.class.getDeclaredMethod("clickMouse", (Class<?>[])new Class[0]);
        }
        catch (NoSuchMethodException e) {
            try {
                KeyBindUtils.clickMouse = Minecraft.class.getDeclaredMethod("clickMouse", (Class<?>[])new Class[0]);
            }
            catch (NoSuchMethodException ex) {
                ex.printStackTrace();
            }
        }
        try {
            KeyBindUtils.rightClickMouse = Minecraft.class.getDeclaredMethod("rightClickMouse", (Class<?>[])new Class[0]);
        }
        catch (NoSuchMethodException e) {
            try {
                KeyBindUtils.rightClickMouse = Minecraft.class.getDeclaredMethod("rightClickMouse", (Class<?>[])new Class[0]);
            }
            catch (NoSuchMethodException e2) {
                e.printStackTrace();
            }
        }
        if (KeyBindUtils.clickMouse != null) {
            KeyBindUtils.clickMouse.setAccessible(true);
        }
        if (KeyBindUtils.rightClickMouse != null) {
            KeyBindUtils.rightClickMouse.setAccessible(true);
        }
    }

    public static void leftClick() {
        try {
            KeyBindUtils.clickMouse.invoke(Minecraft.getMinecraft());
        }
        catch (InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public static void rightClick() {
        try {
            KeyBindUtils.rightClickMouse.invoke(Minecraft.getMinecraft());
        }
        catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public static void updateKeys(boolean forward, boolean back, boolean right, boolean left, boolean attack) {
        updateKeys(forward, back, right, left, attack, false, false);
    }

    public static void updateKeys(boolean forward, boolean back, boolean right, boolean left, boolean attack, boolean crouch, boolean space) {
        if (mc.currentScreen != null) {
            stopMovement();
            return;
        }
        KeyBinding.setKeyBindState(KeyBindUtils.mc.gameSettings.keyBindForward.getKeyCode(), forward);
        KeyBinding.setKeyBindState(KeyBindUtils.mc.gameSettings.keyBindBack.getKeyCode(), back);
        KeyBinding.setKeyBindState(KeyBindUtils.mc.gameSettings.keyBindRight.getKeyCode(), right);
        KeyBinding.setKeyBindState(KeyBindUtils.mc.gameSettings.keyBindLeft.getKeyCode(), left);
        KeyBinding.setKeyBindState(KeyBindUtils.mc.gameSettings.keyBindAttack.getKeyCode(), attack);
        KeyBinding.setKeyBindState(KeyBindUtils.mc.gameSettings.keyBindSneak.getKeyCode(), crouch);
        KeyBinding.setKeyBindState(KeyBindUtils.mc.gameSettings.keyBindJump.getKeyCode(), space);

    }

    public static void stopMovement() {
        KeyBinding.setKeyBindState(KeyBindUtils.mc.gameSettings.keyBindForward.getKeyCode(), false);
        KeyBinding.setKeyBindState(KeyBindUtils.mc.gameSettings.keyBindBack.getKeyCode(), false);
        KeyBinding.setKeyBindState(KeyBindUtils.mc.gameSettings.keyBindRight.getKeyCode(), false);
        KeyBinding.setKeyBindState(KeyBindUtils.mc.gameSettings.keyBindLeft.getKeyCode(), false);
        KeyBinding.setKeyBindState(KeyBindUtils.mc.gameSettings.keyBindAttack.getKeyCode(), false);
        KeyBinding.setKeyBindState(KeyBindUtils.mc.gameSettings.keyBindSneak.getKeyCode(), false);
        KeyBinding.setKeyBindState(KeyBindUtils.mc.gameSettings.keyBindJump.getKeyCode(), false);
    }

    static {
        KeyBindUtils.mc = Minecraft.getMinecraft();
    }
}