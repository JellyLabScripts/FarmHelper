package com.jelly.farmhelper.features;

import com.jelly.farmhelper.macros.MacroHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class AutoReconnect {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final ServerData hypixelServerData = new ServerData("bozo", "mc.hypixel.net", false);
    @SubscribeEvent(receiveCanceled = true)
    public void onOpenGUI(GuiOpenEvent event) {
        if ((event.gui instanceof GuiMultiplayer || event.gui instanceof GuiDisconnected) && MacroHandler.isMacroing && !BanwaveChecker.banwaveOn) {
            MacroHandler.disableCurrentMacro();
            FMLClientHandler.instance().connectToServer(mc.currentScreen instanceof GuiMultiplayer ? mc.currentScreen : new GuiMultiplayer(new GuiMainMenu()), hypixelServerData);
            event.setCanceled(true);
        }
    }

}
