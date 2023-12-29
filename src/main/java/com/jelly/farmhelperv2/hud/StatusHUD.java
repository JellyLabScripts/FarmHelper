package com.jelly.farmhelperv2.hud;

import cc.polyfrost.oneconfig.config.core.OneColor;
import cc.polyfrost.oneconfig.hud.TextHud;
import com.jelly.farmhelperv2.config.FarmHelperConfig;
import com.jelly.farmhelperv2.failsafe.FailsafeManager;
import com.jelly.farmhelperv2.feature.impl.BanInfoWS;
import com.jelly.farmhelperv2.feature.impl.LeaveTimer;
import com.jelly.farmhelperv2.feature.impl.PestsDestroyer;
import com.jelly.farmhelperv2.feature.impl.Scheduler;
import com.jelly.farmhelperv2.handler.MacroHandler;
import com.jelly.farmhelperv2.remote.DiscordBotHandler;
import com.jelly.farmhelperv2.util.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.Loader;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class StatusHUD extends TextHud {

    private final boolean jdaDependencyPresent = Loader.isModLoaded("farmhelperjdadependency");

    public StatusHUD() {
        super(true, Minecraft.getMinecraft().displayWidth - 100, Minecraft.getMinecraft().displayHeight - 100, 1, true, true, 4f, 5, 5, new OneColor(0, 0, 0, 150), false, 2, new OneColor(0, 0, 0, 127));
    }

    @Override
    protected void getLines(List<String> lines, boolean example) {
        if (example) {
            lines.add("Break for 25m 35s");
            lines.add("Staff bans in the last 15 minutes: 999");
            lines.add("FarmHelper's bans in the last 15 minutes: 0");
            lines.set(0, centerText(lines.get(0), scale));
        } else {
            List<String> tempLines = new ArrayList<>();
            tempLines.add(getStatusString());

            if (PestsDestroyer.getInstance().getTotalPests() > 0) {
                tempLines.add("Pests in Garden: " + PestsDestroyer.getInstance().getTotalPests());
            }

            if (BanInfoWS.getInstance().isRunning() && FarmHelperConfig.banwaveCheckerEnabled && BanInfoWS.getInstance().isConnected()) {
                tempLines.add("Staff bans in the last " + BanInfoWS.getInstance().getMinutes() + " minutes: " + BanInfoWS.getInstance().getStaffBans());
                tempLines.add("FarmHelper's bans in the last 15 minutes: " + BanInfoWS.getInstance().getBansByMod());
            } else if (!BanInfoWS.getInstance().isConnected() && FarmHelperConfig.banwaveCheckerEnabled) {
                tempLines.add("Connecting to the analytics server...");
            }
            if (LeaveTimer.getInstance().isRunning())
                tempLines.add("Leaving in " + LogUtils.formatTime(Math.max(LeaveTimer.leaveClock.getRemainingTime(), 0)));

            if (FarmHelperConfig.enableRemoteControl && jdaDependencyPresent) {
                if (!Objects.equals(DiscordBotHandler.getInstance().getConnectingState(), "")) {
                    tempLines.add("");
                    tempLines.add(DiscordBotHandler.getInstance().getConnectingState());
                }
            }

            for (String line : tempLines) {
                lines.add(centerText(line, scale, tempLines));
            }
        }
    }

    private String centerText(String text, float scale) {
        return centerText(text, scale, lines);
    }

    private String centerText(String text, float scale, List<String> lines) {
        if (lines == null || lines.isEmpty()) return text;
        float maxTextLength = getLineWidth(text, scale);
        float maxLongestLine = getWidth(scale, false, lines);
        int difference = (int) (((maxLongestLine - maxTextLength) / 3.5f) / (2 * scale)) - 1;
        return (difference > 0) ? new String(new char[difference]).replace("\0", " ") + text : text;
    }

    protected float getWidth(float scale, boolean example, List<String> lines) {
        if (lines == null) return 0;
        float width = 0;
        for (String line : lines) {
            width = Math.max(width, getLineWidth(line, scale));
        }
        return width;
    }

    public String getStatusString() {
        if (FailsafeManager.getInstance().triggeredFailsafe.isPresent()) {
            return "Emergency: §l§5" + LogUtils.capitalize(FailsafeManager.getInstance().triggeredFailsafe.get().getType().name()) + "§r";
        } else if (FailsafeManager.getInstance().getRestartMacroAfterFailsafeDelay().isScheduled()) {
            return "§l§6Restarting after failsafe in " + LogUtils.formatTime(FailsafeManager.getInstance().getRestartMacroAfterFailsafeDelay().getRemainingTime()) + "§r";
        } else if (!MacroHandler.getInstance().isMacroToggled()) {
            return "Idling";
        } else if (Scheduler.getInstance().isRunning()) {
            return Scheduler.getInstance().getStatusString();
        } else {
            return "Macroing";
        }
    }
}
