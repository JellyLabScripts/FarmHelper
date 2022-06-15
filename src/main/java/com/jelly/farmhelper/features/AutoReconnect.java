package com.jelly.farmhelper.features;

import com.jelly.farmhelper.config.interfaces.JacobConfig;
import com.jelly.farmhelper.config.interfaces.MiscConfig;
import com.jelly.farmhelper.macros.MacroHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class AutoReconnect {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final ServerData hypixelServerData = new ServerData("bozo", "mc.hypixel.net", false);

    @SubscribeEvent
    public final void tick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.END || !MacroHandler.isMacroing)
            return;
        if(BanwaveChecker.banwaveOn && MiscConfig.banwaveDisconnect)
            return;
        if(!Failsafe.jacobWait.passed() && JacobConfig.jacobFailsafe)
            return;
        if((mc.currentScreen instanceof GuiDisconnected || mc.currentScreen instanceof GuiMultiplayer)){
            FMLClientHandler.instance().connectToServer(mc.currentScreen instanceof GuiMultiplayer ? mc.currentScreen : new GuiMultiplayer(new GuiMainMenu()), hypixelServerData);
        }
    }

}
