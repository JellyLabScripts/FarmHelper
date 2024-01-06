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
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StringUtils;
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
            lines.add("Bans in the last 15 minutes");
            lines.add("detected by FarmHelper: 0");
            lines.set(0, centerText(lines.get(0), scale));
        } else {
            List<String> tempLines = new ArrayList<>();
            tempLines.add(getStatusString());

            if (PestsDestroyer.getInstance().getTotalPests() > 0) {
                tempLines.add(EnumChatFormatting.UNDERLINE + "Pests in Garden:" + EnumChatFormatting.RESET + " " + EnumChatFormatting.BOLD + EnumChatFormatting.RED + PestsDestroyer.getInstance().getTotalPests());
            }

            if (BanInfoWS.getInstance().isRunning() && FarmHelperConfig.banwaveCheckerEnabled && BanInfoWS.getInstance().isConnected()) {
                tempLines.add("Ban stats from the last " + BanInfoWS.getInstance().getMinutes() + " minutes");
                tempLines.add("Staff bans: " + BanInfoWS.getInstance().getStaffBans());
                tempLines.add("Detected by FarmHelper: " + BanInfoWS.getInstance().getBansByMod());
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
        float width = getWidth(scale, lines);
        float lineWidth = getLineWidth(text, scale);
        int spaces = (int) ((width - lineWidth) / (scale * 4));
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < spaces / 2; i++) {
            builder.append(" ");
        }
        return builder + text + builder;
    }

    protected float getWidth(float scale, List<String> lines) {
        if (lines == null) return 0;
        float width = 0;
        for (String line : lines) {
            width = Math.max(width, getLineWidth(StringUtils.stripControlCodes(line), scale));
        }
        return width;
    }

    public String getStatusString() {
        if (FailsafeManager.getInstance().triggeredFailsafe.isPresent()) {
            return "Emergency: §l§5" + LogUtils.capitalize(FailsafeManager.getInstance().triggeredFailsafe.get().getType().name()) + "§r";
        } else if (FailsafeManager.getInstance().getRestartMacroAfterFailsafeDelay().isScheduled()) {
            return "§l§6Restarting after failsafe in " + LogUtils.formatTime(FailsafeManager.getInstance().getRestartMacroAfterFailsafeDelay().getRemainingTime()) + "§r";
        } else if (!MacroHandler.getInstance().isMacroToggled()) {
            return (EnumChatFormatting.AQUA + "Idling");
        } else if (Scheduler.getInstance().isRunning()) {
            return Scheduler.getInstance().getStatusString();
        } else {
            return "Macroing";
        }
    }
}
