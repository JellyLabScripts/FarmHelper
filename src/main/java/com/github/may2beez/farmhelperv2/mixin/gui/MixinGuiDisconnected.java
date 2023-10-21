package com.github.may2beez.farmhelperv2.mixin.gui;

import cc.polyfrost.oneconfig.utils.Notifications;
import com.github.may2beez.farmhelperv2.config.FarmHelperConfig;
import com.github.may2beez.farmhelperv2.feature.impl.BanInfoWS;
import com.github.may2beez.farmhelperv2.feature.impl.AutoReconnect;
import com.github.may2beez.farmhelperv2.feature.impl.Failsafe;
import com.github.may2beez.farmhelperv2.handler.GameStateHandler;
import com.github.may2beez.farmhelperv2.handler.MacroHandler;
import com.github.may2beez.farmhelperv2.util.LogUtils;
import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.util.StringUtils;
import net.minecraftforge.fml.client.FMLClientHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(GuiDisconnected.class)
public class MixinGuiDisconnected {
    @Shadow
    private List<String> multilineMessage;

    @Unique
    private boolean farmHelperV2$isBanned = false;

    @Unique
    private List<String> farmHelperV2$multilineMessageCopy = new ArrayList<String>(2) {{
        add("");
        add("");
        add("");
    }};

    @Inject(method = "initGui", at = @At("RETURN"))
    public void initGui(CallbackInfo ci) {
        if (multilineMessage.get(0).contains("banned")) return;
        if (BanInfoWS.getInstance().isBanwave()) return;
        if (!MacroHandler.getInstance().isMacroToggled()) return;

        if (!AutoReconnect.getInstance().isRunning() && AutoReconnect.getInstance().isToggled()) {
            System.out.println("[Reconnect] Disconnected from server! Trying to reconnect...");
            Notifications.INSTANCE.send("Farm Helper", "Disconnected from server! Trying to reconnect...");
            AutoReconnect.getInstance().getReconnectDelay().schedule(5_000);
            AutoReconnect.getInstance().start();
        } else if (!AutoReconnect.getInstance().isRunning() && !AutoReconnect.getInstance().isToggled()) {
            System.out.println("[Reconnect] Disconnected from server! Stopping macro...");
            Notifications.INSTANCE.send("Farm Helper", "Disconnected from server! Stopping macro...");
            MacroHandler.getInstance().disableMacro();
        }
    }

    @Inject(method = "drawScreen", at = @At("TAIL"))
    public void drawScreen(CallbackInfo ci) {
        if (farmHelperV2$isBanned) return;

        if (multilineMessage.get(0).contains("banned")) {
            farmHelperV2$isBanned = true;
            try {
                String duration = StringUtils.stripControlCodes(multilineMessage.get(0)).replace("You are temporarily banned for ", "")
                        .replace(" from this server!", "").trim();
                String reason = StringUtils.stripControlCodes(multilineMessage.get(2)).replace("Reason: ", "").trim();
                int durationDays = Integer.parseInt(duration.split(" ")[0].replace("d", ""));
                String banId = StringUtils.stripControlCodes(multilineMessage.get(5)).replace("Ban ID: ", "").trim();
                BanInfoWS.getInstance().playerBanned(durationDays, reason, banId);
                LogUtils.webhookLog("Banned for " + durationDays + " days for " + reason, true);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }

        if (Failsafe.getInstance().getEmergency() == Failsafe.EmergencyType.BANWAVE && !FarmHelperConfig.banwaveAction) {
            if (BanInfoWS.getInstance().isBanwave()) {
                multilineMessage = farmHelperV2$multilineMessageCopy;
                multilineMessage.set(0, "Will reconnect after end of banwave!");
                multilineMessage.set(1, "Current bans: " + BanInfoWS.getInstance().getBans() + " (threshold: " + FarmHelperConfig.banwaveThreshold + ")");
            } else {
                if (!AutoReconnect.getInstance().isRunning()) {
                    AutoReconnect.getInstance().getReconnectDelay().schedule(FarmHelperConfig.delayBeforeReconnecting * 1_000L);
                    AutoReconnect.getInstance().start();
                    Failsafe.getInstance().stop();
                }
            }
        }

        if (Failsafe.getInstance().getEmergency() == Failsafe.EmergencyType.JACOB && !FarmHelperConfig.jacobFailsafeAction) {
            if (GameStateHandler.getInstance().inJacobContest() || (GameStateHandler.getInstance().getJacobContestLeftClock().isScheduled() && !GameStateHandler.getInstance().getJacobContestLeftClock().passed())) {
                multilineMessage = farmHelperV2$multilineMessageCopy;
                multilineMessage.set(0, "Will reconnect after end of Jacob's contest!");
                multilineMessage.set(1, "Time left: " + LogUtils.formatTime(GameStateHandler.getInstance().getJacobContestLeftClock().getRemainingTime()));
            } else {
                if (!AutoReconnect.getInstance().isRunning()) {
                    AutoReconnect.getInstance().getReconnectDelay().schedule(FarmHelperConfig.delayBeforeReconnecting * 1_000L);
                    AutoReconnect.getInstance().start();
                    Failsafe.getInstance().stop();
                }
            }
        }

        if (AutoReconnect.getInstance().isRunning() && AutoReconnect.getInstance().getState() == AutoReconnect.State.CONNECTING) {
            multilineMessage = farmHelperV2$multilineMessageCopy;
            multilineMessage.set(0, "Reconnecting in " + AutoReconnect.getInstance().getReconnectDelay().getRemainingTime() + "ms");
            multilineMessage.set(1, "Press ESC to cancel");
        }
    }
}
