package com.github.may2beez.farmhelperv2.hud;

import cc.polyfrost.oneconfig.config.core.OneColor;
import cc.polyfrost.oneconfig.hud.TextHud;
import com.github.may2beez.farmhelperv2.config.FarmHelperConfig;
import com.github.may2beez.farmhelperv2.feature.impl.WebSocketConnector;
import com.github.may2beez.farmhelperv2.feature.impl.Scheduler;
import com.github.may2beez.farmhelperv2.handler.MacroHandler;
import net.minecraft.client.Minecraft;

import java.util.List;

public class StatusHUD extends TextHud {

    public StatusHUD() {
        super(true, Minecraft.getMinecraft().displayWidth - 100, Minecraft.getMinecraft().displayHeight - 100, 1, true, true, 4f, 5, 5, new OneColor(0, 0, 0, 150), false, 2, new OneColor(0, 0, 0, 127));
    }

    @Override
    protected void getLines(List<String> lines, boolean example) {
        if (example) {
            lines.add("Idling");
            lines.add("Break for 25m 35s");
            lines.add("Staff bans in last 15 minutes: 999");
            lines.add("FarmHelper's bans in last 15 minute: 0");
            lines.set(0, centerText(lines.get(0), scale, true));
        } else {
            lines.add(getStatusString());

            if (WebSocketConnector.getInstance().isRunning() && FarmHelperConfig.banwaveCheckerEnabled && WebSocketConnector.getInstance().isConnected()) {
                lines.add("Staff bans in last " + WebSocketConnector.getInstance().getMinutes() + " minutes: " + WebSocketConnector.getInstance().getBans());
                lines.add("FarmHelper's bans in last 15 minutes: " + WebSocketConnector.getInstance().getBansByMod());
            }

//            if (FarmHelperConfig.enableRemoteControl)
//                lines.add(RemoteControl.getInstance().getStatusString());

            lines.set(0, centerText(lines.get(0), scale, false));
        }
    }

    private String centerText(String text, float scale, boolean example) {
        float maxTextLength = getLineWidth(text, scale);
        float maxLongestLine = getWidth(scale, example);

        int difference = (int) (((maxLongestLine - maxTextLength) / 3.5f) / (2 * scale)) - 1;
        return (difference > 0) ? new String(new char[difference]).replace("\0", " ") + text : text;
    }

    public String getStatusString() {
        if (!MacroHandler.getInstance().isMacroToggled()) {
            return "Idling";
        }
//        else if (FailsafeNew.restartAfterFailsafeCooldown.isScheduled() && !FailsafeNew.restartAfterFailsafeCooldown.passed()) {
//            setStateString("Restarting in " + Utils.formatTime(FailsafeNew.restartAfterFailsafeCooldown.getEndTime() - System.currentTimeMillis()));
//        }
//        else if (FailsafeNew.cooldown.isScheduled() && !FailsafeNew.cooldown.passed()) {
//            setStateString("Waiting for " + Utils.formatTime(FailsafeNew.cooldown.getEndTime() - System.currentTimeMillis()));
//        }
//        else if (!FailsafeNew.emergency && !FailsafeNew.isJacobFailsafeExceeded) {
//            setStateString(Scheduler.getStatusString());
//        }
//        else if (FailsafeNew.isJacobFailsafeExceeded && FailsafeNew.cooldown.getEndTime() - System.currentTimeMillis() > 0) {
//            setStateString("Jacob failsafe for " + Utils.formatTime(FailsafeNew.cooldown.getEndTime() - System.currentTimeMillis()));
//        }
//        else if (FailsafeNew.emergency) {
//            setStateString("Emergency");
//        }
        else if (Scheduler.getInstance().isRunning()) {
            return Scheduler.getInstance().getStatusString();
        } else {
            return "Macroing";
        }
    }
}
