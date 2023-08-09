package com.jelly.farmhelper.features;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.events.ReceivePacketEvent;
import com.jelly.farmhelper.utils.Clock;
import com.jelly.farmhelper.utils.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.client.C16PacketClientStatus;
import net.minecraft.network.play.server.S01PacketJoinGame;
import net.minecraft.network.play.server.S37PacketStatistics;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class Pinger {

    public static final Clock dontRotationCheck = new Clock();
    private static final Minecraft mc = Minecraft.getMinecraft();
    public static boolean pinging = false;
    public static boolean isOffline = false;
    private static long lastPing = 0;
    private static long lastTry = 0;

    public static boolean noConnection() {
        return System.currentTimeMillis() - lastPing > 1.5 * 1_000L + 1_000L;
    }

    @SubscribeEvent
    public void onTick2(TickEvent.ClientTickEvent event) {
        if (!FarmHelper.config.pingServer) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;

        if (dontRotationCheck.isScheduled() && dontRotationCheck.passed()) {
            dontRotationCheck.reset();
            LogUtils.debugLog("Checking for rotation packets again");
        }

        if (!isOffline && noConnection()) {
            isOffline = true;
            LogUtils.debugLog("Lag spike?");
        } else if (isOffline && !noConnection()) {
            isOffline = false;
            LogUtils.debugLog("Lag spike ended");
            dontRotationCheck.schedule(1_500);
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!FarmHelper.config.pingServer) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (mc.thePlayer.sendQueue == null || mc.thePlayer.sendQueue.getNetworkManager() == null) return;

        if (System.currentTimeMillis() - lastTry > 1.5 * 1_000L && !pinging) {
            try {
                pinging = true;
                mc.thePlayer.sendQueue.getNetworkManager().sendPacket(new C16PacketClientStatus(C16PacketClientStatus.EnumState.REQUEST_STATS), future -> {
                    if (future.isSuccess()) {
                        lastTry = System.currentTimeMillis();
                    }
                });
            } catch (Exception ex) {
                pinging = false;
            }
        }

        if (pinging && System.currentTimeMillis() - lastTry > 1.5 * 1_000L * 3) {
            pinging = false;
        }
    }

    @SubscribeEvent
    public void onPacketReceive(ReceivePacketEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (event.packet instanceof S01PacketJoinGame) {
            lastPing = System.currentTimeMillis();
            pinging = false;
        }
        if (event.packet instanceof S37PacketStatistics) {
            lastPing = System.currentTimeMillis();
            pinging = false;
        }
    }
}
