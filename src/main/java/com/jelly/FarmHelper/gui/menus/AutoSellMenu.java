package com.jelly.FarmHelper.gui.menus;

import com.jelly.FarmHelper.gui.components.Button;
import com.jelly.FarmHelper.gui.components.Slider;
import com.jelly.FarmHelper.gui.components.Toggle;
import com.jelly.FarmHelper.utils.LogUtils;
import com.jelly.FarmHelper.utils.Utils;
import gg.essential.elementa.components.UIContainer;
import me.acattoXD.WartMacro;
import net.minecraft.client.Minecraft;

import java.util.concurrent.TimeUnit;

public class AutoSellMenu extends UIContainer {
    private Minecraft mc = Minecraft.getMinecraft();
    public AutoSellMenu() {
        new Toggle("Enabled", "autoSell").setChildOf(this);
        new Slider("Inventory Full Time", 20.0, 1.0, "fullTime").setChildOf(this);
        new Slider("Inventory Full Ratio", 100.0, 1.0, "fullRatio").setChildOf(this);
        ((Button) new Button("Sell Inventory").setChildOf(this)).setOnClick((component, uiClickEvent) -> {
            WartMacro.openedGUI = false;
            mc.thePlayer.closeScreen();
            WartMacro.cookie = true;
            Utils.ExecuteRunnable(WartMacro.checkFooter);
            WartMacro.timeoutStart = System.currentTimeMillis();
            if (WartMacro.cookie) LogUtils.debugLog("Executing sell in 1 second");
            Utils.ScheduleRunnable(WartMacro.autoSell, 1, TimeUnit.SECONDS);
            return null;
        });
    }
}
