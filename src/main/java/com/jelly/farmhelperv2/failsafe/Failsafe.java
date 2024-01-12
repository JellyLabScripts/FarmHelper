package com.jelly.farmhelperv2.failsafe;

import com.jelly.farmhelperv2.event.BlockChangeEvent;
import com.jelly.farmhelperv2.event.ReceivePacketEvent;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

public abstract class Failsafe {
    public final Minecraft mc = Minecraft.getMinecraft();

    public abstract int getPriority();

    public abstract FailsafeManager.EmergencyType getType();

    public abstract boolean shouldSendNotification();

    public abstract boolean shouldPlaySound();

    public abstract boolean shouldTagEveryone();

    public abstract boolean shouldAltTab();

    public void onBlockChange(BlockChangeEvent event) {
    }

    public void onReceivedPacketDetection(ReceivePacketEvent event) {
    }

    public void onTickDetection(TickEvent.ClientTickEvent event) {
    }

    public void onChatDetection(ClientChatReceivedEvent event) {
    }

    public void onWorldUnloadDetection(WorldEvent.Unload event) {
    }

    public void onDisconnectDetection(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
    }

    public abstract void duringFailsafeTrigger();

    public abstract void endOfFailsafeTrigger();

    private void possibleDetectionOfCheck() {
        FailsafeManager.getInstance().possibleDetection(this);
    }

    public void resetStates() {
    }
}