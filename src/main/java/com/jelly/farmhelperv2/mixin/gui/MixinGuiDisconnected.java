package com.jelly.farmhelperv2.mixin.gui;

import com.jelly.farmhelperv2.config.FarmHelperConfig;
import com.jelly.farmhelperv2.failsafe.FailsafeManager;
import com.jelly.farmhelperv2.feature.impl.AutoReconnect;
import com.jelly.farmhelperv2.feature.impl.BanInfoWS;
import com.jelly.farmhelperv2.feature.impl.Scheduler;
import com.jelly.farmhelperv2.feature.impl.Scheduler.SchedulerState;
import com.jelly.farmhelperv2.handler.GameStateHandler;
import com.jelly.farmhelperv2.handler.MacroHandler;
import com.jelly.farmhelperv2.util.LogUtils;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiDisconnected;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = GuiDisconnected.class, priority = Integer.MAX_VALUE)
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
        System.out.println("initGui");
        if (multilineMessage.get(0).contains("banned")) {
            FailsafeManager.getInstance().stopFailsafes();
        }
    }

    @Inject(method = "drawScreen", at = @At("RETURN"))
    public void drawScreen(CallbackInfo ci) {
        if (farmHelperV2$isBanned) return;

        if (multilineMessage.get(0).contains("banned")) {
            farmHelperV2$isBanned = true;
            return;
        }

        if (FailsafeManager.getInstance().triggeredFailsafe.isPresent() && FailsafeManager.getInstance().triggeredFailsafe.get().getType() == FailsafeManager.EmergencyType.BANWAVE && !FarmHelperConfig.banwaveAction) {
            if (BanInfoWS.getInstance().isBanwave()) {
                multilineMessage = farmHelperV2$multilineMessageCopy;
                multilineMessage.set(0, "Will reconnect after end of banwave!");
                multilineMessage.set(1, "Current bans: " + BanInfoWS.getInstance().getAllBans() + " (threshold: " + FarmHelperConfig.banwaveThreshold + ")");
            } else {
                if (!AutoReconnect.getInstance().isRunning()) {
                    AutoReconnect.getInstance().getReconnectDelay().schedule(FarmHelperConfig.delayBeforeReconnecting * 1_000L);
                    AutoReconnect.getInstance().start();
                }
            }
        }

        if (FailsafeManager.getInstance().triggeredFailsafe.isPresent() && FailsafeManager.getInstance().triggeredFailsafe.get().getType() == FailsafeManager.EmergencyType.JACOB && !FarmHelperConfig.jacobFailsafeAction) {
            if (GameStateHandler.getInstance().inJacobContest() || (GameStateHandler.getInstance().getJacobContestLeftClock().isScheduled() && !GameStateHandler.getInstance().getJacobContestLeftClock().passed())) {
                multilineMessage = farmHelperV2$multilineMessageCopy;
                multilineMessage.set(0, "Will reconnect after Jacob's contest ends.");
                multilineMessage.set(1, "Time left: " + LogUtils.formatTime(GameStateHandler.getInstance().getJacobContestLeftClock().getRemainingTime()));
            } else {
                if (!AutoReconnect.getInstance().isRunning()) {
                    AutoReconnect.getInstance().getReconnectDelay().schedule(FarmHelperConfig.delayBeforeReconnecting * 1_000L);
                    AutoReconnect.getInstance().start();
                }
            }
        }

        if(Scheduler.getInstance().isRunning() && Scheduler.getInstance().getSchedulerState() == SchedulerState.BREAK){
            multilineMessage = farmHelperV2$multilineMessageCopy;
            multilineMessage.set(0, Scheduler.getInstance().getStatusString());
            multilineMessage.set(1, "Press ESC to Disable Macro or press Toggle Macro button to restart instantly.");
        }

//        if (MacroHandler.getInstance().isMacroToggled() && !AutoReconnect.getInstance().isRunning() && AutoReconnect.getInstance().isToggled()) {
//            AutoReconnect.getInstance().getReconnectDelay().schedule(FarmHelperConfig.delayBeforeReconnecting * 1_000L);
//            AutoReconnect.getInstance().start();
//        }

        if (AutoReconnect.getInstance().isRunning() && AutoReconnect.getInstance().getState() == AutoReconnect.State.CONNECTING) {
            multilineMessage = farmHelperV2$multilineMessageCopy;
            multilineMessage.set(0, "Reconnecting in " + AutoReconnect.getInstance().getReconnectDelay().getRemainingTime() + "ms");
            multilineMessage.set(1, "Press ESC to cancel");
        }
    }

    @Inject(method = "actionPerformed", at = @At("HEAD"))
    protected void actionPerformed(GuiButton button, CallbackInfo ci) {
        if (button.id == 0) {
            if (AutoReconnect.getInstance().isRunning()) {
                AutoReconnect.getInstance().stop();
            }
            if (FailsafeManager.getInstance().triggeredFailsafe.isPresent() && FailsafeManager.getInstance().triggeredFailsafe.get().getType() == FailsafeManager.EmergencyType.BANWAVE && !FarmHelperConfig.banwaveAction) {
                FailsafeManager.getInstance().stopFailsafes();
                MacroHandler.getInstance().disableMacro();
            }
        }
    }
}
