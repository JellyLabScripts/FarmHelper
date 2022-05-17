package com.jelly.farmhelper.features;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.macros.MacroHandler;
import com.jelly.farmhelper.utils.Clock;
import com.jelly.farmhelper.utils.LogUtils;
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
        if (MacroHandler.currentMacro == null || !MacroHandler.currentMacro.enabled || event.phase == TickEvent.Phase.END || mc.thePlayer == null || mc.theWorld == null) return;
        if (cooldown.passed()) {
            stuck = Math.abs(mc.thePlayer.posX - lastX) < 1 && Math.abs(mc.thePlayer.posZ - lastZ) < 1;
            if (stuck) {
                LogUtils.webhookLog("Stuck, trying to fix");
                LogUtils.debugLog("Stuck, trying to fix");
            }
            lastX = mc.thePlayer.posX;
            lastZ = mc.thePlayer.posZ;
            cooldown.schedule(3000);
        }
    }
}
