package com.jelly.farmhelperv2.event;

import lombok.Getter;
import net.minecraftforge.fml.common.eventhandler.Event;

@Getter
public class InventoryInputEvent extends Event {
    private final int keyCode;
    private final char typedChar;

    public InventoryInputEvent(int keyCode, char typedChar) {
        this.keyCode = keyCode;
        this.typedChar = typedChar;
    }
}
