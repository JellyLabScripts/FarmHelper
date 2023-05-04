package com.jelly.farmhelper.gui.menus;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.config.interfaces.FailsafeConfig;
import com.jelly.farmhelper.features.Autosell;
import com.jelly.farmhelper.features.Failsafe;
import com.jelly.farmhelper.gui.components.Button;
import com.jelly.farmhelper.gui.components.Slider;
import com.jelly.farmhelper.gui.components.Toggle;
import gg.essential.elementa.components.UIContainer;
import gg.essential.universal.UMatrixStack;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;

public class FailsafeMenu extends UIContainer {
    public FailsafeMenu() {
        new Toggle("Pop-up notifications", "notifications").setChildOf(this);
        new Toggle("Fake movements", "fakeMovements").setChildOf(this);
        new Toggle("Restart 3 min after failsafe", "restartAfterFailsafe").setChildOf(this);
        new Toggle("Ping sound",  "pingSound").setChildOf(this);
        new Toggle("Leave on banwave", "banwaveDisconnect").setChildOf(this);
        new Toggle("Check desync",  "checkDesync").setChildOf(this);
        new Toggle("Auto tp on world change", "autoTpOnWorldChange").setChildOf(this);
        new Toggle("Auto set spawn", "autoSetspawn").setChildOf(this);
        new Slider("Auto set spawn min delay (s)", 120, 1, "autoSetSpawnMinDelay").setChildOf(this);
        new Slider("Auto set spawn max delay (s)", 120, 1, "autoSetSpawnMaxDelay").setChildOf(this);
        new Slider("Rotation check sensitivity (deg)", 10, 1, "rotationSens").setChildOf(this);
        new Slider("Ban threshold (15 mins)", 40, 1, "banThreshold").setChildOf(this);
        new Slider("Delay before reconnect (s)", 20, 1, "reconnectDelay").setChildOf(this);
        ((Button) new Button("Test Failsafe").setChildOf(this)).setOnClick((component, uiClickEvent) -> {
            Failsafe.emergencyFailsafe(Failsafe.FailsafeType.TEST);
            Minecraft.getMinecraft().currentScreen = null;
            return null;
        });
    }

    private boolean oldNotifications = FailsafeConfig.notifications;

    @Override
    public void draw(@NotNull UMatrixStack matrixStack) {
        super.draw(matrixStack);
        if (oldNotifications != FailsafeConfig.notifications) {
            oldNotifications = FailsafeConfig.notifications;
            if (FailsafeConfig.notifications) {
                FarmHelper.registerInitNotification();
            }
        }
    }
}
