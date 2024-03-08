package com.jelly.farmhelperv2.mixin.gui;

import com.jelly.farmhelperv2.FarmHelper;
import com.jelly.farmhelperv2.config.FarmHelperConfig;
import com.jelly.farmhelperv2.gui.AutoUpdaterGUI;
import com.jelly.farmhelperv2.gui.WelcomeGUI;
import net.minecraft.client.gui.GuiMainMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiMainMenu.class)
public class MixinGuiMainMenu {
    @Shadow
    private String splashText;

    @Final
    @Inject(method = "updateScreen", at = @At("RETURN"))
    private void initGui(CallbackInfo ci) {
        if (FarmHelper.isDebug) {
            this.splashText = "Fix Farm Helper <3";
            return;
        }
        if (!FarmHelperConfig.shownWelcomeGUI) {
            WelcomeGUI.showGUI();
        }
        if (!AutoUpdaterGUI.checkedForUpdates) {
            AutoUpdaterGUI.checkedForUpdates = true;
            AutoUpdaterGUI.getLatestVersion();
            if (AutoUpdaterGUI.isOutdated) {
                AutoUpdaterGUI.showGUI();
            }
        }
        if (AutoUpdaterGUI.isOutdated && !FarmHelperConfig.streamerMode)
            this.splashText = "Update Farm Helper <3";
    }
}