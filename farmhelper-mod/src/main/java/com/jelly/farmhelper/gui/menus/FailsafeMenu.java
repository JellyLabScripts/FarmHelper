package com.jelly.farmhelper.gui.menus;

import com.jelly.farmhelper.features.Autosell;
import com.jelly.farmhelper.gui.components.Button;
import com.jelly.farmhelper.gui.components.Slider;
import com.jelly.farmhelper.gui.components.Toggle;
import gg.essential.elementa.components.UIContainer;

public class FailsafeMenu extends UIContainer {
    public FailsafeMenu() {
        new Toggle("Pop-up notifications", "notifications").setChildOf(this);
        new Toggle("Fake movements", "fakeMovements").setChildOf(this);
        new Toggle("Ping sound",  "pingSound").setChildOf(this);
        new Toggle("Leave on banwave", "banwaveDisconnect").setChildOf(this);
        new Slider("Ban threshold (15 mins)", 40, 1, "banThreshold").setChildOf(this);
        new Slider("Delay before reconnect (s)", 20, 1, "reconnectDelay").setChildOf(this);
    }
}
