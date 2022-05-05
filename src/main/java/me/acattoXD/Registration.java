package me.acattoXD;

import com.jelly.FarmHelper.gui.MenuGUI;
import me.acattoXD.Hiders.SessionIDWarning;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;

public class Registration {
    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new WartMacro());
        MinecraftForge.EVENT_BUS.register(new MenuGUI());
        MinecraftForge.EVENT_BUS.register(new UnStuck());
        MinecraftForge.EVENT_BUS.register(new CropUtils());
        MinecraftForge.EVENT_BUS.register(new SessionIDWarning());
    }
}
