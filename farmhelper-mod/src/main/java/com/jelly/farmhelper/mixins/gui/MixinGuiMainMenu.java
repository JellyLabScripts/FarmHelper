package com.jelly.farmhelper.mixins.gui;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.gui.UpdateGUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiMainMenu.class)
public class MixinGuiMainMenu {
    @Shadow private String splashText;

    @Final
    @Inject(method = "updateScreen", at = @At("RETURN"))
    private void initGui(CallbackInfo ci) {
        FarmHelper.ticktask = () -> {
            FarmHelper.ticktask = null;
            UpdateGUI.showGUI();
        };
        if (UpdateGUI.outdated) {
            this.splashText = "Update FarmHelper <3";
        }
    }

    @Final
    @Inject(method = "keyTyped", at = @At("RETURN"))
    private void keyTyped(char typedChar, int keyCode, CallbackInfo ci) {
        if (keyCode == Keyboard.KEY_X) {
            Minecraft.getMinecraft().displayGuiScreen(new UpdateGUI());
        }
    }
}
