package com.jelly.farmhelper.features;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.events.ReceivePacketEvent;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class LagDetection {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static long lastPacket = 0;
    public static boolean lagging = false;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPacketReceive(ReceivePacketEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        lastPacket = System.currentTimeMillis();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onTick(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (lastPacket == 0) return;
        lagging = lastPacket + (long) FarmHelper.config.lagDetectionSensitivity < System.currentTimeMillis();
    }
}