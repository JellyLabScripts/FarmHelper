package com.jelly.farmhelper.mixins.gui;

import com.jelly.farmhelper.config.interfaces.MiscConfig;
import com.jelly.farmhelper.features.AutoReconnect;
import com.jelly.farmhelper.features.BanwaveChecker;
import com.jelly.farmhelper.features.Failsafe;
import com.jelly.farmhelper.macros.MacroHandler;
import com.jelly.farmhelper.remote.command.commands.ReconnectCommand;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiDisconnected;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

import static com.jelly.farmhelper.utils.Utils.formatTime;

@Mixin({ GuiDisconnected.class })
public class MixinGuiDisconnected {
    @Shadow private String reason;

    @Shadow private List<String> multilineMessage;

    @Inject(method={"drawScreen"}, at={@At(value="TAIL")})
    public void drawScreen(CallbackInfo ci) {
        if (ReconnectCommand.reconnectClock.isScheduled() || !ReconnectCommand.reconnectClock.passed()) {
            multilineMessage.set(0, "Time till reconnect: " + formatTime(ReconnectCommand.reconnectClock.getRemainingTime()));
            multilineMessage = multilineMessage.subList(0, 1);
        }

        if(MacroHandler.isMacroing) {
            if (BanwaveChecker.banwaveOn && MiscConfig.banwaveDisconnect) {
                Minecraft.getMinecraft().fontRendererObj.drawString("There is a banwave! " + BanwaveChecker.getBanDisplay(), 5, 5, -1);
            }
            if (!Failsafe.jacobWait.passed()) {
                Minecraft.getMinecraft().fontRendererObj.drawString("In Jacob Failsafe", 5, 20, -1);
            }
            if (AutoReconnect.waitTime > 0) {
                multilineMessage.set(0, "Seconds till reconnect: " + Math.floor(((MiscConfig.reconnectDelay * 20) - AutoReconnect.waitTime) / 20));
                multilineMessage = multilineMessage.subList(0, 1);
            }
        }


    }
}
