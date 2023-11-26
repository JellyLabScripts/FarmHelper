package com.jelly.farmhelperv2.event;

import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.common.eventhandler.Event;

public class DrawScreenAfterEvent extends Event {
    public GuiScreen guiScreen;

    public DrawScreenAfterEvent(GuiScreen guiScreen) {
        this.guiScreen = guiScreen;
    }
}
