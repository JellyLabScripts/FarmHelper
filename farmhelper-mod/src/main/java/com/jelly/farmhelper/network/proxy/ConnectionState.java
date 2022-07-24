package com.jelly.farmhelper.network.proxy;

import net.minecraft.util.EnumChatFormatting;

public enum ConnectionState {
    CONNECTED(EnumChatFormatting.DARK_GREEN),
    INVALID(EnumChatFormatting.RED),
    CONNECTING(EnumChatFormatting.YELLOW),
    DISCONNECTED(EnumChatFormatting.DARK_RED);

    public final EnumChatFormatting color;

    ConnectionState(EnumChatFormatting color) {
        this.color = color;
    }
}
