package com.jelly.farmhelper.mixins.gui;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.gui.UpdateGUI;
import net.minecraft.client.gui.GuiMainMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiMainMenu.class)
public class MixinGuiMainMenu {
    @Shadow private String splashText;
    private static boolean done = false;

    @Final
    @Inject(method = "updateScreen", at = @At("RETURN"))
    private void initGui(CallbackInfo ci) {
        if (!done) {
            FarmHelper.ticktask = () -> {
                FarmHelper.ticktask = null;
                UpdateGUI.showGUI();
                done = true;
            };
        }

        if (UpdateGUI.outdated) {
            this.splashText = "Update FarmHelper <3";
        }
    }
}


