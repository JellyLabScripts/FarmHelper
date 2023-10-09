package com.github.may2beez.farmhelperv2.mixin.gui;

import com.github.may2beez.farmhelperv2.feature.impl.WebSocketConnector;
import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.util.StringUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(GuiDisconnected.class)
public class MixinBanScreen {
    @Shadow
    private List<String> multilineMessage;

    @Unique
    private boolean isBanned = false;

    @Inject(method = "drawScreen", at = @At("TAIL"))
    public void drawScreen(CallbackInfo ci) {
        if (multilineMessage.get(0).contains("banned") && !isBanned) {
            isBanned = true;
            try {
                String duration = StringUtils.stripControlCodes(multilineMessage.get(0)).replace("You are temporarily banned for ", "")
                        .replace(" from this server!", "").trim();
                String reason = StringUtils.stripControlCodes(multilineMessage.get(2)).replace("Reason: ", "").trim();
                int durationDays = Integer.parseInt(duration.split(" ")[0].replace("d", ""));
                String banId = StringUtils.stripControlCodes(multilineMessage.get(5)).replace("Ban ID: ", "").trim();
                System.out.println("Banned for " + durationDays + " days for " + reason + " with ban id " + banId);
                WebSocketConnector.getInstance().playerBanned(durationDays, reason, banId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
