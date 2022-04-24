package com.jelly.FarmHelper.gui.menus;

import com.jelly.FarmHelper.FarmHelper;
import com.jelly.FarmHelper.gui.components.Button;
import com.jelly.FarmHelper.gui.components.Toggle;
import com.jelly.FarmHelper.utils.LogUtils;
import com.jelly.FarmHelper.utils.Utils;
import gg.essential.elementa.components.UIContainer;
import net.minecraft.client.Minecraft;

import java.util.concurrent.TimeUnit;

public class MiscMenu extends UIContainer {
    private Minecraft mc = Minecraft.getMinecraft();
    public MiscMenu() {
        new Toggle("Resync", "resync").setChildOf(this);
        new Toggle("Auto GodPot", "autoGodPot").setChildOf(this);
        new Toggle("Auto Cookie", "autoCookie").setChildOf(this);
        new Toggle("Drop Stone", "dropStone").setChildOf(this);
        new Toggle("Debug Mode", "debugMode").setChildOf(this);
        ((Button) new Button("Buy Cookie").setChildOf(this)).setOnClick((component, uiClickEvent) -> {
            FarmHelper.openedGUI = false;
            mc.thePlayer.closeScreen();
            Utils.ExecuteRunnable(FarmHelper.checkFooter);
            Utils.ScheduleRunnable(FarmHelper.buyCookie, 1, TimeUnit.SECONDS);
            return null;
        });
        ((Button) new Button("Buy God Potion").setChildOf(this)).setOnClick((component, uiClickEvent) -> {
            FarmHelper.openedGUI = false;
            mc.thePlayer.closeScreen();
            Utils.ExecuteRunnable(FarmHelper.checkFooter);
            Utils.ScheduleRunnable(FarmHelper.buyGodPot, 1, TimeUnit.SECONDS);
            return null;
        });
    }
}
