package com.jelly.farmhelper.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketDisconnectHandler implements IMessageHandler<PacketDisconnect, IMessage> {

    @Override
    public IMessage onMessage(PacketDisconnect message, MessageContext messageContext) {
        if (messageContext.side.isClient()) {
            String disconnectMessage = message.getMessage();
            Minecraft.getMinecraft().displayGuiScreen(new GuiDisconnected(
                    new GuiMainMenu(), "Disconnected", new ChatComponentText(disconnectMessage)));
        }
        return null;
    }
}
