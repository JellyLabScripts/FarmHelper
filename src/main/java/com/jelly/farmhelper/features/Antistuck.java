package com.jelly.farmhelper.features;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.macros.MacroHandler;
import com.jelly.farmhelper.utils.Clock;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class Antistuck {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public static boolean stuck;
    private static double lastX;
    private static double lastZ;
    public static final Clock cooldown = new Clock();

    @SubscribeEvent
    public final void tick(TickEvent.ClientTickEvent event) {
        if (!MacroHandler.on || event.phase == TickEvent.Phase.END || mc.thePlayer == null || mc.theWorld == null) return;
        if (cooldown.passed()) {
            stuck = Math.abs(mc.thePlayer.posX - lastX) < 1 && Math.abs(mc.thePlayer.posZ - lastZ) < 1;
            lastX = mc.thePlayer.posX;
            lastZ = mc.thePlayer.posZ;
            cooldown.schedule(3000);
        }
    }
}
