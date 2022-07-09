package com.jelly.farmhelper.utils;


import com.jelly.farmhelper.config.FarmHelperConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.lwjgl.input.Keyboard;

public class KeyBindUtils
{
    private static Minecraft mc;
    public static KeyBinding[] customKeyBinds = new KeyBinding[3];

    static {
        KeyBindUtils.mc = Minecraft.getMinecraft();
    }

    public static void setup() {
        customKeyBinds[0] = new KeyBinding("Open GUI", ((Long) FarmHelperConfig.get("openGUIKeybind")).intValue(), "FarmHelper");
        customKeyBinds[1] = new KeyBinding("Toggle script", ((Long) FarmHelperConfig.get("startScriptKeybind")).intValue(), "FarmHelper");
        for (KeyBinding customKeyBind : customKeyBinds) {
            ClientRegistry.registerKeyBinding(customKeyBind);
        }
    }

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


}