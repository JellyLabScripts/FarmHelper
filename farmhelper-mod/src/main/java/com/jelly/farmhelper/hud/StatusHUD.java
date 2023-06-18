package com.jelly.farmhelper.hud;

import com.jelly.farmhelper.FarmHelper;
import cc.polyfrost.oneconfig.config.core.OneColor;
import cc.polyfrost.oneconfig.hud.TextHud;
import com.jelly.farmhelper.features.BanwaveChecker;
import com.jelly.farmhelper.utils.StatusUtils;

import java.util.List;

public class StatusHUD extends TextHud {
    public StatusHUD() {
        super(true, -1, -1, 1.0f, true, true, 5, 8, 8, new OneColor(0, 0, 0, 150), false, 2, new OneColor(0, 0, 0, 127));
    }

    @Override
    protected void getLines(List<String> lines, boolean example) {
        if (example) {
            lines.add("§fIdling");
            lines.add("§fFarming for 25m 35s");
            lines.add("§fStaff ban: NaN");
            lines.add("§fAutoCookie Fails: 0");
            lines.add("§fAutoPot Fails: 0");
            lines.add("§fWebSocket: §aConnected");
        } else {

            lines.add("§f" + StatusUtils.status);
            if (!BanwaveChecker.staffBan.isEmpty() && FarmHelper.config.banwaveCheckerEnabled)
                lines.add("§f" + BanwaveChecker.staffBan);

            if (FarmHelper.config.autoCookie)
                lines.add("§f" + StatusUtils.cookieFail);

            if (FarmHelper.config.autoGodPot)
                lines.add("§f" + StatusUtils.potFail);

            if (FarmHelper.config.enableRemoteControl)
                lines.add("§f" + StatusUtils.connecting);
        }
    }
}
