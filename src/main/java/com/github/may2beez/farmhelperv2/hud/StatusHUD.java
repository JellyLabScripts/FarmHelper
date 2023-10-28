package com.github.may2beez.farmhelperv2.hud;

import cc.polyfrost.oneconfig.config.core.OneColor;
import cc.polyfrost.oneconfig.hud.TextHud;
import com.github.may2beez.farmhelperv2.config.FarmHelperConfig;
import com.github.may2beez.farmhelperv2.feature.impl.BanInfoWS;
import com.github.may2beez.farmhelperv2.feature.impl.Failsafe;
import com.github.may2beez.farmhelperv2.feature.impl.LeaveTimer;
import com.github.may2beez.farmhelperv2.feature.impl.Scheduler;
import com.github.may2beez.farmhelperv2.handler.MacroHandler;
import com.github.may2beez.farmhelperv2.remote.DiscordBotHandler;
import com.github.may2beez.farmhelperv2.util.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.Loader;

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
            lines.add("FarmHelper's bans in last 15 minutes: 0");
            lines.set(0, centerText(lines.get(0), scale, true));
        } else {
            lines.add(getStatusString());

            if (BanInfoWS.getInstance().isRunning() && FarmHelperConfig.banwaveCheckerEnabled && BanInfoWS.getInstance().isConnected()) {
                lines.add("Staff bans in last " + BanInfoWS.getInstance().getMinutes() + " minutes: " + BanInfoWS.getInstance().getBans());
                lines.add("FarmHelper's bans in last 15 minutes: " + BanInfoWS.getInstance().getBansByMod());
            } else if (!BanInfoWS.getInstance().isConnected() && FarmHelperConfig.banwaveCheckerEnabled) {
                lines.add("Connecting to analytics server...");
            }
            if (LeaveTimer.getInstance().isRunning())
                lines.add("Leaving in " + LogUtils.formatTime(Math.max(LeaveTimer.leaveClock.getRemainingTime(), 0)));

            if (FarmHelperConfig.enableRemoteControl && Loader.isModLoaded("farmhelperjdadependency")) {
                lines.add("");
                lines.add(DiscordBotHandler.getInstance().getConnectingState());
            }

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
        if (Failsafe.getInstance().isEmergency()) {
            return "Emergency: §l§5" + LogUtils.capitalize(Failsafe.getInstance().getEmergency().name()) + "§r";
        } else if (Failsafe.getInstance().getRestartMacroAfterFailsafeDelay().isScheduled()) {
            return "§l§6Restarting after failsafe in " + LogUtils.formatTime(Failsafe.getInstance().getRestartMacroAfterFailsafeDelay().getRemainingTime()) + "§r";
        }
        else if (!MacroHandler.getInstance().isMacroToggled()) {
            return "Idling";
        } else if (Scheduler.getInstance().isRunning()) {
            return Scheduler.getInstance().getStatusString();
        } else {
            return "Macroing";
        }
    }
}
