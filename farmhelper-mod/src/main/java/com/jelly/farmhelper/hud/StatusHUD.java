package com.jelly.farmhelper.hud;

import com.jelly.farmhelper.FarmHelper;
import cc.polyfrost.oneconfig.config.core.OneColor;
import cc.polyfrost.oneconfig.hud.TextHud;
import com.jelly.farmhelper.features.BanwaveChecker;
import com.jelly.farmhelper.utils.StatusUtils;

import java.util.List;

public class StatusHUD extends TextHud {
    public StatusHUD() {
        super(true, -7, -7, 0.8f, true, false, 10, 8, 8, new OneColor(0, 0, 0, 150), false, 2, new OneColor(0, 0, 0, 240));
    }

    @Override
    protected void getLines(List<String> lines, boolean example) {
        if (example) {
            lines.add("§f§lIdling");
            lines.add("§f§lFarming for 25m 35s");
            lines.add("§f§lStaff ban in Last 15 Minutes: 999");
            lines.add("§f§lAutoCookie Fails: 0");
            lines.add("§f§lAutoPot Fails: 0");
            lines.add("§f§lWebSocket: §a§lConnected");
        } else {

            lines.add("§f§l " + StatusUtils.status);
            if (!BanwaveChecker.staffBan.isEmpty())
                lines.add("§f§l " + BanwaveChecker.staffBan);

            if (FarmHelper.config.autoCookie)
                lines.add("§f§l " + StatusUtils.cookieFail);

            if (FarmHelper.config.autoGodPot)
                lines.add("§f§l " + StatusUtils.potFail);

            if (FarmHelper.config.enableRemoteControl)
                lines.add("§f§l " + StatusUtils.connecting);
        }
    }
}
