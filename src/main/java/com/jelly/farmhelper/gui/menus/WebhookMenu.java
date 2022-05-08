package com.jelly.farmhelper.gui.menus;

import com.jelly.farmhelper.gui.components.Slider;
import com.jelly.farmhelper.gui.components.TextBox;
import com.jelly.farmhelper.gui.components.Toggle;
import gg.essential.elementa.components.UIContainer;

public class WebhookMenu extends UIContainer {
    public WebhookMenu() {
        new Toggle("Send Logs", "webhookLogs").setChildOf(this);
        new Toggle("Send Status Updates", "webhookStatus").setChildOf(this);
        new Slider("Status Cooldown (m)", 60, 1,"webhookStatusCooldown").setChildOf(this);
        new TextBox("Webhook URL", "Paste here", "webhookURL").setChildOf(this);
    }
}
